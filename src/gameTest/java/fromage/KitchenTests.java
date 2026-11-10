package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

@GameTestHolder("fromage")
@PrefixGameTestTemplate(false)
public final class KitchenTests {
    private static final BlockPos FIRE = new BlockPos(2, 1, 2);
    private static final BlockPos POT = FIRE.above();
    private static final BlockPos TABLE = new BlockPos(4, 2, 2);

    @GameTest(template = "kitchen")
    public static void itemTooltips(GameTestHelper test) {
        for (var entry : Fromage.ITEMS.getEntries()) {
            ItemStack stack = entry.get().getDefaultInstance();
            String key = stack.getDescriptionId() + ".tooltip";
            var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.Default.NORMAL);
            test.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(key)), "Missing usage tooltip for " + entry.getId());
        }
        test.succeed();
    }

    @GameTest(template = "kitchen", timeoutTicks = 450)
    public static void dairyProduction(GameTestHelper test) {
        boolean delight = ModList.get().isLoaded(Fromage.FARMERS_DELIGHT);
        boolean cookery = ModList.get().isLoaded(Fromage.COOKERY);
        checkRecipe(test, "curd_knife", !delight && !cookery);
        checkRecipe(test, "farmers_delight/cheese_crumbles", delight);
        checkRecipe(test, "farmers_delight/cheese_dumplings", delight);
        checkRecipe(test, "kaleidoscope/cheese_flatbread", cookery);
        checkRecipe(test, "cheese_sandwich", true);

        if (delight) {
            makeCheese(test, item("farmersdelight:flint_knife"), () -> {
                if (cookery) makeCheese(test, item("kaleidoscope_cookery:iron_kitchen_knife"), test::succeed);
                else test.succeed();
            });
        } else {
            makeCheese(test, cookery ? item("kaleidoscope_cookery:iron_kitchen_knife") : Fromage.KNIFE.get(),
                test::succeed);
        }
    }

    @GameTest(template = "kitchen", timeoutTicks = 240)
    public static void delightKitchen(GameTestHelper test) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded(Fromage.FARMERS_DELIGHT)) {
            test.succeed();
            return;
        }

        test.setBlock(TABLE.below(), Blocks.OAK_PLANKS);
        place(test, TABLE, "farmersdelight:cutting_board");
        Player player = player(test);
        use(test, TABLE, player, Fromage.FRESH.get());
        use(test, TABLE, player, item("farmersdelight:flint_knife"));
        test.assertItemEntityCountIs(Fromage.CRUMBLES.get(), TABLE, 3, 1);

        test.setBlock(FIRE, Blocks.CAMPFIRE);
        place(test, POT, "farmersdelight:cooking_pot");
        var pot = test.getBlockEntity(POT);
        // Only the test harness reflects on the inventory so production has no optional Java dependency.
        var inventory = (IItemHandlerModifiable) pot.getClass().getMethod("getInventory").invoke(pot);
        inventory.setStackInSlot(0, new ItemStack(Fromage.CRUMBLES.get()));
        inventory.setStackInSlot(1, new ItemStack(item("farmersdelight:wheat_dough")));
        inventory.setStackInSlot(2, new ItemStack(item("farmersdelight:cabbage")));

        test.succeedWhen(() -> {
            var output = inventory.getStackInSlot(inventory.getSlots() - 1);
            test.assertTrue(output.is(Fromage.DUMPLINGS.get()) && output.getCount() == 1,
                "Cooking Pot did not produce one serving of cheese dumplings");
            for (int slot = 0; slot < 3; slot++) {
                test.assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Cooking Pot did not consume an ingredient");
            }
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 260)
    public static void cookeryKitchen(GameTestHelper test) {
        if (!ModList.get().isLoaded(Fromage.COOKERY)) {
            test.succeed();
            return;
        }

        test.setBlock(FIRE, Blocks.CAMPFIRE);
        place(test, POT, "kaleidoscope_cookery:pot");
        Player player = player(test);
        use(test, POT, player, item("kaleidoscope_cookery:oil"));
        test.runAfterDelay(40, () -> {
            for (Item ingredient : new Item[] {
                Fromage.CHEDDAR_WEDGE.get(), item("kaleidoscope_cookery:flour"),
                item("kaleidoscope_cookery:flour"), item("kaleidoscope_cookery:lettuce")
            }) {
                use(test, POT, player, ingredient);
                test.assertTrue(player.getMainHandItem().isEmpty(), "Cookery Pot did not consume " + ingredient);
            }
            Item shovel = item("kaleidoscope_cookery:kitchen_shovel");
            use(test, POT, player, shovel);
            for (int tick : new int[] {20, 40, 60}) {
                test.runAfterDelay(tick, () -> use(test, POT, player, shovel));
            }
            test.runAfterDelay(140, () -> {
                player.setShiftKeyDown(true);
                use(test, POT, player, shovel);
                test.assertTrue(player.getInventory().countItem(Fromage.FLATBREAD.get()) == 1,
                    "Cookery Pot did not serve one cheese flatbread");
                use(test, POT, player, shovel);
                test.assertTrue(player.getInventory().countItem(Fromage.FLATBREAD.get()) == 1,
                    "Cookery Pot served twice");
                test.succeed();
            });
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 300)
    public static void blueDairyProduction(GameTestHelper test) {
        makeCheese(test, Fromage.KNIFE.get(), true, test::succeed);
    }

    private static void makeCheese(GameTestHelper test, Item knife, Runnable done) {
        makeCheese(test, knife, false, done);
    }

    private static void makeCheese(GameTestHelper test, Item knife, boolean blue, Runnable done) {
        test.setBlock(FIRE, Blocks.CAMPFIRE);
        test.setBlock(POT, Fromage.VAT.get());
        test.setBlock(TABLE, Fromage.TABLE.get());
        Player player = player(test);
        use(test, POT, player, Items.MILK_BUCKET);
        test.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Vat did not return an empty bucket");
        use(test, POT, player, Items.AIR);
        test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.STAGE) == 1, "Milk should warm without stirring");
        test.runAfterDelay(Fromage.Station.WARM_TICKS + 2, () -> {
            test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.STAGE) == 2, "Milk did not warm on its own");
            use(test, POT, player, blue ? Fromage.BLUE_STARTER.get() : Fromage.CULTURE.get());
            test.assertTrue(player.getMainHandItem().isEmpty(), "Vat did not consume the culture");
            use(test, POT, player, Items.AIR);
            test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.DRAIN_PROGRESS) > 0, "Stir did not start coagulation");
            use(test, POT, player, knife);
            test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.STAGE) == 3, "Curds were cut before they set");
            test.runAfterDelay(Fromage.Station.COAGULATE_TICKS + 2, () -> {
                test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.STAGE) == 4, "Curds did not finish setting");
                use(test, POT, player, knife);
                use(test, POT, player, Fromage.CLOTH.get());
        Item curds = (blue ? DairyBatch.BLUE : DairyBatch.PLAIN).curds();
        test.assertTrue(player.getInventory().countItem(curds) == 1, "Vat did not produce cut curds");
        test.assertTrue(test.getBlockState(POT).getValue(Fromage.Station.STAGE) == Fromage.Station.WHEY_IN_VAT,
            "Whey did not remain in the vat");
        Item whey = (blue ? DairyBatch.BLUE : DairyBatch.PLAIN).whey();
        use(test, POT, player, Items.GLASS_BOTTLE);
        test.assertTrue(player.getInventory().countItem(whey) == 1, "Vat did not yield one portion of whey");

        ItemStack bottled = player.getMainHandItem();
        if (!bottled.isEmpty()) player.getInventory().setItem(player.getInventory().getFreeSlot(), bottled);
        int curdSlot = 0;
        while (curdSlot < player.getInventory().getContainerSize()
                && !player.getInventory().getItem(curdSlot).is(curds)) curdSlot++;
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().removeItem(curdSlot, 1));
        test.useBlock(TABLE, player);
        test.assertTrue(player.getMainHandItem().isEmpty(), "Table did not consume the curds");
        Item drained = blue ? Fromage.DRAINED_BLUE.get() : Fromage.DRAINED_FARMER.get();
        use(test, TABLE, player, Items.AIR);
        test.assertTrue(player.getInventory().countItem(drained) == 0, "Wet curds were collected early");
        test.runAfterDelay(Fromage.Station.DRAIN_TICKS + 2, () -> {
            use(test, TABLE, player, Items.AIR);
            test.assertTrue(player.getInventory().countItem(drained) == 1, "Table did not return the drained curds");
            test.assertTrue(player.getInventory().countItem(Fromage.CLOTH.get()) == 1, "Table did not return the cloth");
            test.assertTrue(player.getInventory().countItem(whey) == 1, "Table duplicated the vat whey");
            use(test, TABLE, player, Items.AIR);
            test.assertTrue(player.getInventory().countItem(drained) == 1, "Table yielded curds twice");
            done.run();
        });
        });
        });
    }

    private static void checkRecipe(GameTestHelper test, String path, boolean expected) {
        var id = ResourceLocation.fromNamespaceAndPath("fromage", path);
        test.assertTrue(test.getLevel().getRecipeManager().byKey(id).isPresent() == expected,
            "Unexpected recipe availability for " + id);
    }

    private static Player player(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        Vec3 position = Vec3.atCenterOf(test.absolutePos(FIRE));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static void use(GameTestHelper test, BlockPos pos, Player player, Item item) {
        ItemStack previous = player.getMainHandItem();
        if (!previous.isEmpty()) {
            player.getInventory().setItem(player.getInventory().getFreeSlot(), previous);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, item.getDefaultInstance());
        test.useBlock(pos, player);
    }

    private static void place(GameTestHelper test, BlockPos pos, String id) {
        test.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}








