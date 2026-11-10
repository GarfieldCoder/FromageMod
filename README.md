# Fromage & Fermentation 0.3.4

A fresh-cheese workshop for Minecraft Java 1.21.1, NeoForge 21.1.250, and Java 21. The vanilla workshop and toasted cheese sandwich work with Fromage & Fermentation alone.

Version 0.3.0 adds a renewable starter loop and a cheddar process using the existing vat, draining table, and mold. Cheddar is turned and salted on the table, pressed in a screw press, and aged on modular carved shelves.

Version 0.3.1 makes the shelf joints continuous in offset stacks, adds persistent workstation hints, and animates stirring, cutting, and lifting. Aim at a station to see its current instruction. Holding an ingredient or mold updates the instruction when that item has a use at the current step.

Version 0.3.2 adds a handheld Cheese Grater, grated cheddar, placeable pasta bowls, and the Pungent effect.

Version 0.3.3 uses one cloth per batch and one mold for every cheese. The mold presses cheddar directly. The rack shows four distinct stages for cheddar and blue cheese.

Version 0.3.4 raises the vat contents to a consistent level below the rim, brightens the whey, and prioritizes the bottle prompt after lifting the curds. Young blue cheese goes straight from the mold to the rack.

## Install and build

Replace the previous Fromage & Fermentation JAR with fromage-0.3.4.jar in the mods folder of your NeoForge 1.21.1 profile. Use the same version on client and server. The Farmer's Delight, Kaleidoscope Cookery, and JEI JARs are separate downloads.

On Windows, run `.\gradlew.bat build` from the project directory. The mod is written to build/libs. Run `.\gradlew.bat runClient` for the development profile. The local development profile has JEI 19.70.0.469 installed in run/mods. For a fresh checkout, put the NeoForge JEI JAR in that folder. Find the workshop under the **Fromage & Fermentation** Creative tab, which shows the dishes available with the active integrations.

## Make fresh cheese

1. Craft the vat from seven copper ingots in a U shape with all three rows. Set it directly above a lit campfire.
2. Craft the draining table from three spruce slabs across the top and sticks in the four outer slots below. Make cheesecloth from four string in a square.
3. Craft acid culture from an apple and sugar. Right-click the vat with a milk bucket, then right-click with an empty hand to stir over the lit fire.
4. Add acid culture. Use an empty hand to stir, then use it again to cut. Lift the cut curds with cheesecloth. Use a glass bottle to collect the whey in the vat.
5. Put the wrapped curds on the table. Their cloth lines the table as they drain. Collect the drained curds and recover the cloth. Pack the curds into the mold, let them settle, then lift two portions of soft farmer's cheese.
6. Craft bread and farmer's cheese together and cook the resulting sandwich in a smoker.

## Keep a culture going

Warm a bucket of milk in either vat. Sneak and use the vat with empty hands to start fermenting it. After five seconds, use a glass bottle on the vat to collect one Vat Starter Culture.

Add that starter to a new vat of warm milk. It returns the empty bottle and makes cultured curds. Lift the curds with cheesecloth, then bottle the Cultured Whey. Use it in place of starter in the next batch to get the empty bottle back and eventually collect one replacement portion. A cultured batch still uses a fresh bucket of milk. Blue cheese and cheddar also produce reusable whey, which starts a plain cultured batch.

## Firebox Milk Vat

Craft a copper vat above a campfire in the crafting grid. The combined vat fits inside a single block. Vats already placed in earlier versions fold their upper basin and its batch into the lower block when loaded. Right-click the lower firebox with coal or charcoal to start or feed the fire. Each piece burns for 80 seconds at normal tick rate. A shovel banks the fire and saves the remaining fuel. Flint and steel relights banked fuel. Aim at the copper basin to stir or collect the batch. Aim low with an empty hand to check fuel.

## Shape cheese

Craft a Cheese Mold from five oak slabs in a U shape. Place it and use the drained curds on it. The mold stays in place after you collect the cheese.

## Make blue cheese

Craft blue cheese starter from acid culture, a blue orchid, and a brown mushroom. Use it on warm milk. Cut and drain the blue curds as usual, then pack them loosely into the mold. Lift the young wheel from the mold and place it on the rack. The mold prepares air channels as the wheel comes out. The blue-green veins deepen as it ages.

Young farmer's cheese blocks from older saves still age when placed. If a full mold from an older save sits idle, interact with it to start its first ripening cycle.

## Make cheddar

1. Craft Vegetable Rennet from a fern and sugar. Craft salt from kelp and a water bucket. That recipe returns the empty bucket.
2. Add culture to warm milk. Add rennet before stirring to set the curd. Cut the curds, lift them with cloth, and take the bundle to the draining table. Bottle the cultured whey from the vat.
3. Turn the knitted slab with an empty hand, add salt, then collect both portions and use them on a placed mold.
4. Use an empty hand to start the press plate. Tap again as the plate seats and the prompt says to press. A missed beat restarts the motion. Lift the young cheddar out of the mold and put it on a rack.

## Age cheese

Place a young cheddar or blue wheel on either level of an Aging Shelf. At normal tick rate the four stages are fresh, rind forming at about 7.5 seconds, mature at about 15 seconds, and extra-aged at about 30 seconds. Leave a mature wheel on the rack to reach the final stage. Each wheel keeps its own clock.

Use an empty hand to turn a developing wheel. Each eligible turn advances its clock by 0.6 seconds, with five seconds between bonuses. Use an empty hand on a mature wheel to collect it. Sneak-use lifts a developing wheel with its progress intact or turns a mature wheel. Place shelves facing the same direction to join them side by side or stack them.

## How the workshop is implemented

The server validates interactions and updates each block's saved state. The vat and draining table share a station class, and `DairyBatch` selects which curds and whey come out. The mold, press, and shelf each have their own rules. Block entities store timers that need to survive a reload, including the two independent shelf aging slots. Optional cooking recipes are data files loaded only with their corresponding mod.

The shelf model assembles rails, ends, and cheeses from its synchronized block state. The client also reads the synchronized state to show one contextual hint for the station under the crosshair. Workshop hints do not drive recipes or change which interactions the server accepts.

Stirring, cutting, and lifting send animation events to nearby clients. New curd bundles carry their cloth, and their whey belongs to the vat. Older loose curds still use a manually lined table. Any whey already assigned to that table can be collected with a bottle.

## Slice cheese

Put one whole aged cheese block in your inventory crafting grid to make four wedges. For serving in-world, place the block and right-click it with an empty hand. Each click drops one wedge and removes a visible quarter. Breaking a partly sliced block drops its remaining wedges.

Existing firm farmer's cheese blocks from older saves are labeled Mild Cheese Blocks and remain usable. New farmer's cheese is served fresh as soft curds.

## Grate cheddar over a meal

1. Craft a Cheese Grater from three iron ingots and a stick. Craft a Bowl of Pasta from wheat, beetroot, and a bowl, then place it. A placed vanilla cake also works.
2. Hold a cheddar wedge in your offhand and the grater in your main hand. Aim at the meal and hold use for 1.5 seconds. Each completed grate uses one wedge and one durability.
3. On a plain meal, one of the two portions tops the meal and the other goes into your inventory. Grating over an already topped meal gives you both portions. Use loose grated cheddar directly on a plain meal to top it.
4. Craft loose grated cheddar with a pasta bowl for Cheddar Pasta or with a baked potato for a Cheddar Potato. Grated cheddar is an ingredient. Topping pasta and cake adds no hunger points. Cheddar Potato restores one more hunger point than a baked potato.

Topped pasta and cake give off green aroma particles. Eating them makes you Pungent for 20 seconds. Nearby cows, pigs, sheep, and chickens move away from Pungent survival players. Pasta returns its bowl when served.

## Optional kitchens

- **Farmer's Delight 1.3.4.** Put a portion of soft farmer's cheese on the Cutting Board and cut it into one portion of crumbles. Combine soft farmer's cheese, crumbles, or a cheddar wedge with wheat dough and cabbage in a heated Cooking Pot to make Cabbage and Cheese Dumplings.
- **Kaleidoscope Cookery 1.6.0.** Set its Pot above a lit campfire. Add its cooking oil and let it heat, then add one finished cheese portion, two flour, and one lettuce. Start cooking with a kitchen shovel and stir three times. Sneak and use the kitchen shovel to serve the Pan-Fried Cheese Flatbread.
- **Both kitchens.** Both sets of recipes are available. The same cheese portions work in either kitchen. Each recipe consumes the listed ingredients for one serving.

Recipes load through NeoForge data conditions and use each mod's published recipe serializer. The stockpot and teapot have their own recipe types. This release uses the Pot's exact ingredient recipe type. The vat accepts either mod's knives as an alternative to cutting with an empty hand. Damageable knives lose one durability per cut. The standalone curd knife remains craftable when neither kitchen mod is installed.

Finished farmer's cheese, crumbles, and cheddar wedges are in `#c:foods/cheese` for other mods to use. Both kitchen dishes use `#fromage:cooking_cheese` to require one finished portion. The vat accepts `#c:buckets/milk` for inputs with an empty bucket as their crafting remainder. The cheddar slab accepts `#c:salt`. The Farmer's Delight dish uses `#c:foods/dough/wheat` and `#c:crops/cabbage`. The Kaleidoscope dish uses `#c:flour` and `#c:crops/lettuce`.

## Testing

The notes below describe the earlier compatibility checks. Version 0.3.3 was assembled for manual playtesting. The existing GameTests include interactions from the previous workshop flow.

The integration GameTests run inside Minecraft and live in src/gameTest. Run `.\gradlew.bat runGameTestServer -PintegrationTests -PgameTestDir=run-gametest`. Place the optional mods you want to test in that run directory's mods folder before launching it. Use a separate directory for each combination. Omit `-PintegrationTests` for a release build.

The tests exercise milk-to-cheese production with the available knives, recipe gating, the Cutting Board, actual heated Cooking Pot processing, and the Kaleidoscope Pot's oil, ingredient, stirring, and serving interactions. The optional kitchen tests skip their workflows when the owning mod is absent.

| Minecraft GameTest profile | Result |
| --- | --- |
| Fromage & Fermentation alone | Passed. The fallback knife and complete fresh-cheese loop worked. |
| With Farmer's Delight 1.3.4 | Passed. The flint knife worked in the dairy, and the Cutting Board and Cooking Pot produced the expected items. |
| With Kaleidoscope Cookery 1.6.0 | Passed. The kitchen knife worked in the dairy, and the Pot served one flatbread. |
| With both mods | Passed. Both knives and all three workstation recipes worked in the same world. |

Kaleidoscope 1.6.0 logged two missing-tag diagnostics in its own data for `carryon:block_blacklist` and `kaleidoscope_cookery:cookery_mod_items`. The tested recipes loaded and worked in both profiles that included Kaleidoscope.

The GameTests cover firebox placement, fuel, relighting and burnout, inventory crafting, four placed slices, recovery of partially sliced cheese, automatic drainage, handheld packing, and ripening. They also cover starter fermentation, two batches using the same culture, turning and salting, the press cycle, shelf joins, and independent cheddar aging.

Editable Blockbench projects are included in blockbench/. The texture previews are asset renders.



## References

- [Farmer's Delight release](https://modrinth.com/mod/farmers-delight/version/1.21.1-1.3.4) and [project source](https://github.com/vectorwing/FarmersDelight)
- [Kaleidoscope Cookery release and source archive](https://modrinth.com/mod/kaleidoscope-cookery/version/1.6.0-neoforge%2Bmc1.21.1)
- [NeoForge data load conditions](https://docs.neoforged.net/docs/1.21.1/resources/server/conditions/)
- [Fromage](https://modrinth.com/mod/fromage) for comparison with existing aging and slicing mechanics
- [Pantrywork](https://modrinth.com/project/pantrywork) for shared pantry tag conventions and ingredient cost equivalence
- [Kaleidoscope Compat](https://modrinth.com/mod/kaleidoscope-compat) for its cross-kitchen recipe and ingredient unification approach










## Local development mods

Put the NeoForge 1.21.1 builds of JEI, Farmer's Delight, and Kaleidoscope Cookery in the ignored run/mods directory to enable them in runClient. The source checkout can also run standalone with an empty run/mods directory.
#   F r o m a g e M o d  
 