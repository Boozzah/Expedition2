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

### ☐ Step 1 — the jar on the classpath

- [ ] `mkdir lib && cp ../Lab8-PDC/lib/derby.jar lib/`
- [ ] NetBeans: right-click project → Properties → Libraries → Add JAR/Folder
- [ ] **Choose "Relative Path" when it asks.** An absolute path breaks the fresh-clone test, which is a Week 13 gate
- [ ] Commit the jar itself — the brief requires the project to run with no manual jar import

*Verified already: Derby 10.14.2 embedded works on JDK 25.*

### ☐ Step 2 — `Database`

- [ ] URL constant: `jdbc:derby:data/expedition-db;create=true`
- [ ] `get()` returns a cached connection, reopening if closed
- [ ] `shutdown()` — see the gotcha in §5
- [ ] Throwaway `main` that calls `get()` and prints "connected"; confirm `data/expedition-db/` appears
- [ ] Delete the throwaway before committing

### ☐ Step 3 — `Schema` + `DatabaseInitializer` (tables only, no seeding yet)

- [ ] DDL for `CREW`, `PLANETS`, `HAZARDS`, `HAZARD_OUTCOMES` (§6)
- [ ] Create-if-absent — Derby has no `IF NOT EXISTS` (§5)
- [ ] Call `initialise()` from `Main` before the engine starts
- [ ] Verify in NetBeans: Services → Databases → the tables exist and are empty

### ☐ Step 4 — `CrewDao` + crew seeding

- [ ] `findAll()` and `insertAll()`
- [ ] `DatabaseInitializer`: if `SELECT COUNT(*) FROM CREW` is 0, load via `TextFileDataSource.loadCrew()` and insert
- [ ] Prove the round-trip on the smallest table — 6 rows

### ☐ Step 5 — `PlanetDao` + `HazardDao` + their seeding

- [ ] Planets: watch the `;`-separated site-name list (flatten to a child table, or keep the delimited string — your call, note which you picked)
- [ ] Hazards last: two tables and 1,100-character text, so it's the fiddly one

### ☐ Step 6 — `DerbyDataSource` + the swap

- [ ] Implement the three `GameDataSource` methods over the DAOs
- [ ] `Main.java`: `new TextFileDataSource()` → `new DerbyDataSource()` — one line
- [ ] **Run the gate.** `rm -rf data/expedition-db`, launch, play

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

- [ ] Gate passes from a deleted database
- [ ] No `java.sql` import outside `io.db`
- [ ] Connection URL appears exactly once in the codebase
- [ ] `data/expedition-db/` is gitignored
- [ ] `lib/derby.jar` is committed and the classpath reference is **relative**
- [ ] AI-log row added to [AI-LOG.md](AI-LOG.md) for any AI help taken, with matching `// AI assisted …` comments in the code
