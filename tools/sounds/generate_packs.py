#!/usr/bin/env python3
"""Synthesise the bundled alert sound packs.

Every pack sound is made here from sine and square waves, so the clips are
this project's own work under its BSD-2-Clause licence: no samples, no
third-party audio. Developer tool only; never shipped in the plugin jar.

    python3 tools/sounds/generate_packs.py

Writes src/main/resources/com/vinnyvangogh/combatvitals/sounds/*.wav as
22050 Hz mono 16-bit PCM WAV, which javax.sound (RuneLite's AudioPlayer)
plays everywhere. Deterministic: the same script gives byte-identical files.
"""

import math
import struct
import sys
import wave
from pathlib import Path

RATE = 22050
PEAK = 0.45  # calm by design: well under full scale before the player's volume
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/com/vinnyvangogh/combatvitals/sounds"


def note(freq, start, length, decay, partials=((1, 1.0),), wave_fn=math.sin):
    """One struck note: a 5 ms attack, then an exponential decay."""
    return (freq, start, length, decay, partials, wave_fn)


def square(x):
    """A soft square: odd harmonics up to the 5th, so it is not harsh."""
    return math.sin(x) + math.sin(3 * x) / 3 + math.sin(5 * x) / 5


def render(notes, seconds):
    total = int(RATE * seconds)
    buf = [0.0] * total
    for freq, start, length, decay, partials, wave_fn in notes:
        first = int(start * RATE)
        count = min(total - first, int(length * RATE))
        for i in range(count):
            t = i / RATE
            attack = min(1.0, t / 0.005)
            release = min(1.0, (count - i) / (0.01 * RATE))
            env = attack * release * math.exp(-t / decay)
            value = 0.0
            for multiple, amp in partials:
                value += amp * wave_fn(2 * math.pi * freq * multiple * t)
            buf[first + i] += env * value
    top = max(abs(v) for v in buf) or 1.0
    return [v / top * PEAK for v in buf]


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", int(v * 32767)) for v in samples))
    print(f"{path.relative_to(OUT.parents[6])}: {path.stat().st_size} bytes")


PACKS = {
    # Two soft bell tones a fourth apart, with a quiet octave partial.
    "soft-chime.wav": (
        [
            note(659.25, 0.00, 1.2, 0.35, ((1, 1.0), (2, 0.18))),
            note(880.00, 0.16, 1.1, 0.40, ((1, 1.0), (2, 0.15))),
        ],
        1.2,
    ),
    # A rising C major arpeggio, plucked like a music box comb.
    "music-box.wav": (
        [
            note(1046.50, 0.00, 0.9, 0.22, ((1, 1.0), (3, 0.08))),
            note(1318.51, 0.13, 0.9, 0.22, ((1, 1.0), (3, 0.08))),
            note(1567.98, 0.26, 0.9, 0.28, ((1, 1.0), (3, 0.08))),
        ],
        1.1,
    ),
    # Two short soft-square blips, an old handheld's "ding-ding".
    "retro-blip.wav": (
        [
            note(880.0, 0.00, 0.10, 0.08, wave_fn=square),
            note(1318.5, 0.12, 0.16, 0.10, wave_fn=square),
        ],
        0.35,
    ),
}


def main():
    for name, (notes, seconds) in PACKS.items():
        write(name, render(notes, seconds))
    return 0


if __name__ == "__main__":
    sys.exit(main())
