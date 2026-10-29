# Hiding the game's chat line under the readout

The overhead readout sits where the game draws an actor's overhead chat. Since
PR #2 the overlay draws a copy of the chat just above the readout, but the
game's own line still renders underneath and shows through. *Display > Hide
game chat under readout* takes the game's line away while our copy is up, so
only one line exists. Board request, 2026-10-22.

## The hook

The RuneLite API gives four calls on `Actor`
([`Actor.java`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Actor.java)):
`getOverheadText()`, `setOverheadText(String)`, `getOverheadCycle()` ("the
number of cycles/client ticks remaining before the overhead text is timed
out") and `setOverheadCycle(int)`. `OverheadTextChanged` fires when the game
posts a message.

Options considered:

| Route | Verdict |
| --- | --- |
| Swap the text for `" "`, leave the countdown | **Used.** Exactly what the base client's Chat Filter does to a filtered overhead message (below). The game keeps counting down, so the message lasts its normal time; restoring writes the text back and the game draws it for whatever time is left. The text stays non-null, so the client's 2D stacking (health bar 12 px higher while chat is up, see `native-hp-bar.md`) does not jump on hide or restore. |
| Zero the countdown and keep our own expiry | Rejected. We would have to reproduce the game's timing (unit of a "cycle", per-message durations) and restore by writing a guessed cycle count back. Any mismatch shortens or lengthens someone's message. |
| `Client.setDraw2DMask` without `DRAW_2D_OVERHEAD_TEXT` | Rejected. Global: hides every actor's chat, bystanders included. |
| `OverheadTextChanged` hold-on-arrival | Not needed. Holds are decided from the frame that actually drew the copy, on `ClientTick`; worst case the game's line shows for one frame (~20 ms) before it is taken. |

Precedent, base client
[`ChatFilterPlugin.onOverheadTextChanged`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/chatfilter/ChatFilterPlugin.java):

```java
String message = censorMessage(event.getActor().getName(), event.getOverheadText());
if (message == null)
{
	message = " ";
}
event.getActor().setOverheadText(message);
```

## How it works

Code: `OverheadChatHolder.java` (state, unit tested in
`OverheadChatHolderTest.java`), wired in `CombatVitalsPlugin`
(`onClientTick` → `updateChatHolds`, `overheadChat`, `setChatCovered`,
`shutDown`, `onGameStateChanged`) and `CombatVitalsOverheadOverlay.render` /
`drawChatAbove`.

1. Every frame the overlay draws the readout for the opponent and for you. For
   each, `drawChatAbove` asks the plugin for the actor's chat (the held text if
   we hold it, otherwise the game's live text) and draws the copy only if the
   readout covers the game's line. Actors that got a copy go into a fresh set,
   published to the plugin at the end of the frame, in a `finally` so a
   failing draw still publishes.
2. Every client tick, with the option on (and *Show over target* on), the
   holder:
   - lets go of every held actor not in the latest set, writing the real text
     back;
   - forgets, without writing, any hold whose blank was replaced by a newer
     message from the game or whose countdown ran out;
   - takes the line off every actor in the set that is currently talking.
3. Option off, overlay off, logout/hop, or plugin shutdown: everything held is
   written back (shutdown via `ClientThread.invoke`).

Only two actors can ever be held: your tracked opponent and you. Bystanders,
NPC dialogue on other NPCs and anyone the readout does not cover are never
touched.

### Never dropping a message

- A hold exists only while the copy is being drawn. Any frame where the copy
  is not drawn (readout moved clear, actor off screen, text location null,
  target despawned or retired) releases it on the next tick.
- Release writes the original text back with the countdown untouched, so the
  game draws it for exactly the time it had left.
- A new message from the game always wins: the stale hold is dropped, never
  written over the new text; if still covered, the new one is held in turn.
- Expired holds are not restored, matching what the game would show.

## Risks

- **Other plugins reading overhead text** see `" "` for a held actor while it
  is held. Chat Filter itself only acts on `OverheadTextChanged`; if writing
  the text fires that event, it would re-filter `" "` (a no-op) and, on
  restore, the already filtered text. Unverified in game whether
  `setOverheadText` fires the event.
- **Colour and effects.** The API does not expose chat colour or effects
  (wave, scroll, etc.), so our copy is plain yellow. With the game's line gone,
  a coloured or animated message now shows only in plain yellow while
  covered.
- **One frame** of the game's line at the start of each message (see above).
- Default **off** until checked in game: it writes actor state that other
  plugins can read.

## Plugin Hub rules

Nearest rules: none of Jagex's third-party client guidelines or RuneLite's
rejected features cover relocating chat; it gives no information the player
did not already have (the same text, same duration, moved a few pixels up).
Precedent for writing `setOverheadText`: base-client Chat Filter (above). No
reflection, no new network use. Compliance otherwise unchanged from
`BRIEF.md` Phase 0.

## In-game check (`./gradlew run`)

1. Enable *Display > Hide game chat under readout* (default off). Keep *Style:
   Bar behind text*, *Position: Top*, *Offset 0*.
2. Attack a training dummy or NPC so your own readout shows. Type a public chat
   message. Expect: one yellow line above your readout, nothing showing through
   the bar, lasting the usual ~3 s.
3. Fight an NPC that talks (or a player who types). Same: one line above their
   readout.
4. While a message is up, set *Overhead offset* to +60 so the readout clears
   the chat. Expect: the game's own line reappears immediately with its time
   left, and no copy.
5. Turn the option off mid-message. Expect: the game's line comes back
   underneath, our copy stays above (old behaviour).
6. Turn the plugin off mid-message. Expect: the game's line comes back.
7. Type two messages quickly. Expect: the second replaces the first and is
   shown once, never the first coming back afterwards.
8. With Chat Filter on and a filtered word typed by a player you are fighting:
   expect the filtered form, shown once.
9. A bystander's chat, not your opponent: unaffected.
