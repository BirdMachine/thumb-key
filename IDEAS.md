# Keywi Ideas / Parked Concepts

This file is for ideas worth keeping around without implying they are ready to implement yet.

## Customizable swipeable keyboard boards

**Status:** Parked for later — revisit after the current Keywi tasks wrap up.

### Core idea

Allow the square keyboard area to contain multiple horizontally pageable **boards**.

The existing `abc/#` keyboard becomes the first/default board rather than a special one-off layout. Swiping to the right pages through:

1. the current/default `abc/#` board;
2. any additional user-created boards.

Users can add, remove, reorder, and customize additional boards.

### Board model

Each board has two sides:

- **Side A**
- **Side B**

The default board demonstrates the model:

- Side A = `abc`
- Side B = `#` / symbols

Additional boards use the same square-key geometry as the default board. They do **not** need special input modes, hidden character logic, or secret character sets; they are simply more square boards whose key contents are user-configurable.

For each custom board, the user can choose the character/text assigned to each key on Side A and Side B.

### Interaction sketch

- Horizontal swipe in the square keyboard area moves between boards.
- The existing A/B-side mechanism remains local to the currently selected board.
- The default `abc/#` board remains available and behaves like it does now.
- Custom boards should feel like extra pages of the same keyboard, not separate keyboards or modes.

### Why this is interesting

This turns the existing square-key layout into a small user-programmable input surface without introducing a new input grammar. People could make boards for symbols, Unicode oddities, snippets, niche alphabets, game/chat shorthand, math/science characters, or whatever else fits their own habits.

### Later design questions

These are intentionally unanswered for now:

- Where does **Add board** live in the UI?
- How are boards named / identified visually?
- Should board order be drag-reorderable?
- What is the cleanest gesture boundary between board paging and per-key swipe gestures?
- Can a board key contain only one character, or arbitrary short text?
- Do boards participate in import/export / profile backup?
- Should users be able to duplicate a board as a starting point?
- How should A/B side state behave when paging between boards (remember per-board state vs. global side state)?

No implementation work is implied by this note yet.
