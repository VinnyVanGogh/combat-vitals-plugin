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
| `OverheadTextChanged` hold-on-arrival | **Used** since the Board's in-game test (2026-11-04). Deciding holds only on `ClientTick` from the last frame's copy let the game's line show for a frame or more before it was taken, which read as the message drawn twice. A new message over an actor the readout is drawn over is now blanked inside the event, the same hook Chat Filter uses, before any frame is drawn. `ClientTick` still takes a message that was already up when the readout appeared. |

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
(`onOverheadTextChanged`, `onClientTick` → `updateChatHolds`, `heldChat`,
`setChatCovered`, `shutDown`, `onGameStateChanged`) and
`CombatVitalsOverheadOverlay.render` / `drawHeldChat`.

1. Every frame the overlay draws the readout for the opponent, and for you
   your own bar and the skilling badge (PR #7, over your own head since PR #9),
   stacked. Every actor it drew a box over goes into a fresh set, talking or
   not, published to the plugin at the end of the frame, in a `finally` so a
   failing draw still publishes. For each, `drawHeldChat` draws the line the
   plugin holds, if any: above the box when the box covers the game's line,
   otherwise in the game's own place. Nothing held, nothing drawn: with the
   option off the game's line is the only one.
2. When the game posts a message (`OverheadTextChanged`) over an actor in the
   latest set, with the option on, the holder takes it at once
   (`OverheadChatHolder.hold`): it keeps the text and writes the blank inside
   the event, so no frame ever draws the game's copy. Our own blank posts back
   through the same event and is ignored.
3. Every client tick, with the option on (and *Show over target* on), the
   holder:
   - lets go of every held actor not in the latest set, writing the real text
     back;
   - forgets, without writing, any hold whose blank was replaced by a newer
     message from the game or whose countdown ran out;
   - takes the line off every actor in the set that is currently talking.
4. Option off, overlay off, logout/hop, or plugin shutdown: everything held is
   written back (shutdown via `ClientThread.invoke`).

Only two actors can ever be held: your tracked opponent, and you. Bystanders,
NPC dialogue on other NPCs, the skilling NPC you are working, and anyone this
plugin draws nothing over are never touched.

The set follows what the overlay draws. The readout follows `CombatState`
(PR #4): a target gets a readout only if it can be fought (`Attack` option,
combat level, or a hit you dealt it), and your own only while in combat. The
badge follows `SkillingActions` and `ObjectSkilling` (PR #7, moved over your
own head in PR #9, extended to skilling objects and clicked-option NPCs in
PR #12): only while a skilling NPC or object is being worked. So a banker or a
pet never gets a chat move or a hidden line, and your own chat is only ever
touched while your own bar or the badge is up.

### The skilling badge

The badge (PR #9) is drawn over your own head at the same anchor as your own
health bar, never over the NPC. It covers your own chat line the way the bar
does, so it lifts the chat by the same rule: one rule, "whatever this plugin
draws over an actor's head lifts that actor's chat above it and hides the
game's line underneath; nothing drawn, nothing touched". The alternative,
leaving the badge out, would put the badge over your own line unreadably,
which is the bug this work exists to fix. Decision: yes, the badge lifts
overhead chat the same way the readout does.

Overlap between the badge and a moved chat line cannot happen. When your bar
and the badge are both up (hit while fishing) the badge stacks `GAP` px above
the bar (`OverheadStack.liftAbove`), and the overlay lifts one copy of your
chat above the union of the two boxes (`OverheadStack.union`), so the copy
clears the topmost box and the message is never drawn twice
(`OverheadChatTest.badgeStackedOnTheBarLiftsChatClearOfBoth`). The badge and
the opponent readout are mutually exclusive (`buildSkillingReadout` returns
null while `CombatState.showTarget()`), and the opponent is a different actor
at a different screen position anyway, exactly like two readouts on
neighbouring actors.

### Never dropping a message

- A hold exists only while a box is drawn over the actor. Any frame where it
  is not (readout gone, actor off screen, target despawned or retired)
  releases it on the next tick.
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
- **Event order.** If another plugin rewrites the text in its own
  `OverheadTextChanged` handler after ours (Chat Filter censoring), its write
  replaces our blank and posts the event again, and we hold the censored text
  in turn. Unit tested with a fake that posts back; not yet seen in game.
- **A message already up when the readout appears** is taken on the next
  client tick, so it can show under the readout for one frame. Only new
  messages are held on arrival.
- Default **on** since 2026-11-04 (Board): it writes actor state that other
  plugins can read, the way Chat Filter does.

## Plugin Hub rules

Nearest rules: none of Jagex's third-party client guidelines or RuneLite's
rejected features cover relocating chat; it gives no information the player
did not already have (the same text, same duration, moved a few pixels up).
Precedent for writing `setOverheadText`: base-client Chat Filter (above). No
reflection, no new network use. Compliance otherwise unchanged from
`BRIEF.md` Phase 0.

## In-game check (`./gradlew run`)

1. Enable *Display > Chat above readout* (default on). Keep *Style:
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
10. Click a fishing spot or banker (no readout, per PR #4) and type a message.
    Expect: no readout, the game's own line drawn as normal over your head,
    nothing moved or hidden. Then attack an NPC: your readout appears and the
    next message you type moves above it.
11. With the skilling badge on, click a fishing spot (no readout) and type a
    message. Expect: the badge over your own head, one yellow line above the
    badge, nothing showing through it, no overlap between the two. The spot
    itself has nothing drawn over it. Turn *Skilling badge* off mid-message:
    the badge goes and the game's own line comes back over your head with its
    time left.
12. Badge and bar together: attack an NPC, then click a fishing spot before
    your bar times out (or fish where something can hit you). Expect: your bar
    at the anchor, the badge stacked above it, and a typed message as one line
    above the badge, never between the two and never twice.
