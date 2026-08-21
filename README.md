# Wilderness Loadouts

Wilderness Loadouts is a RuneLite Plugin Hub plugin that finds the strongest defensive equipment combination in gear you own while respecting a simple filler-risk budget.

## Features

- Optimizes for Overall, Magic, Ranged, or Melee Defence.
- Scans equippable gear from the bank and currently worn equipment.
- Supports three or four protected/core items and RuneScape-style budgets such as `500k` or `1.5m`.
- Allows every equipment slot to be Auto, Locked to an owned item, or Empty.
- Enforces two-handed weapon and shield compatibility.
- Shows owned alternatives and their defensive stats, objective score, and approximate price.
- Creates a virtual Bank Tags layout without moving real bank positions.

## Risk model

Protected/core items are assumed protected for loadout planning. All other selected items count toward the filler-risk budget using RuneLite's current item price. This is an estimate, not a prediction of the items kept or lost on an actual Wilderness death.

The first version intentionally does not model skull state, Protect Item prayer state beyond choosing three or four core items, Trouver parchment, degradation, charges, repair values, or special death/reclaim rules. Unpriced selected items are visibly flagged because displayed risk may be incomplete.

## Usage

1. Open your bank once per RuneLite session so the plugin can scan owned gear.
2. Open the **Wilderness Loadouts** sidebar panel.
3. Choose a defensive focus, protected/core item count, filler-risk budget, and enabled slots.
4. Select **Calculate**.
5. Select an equipment slot to lock an alternative, return it to Auto, or force it Empty.
6. Open the bank and select **Show in Bank** to view the generated virtual layout. The core Bank Tags plugin is required for this step and can be enabled automatically.

## Development

This repository follows the official [RuneLite external-plugin template](https://github.com/runelite/example-plugin) and targets Java 11.

```text
./gradlew test
./gradlew run
```

The `run` task launches a development RuneLite client. Do not use browser or input automation to interact with RuneScape; log in and verify the plugin manually.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
