# Keywi-Penguin 🐧🫧

An experimental Keywi branch for touch-reactive GPU-rendered keyboard backdrops.

The idea: instead of a static image, GIF, or gradient behind the keyboard, render a fluid-like surface beneath semi-transparent keys. Taps and swipes both perform their normal keyboard actions **and** disturb the fluid underneath.

> touch → inject force / color → update simulation → render texture → translucent keyboard UI on top

---

## Phase 1 — Barebones Penguin

Goal: prove the interaction feels good before building an actual fluid simulator.

### Minimum viable experiment

- Add a GPU-rendered backdrop beneath the keyboard UI.
- Use a procedural GLSL shader that *looks* fluid/plasma-like rather than a full Navier–Stokes simulation.
- Feed touch coordinates into the shader.
- Let normal keyboard gestures continue functioning exactly as before.
- Make keys semi-transparent so the animated surface remains visible below them.
- A tap should create a local ripple / color disturbance.
- A swipe should leave a visible wake along the finger path.
- Stop rendering entirely when the keyboard is hidden.

### Suggested architecture

```text
BackdropEngine
├── StaticImage
├── Gradient
├── AnimatedGradient
├── ShaderBackdrop
└── FluidBackdrop
```

The keyboard should not need to care what kind of backdrop is active. It should only forward touch information:

```kotlin
backdrop.onTouch(x, y, pressure, action)
```

### Success criteria

Barebones Penguin is successful if:

1. typing remains responsive,
2. touch-reactive motion visibly follows taps and swipes,
3. the effect looks convincing beneath translucent keys,
4. battery/GPU usage is reasonable,
5. the renderer can be cleanly swapped for something more sophisticated later.

---

# Phase 2 — The Cursed-Beautiful Version 😈🌈🫧

Goal: turn the backdrop into a genuine real-time fluid playground rather than merely a fluid-looking shader.

## Core simulation

Use a low-resolution GPU fluid field and upscale the result behind the keyboard.

Candidate simulation textures:

```text
velocity
pressure
divergence
dye / color
```

Typical update pipeline:

```text
advect velocity
      ↓
add finger force
      ↓
calculate divergence
      ↓
solve pressure
      ↓
subtract pressure gradient
      ↓
advect dye
      ↓
render
```

A semi-Lagrangian 2D fluid solver with iterative pressure solving is a likely starting point.

The simulation does **not** need to match screen resolution. Something around `256×128` or `384×192` may be enough, then upscale smoothly to the keyboard surface.

## Touch becomes part of the fluid

Keyboard interaction should double as simulation input without stealing gestures from the IME.

Possible mappings:

- **tap** → pressure impulse + small dye bloom
- **swipe** → directional velocity injection + trailing wake
- **long press** → slowly growing vortex / bloom
- **multi-touch** → multiple simultaneous currents
- **fast swipe** → stronger impulse
- **slow drag** → thick ink-like trail
- **pressure**, where available → force / dye intensity

The keyboard still types normally. The fluid is simply listening to the same motion.

## Rendering / compositing

The visual stack should resemble:

```text
┌──────────────────────────────┐
│       fluid simulation       │
│   dye • velocity • bloom     │
│                              │
│   translucent key surfaces   │
│   labels / icons / gestures  │
└──────────────────────────────┘
```

Potential effects:

- bloom
- refraction
- chromatic dispersion
- viscosity controls
- dye diffusion
- vortices
- particles riding the flow field
- glow trails
- edge lighting
- glass / frosted key surfaces
- subtle distortion underneath pressed keys

## Performance strategy

A keyboard should not run a tiny GPU furnace every time somebody types `lol`.

Suggested adaptive render cadence:

```text
finger touching  → 60 fps
just released    → 60 → 30 fps while settling
mostly settled   → 15 fps
stationary       → freeze rendering
keyboard hidden  → stop simulation entirely
```

Other knobs:

- dynamically reduce simulation resolution on slower hardware,
- cap pressure iterations,
- pause expensive post-processing when idle,
- expose an FPS / quality selector,
- optionally disable animation in battery-saver mode.

## Presets we absolutely should abuse

- Oil Slick
- Cotton Candy Smoke
- Bioluminescent Ocean
- Lava Lamp
- Plasma
- Ink in Water
- Toxic Aurora
- Radioactive Chartreuse + Gold Mica + Blue UV Marble
- Cyberpunk Koi Pond
- Penguin Meltdown

## Stretch-goal nonsense

Once the backdrop system exists, Penguin does not have to stop at fluids.

Possible future `BackdropEngine` implementations:

- reaction-diffusion slime mold
- starfield
- particle sand
- cellular automata
- Conway's Game of Life
- metaballs
- gravity wells
- audio-reactive fields
- gyroscope-influenced liquid
- weather / rain effects
- user-authored fragment shaders

At that point **Keywi-Penguin** becomes less “Keywi with a fancy background” and more “the experimental GPU keyboard branch where typing disturbs reality.”

Which is, frankly, the correct amount of nonsense. 🐧✨
