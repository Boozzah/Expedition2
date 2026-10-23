# Design patterns in Expedition

Where each pattern is used in the code, and why it is there (GDD P2 §6).

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `hazard/effect/Effect` and its 8 records (`FuelEffect`, `HullEffect`, `CrewStatEffect`, ...) | A hazard or jump event is a list of effects. Each effect does its own thing behind one `apply(ctx)` method, so `HazardOutcome` and `JumpEvent` never need to know which kinds they hold. |
| **Strategy** | `crew/hooks/*` (`DrillModifier`, `AscentModifier`, `ItemFinder`, `VeinInsight`, `FieldMedic`) | Each crew ability is a hook interface that the role implements. `DrillSession` and `AscentPhase` ask the `Expedition` for whoever has a hook and call it, with no `instanceof` checks on roles. |
| **Factory** | `hazard/effect/EffectParser`, `item/ItemFactory`, `crew/CrewFactory` | Turn the text in the data files and the database (`"HULL:-12"`, `REPAIR`, `MINER`) into the right object, in one place each. |
| **Dependency Inversion** | `io/GameDataSource` (`TextFileDataSource`, `DerbyDataSource`) | The engine loads content through the interface, so it doesn't know the content now comes from Derby. The text files are still used to seed the database. |
| **Dependency Inversion** | `ui/GameView` (`ConsoleView`, `swing/SwingView`) | The engine and game classes only talk to `GameView`, so the same game runs in the console and in the window. |
| **Dependency Inversion** | `io/SaveStore`, `io/ScoreStore` (`DerbySaveStore`, `DerbyScoreStore`) | The engine saves runs and scores without knowing about SQL. `BackInTimeTest` swaps in in-memory stores. |
| **DAO** | `io/db/CrewDao`, `PlanetDao`, `HazardDao`, `ItemDao`, `EventDao`, `SavedRunDao`, `ScoreDao` | One class per table (or pair of tables) holds all the SQL for it. |
| **Singleton** | `io/db/DBManager` | The one embedded Derby connection, and the only place the database URL is written. |
| **MVC** | Model: `engine/RunState`, `ship/*`, `crew/*`, `world/*`. Controller: `engine/GameEngine`. View: `GameView` and its two implementations | The engine asks the view one screen at a time and gets back what the player picked. The model never touches the console or Swing. |
| **View model / DTO** | `ui/screen/*` records (`ShipStatus`, `CrewCard`, `DrillStatus`, `PlanetInfo`, ...) | Read-only snapshots the engine hands the view, so a screen can't change the run by accident. |
| **Template Method** | `ui/GameView`'s default screen methods | Each screen has a text version built on `print`/`readInt`/`readChoice`, which `ConsoleView` and the tests use. `SwingView` overrides them with Nadia's screens. |
| **Template Method** | `ui/swing/GameScreen` | Every window screen gets the same answer-once-per-question handling. Each screen only adds its own drawing and buttons. |
| **Producer-consumer** | `SwingView`'s answer queue | Swing's event thread puts the player's clicks in a `LinkedBlockingQueue`, and the engine thread takes them out. The engine can wait for an answer without freezing the window. |
| **Immutable value objects** | `HazardOutcome`, `JumpEvent`, `SavedRun`, `HighScore`, `Ending`, `SaveSlot` (records) | Shared content can't be changed by one run and leak into the next. For example, the Pilot's ability makes a new outcome instead of editing the loaded one. |
