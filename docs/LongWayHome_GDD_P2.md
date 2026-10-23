# LONG WAY HOME — Project 2

**Game Design Document — Project 2 Addendum**

*GUI + database build of the Project 1 command-line game*

**COMP603 / ENSE600 — Software Development Project 2**
2026 Semester 2 · Two-person team (Adam, Nadia)

*Version 2.0. This document does not restate the game design — [LongWayHome_GDD.md](LongWayHome_GDD.md) remains the authority on rules, tuning and narrative. This covers only what changes for Project 2: the GUI, the database, the deferred features, and the plan to land them.*

---

## Contents

1. [What Project 2 Actually Requires](#1-what-project-2-actually-requires)
2. [Where Project 1 Left Off](#2-where-project-1-left-off)
3. [Architecture: The GUI Problem](#3-architecture-the-gui-problem)
4. [Architecture: The Database](#4-architecture-the-database)
5. [Gameplay Scope — The Deferred Features](#5-gameplay-scope--the-deferred-features)
6. [Design Patterns](#6-design-patterns)
7. [Testing](#7-testing)
8. [Version Control Strategy](#8-version-control-strategy)
9. [Art Pipeline](#9-art-pipeline)
10. [Roadmap](#10-roadmap)
11. [Work Split](#11-work-split)
12. [Risks](#12-risks)
13. [Definition of Done](#13-definition-of-done)

---

## 1. What Project 2 Actually Requires

From the assessment brief and the marking guideline. Every row below is a graded line item, not a nice-to-have.

| Requirement | Bar for "Excellent" (80–100%) | Weight |
|---|---|---|
| **GUI** | Users figure it out easily, friendly messages on bad input, components organised, can quit at any step, can return to menu / restart at any time | 20% (shared) |
| **Database** | Derby **Embedded**, connects with no service started, **more than three read interactions AND more than three write interactions**, connection details in one place | 20% (shared) |
| **Functionality & usability** | Opens in NetBeans, compiles and runs with zero manual configuration (no jar imports, no DB setup), handles any input without exceptions | 20% |
| **Design & implementation** | 12+ reasonable classes, all four OOP concepts, clear UI/model separation, design patterns applied, no obvious code smells, **version control showing multiple commits and branches merging**, ≥5 well-named JUnit tests covering important functionality | 25% |
| **AI usage documentation** | Appendix A interaction log with real prompts + Appendix B one-page reflection in our own words, consistent with in-code AI comments | 5% |
| **Labs** | Complete 2 of 4 lab exercises | 10% |
| **Deliverables** | ZIP named `P<group>_<id1>_<id2>.zip`, includes `.git`, the database folder, <1 page report, <5 min video | penalty if missing |

### Non-negotiables worth calling out early

- **Derby only.** MySQL, SQLite, Mongo etc. score zero on the database criterion.
- **Embedded mode only.** Network mode is explicitly listed as *Satisfactory* (40–60%) because it needs a service started.
- **No manual configuration.** The DB must create and populate itself on first launch. The jar must be committed to the repo and on the project classpath, not "installed".
- **Branches that merge.** A linear commit history caps this sub-criterion at *Good*. See §8 — this affects how we work from day one, and our repo currently has one commit on `main`.
- **JUnit.** The brief's *Unsatisfactory* band is literally "test cases are NOT implemented using the Junit framework". We currently have zero JUnit tests (§2).
- **AI comments in code must match the log.** We already have `// Generated with assistance from…` style comments from Project 1 — keep the habit, because Appendix A is marked for consistency with them.

---

## 2. Where Project 1 Left Off

### What shipped and works

~3,800 lines across 76 classes in 13 packages. The core loop is complete: crew selection → planet visit → site choice → drill session with per-level hazard rolls → jump to next planet → ending. Seeded RNG makes runs reproducible via `--seed`.

Genuinely good foundations we should not touch:

- **`GameDataSource`** (`src/expedition/io/GameDataSource.java`) is already an interface with `TextFileDataSource` behind it. The Project 1 GDD called this out as deliberate preparation for "a database or a GUI". It was right, and it makes §4 cheap.
- **`Effect` hierarchy** (`src/expedition/hazard/effect/`) — 8 effect types behind one interface, built by `EffectParser` from data-file codes. This is Strategy + Factory already working.
- **Hook interfaces** (`src/expedition/crew/hooks/`) — `DrillModifier`, `AscentModifier`, `ItemFinder`, `VeinInsight`, `FieldMedic`. Declared, unused.
- **Data-driven content** — hazards, planets, crew and tuning all live in `data/*.txt`, so balance changes never need a recompile.

### The three real gaps

**1. There are no JUnit tests.** `test/` holds three files — `SiteTierTest.java`, `TestVein/veinTest.java`, `VeinGeneratorCheck/veinGeneratorCheck.java` — and all three are `main()` methods that print to the console. One is entirely commented out. There is no JUnit on the classpath. Against the marking guideline this currently sits in the 0–40% band for testing.

**2. UI and model are mixed.** There are ~78 direct `ui.print(...)` / `input.read...(...)` calls, and only 6 of them are in the `ui` package:

| Class | UI calls |
|---|---|
| `engine/GameEngine.java` | 43 |
| `expedition/ExpeditionSelector.java` | 16 |
| `expedition/DrillSession.java` | 13 |
| `Main.java` | 6 |
| `io/RunLogWriter.java` | 1 |

The marking guideline gives *Excellent* only for "a clear separation of UI and model". This is the single biggest refactor in Project 2, and §3 is how we do it without rewriting the game.

**3. A third of the design is stubbed.** 21 classes carry explicit "project 2" markers. Full list in §5.

---

## 3. Architecture: The GUI Problem

### The problem stated plainly

`GameEngine.run()` is one long blocking loop. It prints, then calls `input.readInt(...)`, which blocks on `Scanner` until the player types something. A Swing GUI is the opposite shape: it never blocks, it sits idle and reacts to events on the Event Dispatch Thread. You cannot call a blocking read from an `ActionListener` without freezing the window.

There are two honest ways out.

**Option A — rewrite the engine as an event-driven state machine.** Every prompt becomes a state; every button press advances it. Architecturally the "correct" MVC answer, and roughly a full rewrite of `GameEngine`, `DrillSession` and `ExpeditionSelector` — the three most complex and most-tested classes we own.

**Option B — invert the UI behind an interface and run the engine on a worker thread.** Extract exactly what `GameEngine` needs from a UI into an interface; implement it twice.

```java
public interface GameView {
    void print(String line);
    int  readInt(String prompt, int min, int max)        throws QuitGameException;
    String readChoice(String prompt, List<String> opts)  throws QuitGameException;
    boolean readYesNo(String prompt)                     throws QuitGameException;
    void showState(RunState state);   // GUI paints panels; console no-ops
}
```

`ConsoleView` is the Project 1 behaviour, unchanged. `SwingView` renders to panels, and its `readInt` blocks the **worker** thread on a `SynchronousQueue` that the button's `ActionListener` fills from the EDT. The engine never learns it is talking to Swing.

**We are taking Option B.** It keeps every line of tested game logic intact, it delivers the UI/model separation the marking guideline asks for (the model depends on an interface, never on `javax.swing`), it is the same Dependency Inversion trick `GameDataSource` already demonstrates, and it lets us keep the console build alive as a debugging tool. The cost is threading discipline: engine on a `SwingWorker`, all widget mutation via `SwingUtilities.invokeLater`.

### Screen inventory

| Screen | Contents | Notes |
|---|---|---|
| **Main menu** | New run · Resume run · High scores · How to play · Quit | "Resume" greyed out when no save row exists |
| **Ship / status** | Crew cards (sprite, name, role, health/stamina/composure bars), fuel gauge, hull, inventory, planet counter | Persistent left panel — this is the "organised components" mark |
| **Planet arrival** | Planet sprite, archetype, intro flavour, site list with tier and risk | |
| **Crew selection** | Pick 3 of 6; ineligible crew listed **with the reason**, per GDD §13.6 | Reuses `ExpeditionSelector` rules |
| **Drill session** | Depth readout, per-level log, hazard text panel, Drill Again / Ascend | The tense screen — sprite + text, not a table |
| **Ascent** | Outcome text, Pilot mitigation stated explicitly | New in P2 (§5) |
| **Inventory / use item** | Item list, target crew member picker | New in P2 (§5) |
| **Ending** | Ending text, run summary, score, "back to menu" | Writes score row |

Menu bar carries **Quit** and **Return to menu** on every screen — the guideline names both explicitly.

---

## 4. Architecture: The Database

### Recommendation: yes, move to Derby — but keep the text files as seed data

The question was whether to replace the text-file reads and writes with Derby. The answer is yes, with one important qualification.

**Why yes.** The marking guideline wants *more than three* read interactions **and** *more than three* write interactions, contributing to real functionality. Our text I/O already covers most of it, and `GameDataSource` was built as the swap point — `DerbyDataSource implements GameDataSource` is the one-class change the Project 1 GDD predicted. Leaving the game on flat files forfeits 20% of the grade.

**The qualification.** Do **not** hand-author content in SQL. `data/hazards.txt` is 77 lines of prose that we edit constantly while balancing, and `data/config.txt` is the master tuning table. Turning those into `INSERT` statements makes the game harder to tune and gains nothing — the marker cannot see where the rows came from.

Instead: **the text files become seed data.** On first launch `DatabaseInitializer` creates the schema, sees the tables are empty, and populates them from `data/*.txt` using the existing `TextFileDataSource` parsers. Every launch after that reads from Derby. This satisfies "the DB should be set up automatically without any manual configurations" perfectly, keeps content authorable in a text editor, and reuses parser code we already trust.

### Verified: Derby 10.14.2 embedded works on JDK 25

Confirmed by running an actual create/insert/select against `jdbc:derby:testdb;create=true` on `java 25.0.3` — table created, row written, row read back. The jar is already on this machine at `Lab8-PDC/lib/derby.jar` (3.2 MB, Derby 10.14.2, single-jar embedded). Copy it to `Expedition2/lib/derby.jar`, commit it, and add it to `javac.classpath` so the project runs with no manual jar import.

### Connection handling

The guideline explicitly demotes projects where "the same database connection data is scattered in a few classes". One class owns the URL, full stop:

```java
public final class Database {                       // Singleton
    private static final String URL =
        "jdbc:derby:data/expedition-db;create=true";
    private static Connection conn;
    public static synchronized Connection get() throws SQLException { … }
    public static synchronized void shutdown() { … }   // Derby wants an explicit shutdown
}
```

The DB lives at `data/expedition-db/` — inside the project folder, as the brief requires for the ZIP. Add `data/expedition-db/` to `.gitignore` for day-to-day work, then commit it deliberately for submission.

### Schema

| Table | Purpose | R | W |
|---|---|---|---|
| `CREW` | Roster: name, role, base stats | ✓ | seed |
| `PLANETS` | Name, archetype, flavour, site pool | ✓ | seed |
| `HAZARDS` / `HAZARD_OUTCOMES` | Hazard header + six severity rows (text, effect codes) | ✓ | seed |
| `ITEMS` | Item definitions, effects, drop weights | ✓ | seed |
| `EVENTS` | Jump events: text, effect codes, points, weight — **added back in Week 13**, see §5.4 | ✓ | seed |
| `SETTINGS` | Tuning values from `config.txt`; editable in-game | ✓ | ✓ |
| `SAVED_RUN` | Full run state: seed, planet index, fuel, hull, crew stats, inventory | ✓ | ✓ |
| `RUN_LOG` | Ship's log lines, one row per entry, keyed by run | ✓ | ✓ |
| `SCORES` | Completed runs: date, planets reached, survivors, ending, score | ✓ | ✓ |

### Interaction tally

**Reads (7):** load crew · load planets · load hazards + outcomes · load items · load settings · load saved run on Resume · load high-score table.

**Writes (6):** create + seed schema on first launch · autosave run state at each planet · append run-log line · insert final score at ending · update settings from the options screen · delete the save row when a run completes.

Comfortably past the "more than three each" bar, and every one of them is load-bearing — nothing exists purely to satisfy the rubric.

### What this replaces

| Project 1 | Project 2 |
|---|---|
| `TextFileDataSource` reads `data/*.txt` every launch | Reads Derby; text files used once as seed |
| `RunLogWriter` → `data/runlog.txt` | `RUN_LOG` table (+ optional text export for flavour) |
| `RunLogWriter` → `data/scores.txt` | `SCORES` table, surfaced as a high-score screen |
| `GameConfig.load()` → `data/config.txt` | `SETTINGS` table, seeded from the file |
| `data/save.txt` — designed but never built | `SAVED_RUN` table, powering Resume |

`TextFileDataSource` stays in the tree. It still implements `GameDataSource`, it still parses the seed files, and keeping both implementations is the cleanest possible demonstration of why the interface exists.

---

## 5. Gameplay Scope — The Deferred Features

21 classes carry "project 2" markers. Grouped by system, with the marker locations:

### 5.1 Items and inventory — *the biggest gap*

`Inventory.java` and all five item subclasses are empty shells. `Item.java` declares `use(EffectContext, CrewMember)` and `needsCrewTarget()`; nothing implements them. `ItemGrantEffect.java:12` has a live TODO waiting on this.

- `Inventory` — fixed-capacity bag, add/remove, stacked `name → count` view
- `MedKit` → health · `Stimulant` → stamina · `ComfortItem` → composure · `RepairKit` → hull · `FuelItem` → fuel
- Wire `ItemGrantEffect` to actually grant
- GUI inventory screen with targeting

### 5.2 Crew abilities

Project 1 implemented one of six. The hook interfaces exist; the roles need to implement them and the systems need to consult them.

| Role | Hook | Fires |
|---|---|---|
| Geologist | `VeinInsight` | Scan — describe levels remaining |
| Miner | `DrillModifier` | Yield calculation |
| Surveyor | `ItemFinder` | Item drop roll |
| Engineer | `ItemFinder` | Item drop roll (repair bias) |
| Doctor | `FieldMedic` | After health loss — single use, resets per session (`DrillSession.java:28`) |
| Pilot | `AscentModifier` | Ascent hazard |

Per GDD §13.1, each system collects the hooks from expedition members and never uses `instanceof`. Fatigued members are filtered out where hooks are gathered.

### 5.3 Ascent phase

`AscentPhase.java` is empty. Roll whether an ascent hazard fires, let `AscentModifier` transform it, apply the outcome. This is what makes the Pilot matter and what makes `hull` meaningful — `GameEngine.java:248` notes hull "does not mean anything at this stage".

### 5.4 Jump events — **added back** *(Week 13, from Nadia's screen designs)*

Built after all: `data/events.txt` seeds the `EVENTS` table, `EventTable` rolls one on `JUMP_EVENT_CHANCE` of jumps with a weighted pick, and the effects reuse the hazard effect codes (anything but `OIL_PCT` and `CLOSE_SITE`, which need a drill site). Each event also carries points for the run's score. The events in `events.txt` are placeholders to be rewritten. The original reasoning for the cut is kept below.

`JumpEvent.java` and `EventTable.java` are empty and will stay that way. A weighted event table rolled during the jump, with eligibility conditions (GDD §9), was the plan — but with three build weeks it is the one system that is entirely greenfield, carries no marking-guideline weight of its own, and has nothing depending on it.

Leave the two classes in place with their existing comments updated to say the feature was descoped, rather than deleting them. First thing to add back if Week 11 finishes early.

### 5.5 Hull-loss ending

`GameEngine.java:324` — "not actually a reachable end until project 2". Becomes reachable once the ascent phase (§5.3) can damage the hull, so it survives the jump-event cut on its own.

### 5.6 Save / resume

Designed in GDD §13.4, never built. Now a DB feature (§4) and a main-menu option.

### 5.7 Known code smell to fix

`ExpeditionSelector.java:26` — eligibility thresholds duplicated from `CrewMember.isEligible()`. The comment already names the fix. The guideline marks "no obvious code smell", so clear it.

---

## 6. Design Patterns

Applied where they earn their place, not sprinkled for the rubric.

| Pattern | Where | Status |
|---|---|---|
| **Strategy** | `Effect` hierarchy; crew hook interfaces | Exists — extend |
| **Factory** | `EffectParser` builds effects from codes; new `ItemFactory` | Partly exists |
| **Dependency Inversion** | `GameDataSource`; new `GameView` | Exists — extend to UI |
| **DAO / Repository** | One DAO per entity behind `DerbyDataSource` | New |
| **Singleton** | `Database` connection holder | New |
| **Observer** | Engine publishes state changes; `SwingView` panels subscribe | New |
| **MVC** | Model (engine/world/crew) · View (`GameView` impls) · Controller (`GameEngine`) | New — the §3 refactor |
| **Template Method** | `Item.use()` — shared validate/apply/log skeleton, subclass fills the effect | New |

Document each in a short `docs/PATTERNS.md` with file references. Cheap to write, and it makes the pattern criterion trivial to mark.

---

## 7. Testing

Target: **well past five** JUnit 5 tests, named for behaviour, covering logic a marker would call important.

Setup: add JUnit 5 via NetBeans (Tools → Libraries), commit the jars to `lib/`, wire `javac.test.classpath`. Delete or convert the three `main()`-method files in `test/`.

Priority targets — deterministic, pure, high-value:

1. `VeinGeneratorTest` — pocket placement rules, non-adjacency, bottom detection (GDD §5 rules; a `main()` version already exists to convert)
2. `SiteTierTest` — hazard chance curve per tier and depth (same)
3. `SeverityRollerTest` — severity distribution under a fixed seed
4. `EffectParserTest` — every effect code parses; malformed codes fail cleanly
5. `FuelEconomyTest` — jump cost escalation
6. `InventoryTest` — capacity limit, add/remove, stacked view
7. `CrewMemberTest` — eligibility thresholds and fatigue
8. `DerbyDataSourceTest` — seed an in-memory/temp DB, write and read a run, assert round-trip
9. `SaveRoundTripTest` — save state, reload, assert the run resumes identically

Items 8–9 double as evidence that the database contributes to functionality.

---

## 8. Version Control Strategy

**This needs attention now.** The guideline reserves *Excellent* for history "showing multiple commits and branches merging"; our repo is currently a single commit on `main`.

Working rule from here: **no direct commits to `main`.** Every feature gets a branch, and every branch merges back with `--no-ff` so the merge is visible in the graph.

```
main
 ├── feature/derby-schema        → merge
 ├── feature/swing-shell         → merge
 ├── feature/items-inventory     → merge
 ├── feature/crew-abilities      → merge
 ├── feature/ascent-phase        → merge
 ├── feature/jump-events         → merge
 ├── feature/save-resume         → merge
 ├── feature/junit-suite         → merge
 └── art/sprites                 → merge   (Nadia)
```

```sh
git checkout -b feature/derby-schema
# … commit as you go, small and often …
git checkout main
git merge --no-ff feature/derby-schema
git push
```

Commit in small increments as work happens, not in one dump at the end — the history is the evidence, and both of us need commits under our own name for the contribution criterion.

---

## 9. Art Pipeline

Nadia is producing sprites, which runs in parallel with the code and must not block it in either direction.

**Agree the asset manifest first** — filenames and dimensions before art exists. Code then loads by name against placeholder PNGs from day one, and dropping in finished art is a file copy with no code change.

```
resources/sprites/
  crew/       braugn.png  mikela.png  vasquez.png  hannah.png  henry.png  alexander.png
              (miner)     (doctor)    (pilot)     (surveyor)  (engineer)  (geologist)
  planets/    frozen.png  desert.png  volcanic.png  toxic.png  barren.png  storm.png
  items/      medkit.png  stimulant.png  comfort.png  repairkit.png  fuel.png
  ui/         panel_bg.png  fuel_gauge.png  hull_icon.png
```

Conventions: PNG, transparent background, crew portraits 128×128, planets 256×256, items 48×48. `SpriteLoader` caches by path and returns a visible magenta placeholder on a miss, so a missing file never throws — which also protects the "no exceptions" criterion.

Sprites go in the repo under `resources/` and get copied to `dist/` by the build, so the game runs from the ZIP with no configuration.

---

## 10. Roadmap

**Due: Friday 23 October 2026, 11:59 pm.** It is the end of Week 9. That leaves four teaching weeks, and Week 13 is consumed by submission artifacts — so this is **three build weeks**, not six.

| Week | Dates | Theme |
|---|---|---|
| 10 | Mon 28 Sep – Fri 2 Oct | Foundations: Derby, JUnit, `GameView` |
| 11 | Mon 5 Oct – Fri 9 Oct | Swing shell playable + items |
| 12 | Mon 12 Oct – Fri 16 Oct | Abilities, ascent, persistence — **feature freeze Friday** |
| 13 | Mon 19 Oct – Fri 23 Oct | Polish, hardening, report, video, submit |

*Confirm these against the Canvas calendar — a mid-semester break would shift everything.*

### Scope cut, decided up front

Three build weeks does not fit the full feature list, and discovering that in Week 13 is the expensive way to find out. **Jump events (§5.4) are cut now** — `JumpEvent` and `EventTable` stay empty, and the `events.txt` seed file and its two DB tables are dropped from the schema.

It is the right thing to lose: it is the only deferred system that is entirely greenfield, it carries no marking-guideline weight of its own, and nothing else depends on it. The hull-loss ending (§5.5) survives the cut because ascent damage alone makes it reachable. Everything else on the list either earns marks directly or is nearly free.

If Week 11 finishes early, jump events are the first thing to add back.

### Week 10 — Foundations *(highest risk, do it first)*

- [ ] Copy `derby.jar` into `lib/`, add to classpath, commit
- [ ] `Database` singleton + `DatabaseInitializer` (create schema, seed from `data/*.txt` when empty)
- [ ] `DerbyDataSource implements GameDataSource` — crew, planets, hazards reads
- [ ] Prove it: delete `data/expedition-db/`, launch, confirm the console game runs unchanged off the DB
- [ ] JUnit 5 on the classpath; convert the two existing `main()` checks into real tests
- [ ] Extract the `GameView` interface; `ConsoleView` implements it; refactor the 78 UI calls
- [ ] Agree the sprite manifest with Nadia; commit placeholder PNGs
- [ ] Branch/merge workflow in use from the first commit

**Gate (Fri 2 Oct):** the Project 1 game runs identically — reading from Derby, talking only to `GameView`, zero manual setup.

The `GameView` refactor sits in Week 10 deliberately, ahead of any Swing code. It is mechanical, it is the prerequisite for every GUI task, and it is the one piece that must not be rushed in the same week as the threading work.

### Week 11 — GUI shell and items

- [ ] `SwingView` skeleton: main window, menu bar with Quit / Return to menu, status panel
- [ ] Worker-thread plumbing + blocking queue for input
- [ ] `SpriteLoader` with placeholder fallback
- [ ] Planet, crew-selection and drill screens
- [ ] `Inventory` + all five item subclasses; wire `ItemGrantEffect`
- [ ] Inventory / use-item screen
- [ ] Tests: `InventoryTest`, `EffectParserTest`

**Gate (Fri 9 Oct):** a full run playable end to end in a window, even if ugly.

### Week 12 — Abilities, ascent, persistence

- [ ] All six crew abilities via the hook interfaces
- [ ] `AscentPhase` + Pilot mitigation, stated on screen
- [ ] Hull-loss ending reachable
- [ ] Save / resume against `SAVED_RUN`; Resume live on the main menu
- [ ] High-score screen off `SCORES`
- [ ] Fix the `ExpeditionSelector` duplicate-threshold smell
- [ ] Tests: `SaveRoundTripTest`, `DerbyDataSourceTest`, `CrewMemberTest`

**Gate — FEATURE FREEZE (Fri 16 Oct):** everything in scope is built. Nothing new after this date; Week 13 is polish only. If something is unfinished on Friday, it gets cut rather than carried.

### Week 13 — Polish and submission

**Mon–Tue: harden**
- [ ] Real sprites replace placeholders
- [ ] Screen layout, fonts, colours; resize behaviour (the guideline penalises having to resize the frame)
- [ ] Input hardening — every field rejects bad input with a clear message, never an exception
- [ ] Quit / return-to-menu verified on **every** screen
- [ ] Code-smell pass: dead code, long methods, magic numbers, duplicated logic
- [ ] Full JUnit suite green, 9+ tests
- [ ] `docs/PATTERNS.md`

**Wed: verify**
- [ ] Fresh-clone test — clone to an empty folder, open in NetBeans 28, build and run. No configuration, no missing jars, DB self-creates
- [ ] Commit `data/expedition-db/` for the ZIP

**Thu: paperwork** *(a full day — do not compress it)*
- [ ] Report (<1 page): setup, GitHub URL, contribution split
- [ ] Appendix A — AI interaction log, cross-checked against in-code AI comments
- [ ] Appendix B — one-page reflection, **written by hand, not generated**
- [ ] Video (<5 min): demo + code explanation. Use `--seed` to rehearse and guarantee a severity-6 hazard on camera (GDD §13.5)

**Fri 23 Oct: submit**
- [ ] ZIP as `P<group>_<id1>_<id2>.zip`, including `.git` and the database folder
- [ ] Submit in the morning. The 5%/day late penalty makes an evening submission a bad trade

## 11. Work Split

Nadia is producing sprites this project, so the code load leans to Adam. The contribution criterion still requires **both** members above 40% on coding, so Nadia's coding share is protected below and must show up as commits under Nadia's own name.

| Owner | Area |
|---|---|
| **Together, first session of Week 10** | `GameView` interface, DB schema, sprite manifest. Agree signatures before splitting — GDD §13.7's warning about spending the final week merging instead of building applies much harder with three build weeks. |
| **Adam** | Derby layer (`Database`, `DatabaseInitializer`, `DerbyDataSource`, DAOs) · `GameView` refactor and Swing shell · crew ability hooks · ascent phase · save/resume |
| **Nadia** | All sprite art · `SpriteLoader` and asset integration · inventory and item classes · high-score and main-menu screens · JUnit suite |
| **Either** | Code-smell pass, report, video |

Items are a good fit for Nadia's coding share: self-contained, well-specified in the GDD, and they touch the sprite work directly. Crew abilities moved to Adam (8 Oct): they plug into the drill session and the ascent phase, which Adam is already building.

---

## 12. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| GUI refactor destabilises tested engine logic | High | `GameView` interface keeps engine logic untouched; `ConsoleView` stays as a working control to A/B against |
| Swing threading bugs (frozen window, EDT violations) | High | One worker thread, one input queue, all widget mutation via `invokeLater`. Build the shell in Week 11, not Week 13 |
| Derby jar not on classpath in a fresh clone | High — caps functionality marks | Commit the jar; fresh-clone test is a Week 13 gate |
| Art not ready for integration | Medium | Placeholder pipeline from Week 10; art is a file copy, never a code dependency |
| Linear git history | Medium — caps a 25% criterion | Branch-per-feature from today (§8) |
| **Only three build weeks** | **High — the defining constraint** | Jump events cut up front (§10). Weekly gates, hard feature freeze Fri 16 Oct. If a Week 12 item is unfinished on the Friday it gets cut, not carried into Week 13 |
| Week 13 paperwork squeezed by late features | High — report and appendices carry 5% plus penalties | Feature freeze is absolute. Thursday 22 Oct is reserved for report, appendices and video |
| Reflection written with AI | Severe — zero for that component | Appendix B is handwritten. Non-negotiable |

---

## 13. Definition of Done

Project 2 is finished when, on a **fresh clone into an empty folder**:

1. NetBeans 28 opens the project; it builds and runs with no manual configuration
2. The Derby database creates and seeds itself on first launch, embedded, with no service started
3. A full run is playable in the GUI from main menu to ending, with sprites
4. Quit and return-to-menu work on every screen, and no input of any kind throws an exception
5. Save, quit, relaunch and Resume restores the run exactly
6. The high-score screen shows completed runs
7. `ant test` runs 9+ JUnit tests, all green
8. `git log --graph` shows multiple merged feature branches with commits from both members
9. Zero "project 2" markers remain in `src/` that were not deliberately descoped
10. The ZIP contains the project, `.git`, `data/expedition-db/`, the report with both appendices, and the video
