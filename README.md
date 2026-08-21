# Wilderness Loadouts

Wilderness Loadouts is a RuneLite Plugin Hub plugin that finds the strongest defensive equipment combination in gear you own while respecting a simple filler-risk budget.

## Features

- Optimizes for Overall, Magic, Ranged, or Melee Defence.
- Scans physical equippable gear from the bank, inventory, and currently worn equipment; bank placeholders are ignored.
- Supports three or four protected/core items, or High Risk mode with none, and budgets such as `500k` or `1.5m`.
- Allows every equipment slot to be Auto, Locked to an owned item, or Empty.
- Enforces two-handed weapon and shield compatibility.
- Shows owned alternatives and their defensive stats, objective score, and approximate price.
- Creates a virtual Bank Tags layout without moving real bank positions.

## Risk model

Protected/core items are assumed protected for loadout planning. High Risk mode protects none. All other selected items count toward the filler-risk budget using RuneLite's current item price. This is an estimate, not a prediction of the items kept or lost on an actual Wilderness death.

The first version intentionally does not model skull state, Protect Item prayer state beyond the selected zero, three, or four protected-item assumption, Trouver parchment, degradation, charges, repair values, or special death/reclaim rules. Unpriced selected items are visibly flagged because displayed risk may be incomplete.

### Known valuation gap

Some untradeable or indirectly valued equipment currently has no RuneLite market price and therefore contributes zero to displayed filler risk. This includes items with implicit replacement, parchment, or reclaim value, such as the Pendant of Ates, imbued god capes, Void equipment, and Barrows gloves. Excluding these items or assigning contextual values is intentionally deferred pending a more complete valuation policy; treat any loadout containing an unpriced item as having incomplete risk.

## Usage

1. Open your bank once per RuneLite session so the plugin can scan owned gear.
2. Open the **Wilderness Loadouts** sidebar panel.
3. Choose a defensive focus, protected/core mode, filler-risk budget, and enabled slots. Results update automatically.
4. Select an equipment slot to lock an alternative, return it to Auto, or force it Empty.
5. Open the bank and select **Show in Bank** to view the generated virtual layout; select **Hide in Bank** to close it. The core Bank Tags plugin is required for this step and can be enabled automatically.

## Development

This repository follows the official [RuneLite external-plugin template](https://github.com/runelite/example-plugin) and targets Java 11.

```text
./gradlew test
./gradlew run
```

The `run` task launches a development RuneLite client. Do not use browser or input automation to interact with RuneScape; log in and verify the plugin manually.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
