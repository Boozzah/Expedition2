# LONG WAY HOME
*(working title)*

**Game Design Document**

*A command-line survival / risk-management game in Java*

**COMP603 / ENSE600 — Software Development Project 1**
2026 Semester 2 · Two-person team

*Version 1.0 — design baseline. All numeric values in this document are STARTING values intended to be tuned during playtesting.*

---

## Contents

1. [Premise and Lore](#1-premise-and-lore)
2. [Run Structure](#2-run-structure)
3. [Crew](#3-crew)
4. [Ship and the Fuel Economy](#4-ship-and-the-fuel-economy)
5. [Planets, Sites and Drilling](#5-planets-sites-and-drilling)
6. [The Severity Contract](#6-the-severity-contract)
7. [Hazard Tables — Drilling](#7-hazard-tables--drilling)
8. [Hazard Tables — Ascent](#8-hazard-tables--ascent)
9. [Opportunity Events — During the Jump](#9-opportunity-events--during-the-jump)
10. [Items](#10-items)
11. [Endings](#11-endings)
12. [Master Tuning Table](#12-master-tuning-table)
13. [Technical Design](#13-technical-design)

---

## 1. Premise and Lore

Your ship was a short-range survey tender. It was never built to cross the gap between systems, and it was never meant to be alone.

The mother ship jumped without you. Whether that was an accident, a miscount, or a decision nobody wants to admit to is a question for later — there is a chain of navigation beacons leading home, and the tender has enough tank capacity to reach the next one. That is all.

Each beacon marks a planet. Each planet holds oil somewhere under its surface, and the tender burns oil. So the run is a chain of the same wager, repeated until it kills you or you get home: how much are you willing to risk on this rock, knowing the next jump costs more than the last one?

Six crew. Three seats on the shuttle. Everyone who goes down comes back tired, and everyone who stays behind is one more person who does not know what happened down there. Some of them will not come back at all.

### 1.1 Tone

Grounded industrial science fiction. Nobody is a hero. The crew are contractors doing a dangerous job badly resourced, and the horror is mostly bureaucratic — the drill was rated for this, the seal was certified, the beacon was supposed to be maintained. Hazard text should be written flat and procedural, which makes the severity-6 outcomes land harder by contrast.

The player is not a character. The player is the decision-maker, and the game's central emotional beat is that every death is traceable to a number the player looked at and chose to accept.

### 1.2 Design pillars

- Every death should be the player's arithmetic catching up with them, never the dice being unfair.
- Risk means uncertainty, not just danger. High-risk sites are more volatile, not merely worse.
- Scarcity beats weakness. Abilities are balanced by limiting when they are available, not by making them weak.
- Information is a resource. Knowing how deep the vein goes is worth as much as fuel.
- The interface is text. Anything the player must feel has to be stated on screen in words.

---

## 2. Run Structure

A run consists of 8 planets drawn at random from a pool of 12, visited in the drawn order, followed by a final jump to the mother ship. There are therefore 8 jumps in total: seven between planets and one home.

### 2.1 Planet pool

- The 12 planets live in `planets.txt`. Eight are drawn at run start.
- The drawn order MUST be written into the save file. If the pool is reshuffled on load, the player's planned route changes underneath them.
- Recommended: the 8th planet is fixed rather than drawn — a scripted final stop before the run home. It gives the run a climax and a fixed place to put the best writing.

### 2.2 The loop

For each planet:

- **Arrival** — flavour text introducing the planet, its history and its hazard set.
- **Status** — fuel, tank capacity, NEXT JUMP COST, hull, and crew states are displayed.
- **Scan** — reveals three drill sites with names, estimated oil, and risk tier.
- **Decision** — drill a site, or skip the planet and jump immediately.
- **Crew selection** — choose 3 of 6. Ineligible crew are shown with the reason.
- **Drill session** — repeated level-by-level decisions (see §5).
- **Ascent** — one ascent hazard check on return to orbit.
- **Repeat or jump** — further expeditions are allowed; each costs Stamina and a fresh ascent check.
- **Jump** — pay the fuel cost, then roll the jump event table (§9).

### 2.3 Expeditions per planet

There is no hard limit on expeditions per planet. The limit is emergent: every expedition costs Stamina, and Stamina only recovers by resting, so a second or third trip means sending crew who are fatigued (ability disabled) or eventually ineligible. This is deliberate — the constraint is a resource the player manages, not a rule the game enforces.

---

## 3. Crew

Six crew members, each with a role, a matching ability, and three bars. Three may be sent on any expedition.

### 3.1 Roles, base stats and abilities

| Role | HP | Stam | Comp | Ability |
|---|---|---|---|---|
| Doctor | 90 | 90 | 100 | If a crew member loses Health mid-session, heal another expedition member for 20 Health once per session. |
| Surveyor | 95 | 100 | 90 | +7% item drop chance per drill level. |
| Engineer | 100 | 95 | 90 | Doubles Repair Kit drop weight; reduces Corrosive/Gas/Seal hazard severity by 1 (minimum 1). |
| Miner | 100 | 100 | 85 | +8% oil per drill level, compounding with depth. |
| Pilot | 90 | 95 | 100 | Negates ALL Hull damage from ascent hazards. Does not prevent crew injury or death. |
| Geologist | 85 | 90 | 100 | Displays true levels remaining in the vein, updated live each level. Auto-ejects at true bottom. |

**Note on the Pilot:** the Pilot's negation is balanced by scarcity, not by being weak. Because Stamina forces rotation, the Pilot cannot be on every expedition, so bringing them becomes a scheduling decision made several turns ahead. When the Pilot dies, state it plainly on screen — *"Ascent risk is now unmitigated."* The player must feel the run tilt rather than discovering it three planets later.

**Note on the Geologist:** they read depth, not yield. They know how many levels remain; they do not know which levels pay. On hitting an empty level they can say "3 levels remain" — worth continuing — but cannot promise the next one is not another barren pocket.

### 3.2 The three bars

All three bars run 0–100 and higher is always better. This is a deliberate change from the original concept, where the "fatigue" bar increased when resting. Calling it Stamina makes all three bars read the same direction, which matters enormously in a text interface where the player has no colour or icon cues.

**Health**
- Starts at role base. Lost to hazards.
- Recovers +10 per planet spent aboard (not on the expedition).
- First Aid Kit restores +40.
- Below 30: cannot join an expedition.
- Severity-6 toxic exposure permanently caps maximum Health at 70 for that crew member.

**Stamina**
- Expedition cost: −35. Rest recovery: +25 per planet spent aboard.
- Below 50: crew member is FATIGUED — their role ability does not fire.
- Below 25: cannot join an expedition (soft lock).
- Consequence of these numbers: a crew member can do one expedition at full effectiveness, a second while fatigued, and is locked out on the third until rested. The player is never forced into a fixed rotation, but is always paying something to break it.

**Composure**
- Lost to frightening hazards and to witnessing deaths (−25 to ALL crew per death).
- Recovers +8 per planet spent aboard. Sensory Toy +30, White Monster +15.
- Below 40: crew member is SHAKEN — eligible for the Crew Breakdown jump event.
- Below 25: cannot join an expedition.
- At 0: the crew member breaks. Roll: 50% they damage the ship (Hull −15%), 50% they are unfit for the remainder of the run.

### 3.3 Insufficient crew

If fewer than three crew are eligible, the expedition may still launch with two. With one or none, the planet cannot be drilled at all — the player must jump on whatever fuel they have. This is a common route into the stranded ending and should be signposted clearly when it becomes possible.

---

## 4. Ship and the Fuel Economy

The fuel economy is the spine of the game. Everything else — risk tiers, hazards, crew abilities — exists to make the fuel decision interesting.

### 4.1 Jump costs

| Jump | Cost | Cumulative |
|---|---|---|
| 1 | 100 | 100 |
| 2 | 125 | 225 |
| 3 | 150 | 375 |
| 4 | 175 | 550 |
| 5 | 200 | 750 |
| 6 | 225 | 975 |
| 7 | 250 | 1,225 |
| 8 (home) | 275 | 1,500 |

Total fuel required for a clean run: 1,500. Across 8 planets that is an average of 188 per planet, which sits between the expected yield of a Low site (~125) and a Medium site (~205). Playing safe every time strands you. That is intentional.

### 4.2 Tank capacity — the single most important number

**Tank capacity: 350.**

Without a cap, the optimal strategy is always "drill everything, bank it," and "should we skip this planet?" stops being a question. A cap of roughly two early jumps' worth means the player can bank enough to skip exactly one planet, and only in the first half of the run — by jump 7, two jumps cost 525 and cannot be pre-paid.

This produces a difficulty arc for free: the early game is forgiving and permits caution, the late game forces depth. If only one number is tuned during playtesting, it should be this one.

### 4.3 Hull and its effect on fuel

Hull runs 0–100%. Damage is taken from ascent hazards, some jump events, and Composure breaks. Hull is NOT a second death condition in normal play — the ultimate risk is death and being stranded, so hull should apply pressure rather than end runs directly.

| Hull remaining | Jump cost multiplier | Effective cost of jump 8 (275) |
|---|---|---|
| 100–76% | ×1.00 | 275 |
| 75–51% | ×1.08 | 297 |
| 50–26% | ×1.16 | 319 |
| 25–1% | ×1.24 | 341 |

- At 0% Hull the ship cannot jump. This is a stranded ending, not an explosion.
- Repair Kit restores 25%. The Engineer increases Repair Kit drop weight, and the Pilot prevents the damage in the first place — note the deliberate anti-synergy: bringing both wastes some of each. This is good. It pushes varied team composition.

### 4.4 Fuel visibility — mandatory

The next jump cost MUST be displayed before the player commits to a drill site. Without it, running dry feels like the game cheated; with it, every stranded ending is the player's own arithmetic. Suggested header, shown on the planet screen above the site list:

```
FUEL 84 / 350      NEXT JUMP 140  (×1.08 hull)  =  151      DEFICIT 67
```

---

## 5. Planets, Sites and Drilling

### 5.1 Planet archetypes

Each planet has an archetype that determines which hazard set applies underground. Universal hazards can occur anywhere and should always be mixed into every set.

| Archetype | Hazard set | Flavour |
|---|---|---|
| Frozen | Universal + Frozen | Ice shelves, cryo-vents, cold that gets into the seals. |
| Desert | Universal + Desert | Sand, subsidence, heat, nothing to shelter behind. |
| Volcanic | Universal + Volcanic | Hot rock, ashfall, ground that is not finished moving. |
| Toxic | Universal + Toxic | Gas veins, corrosive slurry, air that eats equipment. |
| Barren | Universal + Barren | Vacuum, no weather, and something that used to live here. |
| Storm | Universal + Storm | Permanent electrical weather; instruments cannot be trusted. |

### 5.2 Site tiers — variance scales with risk

This is the core content model. Risk tier does not simply mean "more danger for more oil" — it means more UNCERTAINTY. A low-risk vein is shallow and predictable. A high-risk vein might bottom out at level 2 and waste two dangerous rolls, or run to level 11 and save the entire run.

| Tier | Depth range | Oil/level | Pocket chance | Hazard trigger chance | Expected total |
|---|---|---|---|---|---|
| Low | 4–6 | 20–30 | ~10% | 5% + 3%/level (cap 40%) | ~125 |
| Medium | 3–8 | 30–45 | ~18% | 10% + 5%/level (cap 60%) | ~205 |
| High | 2–11 | 40–60 | ~25% | 16% + 7%/level (cap 75%) | ~325 |

Consequences worth noticing, all of which fall out of this table rather than needing extra code:

- A High site rolling depth 2 yields ~100 — less than a Low site — after the player has already eaten two of the most dangerous hazard rolls in the game. That bad beat is what makes the gamble real.
- A Low site's ~125 does not cover a jump past the early game, so pure caution still strands the player.
- The Geologist scales naturally: near-useless on Low sites where depth is predictable, run-defining on High sites.
- The floor guarantee lives at PLANET level, not site level. All three sites drilled to the bottom must clear one jump. A single low-risk site must not.

### 5.3 Vein generation and barren pockets

A vein is a list of per-level oil yields generated in full at site creation, not rolled level by level. Generating up front makes runs reproducible under a seed and makes the pocket rules unit-testable.

**Rules**

- Roll depth from the tier's range.
- Fill each level with a yield from the tier's oil-per-level range.
- Place barren pockets (yield exactly 0) at the tier's pocket chance.
- HARD RULE: pockets are exactly one level thick and never adjacent. Two empty levels in a row therefore always means bottom.
- HARD RULE: never place a pocket on the final level. The bottom always pays, so the deepest push feels like a reward.
- Pockets must yield exactly zero. A trickle would distinguish them from bottom and destroy the ambiguity.
- HARD RULE: the message printed for a barren pocket and the message printed for drilling past the bottom must be IDENTICAL, drawn from the same pool of flavour variants. If the wording differs by one word, players decode it immediately.

Why non-adjacency matters: it gives the player a learnable rule (one empty might be a pocket, two always means bottom), guarantees the ambiguity resolves within two levels so nobody grinds a dead vein forever, and prevents the two-empty ejection rule from ever cutting the player off above live oil.

Why pockets exist at all: without them, an empty level only ever occurs at the bottom, which makes "the drill grinds, nothing" an announcement rather than a decision. Pockets are what make the signal genuinely noisy.

### 5.4 The drill loop

At each level the player chooses to drill deeper or ascend. On choosing to drill:

1. The level's yield is added to the session haul (0 if barren).
2. An item drop is rolled — INDEPENDENT of yield, so a pocket can still produce a First Aid Kit. This softens an empty level without leaking information about position in the vein.
3. A hazard trigger is rolled at the tier's chance for that depth.
4. If triggered: pick a hazard from the planet's set, roll severity on the depth-banded weights, apply the outcome.

**Drilling past the bottom still rolls the hazard.** This is the entire punishment for greed and it is easy to remove by accident with an early return. After two consecutive empty levels the site force-closes. If the Geologist is on the expedition, the session auto-ejects at the true bottom instead — that reads as the ability working, not as the game being lenient.

### 5.5 Item drops

Base drop chance 8% per level, +7% with a Surveyor present. Drop weights: Repair Kit 25 (50 with an Engineer), Caffeine 20, First Aid Kit 20, Sensory Toy 15, Condensed Oil 12, White Monster 8.

---

## 6. The Severity Contract

Every hazard in the game — drilling, ascent and jump — resolves on the same 1–6 scale, and each number means the same thing everywhere. Without this, a player cannot build intuition about risk, and the code ends up with fifteen bespoke outcome structures.

| Roll | Meaning |
|---|---|
| 1 | Flavour only. No mechanical effect, or trivial Composure loss. |
| 2 | Small resource loss (fuel, oil, or a modest hull knock). |
| 3 | Significant resource loss, or the site/opportunity closes early. |
| 4 | Stat damage — Health or Composure to one or two crew. |
| 5 | Serious injury plus resource loss. Multiple crew affected. |
| 6 | Death or a permanent consequence. |

### 6.1 Severity weighting by depth

Severity is not rolled evenly. Low numbers dominate near the surface and the distribution shifts as the player descends. This is what makes depth feel dangerous even when the trigger chance is capped.

| Depth band | 1   | 2   | 3   | 4   | 5   | 6   |
| ---------- | --- | --- | --- | --- | --- | --- |
| Levels 1–3 | 40% | 28% | 18% | 9%  | 4%  | 1%  |
| Levels 4–6 | 28% | 26% | 22% | 14% | 7%  | 3%  |
| Levels 7–9 | 18% | 22% | 24% | 19% | 11% | 6%  |
| Levels 10+ | 10% | 16% | 22% | 24% | 18% | 10% |

For ascent and jump events, use the Levels 4–6 band as the default, or the band matching the deepest level reached that planet if you want ascent risk to reflect how hard the crew pushed.

### 6.2 Two dials, kept separate

**Trigger chance controls pacing. Severity weighting controls lethality.** Keep these separate during tuning. If the game feels too deadly, change the severity weights, not the trigger chances — lowering trigger chance makes the game feel empty rather than fair. Rough expectation with these numbers is around 0.7 crew deaths per run from drilling alone, which is the target: death is a real threat but not a formality.

---

## 7. Hazard Tables — Drilling

Fifteen hazards. Three are universal and occur on any planet; the remainder belong to archetype sets. All follow the severity contract in §6. Bracketed text is the mechanical effect; the rest is display flavour and belongs in `hazards.txt`.

### 7.1 Universal hazards

**Cave Collapse**

| Roll | Outcome                                                                                                                                   |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | Loose scree rattles down the shaft. Everyone freezes; nothing follows. [No mechanical effect. All on site −2 Composure.]                  |
| 2    | A support strut buckles and the drill head is fouled. [−15% of oil collected this session.]                                               |
| 3    | Half the gallery comes down. The drill must be cut free. [−30% oil this session. Site closed after this level.]                           |
| 4    | A slab takes a crewmate across the back. [Random crew member −25 Health. All on site −10 Composure.]                                      |
| 5    | A full section gives way. Two are pinned and dug out badly hurt. [Two random crew −40 Health. All on site −20 Composure. −25% oil.]       |
| 6    | The roof goes. One crew member does not come out. [1 random crew member dies. Survivors −25 Health. ALL crew −25 Composure. Site closed.] |

**Drill Malfunction / Fire**

| Roll | Outcome |
|---|---|
| 1 | The bit screams and glazes over. You back it off in time. [No effect.] |
| 2 | Friction fire in the coupling burns off part of the drum. [−40 fuel.] |
| 3 | The fuel drum catches. You smother it, but most of the haul is gone. [−60% oil this session.] |
| 4 | Burning slurry sprays the platform. [Random crew −30 Health. −40 fuel.] |
| 5 | The drum goes up and someone is caught by the blast. [Random crew −50 Health. All oil this session lost. All on site −15 Composure.] |
| 6 | A crewmate falls into the burning fuel. [1 random crew member dies. All oil this session lost. ALL crew −30 Composure.] |

**Shaft Instability**

| Roll | Outcome |
|---|---|
| 1 | The casing groans on every rotation. Unnerving, nothing more. [All on site −5 Composure.] |
| 2 | Casing slips and the drill jams. Time lost clearing it. [−10% oil this session.] |
| 3 | The upper shaft partially collapses. You keep the haul but not the site. [Site closed. Oil retained.] |
| 4 | The winch cable parts and two crew drop into the shoring. [Two random crew −20 Health.] |
| 5 | The platform tilts and slides. One crew is crushed against the rig. [Random crew −55 Health. −20% oil. All on site −15 Composure.] |
| 6 | The shaft fails completely with crew below the collar. [1 random crew member dies. Site closed, all oil this session lost. ALL crew −25 Composure.] |

### Set: Frozen

**Ice Fissure**

| Roll | Outcome |
|---|---|
| 1 | A crack races out from the borehole and stops. [No effect.] |
| 2 | The pad splits and a fuel canister goes into the gap. [−35 fuel.] |
| 3 | A fissure swallows part of the rig. Recovery is slow and cold. [−25% oil. Site closed.] |
| 4 | A crew member goes through the crust to the waist. [Random crew −30 Health, −15 Composure.] |
| 5 | Two go through. You get them back up hypothermic. [Two random crew −40 Health. All on site −15 Composure.] |
| 6 | The shelf calves away. One crew member falls beyond reach and the channel closes. [1 random crew member dies. ALL crew −30 Composure. Site closed.] |

**Cryo-Vent Rupture**

| Roll | Outcome |
|---|---|
| 1 | A jet of vapour vents harmlessly downwind. [No effect.] |
| 2 | Frost fouls the pumps and you lose pressure. [−30 fuel.] |
| 3 | The vent ices the rig solid. Thawing costs you the rest of the seam. [−40% oil. Site closed.] |
| 4 | Supercooled gas catches a suit seam. [Random crew −35 Health.] |
| 5 | The vent blows through the platform. Two suits are compromised. [Two random crew −45 Health. −20% oil.] |
| 6 | A crewmate is caught full in the plume. [1 random crew member dies. Survivors on site −20 Health. ALL crew −25 Composure.] |

### Set: Desert

**Sandstorm**

| Roll | Outcome |
|---|---|
| 1 | Visibility drops for a few minutes, then clears. [All on site −5 Composure.] |
| 2 | Grit in the intakes. You purge the lines and lose product. [−15% oil this session.] |
| 3 | The storm settles in. You shut down and wait it out. [Site closed. Oil retained. All on site −10 Composure.] |
| 4 | Abrasion tears two suits at the shoulder. [Two random crew −20 Health.] |
| 5 | The rig is stripped and a crew member is thrown into the mast. [Random crew −45 Health. −30% oil. Site closed.] |
| 6 | The storm buries the site with crew still on the pad. [1 random crew member dies. Site closed, oil this session lost. ALL crew −25 Composure.] |

**Sinkhole**

| Roll | Outcome |
|---|---|
| 1 | The ground dishes slightly under the rig. You reposition. [No effect.] |
| 2 | A leg sinks and a drum tips. [−40 fuel.] |
| 3 | The pad subsides. The drill is recoverable, the site is not. [Site closed. −15% oil.] |
| 4 | A crew member drops into a void and is hauled out. [Random crew −30 Health, −20 Composure.] |
| 5 | The pad collapses and the rig goes with it. [Two random crew −35 Health. −50% oil. Site closed.] |
| 6 | The void opens under the whole platform. [1 random crew member dies. All oil this session lost. ALL crew −30 Composure.] |

### Set: Volcanic

**Magma Breach**

| Roll | Outcome |
|---|---|
| 1 | The bit hits hot rock and the readouts spike. You pull back. [No effect.] |
| 2 | Heat warps the string. Cutting a new one costs you product. [−20% oil.] |
| 3 | Molten rock enters the shaft. The site seals itself off. [Site closed. −25% oil.] |
| 4 | Radiant heat cooks a crew member through their suit. [Random crew −35 Health.] |
| 5 | A spatter cone opens beside the pad. [Two random crew −40 Health. −40% oil. Site closed.] |
| 6 | The floor gives out over the chamber. [1 random crew member dies. All oil this session lost. ALL crew −30 Composure.] |

**Ashfall**

| Roll | Outcome |
|---|---|
| 1 | Fine ash settles over everything. Cosmetic. [No effect.] |
| 2 | Ash clogs the separators. [−15% oil.] |
| 3 | The fall thickens and you cannot see the pad. [Site closed. Oil retained.] |
| 4 | Hot ash gets into two rebreathers. [Two random crew −25 Health.] |
| 5 | A pyroclastic surge clips the ridge above the site. [Random crew −50 Health. All on site −20 Composure. Site closed.] |
| 6 | The surge reaches the pad. [1 random crew member dies, survivors −30 Health. Site closed. ALL crew −25 Composure.] |

### Set: Toxic

**Toxic Gas Vein**

| Roll | Outcome |
|---|---|
| 1 | A pocket of gas vents and disperses. Alarms only. [All on site −5 Composure.] |
| 2 | You cap it, but the seam is contaminated. [−20% oil.] |
| 3 | The vein floods the shaft. You seal the site behind you. [Site closed. −15% oil.] |
| 4 | A filter fails and someone takes a lungful. [Random crew −30 Health.] |
| 5 | The gas outruns the alarms. Everyone on site is dosed. [All on site −35 Health, −15 Composure.] |
| 6 | It chokes out the shaft. Only those nearest the ladder make it. [1 random crew member dies. Survivors −40 Health AND their maximum Health is permanently capped at 70. ALL crew −30 Composure.] |

**Corrosive Seep**

| Roll | Outcome |
|---|---|
| 1 | The slurry eats the paint off the drill housing. [No effect.] |
| 2 | Seals degrade and a drum weeps. [−35 fuel.] |
| 3 | The seep destroys the pump assembly. [Site closed. −20% oil.] |
| 4 | A glove fails on contact. [Random crew −25 Health, −10 Composure.] |
| 5 | The pad floods ankle-deep before anyone notices. [Two random crew −40 Health. −30% oil.] |
| 6 | A suit fails completely in the seep. [1 random crew member dies. Site closed. ALL crew −25 Composure.] |

### Set: Barren

**Alien Nest**

| Roll | Outcome |
|---|---|
| 1 | Something moves at the edge of the lamps and does not come closer. [All on site −10 Composure.] |
| 2 | The rig is swarmed overnight and cabling is stripped. [−25% oil, −30 fuel.] |
| 3 | The nest wakes properly. You withdraw while you can. [Site closed. Oil retained. All on site −15 Composure.] |
| 4 | Something gets inside a suit. [Random crew −30 Health, −20 Composure.] |
| 5 | A coordinated attack on the pad. [Two random crew −40 Health. All on site −25 Composure. −20% oil.] |
| 6 | They take one of your crew into the dark. [1 random crew member dies (no body recovered). ALL crew −35 Composure.] |

**Vacuum Seal Failure**

| Roll | Outcome |
|---|---|
| 1 | A seal warning clears itself. [No effect.] |
| 2 | Pressure loss in the sample lock spoils part of the haul. [−15% oil.] |
| 3 | The airlock fails and the site must be abandoned. [Site closed. −10% oil.] |
| 4 | A helmet ring cracks and is patched under pressure. [Random crew −35 Health, −15 Composure.] |
| 5 | Two suits vent at once. [Two random crew −45 Health. All on site −15 Composure.] |
| 6 | A suit fails catastrophically in vacuum. [1 random crew member dies. ALL crew −30 Composure.] |

### Set: Storm

**Lightning Strike**

| Roll | Outcome |
|---|---|
| 1 | The mast draws a discharge and shrugs it off. [No effect.] |
| 2 | The strike wipes the pump controller. [−30 fuel.] |
| 3 | The rig electronics are dead. Nothing more comes out of this hole. [Site closed. Oil retained.] |
| 4 | Ground current catches a crew member through the pad. [Random crew −35 Health.] |
| 5 | A strike walks across the platform. [Two random crew −40 Health. −25% oil. Site closed.] |
| 6 | A direct hit on a crew member. [1 random crew member dies. Site closed. ALL crew −25 Composure.] |

**EM Surge**

| Roll | Outcome |
|---|---|
| 1 | Instruments scramble briefly and re-sync. [No effect.] |
| 2 | The surge corrupts the survey data. [Geologist ability disabled for the rest of this session. −10% oil.] |
| 3 | Every powered tool on the pad browns out. [Site closed. Oil retained.] |
| 4 | Suit systems fail and two crew are blind for several minutes. [Two random crew −15 Health, −25 Composure.] |
| 5 | Life support cycles hard on every suit at once. [All on site −35 Health.] |
| 6 | A suit's regulator locks closed and cannot be cleared in time. [1 random crew member dies. ALL crew −25 Composure.] |

### 7.x Expansion stubs

Additional hazards to write if content runs thin, following the same contract: Whiteout (Frozen), Heat Exhaustion (Desert), Tremor (Volcanic), Spore Bloom (Toxic), Ancient Structure (Barren — can be net positive at low rolls), Ion Squall (Storm), Equipment Theft (Barren), Flash Flood (Toxic).

---

## 8. Hazard Tables — Ascent

One ascent check occurs per expedition on return to orbit. Base trigger chance 20%, +8% for each additional expedition already flown at this planet (crew and shuttle are progressively more worn).

**Pilot interaction:** the Pilot negates ALL Hull damage from ascent hazards. They do NOT prevent crew injury, oil loss, fuel loss or death. Strip only the hull component from the outcome.

**Debris Field**

| Roll | Outcome |
|---|---|
| 1 | Light particulate rattles the hull. [No effect.] |
| 2 | Minor plating damage. [Hull −5%.] |
| 3 | A larger fragment holes a cargo bay. [Hull −12%, −40 fuel.] |
| 4 | The shuttle is punched through near the seats. [Hull −18%. Random crew −25 Health.] |
| 5 | A sustained impact spread cracks the frame. [Hull −28%. Two random crew −30 Health. All crew −15 Composure.] |
| 6 | Something comes through the cabin and the air goes with it. [Hull −35%. 1 random crew member dies. ALL crew −30 Composure.] |

**Launch System Failure**

| Roll | Outcome |
|---|---|
| 1 | A thruster runs rough and settles. [No effect.] |
| 2 | Uneven burn wastes propellant. [−50 fuel.] |
| 3 | A thruster cuts out and the ascent is dumped and reflown. [−80 fuel. Hull −8%.] |
| 4 | Hard gimbal correction throws the cabin. [Hull −15%. Random crew −20 Health.] |
| 5 | Partial engine failure. You reach orbit barely and hot. [Hull −25%. Two random crew −30 Health. −60 fuel.] |
| 6 | The ascent stage fails and the shuttle is recovered wrecked. [Hull −35%. 1 random crew member dies. All oil aboard this shuttle lost.] |

**Gravity Well Snag**

| Roll | Outcome |
|---|---|
| 1 | A longer burn than planned. [No effect.] |
| 2 | Poor trajectory costs propellant. [−45 fuel.] |
| 3 | You are pulled into a decaying arc and must fight out. [−75 fuel. Hull −10%.] |
| 4 | Sustained high-g on the correction burn. [All on site crew −20 Health.] |
| 5 | The shuttle is dragged through the upper atmosphere. [Hull −30%. Two random crew −30 Health. −15 Composure all crew.] |
| 6 | The burn is not enough. Mass is jettisoned, including a crew pod. [1 random crew member dies. Hull −20%. ALL crew −30 Composure.] |

**Docking Failure**

| Roll | Outcome |
|---|---|
| 1 | A clumsy but harmless approach. [No effect.] |
| 2 | Contact damage to the collar. [Hull −6%.] |
| 3 | The clamp will not seat and fuel is burned holding station. [−55 fuel. Hull −8%.] |
| 4 | The shuttle strikes the ring hard. [Hull −18%. Random crew −25 Health.] |
| 5 | The collar tears and the transfer tunnel vents. [Hull −25%. Two random crew −35 Health.] |
| 6 | The shuttle strikes the ship and breaks up on the tunnel. [Hull −30%. 1 random crew member dies. ALL crew −30 Composure.] |

---

## 9. Opportunity Events — During the Jump

Jump events are distinct from hazards. They can be positive, negative or a genuine choice, and they are the main source of story variety across a run. There are only 8 jumps, so events should be memorable rather than incremental.

**Event occurrence chance: 60% per jump.** The remaining 40% is an uneventful crossing with short flavour text — do not skip the text, because silence reads as a bug in a text interface. When an event occurs, select from the weighted table below, then roll 1–6 for the outcome using the Levels 4–6 severity band.

### 9.1 Event weights

| Event | Weight | Type |
|---|---|---|
| Alien Trade Vessel | 18% | Mixed |
| Derelict Freighter | 15% | Mixed |
| Distress Beacon | 12% | Mixed |
| Abandoned Fuel Depot | 10% | Positive |
| Micrometeoroid Swarm | 12% | Negative |
| Navigational Drift | 10% | Negative |
| Crew Breakdown | 8% | Negative |
| Quiet Passage | 10% | Positive |
| Salvager Ambush | 5% | Negative |

Weights total 100%. Three positive, four negative and two genuinely mixed events, plus one conditional. Roughly 35% of events are net-positive, which is deliberate: the jump is the only place in the game where good things happen unprompted, and it needs to be a moment of hope between planets.

### 9.2 Event tables

**Alien Trade Vessel — 18% · Mixed**

| Roll | Outcome |
|---|---|
| 1 | They are generous. [+140 fuel, +2 random items.] |
| 2 | A fair trade for scrap. [+100 fuel.] |
| 3 | They want fuel for repairs and pay in supplies. [−60 fuel, +3 random items.] |
| 4 | They offer a great deal for one of your crew. [PLAYER CHOICE: +260 fuel and lose 1 chosen crew member, or decline for nothing.] |
| 5 | The deal sours halfway through. [−80 fuel. Two random crew −25 Health.] |
| 6 | It was bait. A boarding fight breaks out. [Outcome scales with number of healthy crew: 5–6 healthy → hull −15%, all crew −25 Health. 3–4 healthy → hull −25%, 1 crew dies. ≤2 healthy → hull −35%, 2 crew die. ALL crew −30 Composure.] |

**Derelict Freighter — 15% · Mixed**

| Roll | Outcome |
|---|---|
| 1 | Picked clean, but the galley is intact. [+1 random item.] |
| 2 | Usable salvage in the hold. [+80 fuel.] |
| 3 | A full bunker still pressurised. [+160 fuel.] |
| 4 | Salvage plus a working repair bay. [+90 fuel, hull +20%.] |
| 5 | The reactor is unstable and you leave in a hurry. [Hull −15%, all crew −15 Composure.] |
| 6 | Whatever killed the crew is still aboard. [1 random crew member dies. ALL crew −30 Composure.] |

**Distress Beacon — 12% · Mixed**

| Roll | Outcome |
|---|---|
| 1 | An automated relic. Nothing there. [No effect.] |
| 2 | Survivors, grateful, with little to give. [+1 random item. All crew +10 Composure.] |
| 3 | A stranded prospector trades knowledge for passage. [Next planet's site risks revealed exactly before choosing.] |
| 4 | They need fuel more than you do. [PLAYER CHOICE: give 80 fuel for +2 items and all crew +15 Composure, or refuse for all crew −10 Composure.] |
| 5 | A trap dressed as a rescue. [−90 fuel, hull −12%.] |
| 6 | Ambush. [Hull −25%, 1 random crew member dies, ALL crew −25 Composure.] |

**Abandoned Fuel Depot — 10% · Positive**

| Roll | Outcome |
|---|---|
| 1 | Dry tanks, but the maintenance locker is stocked. [+2 random items.] |
| 2 | Residue worth scraping. [+70 fuel.] |
| 3 | A partial reserve. [+130 fuel.] |
| 4 | A full reserve. [+200 fuel.] |
| 5 | A full reserve and a working repair rig. [+200 fuel, hull +25%.] |
| 6 | Intact station stores. [+240 fuel, +3 random items, all crew +20 Composure.] |

**Micrometeoroid Swarm — 12% · Negative**

| Roll | Outcome |
|---|---|
| 1 | A light peppering. [No effect.] |
| 2 | Sensor damage on the forward array. [Next planet scan shows oil amounts but not risk tiers.] |
| 3 | Tank puncture. [−80 fuel, hull −8%.] |
| 4 | Multiple penetrations through the crew deck. [Hull −18%. Random crew −30 Health.] |
| 5 | A dense pass shreds the port side. [Hull −28%, −60 fuel, two random crew −30 Health.] |
| 6 | A hull breach in the sleeping quarters. [Hull −30%. 1 random crew member dies. ALL crew −25 Composure.] |

**Navigational Drift — 10% · Negative**

| Roll | Outcome |
|---|---|
| 1 | A minor correction. [No effect.] |
| 2 | The correction burn costs you. [−50 fuel.] |
| 3 | You arrive well off-target. [−90 fuel.] |
| 4 | A long detour with rationing aboard. [−70 fuel. All crew −15 Stamina.] |
| 5 | Weeks off course. [−120 fuel. All crew −20 Stamina, −15 Composure.] |
| 6 | You emerge somewhere unmapped and burn hard to recover. [−180 fuel. All crew −25 Stamina, −25 Composure.] |

**Crew Breakdown — 8% · Negative**

*Only rolls if at least one crew member is below 40 Composure. Otherwise re-roll the event table.*

| Roll | Outcome |
|---|---|
| 1 | An argument in the mess, quickly over. [All crew −5 Composure.] |
| 2 | Someone stops speaking to the rest. [Lowest-Composure crew −15 Stamina.] |
| 3 | Rations are hoarded and hidden. [−40 fuel equivalent in supplies. All crew −10 Composure.] |
| 4 | A crew member wrecks equipment in the night. [Hull −12%, −1 random item.] |
| 5 | A crew member vents a compartment before being restrained. [Hull −20%. Two random crew −30 Health.] |
| 6 | A crew member takes an airlock alone. [That crew member dies. ALL crew −30 Composure.] |

**Quiet Passage — 10% · Positive**

| Roll | Outcome |
|---|---|
| 1 | Nothing at all, which is its own relief. [All crew +5 Composure.] |
| 2 | Long shifts of rest. [All crew +15 Stamina.] |
| 3 | Repairs get done properly for once. [Hull +15%.] |
| 4 | Rest and a thorough overhaul. [All crew +20 Stamina, hull +10%.] |
| 5 | The nebula outside is the most beautiful thing anyone has seen. [All crew +25 Composure, +15 Stamina.] |
| 6 | A genuinely good stretch. [All crew +25 Stamina, +25 Composure, +20 Health.] |

**Salvager Ambush — 5% · Negative**

| Roll | Outcome |
|---|---|
| 1 | They scan you, judge you not worth it, and leave. [All crew −10 Composure.] |
| 2 | They demand a toll. [−70 fuel.] |
| 3 | They take fuel and cargo. [−110 fuel, −2 random items.] |
| 4 | They board briefly and take what they like. [−140 fuel, −2 items, random crew −30 Health.] |
| 5 | A running fight. [Hull −25%, −100 fuel, two random crew −35 Health.] |
| 6 | They cripple you and strip the hold. [Hull −35%, −200 fuel, 1 random crew member dies, ALL crew −30 Composure.] |

---

## 10. Items

| Item | Target | Effect | Rarity | Notes |
|---|---|---|---|---|
| Repair Kit | Ship | Restores 25% Hull. | Common | Engineer doubles its drop weight. |
| Caffeine | Crew | +20 Stamina to one crew member. | Common | — |
| White Monster | Crew | +35 Stamina and +15 Composure to one crew member. | Uncommon | Strictly better than Caffeine; keep it rarer. |
| Condensed Oil | Fuel | +60 fuel immediately. | Uncommon | Ignores tank cap on the turn it is used. |
| First Aid Kit | Crew | +40 Health to one crew member. | Common | Cannot exceed a permanently capped maximum. |
| Sensory Toy | Crew | +30 Composure to one crew member. | Common | — |

- Inventory cap: 8 slots. Finding a 9th item forces a discard choice. This is optional but recommended — it turns a pure positive into a small decision.
- Items may be used at any point between drill levels, during crew selection, or before a jump.
- First Aid cannot exceed a permanently capped maximum Health (see §3.2).

---

## 11. Endings

There are five terminal states. None of them should be a print statement at the point of failure — model an Ending object with a reason code, load the narrative text from `endings.txt`, and have the engine resolve exactly one ending at the end of the run.

| Ending | Trigger | Tone |
|---|---|---|
| Home | Final jump completed with sufficient fuel. | Earned, exhausted, not triumphant. Name the dead. |
| Stranded on planet | Crew on the surface, no fuel to reach orbit, no oil left to take. | Split narrative: those on the surface, and those on the ship who watch and then wait. |
| Adrift | Aboard the ship between planets with insufficient fuel to jump. | Slow. Rationing, then silence. The worst ending and it should feel like it. |
| Hull failure | Hull reaches 0%. Cannot jump. | Mechanical and sudden. A ship that stops being a ship. |
| Crew lost | Fewer than one crew member alive. | Short. Nobody left to make a decision. |

**Handle the general case for split endings.** The original concept described "3 die on the planet and 3 die in space," but by the time a run fails there may be four crew alive, or two. Model it as whoever was on the surface versus whoever was aboard, which the engine already tracks. Never hard-code the count.

---

## 12. Master Tuning Table

Every number the game depends on, in one place. These belong in a config file, not in Java constants — balancing by recompiling is a waste of the file I/O layer you already have to build.

| Constant | Value | Notes |
|---|---|---|
| PLANET_POOL_SIZE | 12 | Entries in planets.txt. |
| PLANETS_PER_RUN | 8 | Drawn at run start; order persisted to save. |
| JUMP_COST_BASE | 100 | Cost of jump 1. |
| JUMP_COST_INCREMENT | 25 | Added per subsequent jump. |
| TANK_CAPACITY | 350 | PRIMARY BALANCE DIAL. Controls whether skipping is possible. |
| HULL_MAX | 100 | Percent. |
| HULL_FUEL_PENALTY_PER_BAND | 8% | Applied per 25% hull lost. |
| CREW_COUNT | 6 | — |
| EXPEDITION_SLOTS | 3 | Minimum 2 if fewer are eligible. |
| STAMINA_EXPEDITION_COST | 35 | — |
| STAMINA_REST_RECOVERY | 25 | Per planet spent aboard. |
| STAMINA_FATIGUE_THRESHOLD | 50 | Below this, role ability does not fire. |
| STAMINA_MIN_ELIGIBLE | 25 | Soft lock. |
| HEALTH_REST_RECOVERY | 10 | Per planet spent aboard. |
| HEALTH_MIN_ELIGIBLE | 30 | — |
| COMPOSURE_REST_RECOVERY | 8 | Per planet spent aboard. |
| COMPOSURE_DEATH_PENALTY | 25 | Applied to ALL living crew per death. |
| COMPOSURE_SHAKEN_THRESHOLD | 40 | Enables Crew Breakdown event. |
| COMPOSURE_MIN_ELIGIBLE | 25 | — |
| ITEM_DROP_BASE | 8% | Per drill level. |
| ITEM_DROP_SURVEYOR_BONUS | 7% | Additive. |
| ASCENT_TRIGGER_BASE | 20% | Per expedition. |
| ASCENT_TRIGGER_PER_EXTRA_TRIP | 8% | Additive per additional expedition at this planet. |
| JUMP_EVENT_CHANCE | 60% | Otherwise an uneventful crossing (still print flavour). |
| EMPTY_LEVELS_BEFORE_CLOSE | 2 | Force-close after two consecutive barren levels. |
| INVENTORY_SLOTS | 8 | Optional. |
| MINER_OIL_BONUS | 8% | Per level, compounding. |

### 12.1 Design couplings — change one, you change two

Three places where a single dial silently controls two things. Write these on the wall.

| Dial | Obvious effect | Hidden effect |
|---|---|---|
| Vein depth variance | Pacing — how long a drill session runs. | The Geologist's power. If depth becomes predictable the ability is dead weight; if it is wild the ability is run-defining. |
| Stamina cost / recovery | Rotation pressure and pacing. | The Pilot's balance. Scarcity is what stops ascent-damage negation being an auto-include. Make Stamina generous and the Pilot becomes always-available. |
| Tank capacity | How much fuel can be banked. | Whether skipping a planet is ever a real option, which is the game's second-biggest decision after drill depth. |

---

## 13. Technical Design

### 13.1 The role ability problem

The six role abilities fire at six different moments: Geologist during the scan and drill loop, Miner during yield calculation, Surveyor and Engineer during item rolls, Doctor after health loss, Pilot during ascent. A single overridden `useAbility()` on an abstract `CrewMember` cannot be called meaningfully from any one place, and attempting it leads to `instanceof` checks scattered through every system — which breaks polymorphism and the Open/Closed Principle at once.

The solution is small interfaces per hook, implemented only by the roles that need them:

```java
public interface DrillModifier  { DrillYield modifyYield(DrillYield base, int depth); }
public interface AscentModifier { HazardOutcome modifyAscent(HazardOutcome base); }
public interface ItemFinder    { double modifyDropChance(double base); }
public interface VeinInsight   { String describeRemaining(int drillsLeft); }
public interface FieldMedic    { void treat(List<CrewMember> team); }
```

Each system asks whether any expedition member implements the relevant interface and lets them transform the result. The system never learns which role it is talking to. Adding a seventh role touches zero existing code. This demonstrates polymorphism, Interface Segregation and Open/Closed simultaneously — three things the marking guide names explicitly.

**Fatigue interaction:** the check for FATIGUED status happens once, where the hooks are collected. A fatigued crew member is simply not included in the list of modifiers. No system needs to know about Stamina.

### 13.2 Vein representation

Generate the whole vein up front rather than rolling per level. This makes runs deterministic under a seed (essential for the demo video) and makes the pocket placement rules directly unit-testable by inspecting the list.

```java
public class Vein {
    private final List<Integer> levels;   // oil per level; 0 == barren pocket
    private int drilled = 0;

    public int drillNext() {                    // caller handles the hazard roll
        if (drilled >= levels.size()) return 0; // past bottom
        return levels.get(drilled++);
    }

    public int levelsRemaining() {              // Geologist only
        return Math.max(0, levels.size() - drilled);
    }
}
```

### 13.3 Proposed class list

Guiding rule: inheritance where behaviour differs, data files where only numbers differ. Hazards differ only in text and effect values, so they are ONE class driven by a table — fifteen hazard subclasses would be padding, and the marking guide asks for reasonable classes, not merely many.

| Group | Classes |
|---|---|
| Crew | CrewMember (abstract), Doctor, Surveyor, Engineer, Miner, Pilot, Geologist |
| Hooks | DrillModifier, AscentModifier, ItemFinder, VeinInsight, FieldMedic (interfaces) |
| World | Planet, DrillSite, Vein, PlanetArchetype |
| Expedition | Expedition, DrillSession, AscentPhase |
| Hazards | Hazard, HazardOutcome (record), SeverityRoller, WeightedRoller |
| Events | JumpEvent, EventOutcome (record), EventTable |
| Items | Item (abstract), RepairKit, Stimulant, MedKit, ComfortItem, FuelItem, Inventory |
| Ship | Ship, FuelEconomy |
| Engine | GameEngine, RunState, Ending |
| I/O | GameDataSource (interface), TextFileDataSource, SaveManager, RunLogWriter |
| UI | ConsoleUI, InputValidator, ScreenRenderer |
| Exceptions | InvalidInputException, DataLoadException, IneligibleCrewException |

**GameDataSource as an interface is deliberate.** If Project 2 requires a database or a GUI, swapping the implementation is a one-class change rather than a rewrite. That is Dependency Inversion and it costs almost nothing to put in now.

### 13.4 Data files

| File | Direction | Contents |
|---|---|---|
| planets.txt | Read | 12 planets: name, archetype, intro flavour, site name pool. |
| sites.txt | Read | Tier definitions: depth range, oil/level, pocket chance, trigger curve. |
| hazards.txt | Read | Hazard name, set, and six outcome rows (text + effect codes). |
| events.txt | Read | Jump events: name, weight, six outcome rows. |
| items.txt | Read | Item definitions, effects, drop weights. |
| crew.txt | Read | Role base stats and ability parameters. |
| endings.txt | Read | Narrative text per ending reason code. |
| save.txt | Write | Full run state including drawn planet order and RNG seed. |
| runlog.txt | Write | Ship's log — appended each planet. Doubles as the in-fiction record. |
| scores.txt | Write | Completed runs: planets reached, crew survived, ending reached. |

Seven reads and three writes, all of which contribute to functionality rather than existing to satisfy a requirement. Route every one of them through the I/O layer — the marking guide explicitly penalises file operations scattered through the program.

### 13.5 Determinism and the demo

- All randomness goes through one seeded `Random` owned by `RunState`. No `new Random()` anywhere else.
- The seed is written to the save file, so loading reproduces the run exactly.
- Keep a seed entry option in the FINAL build — a hidden menu key or a launch argument. It costs nothing, markers will not find it, and it lets you rehearse the video and reliably demonstrate a severity-6 hazard on camera.
- A full run is 8 planets and 40+ drill levels. Budget the demo video around one planet shown in full plus a scripted jump event, not a complete playthrough.

### 13.6 CUI requirements

- Every prompt routes through one input helper. Single place for validation, single place for the quit and back options.
- The marking guide requires the player be able to quit at any step without an error. Support Q and B at every prompt, including mid-drill.
- Ineligible crew must be listed with the REASON, not hidden. "Reyes (Pilot) — FATIGUED, ability unavailable" and "Okonjo (Miner) — Health 24, cannot deploy."
- Soft costs need loud signalling. A fatigued crew member's disabled ability must be stated at selection, at deployment, and at the moment it would have fired.

### 13.7 Work split for two people

Agree the interfaces and record types FIRST — method signatures with no bodies — then work in parallel. Skipping this step means spending week 6 merging instead of building.

| Owner | Area |
|---|---|
| Together, first session | GameEngine skeleton, ConsoleUI, GameDataSource + file formats, all hook interfaces and records. |
| Person A | CrewMember hierarchy and roles, the three stat systems, Hazard + SeverityRoller + WeightedRoller, ascent phase. |
| Person B | Planet / DrillSite / Vein generation, DrillSession, Item hierarchy and Inventory, Ship and FuelEconomy, jump events. |

- Use Git from day one, both committing under your own accounts. The commit history is the evidence for the contribution requirement, and losing code is not grounds for special consideration.
- Do not split by role — one person coding and one writing the report fails the 40% contribution rule outright.
- Submissions are checked with Codequiry. Do not lift a roguelike scaffold from GitHub even as a reference.
- Keep the AI interaction log as you go. Reconstructing it in week 7 produces exactly the vague log the rubric marks down.

### 13.8 Open questions

- Is the 8th planet fixed and scripted, or drawn like the rest?
- Can the player abandon an expedition mid-drill and keep the haul, or is ascent always a hazard check?
- Does the Doctor's heal fire automatically or is it a player-triggered choice?
- Should a permanently capped Health crew member be visibly marked for the rest of the run? (Recommended: yes.)
- Is there any way to recover a dead crew member's role, or is losing the Pilot permanent? (Recommended: permanent — it is the run tilting, and that is the story.)

---

*Long Way Home — GDD v1.0*
