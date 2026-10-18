# SleepLab Roadmap

Each phase ships as its own branch + PR, is tested on a physical device, and only then do we start
the next one. Tick the boxes as items land on `main`.

Legend: **DB** = needs a Room migration · **Test** = what to check on the phone before merging.

---

## Phase 1 — Foundations (`feat/foundations`) ✅ merged in #6

Goal: make data safe to keep, and make every night editable, before adding anything that changes the schema.

- [x] **Real Room migrations** — export schemas to `app/schemas/`, drop `fallbackToDestructiveMigration`
      for v2+, add `MIGRATION_2_3`, plus an instrumented migration test.
- [x] **Per-night detection settings** (DB) — store `thresholdDb` and `noiseFloorDb` on each session;
      Session detail draws the threshold that was actually used that night.
- [x] **Delete a night** — from Session detail, with confirmation; removes its events and samples.
- [x] **Night notes** (DB) — free-text note per night, editable from Session detail.
- [x] **Morning rating** (DB) — 1–5 "How did you sleep?" prompt on Home after a night, also editable
      in Session detail.
- [x] **Room calibration** (pulled forward from Phase 2) — measure room noise for the first 30 s of
      each night, set threshold = noise floor + margin; "Auto" toggle with manual override.

**Test:** upgrade over the current build keeps old nights · start a night → "Calibrating" for ~30 s,
then room noise + threshold shown · stop → Session detail shows that night's threshold · add note,
rate, delete.

## Phase 2 — Detection quality (`feat/detection`)

Goal: fewer false positives (talking, traffic, TV).

- [ ] **Frequency filter** — band-pass / FFT energy check focused on the low band where snoring sits
      (roughly 60–500 Hz) instead of broadband RMS only.
- [ ] **Breathing-rhythm check** — an event only counts as snoring when bursts repeat at a breathing
      cadence (about every 3–6 s); lone loud noises are ignored.
- [ ] **Adaptive floor** — slowly re-estimate the noise floor during the night (heating, fan turning on).
- [ ] **New snore score** — % of night snoring weighted by loudness above the floor (0–100), replacing
      `(avgDb + maxDb) / 2`. Keep the old value for existing nights.
- [ ] **Unit tests** for the detector using synthetic signals (tone, noise, rhythmic bursts).

**Test:** a night with TV/talking in the room before sleep produces far fewer events than before.

## Phase 3 — Audio clips (`feat/audio-clips`)

Goal: let users *hear* their snoring — the feature that makes this kind of app convincing.

- [ ] **Clip capture** (DB) — ring buffer keeps the last ~5 s; on a detected event save ~10 s
      (before + after) as AAC/M4A in app-private storage, linked to the event.
- [ ] **Playback** in Session detail (tap an event or a point on the timeline).
- [ ] **Privacy controls** — off by default, opt-in screen, auto-delete after N days, "Delete all clips",
      storage used shown in settings. Update the mic permission wording.
- [ ] **Limits** — max clips per night (keep the loudest), so storage stays bounded.

**Test:** clips play back, survive app restart, are deleted by auto-delete and by deleting the night.

## Phase 4 — Bedtime UX (`feat/bedtime`)

- [ ] **Start delay** — 0 / 10 / 15 / 30 min before detection starts (falling-asleep time).
- [ ] **Auto-stop** — stop at a chosen time or after a max duration (also fixes the 10 h wake-lock cap).
- [ ] **Bedtime reminder** — daily notification at a chosen time (exact alarm not required).
- [ ] **Charger check** — warn on start if not charging and battery is below ~50 %.
- [ ] **Quick Settings tile** to start/stop tracking; optional home-screen widget later.

**Test:** delayed start doesn't log events during the delay; auto-stop saves the night correctly.

## Phase 5 — Insights (`feat/insights`)

- [ ] **Factors** (DB) — tag a night with factors (alcohol, late meal, congestion, back sleeping,
      exhausted, custom). Picked at start or in the morning prompt.
- [ ] **Factor impact** — like treatment impact: avg snoring with vs. without each factor (needs ≥ 3
      nights each side before showing).
- [ ] **Trends** — week / month toggle on Analytics; calendar heatmap of nights.
- [ ] **Rating vs. snoring** — does a quieter night line up with a better morning rating?

## Phase 6 — Sharing (`feat/reports`)

- [ ] **PDF report** for a chosen period: nights, trend chart, treatment/factor comparison.
- [ ] **CSV export** of sessions and events.
- [ ] **Breathing-pause hints** (research spike first) — flag long silences between snores as
      "worth discussing with a doctor". Never presented as a diagnosis.

## Phase 7 — Quality & polish (ongoing, `chore/*`)

- [ ] Unit tests for `AnalyticsViewModel` math and `SettingsRepository.resolveTreatmentId`.
- [ ] Onboarding (3 screens: what it does, mic/privacy, phone placement).
- [ ] In-app language switch (EN/FR via `localeConfig`).
- [ ] Baseline profile / startup performance check; lint clean.
