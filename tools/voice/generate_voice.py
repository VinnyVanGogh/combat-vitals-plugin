#!/usr/bin/env python3
"""Render the bundled voice-line clips from the word list.

Reads src/main/resources/com/vinnyvangogh/combatvitals/voice/lines.tsv (the
same list the plugin composes lines from) and writes one WAV per clip id
beside it: voice/<clip>.wav, 16 kHz mono 16-bit PCM, silence trimmed.

Developer tool only, run offline before a release; never shipped in the jar
and never run by the plugin. The plugin only plays the files this writes.

Voice: Kokoro-82M (https://huggingface.co/hexgrad/Kokoro-82M), Apache-2.0
weights and voices, run through kokoro-onnx (MIT,
https://github.com/thewh1teagle/kokoro-onnx). Voice "af_heart": a synthetic
voice, not a recording or imitation of any real person. See NOTICE.

Setup, once, in a scratch virtualenv (Python 3.10 to 3.12 for onnxruntime):

    python3.12 -m venv "$TMPDIR/kokoro" && . "$TMPDIR/kokoro/bin/activate"
    pip install kokoro-onnx==0.4.9 soundfile numpy
    curl -LO https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/kokoro-v1.0.int8.onnx
    curl -LO https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/voices-v1.0.bin

Then:

    python tools/voice/generate_voice.py --model kokoro-v1.0.int8.onnx --voices voices-v1.0.bin

Options: --voice af_heart, --speed 1.0, --only <clip id> to redo one clip.
"""

import argparse
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VOICE_DIR = ROOT / "src/main/resources/com/vinnyvangogh/combatvitals/voice"
LIST = VOICE_DIR / "lines.tsv"
# The plugin joins clips by hand and takes this one format only
# (AlertSounds.Wav.VOICE; AlertSoundsTest checks every bundled clip).
# Change both together, and re-render every clip.
RATE = 16000
SILENCE = 0.02  # below this (of full scale) counts as silence when trimming
PAD = 0.04  # seconds of silence kept after each clip, so joined parts breathe

# Openings are said as part of a sentence; the rest are ends of one. Spoken
# alone, "You've stopped" would fall in pitch like a full stop, so it is
# rendered with a trailing comma to keep the voice lifted.
LIFTED = {"stopped", "finished"}


def read_clips(path):
    """Clip id -> words, each id once, in list order."""
    clips = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 4:
            raise SystemExit(f"bad row in {path.name}: {line!r}")
        _kind, _key, clip, words = (c.strip() for c in cells)
        clips.setdefault(clip, words)
    return clips


def trim(samples, np):
    loud = np.flatnonzero(np.abs(samples) > SILENCE)
    if loud.size == 0:
        return samples
    start = max(0, loud[0] - int(0.01 * RATE))
    end = loud[-1] + 1
    return np.concatenate([samples[start:end], np.zeros(int(PAD * RATE), dtype=samples.dtype)])


def resample(samples, rate, np):
    """Linear resample to RATE after a short moving-average low-pass."""
    if rate == RATE:
        return samples
    width = max(1, round(rate / RATE))
    smoothed = np.convolve(samples, np.ones(width) / width, mode="same")
    positions = np.arange(0, len(smoothed), rate / RATE)
    return np.interp(positions, np.arange(len(smoothed)), smoothed)


def write(path, samples, np):
    peak = float(np.max(np.abs(samples))) or 1.0
    pcm = (samples / peak * 0.8 * 32767).astype("<i2")
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(pcm.tobytes())


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--model", required=True)
    parser.add_argument("--voices", required=True)
    parser.add_argument("--voice", default="af_heart")
    parser.add_argument("--speed", type=float, default=1.0)
    parser.add_argument("--only")
    args = parser.parse_args()

    import numpy as np
    from kokoro_onnx import Kokoro

    kokoro = Kokoro(args.model, args.voices)
    clips = read_clips(LIST)
    total = 0
    for clip, words in clips.items():
        if args.only and clip != args.only:
            continue
        text = words + "," if clip in LIFTED else words
        samples, rate = kokoro.create(text, voice=args.voice, speed=args.speed, lang="en-us")
        samples = trim(resample(np.asarray(samples, dtype=np.float64), rate, np), np)
        path = VOICE_DIR / f"{clip}.wav"
        write(path, samples, np)
        size = path.stat().st_size
        total += size
        print(f"{clip}: {words!r} {size} bytes")
    print(f"{len(clips)} clips, {total} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
