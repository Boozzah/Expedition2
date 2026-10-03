# Derby Foundation — Working Plan

**Branch:** `feature/derby-foundation`
**Week 10 (28 Sep – 2 Oct)** · first task of Project 2
**Owner:** Adam

Companion to [LongWayHome_GDD_P2.md](LongWayHome_GDD_P2.md) §4. That section says *what and why*; this one is the checklist for actually doing it.

---

## Goal

Move game content out of `data/*.txt` and into an embedded Derby database, without `GameEngine` changing a single line.

**Gate — the branch is done when:**

```sh
rm -rf data/expedition-db
ant run            # or Run in NetBeans
```

…and the console game plays exactly as it does today. Same crew, same planets, same hazards — now read out of Derby, with the database having created and populated itself on that first launch.

---

## 1. Create the branch

You're on a clean `main` that matches origin.

```sh
cd /Users/home/NetBeansProjects/Expedition2
git checkout -b feature/derby-foundation
git branch                                    # the * shows where you are
```

First push:

```sh
git push -u origin feature/derby-foundation
```

After that, plain `git push`.

**Aim for 6–8 small commits on this branch, not one at the end.** The build order in §4 is designed so each step is a commit and each step still runs.

---

## 2. Package layout

New package nested under `io`, not at the top level:

```
src/expedition/io/
    GameDataSource.java          ← unchanged. The interface everything depends on
    TextFileDataSource.java      ← unchanged. Now doubles as the seeder
    RunLogWriter.java            ← unchanged on this branch (see §7)
    db/
        Database.java
        Schema.java
        DatabaseInitializer.java
        DerbyDataSource.java
        CrewDao.java
        PlanetDao.java
        HazardDao.java
```

**Why `io.db` and not a top-level `expedition.database`:** a database *is* input/output, and `expedition.io` is already the persistence boundary — `GameDataSource` lives there. Nesting says "one implementation beneath that interface", and the project tree then shows the architecture at a glance: interface at the top, two implementations under it. A sibling package would imply the DB is separate from `GameDataSource`, which is the opposite of the point.

DAOs stay flat in `io.db` for now. Split an `io.db.dao` package out only if it passes ~10 classes.

### The one rule for this package

> **No game logic goes in, no `java.sql` comes out.**

Nothing outside `io.db` should ever import `Connection`, `ResultSet` or `SQLException`. The DAOs return `CrewMember`, `Planet`, `Hazard` — real domain objects. If a `SQLException` reaches `GameEngine`, the abstraction has leaked and the design mark goes with it.

---

## 3. Class responsibilities

| Class | Owns | Shape |
|---|---|---|
| `Database` | The **only** place the connection URL exists | `static Connection get()` · `static void shutdown()` |
| `Schema` | `CREATE TABLE` statements as constants. Nothing else | `static final String CREW = "CREATE TABLE ..."` |
| `DatabaseInitializer` | Runs once at startup: create tables if absent, seed from `TextFileDataSource` if empty | `void initialise()` |
| `DerbyDataSource` | `implements GameDataSource`. Delegates to DAOs, translates `SQLException` → `DataLoadException` | the three `load*()` methods |
| `CrewDao` `PlanetDao` `HazardDao` | One table each, owns its SQL, returns domain objects | `List<X> findAll()` · `void insertAll(List<X>)` |

`Database` exists because of one line in the marking guideline — the *Satisfactory* band is "the same database connection data is scattered in a few classes." One constant, one class, right side of that line.

The `SQLException` → `DataLoadException` translation matters: `DataLoadException` is already what `GameEngine` and `Main` catch, so all the Project 1 error handling keeps working untouched.

---

## 4. Build order

Each step is a commit. Each step still runs.

### ☑ Step 1 — the jar on the classpath — **DONE**

- [ ] `mkdir lib && cp ../Lab8-PDC/lib/derby.jar lib/`
- [ ] NetBeans: right-click project → Properties → Libraries → Add JAR/Folder
- [ ] **Choose "Relative Path" when it asks.** An absolute path breaks the fresh-clone test, which is a Week 13 gate
- [ ] Commit the jar itself — the brief requires the project to run with no manual jar import

*Verified already: Derby 10.14.2 embedded works on JDK 25.*

### ☑ Step 2 — `Database` (built as `DBManager`) — **DONE**

- [ ] URL constant: `jdbc:derby:data/expedition-db;create=true`
- [ ] `get()` returns a cached connection, reopening if closed
- [ ] `shutdown()` — see the gotcha in §5
- [ ] Throwaway `main` that calls `get()` and prints "connected"; confirm `data/expedition-db/` appears
- [ ] Delete the throwaway before committing

### ☑ Step 3 — `Schema` + `DatabaseInitialiser` (tables only, no seeding yet) — **DONE**

- [ ] DDL for `CREW`, `PLANETS`, `HAZARDS`, `HAZARD_OUTCOMES` (§6)
- [ ] Create-if-absent — Derby has no `IF NOT EXISTS` (§5)
- [ ] Call `initialise()` from `Main` before the engine starts
- [ ] Verify in NetBeans: Services → Databases → the tables exist and are empty

### ☑ Step 4 — `CrewDao` + crew seeding — **DONE**

- [x] `findAll()` and `insertAll()`
- [x] `DatabaseInitialiser`: if `SELECT COUNT(*) FROM CREW` is 0, load via `TextFileDataSource.loadCrew()` and insert
- [x] Prove the round-trip on the smallest table — 6 rows (`CrewDaoTest`)

### ☑ Step 5 — `PlanetDao` + `HazardDao` + their seeding — **DONE**

- [x] Planets: **kept the delimited string.** `SITES` holds `North Shelf;The Silo;…`, joined on insert and split on read
- [x] Hazards: one `HAZARDS` row + six `HAZARD_OUTCOMES` rows each, linked by `getGeneratedKeys()`

### ☑ Step 6 — `DerbyDataSource` + the swap — **DONE**

- [x] Implement the three `GameDataSource` methods over the DAOs
- [x] `Main.java`: `new TextFileDataSource()` → `new DerbyDataSource()` — one line
- [x] **Run the gate.** `rm -rf data/expedition-db`, launch, play — passed 3 Oct

That one-line swap is the cleanest demo of why `GameDataSource` was built as an interface. Worth saying out loud in the video.

---

## 5. Derby gotchas

**`CREATE TABLE IF NOT EXISTS` doesn't exist.** Either query `DatabaseMetaData.getTables()` first, or catch `SQLException` with SQLState `X0Y32` ("table already exists") and carry on.

**Clean shutdown throws on success.** This is normal, not a bug:

```java
try {
    DriverManager.getConnection("jdbc:derby:;shutdown=true");
} catch (SQLException e) {
    // SQLState XJ015 means the shutdown worked. Swallow it.
}
```

Skip the shutdown and the database still survives, but Derby logs a recovery on next boot — noisy on camera.

**One JVM at a time.** Leave the game running and hit Run again and the second gets *"another instance of Derby may have already booted the database."* Not a bug. Close the first.

**`VARCHAR` needs an explicit length**, and the longest hazard text today is 1,133 characters. `VARCHAR(2000)` leaves headroom. Derby's limit is 32,672.

**`SET` is a reserved word** — `HazardSet` can't map to a column called `SET`. Use `SET_` or `HAZARD_SET`.

**Paths are relative to the working directory**, the same way `data/crew.txt` already is. So `data/expedition-db` lands inside the project folder, which is what the brief needs for the ZIP.

---

## 6. Schema for this branch

Sketch — adjust as you build. Only the three reads are in scope here; `SAVED_RUN`, `RUN_LOG`, `SCORES` and `SETTINGS` come in Week 12.

```sql
CREATE TABLE CREW (
    ID        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME      VARCHAR(40) NOT NULL,
    ROLE      VARCHAR(20) NOT NULL,
    HEALTH    INTEGER     NOT NULL,
    STAMINA   INTEGER     NOT NULL,
    COMPOSURE INTEGER     NOT NULL
);

CREATE TABLE PLANETS (
    ID        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME      VARCHAR(40)  NOT NULL,
    ARCHETYPE VARCHAR(20)  NOT NULL,
    FLAVOUR   VARCHAR(500) NOT NULL,
    SITES     VARCHAR(500) NOT NULL   -- or a child table; decide in step 5
);

CREATE TABLE HAZARDS (
    ID         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME       VARCHAR(80) NOT NULL,
    HAZARD_SET VARCHAR(20) NOT NULL
);

CREATE TABLE HAZARD_OUTCOMES (
    HAZARD_ID INTEGER       NOT NULL REFERENCES HAZARDS(ID),
    SEVERITY  INTEGER       NOT NULL,     -- 1..6
    TEXT      VARCHAR(2000) NOT NULL,
    EFFECTS   VARCHAR(500)  NOT NULL,     -- raw effect codes, unparsed
    PRIMARY KEY (HAZARD_ID, SEVERITY)
);
```

**Keep `EFFECTS` as the raw code string** (`STAT: COMPOSURE: -2: ALL:0;`). `EffectParser` already turns those into `Effect` objects and it works. Modelling effects as tables would be a rewrite of working code for zero marks.

---

## 7. Scope guards

Things that are **not** on this branch, so it can actually finish:

- **`RunLogWriter`** keeps writing to text files. Moving log and scores into Derby is Week 12 and depends on tables not designed yet.
- **`GameConfig`** keeps reading `config.txt`. The `SETTINGS` table is Week 12.
- **Save/resume** is Week 12.
- **Anything GUI.** Different branch, after this one.

Add `data/expedition-db/` to `.gitignore` now so binary database files don't land in every commit. Week 13 you deliberately `git add -f` it for the submission ZIP.

---

## 8. Merging

When the gate passes:

```sh
git checkout main
git merge --no-ff feature/derby-foundation
git push
```

`--no-ff` is the important flag. Without it git fast-forwards and the branch disappears from the history as though you'd committed straight to `main` — which is exactly the evidence the rubric wants. With it you get a permanent merge commit and a visible fork-and-rejoin in `git log --graph`.

---

## 9. Before you merge

- [x] Gate passes from a deleted database
- [x] No `java.sql` import outside `io.db`
- [x] Connection URL appears exactly once in the codebase (`DBManager.java:18`)
- [x] `data/expedition-db/` is gitignored
- [x] `lib/derby.jar` is committed and the classpath reference is **relative**
- [ ] AI-log row added to [AI-LOG.md](AI-LOG.md) for any AI help taken, with matching `// AI assisted …` comments in the code — **log rows added 3 Oct; some code comments still missing, see AI-LOG**

---

## Where we got to — 3 Oct 2026

**Branch:** `feature/derby-foundation`. **All 6 steps complete and the gate passes.** Next action: merge into `main` with `--no-ff` (§8).

The game now reads crew, planets and hazards from Derby. On first launch the database creates itself and seeds from `data/*.txt`; every launch after that reads straight from the tables.

### Built on this branch

| File | What it does |
|---|---|
| `lib/derby.jar` | Derby 10.14.2, committed, on the compile classpath by **relative** path |
| `src/expedition/io/db/DBManager.java` | Holds the one connection URL. `get()` caches a connection, `shutdown()` closes Derby down. (Named `DBManager`, not `Database` as this doc originally said — adapted from the PDC lab class.) |
| `src/expedition/io/db/Schema.java` | The four `CREATE TABLE` statements as constants |
| `src/expedition/io/db/DatabaseInitialiser.java` | Creates any missing tables at startup, then seeds each empty table from the `GameDataSource` it is given. Safe to run every launch |
| `src/expedition/io/db/CrewDao.java` | `CREW` table. `PreparedStatement` + batch insert; `findAll()` rebuilds subclasses through `CrewFactory` |
| `src/expedition/io/db/PlanetDao.java` | `PLANETS` table. Site names joined with `;` into one column and split back out |
| `src/expedition/io/db/HazardDao.java` | `HAZARDS` + `HAZARD_OUTCOMES`. Inserts each hazard, reads its ID back with `getGeneratedKeys()`, then batches its six outcomes. Reads both tables with one `JOIN` |
| `src/expedition/io/db/DerbyDataSource.java` | `implements GameDataSource` over the three DAOs. Turns `DatabaseException` into `DataLoadException` so `GameEngine` is unchanged |
| `src/expedition/exception/DatabaseException.java` | Checked exception. `SQLException` is translated here so nothing outside `io.db` imports `java.sql` |
| `src/expedition/exception/DataLoadException.java` | Gained a `(source, reason, cause)` constructor for errors that come from a table rather than a file line |
| `src/expedition/hazard/HazardOutcome.java` | Gained an `effectCodes` field — the raw effect string, kept only so it can be saved and re-parsed |
| `src/expedition/world/Planet.java` | Gained a `siteNamePool()` getter so `PlanetDao` can save the site names |
| `src/expedition/Main.java` | Seeds via `new DatabaseInitialiser(new TextFileDataSource())`, plays from `new DerbyDataSource()`, calls `DBManager.shutdown()` on exit |

### Tests passing (6)

- `DBManagerTest.getReturnsTheSameConnectionWhenCalledTwice`
- `DatabaseInitialiserTest.initialiseCreatesAllFourTables`
- `DatabaseInitialiserTest.initialiseIsSafeToRunTwice`
- `CrewDaoTest.findAllReturnsTheSameCrewAsTheTextFile`
- `PlanetDaoTest.findAllReturnsTheSamePlanetsAsTheTextFile`
- `HazardDaoTest.findAllReturnsTheSameHazardsAsTheTextFile`

The three DAO tests never insert test rows — they run the initialiser and compare the database against the text files, so no fake data ends up in the real game database.

### Verified

- **The gate:** deleted `data/expedition-db`, launched, played an expedition. Database created and seeded itself; game plays as before.
- Second launch starts without reseeding.
- Row counts after seeding: `CREW` 6, `PLANETS` 12, `HAZARDS` 11, `HAZARD_OUTCOMES` 66.
- `db.lck` is gone after quitting from the menu, so the clean shutdown works.

### Things decided along the way

- **Every insert uses `PreparedStatement`.** `planets.txt` has 13 apostrophes and `hazards.txt` has 3; string-built SQL would have broken on the first one. Plain `Statement` is only used where the SQL is fixed (`SELECT_ALL`, `CREATE TABLE`).
- **`SITES` stays a delimited string**, not a child table. The game never queries individual site names, so a second table would add a join for nothing.
- **`HazardOutcome` keeps its raw `effectCodes`.** The alternative was a `toCode()` on every `Effect` class to rebuild the string. `Pilot.modifyAscent` passes the original codes through unchanged; harmless, because only saving reads them and that copy is never saved.
- **Crew come back alphabetically** (`ORDER BY NAME`), not in `crew.txt` order. This is the one visible difference from the text-file version. **Kept deliberately.** Planets and hazards use `ORDER BY ID`, so they keep file order — the seeded-run replay depends on that.
- **`CREW` keys on `NAME`**; `PLANETS` and `HAZARDS` use generated IDs. Still slightly inconsistent — worth a sentence in the report.
- **The text files are the seed, Derby is the source.** `Main` has two data sources: `TextFileDataSource` only feeds the initialiser, `DerbyDataSource` feeds the game. Passing the Derby one to the initialiser by mistake seeds empty tables, and the run ends instantly with "There is noone left" — hit this once during the gate.
- **NetBeans' bundled "Java DB (Embedded)" driver can't open this database.** Services → Databases needs a driver pointed at `lib/derby.jar`, and must be **disconnected** before running the game or tests (embedded Derby allows one JVM at a time).
- **JUnit 4, not 5.** NetBeans' Ant build only detects `org.junit.Test` / `junit.framework.Test`, so JUnit 5 tests compile but silently never run. GDD §7 still says JUnit 5 — fix it and tell Nadia.
- **`docs/AI-LOG.md` is untracked and gitignored**, and was scrubbed from git history on 26 Sep. It lives on disk only, so **back it up somewhere outside the repo** — it is 5% of the grade.
- `test/VeinGeneratorCheck/veinGeneratorCheck.java` is still there, fully commented out, awaiting conversion to a real `VeinGeneratorTest`.

### Worth adding later

- `DerbyDataSourceTest` — delete the database, initialise, assert `loadCrew()` is not empty. None of the current tests would have caught the wrong-seed-source bug above.

### Still to do in Week 10 after this branch

- `GameView` interface extraction — the ~78 UI calls in `GameEngine`, `ExpeditionSelector`, `DrillSession`. **This is the one that cannot slip**; Week 11 is the Swing shell.
- Sprite manifest agreed with Nadia, placeholder PNGs committed.
