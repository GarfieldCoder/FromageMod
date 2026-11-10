package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fromage")
@PrefixGameTestTemplate(false)
public final class CheddarTests {
    private static final BlockPos VAT = new BlockPos(2, 2, 2);
    private static final BlockPos TABLE = new BlockPos(4, 2, 2);
    private static final BlockPos MOLD = new BlockPos(2, 2, 4);
    private static final BlockPos PRESS = new BlockPos(4, 2, 4);
    private static final BlockPos SHELF = new BlockPos(4, 2, 6);

    @GameTest(template = "kitchen", timeoutTicks = 750)
    public static void vatStarterAndCulturedWheyLoop(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 8;
        test.setBlock(VAT, Fromage.FIREBOX.get());
        test.setBlock(TABLE, Fromage.TABLE.get());
        use(test, VAT, player, Items.COAL);
        warm(test, player, () -> {
            player.setShiftKeyDown(true);
            use(test, VAT, player, Items.AIR);
            player.setShiftKeyDown(false);
            use(test, VAT, player, Items.GLASS_BOTTLE);
            test.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Collected culture before fermentation");
            test.runAfterDelay(Fromage.Station.STARTER_TICKS + 2, () -> {
                use(test, VAT, player, Items.GLASS_BOTTLE);
                test.assertTrue(player.getMainHandItem().is(Fromage.VAT_STARTER.get()), "Firebox did not ferment a starter");
                ItemStack starter = player.getMainHandItem().copy();
                warm(test, player, () -> {
                    use(test, VAT, player, starter.getItem());
                    test.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Vat did not return starter bottle");
                    drainBatch(test, player, DairyBatch.CULTURED, () -> {
                        int slot = -1;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            if (player.getInventory().getItem(i).is(Fromage.CULTURED_WHEY.get())) {
                                slot = i;
                                break;
                            }
                        }
                        test.assertTrue(slot >= 0, "Starter batch did not produce reusable whey");
                        int wheySlot = slot;
                        warm(test, player, () -> {
                            ItemStack whey = player.getInventory().removeItem(wheySlot, 1);
                            player.setItemInHand(InteractionHand.MAIN_HAND, whey);
                            test.useBlock(VAT, player);
                            test.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Vat did not return the whey bottle");
                            drainBatch(test, player, DairyBatch.CULTURED, () -> {
                                test.assertTrue(player.getInventory().countItem(Fromage.CULTURED_WHEY.get()) == 1,
                                    "The next batch should replenish one starter");
                                test.assertTrue(player.getInventory().countItem(Fromage.DRAINED_FARMER.get()) == 2,
                                    "Both cultured batches should return drained curds");
                                test.assertTrue(player.getInventory().countItem(Fromage.WHEY.get()) == 0, "Cultured whey became ordinary whey");
                                test.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 850)
    public static void turnSaltPressAndAgeCheddar(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 8;
        test.setBlock(VAT.below(), Blocks.CAMPFIRE);
        test.setBlock(VAT, Fromage.VAT.get());
        test.setBlock(TABLE, Fromage.TABLE.get());
        test.setBlock(MOLD, Fromage.MOLD.get());
        test.setBlock(PRESS, Fromage.PRESS.get());
        test.setBlock(SHELF, Fromage.SHELF.get());
        warm(test, player, () -> {
        use(test, VAT, player, Fromage.CULTURE.get());
        use(test, VAT, player, Fromage.RENNET.get());
        drainBatch(test, player, DairyBatch.CHEDDAR, () -> {
            use(test, TABLE, player, Fromage.SALT.get());
            test.assertTrue(player.getMainHandItem().is(Fromage.SALT.get()), "Salt went in before turning");
            use(test, TABLE, player, Items.AIR);
            test.assertTrue(test.getBlockState(TABLE).getValue(Fromage.Station.DRAIN_PROGRESS) == 1, "Slab did not turn");
            use(test, TABLE, player, Fromage.SALT.get());
            test.assertTrue(player.getMainHandItem().isEmpty(), "Turned slab did not consume salt");
            use(test, TABLE, player, Items.AIR);
            test.assertTrue(player.getInventory().countItem(Fromage.SALTED_CURDS.get()) == 2, "Missing salted portions");
            test.assertTrue(player.getInventory().countItem(Fromage.CULTURED_WHEY.get()) == 1, "Table duplicated the vat whey");
            moveToHand(player, Fromage.SALTED_CURDS.get());
            test.useBlock(MOLD, player);
            moveToHand(player, Fromage.SALTED_CURDS.get());
            test.useBlock(MOLD, player);
            test.assertTrue(test.getBlockState(MOLD).getValue(CheeseMoldBlock.CHEDDAR), "Mold lost the variety");
            test.useBlock(MOLD, player);
            test.assertTrue(test.getBlockState(MOLD).getValue(CheeseMoldBlock.AGE) == 1, "Press did not start");
            test.assertBlockPresent(Fromage.MOLD.get(), MOLD);
            test.runAfterDelay(16, () -> {
                test.assertTrue(test.getBlockState(MOLD).getValue(CheeseMoldBlock.PRESS_DEPTH) >= 1,
                    "Plate did not lower on its own");
                test.useBlock(MOLD, player);
            });
            test.runAfterDelay(22, () -> test.assertTrue(
                test.getBlockState(MOLD).getValue(CheeseMoldBlock.PRESS_DEPTH) >= 1,
                "Another click restarted the press"));
            test.runAfterDelay(CheeseMoldBlock.BEAT_TICKS + 3, () -> {
                test.assertTrue(test.getBlockState(MOLD).getValue(CheeseMoldBlock.AGE) == 2, "Press did not finish");
                test.assertTrue(test.getBlockState(MOLD).getValue(CheeseMoldBlock.PRESS_DEPTH) == 3,
                    "Plate did not stay seated");
                test.useBlock(MOLD, player);
                test.assertTrue(player.getInventory().countItem(Fromage.YOUNG_CHEDDAR.get()) == 1,
                    "Finished mold did not return a young cheddar");
                test.assertBlockPresent(Fromage.MOLD.get(), MOLD);
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (player.getInventory().getItem(slot).is(Fromage.YOUNG_CHEDDAR.get())) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.PACKED_CHEDDAR_MOLD.get().getDefaultInstance());
            test.useBlock(PRESS, player);
            test.assertTrue(player.getMainHandItem().isEmpty(), "Press did not take the mold");
            test.useBlock(PRESS, player);
            test.runAfterDelay(10, () -> test.assertTrue(test.getBlockState(PRESS).getValue(CheesePressBlock.STAGE) == 3,
                "Press did not lower its plate"));
            test.runAfterDelay(CheesePressBlock.PRESS_TICKS + 2, () -> {
                test.useBlock(PRESS, player);
                test.assertTrue(player.getInventory().countItem(Fromage.MOLD_ITEM.get()) == 1, "Press did not return one mold");
                moveToHand(player, Fromage.YOUNG_CHEDDAR.get());
                useShelf(test, player, false, false);
                test.assertTrue(player.getMainHandItem().isEmpty(), "Shelf did not take the cheddar");
                test.runAfterDelay(152, () -> {
                    test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.LOWER) == 2, "Missing developing rind");
                    useShelf(test, player, false, true);
                    moveToHand(player, Fromage.YOUNG_CHEDDAR.get());
                    useShelf(test, player, true, false);
                    test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.UPPER) == 2, "Moved cheese lost its progress");
                    player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.YOUNG_CHEDDAR.get().getDefaultInstance());
                    useShelf(test, player, false, false);
                });
                test.runAfterDelay(305, () -> {
                    test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.UPPER) == 3, "Cheddar did not mature after 15 seconds");
                    test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.LOWER) == 2, "Shelf slots did not age independently");
                    useShelf(test, player, true, false);
                    test.assertTrue(player.getInventory().countItem(Fromage.CHEDDAR_BLOCK_ITEM.get()) == 1, "Shelf did not return one aged block");
                    test.setBlock(PRESS, Fromage.CHEDDAR_BLOCK.get());
                    for (int slice = 0; slice < 4; slice++) test.useBlock(PRESS, player);
                    test.assertItemEntityCountIs(Fromage.CHEDDAR_WEDGE.get(), PRESS, 2, 4);
                    test.succeed();
                });
            });
            });
        });
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 320)
    public static void plainStageEightMilkReplacesWhey(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 8;
        test.setBlock(VAT.below(), Blocks.CAMPFIRE);
        test.setBlock(VAT, Fromage.VAT.get());
        warm(test, player, () -> {
            use(test, VAT, player, Fromage.CULTURE.get());
            use(test, VAT, player, Items.AIR);
            test.runAfterDelay(Fromage.Station.COAGULATE_TICKS + 2, () -> {
                use(test, VAT, player, Items.AIR);
                use(test, VAT, player, Fromage.CLOTH.get());
                test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == Fromage.Station.WHEY_IN_VAT,
                    "Plain whey was not left in the vat");
                test.assertTrue(player.getInventory().countItem(Fromage.WHEY.get()) == 0, "Plain whey was bottled by itself");
                use(test, VAT, player, Items.MILK_BUCKET);
                test.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Plain whey did not accept the next milk");
                test.assertTrue(player.getInventory().countItem(Fromage.WHEY.get()) == 0, "Replacing plain whey also produced a whey item");
                test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 1,
                    "The next milk should start from poured milk, not stay at stage eight");
                test.runAfterDelay(Fromage.Station.WARM_TICKS + 2, () -> {
                    test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 2, "Replacement milk did not warm");
                    use(test, VAT, player, Fromage.BLUE_STARTER.get());
                    use(test, VAT, player, Items.AIR);
                    test.runAfterDelay(Fromage.Station.COAGULATE_TICKS + 2, () -> {
                        use(test, VAT, player, Items.AIR);
                        use(test, VAT, player, Fromage.CLOTH.get());
                        test.assertTrue(Fromage.Station.batch(test.getBlockState(VAT)) == DairyBatch.BLUE,
                            "Blue whey was treated as plain");
                        use(test, VAT, player, Items.MILK_BUCKET);
                        test.assertTrue(player.getMainHandItem().is(Items.MILK_BUCKET), "Cultured whey accepted milk");
                        test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == Fromage.Station.WHEY_IN_VAT,
                            "Refused milk changed the vat");
                        use(test, VAT, player, Items.GLASS_BOTTLE);
                        test.assertTrue(player.getInventory().countItem(Fromage.CULTURED_WHEY.get()) == 1,
                            "Bottling cultured whey did not yield one portion");
                        test.assertTrue(player.getInventory().countItem(Fromage.WHEY.get()) == 0, "Cultured whey became plain whey");
                        test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 0, "Bottling did not empty the vat");
                        test.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 260)
    public static void rennetBeforeCultureDoesNotRestartSetting(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 8;
        test.setBlock(VAT.below(), Blocks.CAMPFIRE);
        test.setBlock(VAT, Fromage.VAT.get());
        warm(test, player, () -> {
            use(test, VAT, player, Fromage.RENNET.get());
            test.assertTrue(player.getMainHandItem().isEmpty(), "Warm milk did not take rennet");
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 2, "Rennet advanced the vat");
            test.assertTrue(Fromage.Station.batch(test.getBlockState(VAT)) == DairyBatch.CHEDDAR, "Rennet did not mark cheddar");
            use(test, VAT, player, Fromage.BLUE_STARTER.get());
            test.assertTrue(player.getMainHandItem().is(Fromage.BLUE_STARTER.get()), "Blue starter was consumed after rennet");
            test.assertTrue(Fromage.Station.batch(test.getBlockState(VAT)) == DairyBatch.CHEDDAR, "Blue starter replaced rennet");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            use(test, VAT, player, Items.AIR);
            player.setShiftKeyDown(false);
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 2, "Sneak fermented milk that already had rennet");
            use(test, VAT, player, Fromage.CULTURE.get());
            test.assertTrue(player.getMainHandItem().isEmpty(), "Culture was not accepted after rennet");
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 3, "Culture did not move on to stirring");
            test.assertTrue(Fromage.Station.batch(test.getBlockState(VAT)) == DairyBatch.CHEDDAR, "Culture removed the rennet");
            test.setBlock(VAT, Fromage.VAT.get());
            warm(test, player, () -> {
                use(test, VAT, player, Fromage.CULTURE.get());
                use(test, VAT, player, Items.AIR);
                int progress = test.getBlockState(VAT).getValue(Fromage.Station.DRAIN_PROGRESS);
                test.assertTrue(progress > 0 && test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 3,
                    "Stir did not start coagulation");
                test.runAfterDelay(20, () -> {
                    use(test, VAT, player, Fromage.RENNET.get());
                    test.assertTrue(player.getMainHandItem().isEmpty(), "Rennet was not accepted while the curds set");
                    test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.DRAIN_PROGRESS) == progress,
                        "Rennet restarted coagulation");
                    test.assertTrue(Fromage.Station.batch(test.getBlockState(VAT)) == DairyBatch.CHEDDAR,
                        "Rennet during setting did not make cheddar");
                });
                test.runAfterDelay(Fromage.Station.COAGULATE_TICKS + 2, () -> {
                    test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 4,
                        "Rennet delayed the set curds");
                    use(test, VAT, player, Items.AIR);
                    test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 5, "Set cheddar curds could not be cut");
                    use(test, VAT, player, Fromage.RENNET.get());
                    test.assertTrue(player.getMainHandItem().is(Fromage.RENNET.get()), "Rennet was accepted after cutting");
                    test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 5, "Late rennet changed the cut curds");
                    test.succeed();
                });
            });
        });
    }

    @GameTest(template = "kitchen")
    public static void turnBonusSurvivesRerack(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().selected = 8;
        player.noPhysics = true;
        test.setBlock(SHELF, Fromage.SHELF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.YOUNG_CHEDDAR.get().getDefaultInstance());
        useShelf(test, player, false, false);
        test.assertTrue(player.getMainHandItem().isEmpty(), "Shelf did not take the wheel");
        useShelf(test, player, false, false);
        useShelf(test, player, false, true);
        moveToHand(player, Fromage.YOUNG_CHEDDAR.get());
        int gained = YoungWheelBlock.elapsed(player.getMainHandItem());
        test.assertTrue(gained == AgingShelfBlock.TURN_BONUS, "The first turn did not age the wheel by its bonus, was " + gained);

        BlockPos floor = new BlockPos(2, 1, 2);
        test.setBlock(floor, Blocks.OAK_PLANKS);
        BlockPos clicked = test.absolutePos(floor);
        player.setPos(Vec3.atBottomCenterOf(clicked.south()));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atBottomCenterOf(clicked.above()), Direction.UP, clicked, false)));
        test.assertBlockPresent(Fromage.CHEDDAR_YOUNG_BLOCK.get(), floor.above());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.useBlock(floor.above(), player);
        moveToHand(player, Fromage.YOUNG_CHEDDAR.get());
        test.assertTrue(YoungWheelBlock.elapsed(player.getMainHandItem()) == gained, "Picking up the wheel wiped its age");

        useShelf(test, player, false, false);
        useShelf(test, player, false, false);
        useShelf(test, player, false, true);
        moveToHand(player, Fromage.YOUNG_CHEDDAR.get());
        int reracked = YoungWheelBlock.elapsed(player.getMainHandItem());
        test.assertTrue(reracked < gained + AgingShelfBlock.TURN_BONUS,
            "Re-racking refreshed the turn bonus, elapsed " + reracked);
        test.succeed();
    }

    @GameTest(template = "kitchen")
    public static void shelfJoinsAndRestoresOuterSupports(GameTestHelper test) {
        test.setBlock(SHELF, Fromage.SHELF.get());
        test.setBlock(SHELF.west(), Fromage.SHELF.get());
        test.setBlock(SHELF.east(), Fromage.SHELF.get());
        test.setBlock(SHELF.above(), Fromage.SHELF.get());
        test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.LEFT)
            && test.getBlockState(SHELF).getValue(AgingShelfBlock.RIGHT), "Center shelf kept an internal support");
        test.assertTrue(test.getBlockState(SHELF.west()).getValue(AgingShelfBlock.WIDE), "Outer support did not widen");
        test.assertTrue(test.getBlockState(SHELF).getValue(AgingShelfBlock.ABOVE)
            && test.getBlockState(SHELF.above()).getValue(AgingShelfBlock.BELOW), "Stack did not join");
        test.assertTrue(test.getBlockState(SHELF.above()).getValue(AgingShelfBlock.WIDE),
            "Offset upper shelf did not match the wider lower shelf");
        test.assertTrue(!test.getBlockState(SHELF.above()).getValue(AgingShelfBlock.LEFT)
            && !test.getBlockState(SHELF.above()).getValue(AgingShelfBlock.RIGHT),
            "Upper shelf lost an exposed end above a longer row");
        test.setBlock(SHELF.west(), Blocks.AIR);
        test.assertTrue(!test.getBlockState(SHELF).getValue(AgingShelfBlock.LEFT), "New outer end did not come back");
        test.succeed();
    }

    @GameTest(template = "kitchen")
    public static void hintsFollowBatchAndHeldTool(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        test.setBlock(VAT.below(), Blocks.CAMPFIRE);
        test.setBlock(VAT, Fromage.VAT.get().defaultBlockState().setValue(Fromage.Station.STAGE, 2));
        assertHint(test, player, VAT, "vat.2");
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.CULTURED_WHEY.get().getDefaultInstance());
        assertHint(test, player, VAT, "vat.add_whey");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        assertHint(test, player, VAT, "vat.start_fermentation");

        test.setBlock(TABLE, Fromage.Station.withBatch(Fromage.TABLE.get().defaultBlockState()
            .setValue(Fromage.Station.STAGE, 3), DairyBatch.CHEDDAR));
        assertHint(test, player, TABLE, "table.cheddar.0");
        test.setBlock(TABLE, test.getBlockState(TABLE).setValue(Fromage.Station.DRAIN_PROGRESS, 1));
        assertHint(test, player, TABLE, "table.cheddar.1");
        test.setBlock(TABLE, test.getBlockState(TABLE).setValue(Fromage.Station.DRAIN_PROGRESS, 2));
        assertHint(test, player, TABLE, "table.cheddar.2");
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.MOLD_ITEM.get().getDefaultInstance());
        assertHint(test, player, TABLE, "mold.packing");

        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.setBlock(VAT, Fromage.VAT.get().defaultBlockState().setValue(Fromage.Station.STAGE, 1));
        assertHint(test, player, VAT, "vat.warming");
        test.setBlock(VAT, Fromage.Station.withBatch(Fromage.VAT.get().defaultBlockState()
            .setValue(Fromage.Station.STAGE, 3), DairyBatch.CULTURED));
        assertHint(test, player, VAT, "vat.stir");
        test.setBlock(VAT, test.getBlockState(VAT).setValue(Fromage.Station.DRAIN_PROGRESS, 1));
        assertHint(test, player, VAT, "vat.coagulating");
        test.setBlock(VAT, test.getBlockState(VAT).setValue(Fromage.Station.STAGE, 4).setValue(Fromage.Station.DRAIN_PROGRESS, 0));
        assertHint(test, player, VAT, "vat.rennet_or_cut");
        test.setBlock(VAT, Fromage.Station.withBatch(test.getBlockState(VAT), DairyBatch.CHEDDAR));
        assertHint(test, player, VAT, "vat.4");
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.RENNET.get().getDefaultInstance());
        test.setBlock(VAT, Fromage.VAT.get().defaultBlockState().setValue(Fromage.Station.STAGE, 2));
        assertHint(test, player, VAT, "vat.add_rennet");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.setBlock(VAT, Fromage.Station.withBatch(test.getBlockState(VAT), DairyBatch.CHEDDAR));
        assertHint(test, player, VAT, "vat.culture_after_rennet");
        test.succeed();
    }

    private static void assertHint(GameTestHelper test, Player player, BlockPos pos, String key) {
        BlockPos absolute = test.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        var hint = WorkshopStep.forTarget(test.getLevel(), test.getBlockState(pos), player, hit);
        test.assertTrue(Component.translatable("message.fromage." + key).equals(hint), "Unexpected hint at " + pos + ": " + hint);
    }

    private static void warm(GameTestHelper test, Player player, Runnable next) {
        use(test, VAT, player, Items.MILK_BUCKET);
        test.runAfterDelay(Fromage.Station.WARM_TICKS + 2, () -> {
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 2, "Milk did not warm on its own");
            next.run();
        });
    }

    private static void drainBatch(GameTestHelper test, Player player, DairyBatch batch, Runnable drained) {
        use(test, VAT, player, Items.AIR);
        test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.DRAIN_PROGRESS) > 0, "Stir did not start coagulation");
        test.runAfterDelay(Fromage.Station.COAGULATE_TICKS + 2, () -> {
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 4, "Curds did not finish setting");
            use(test, VAT, player, Items.AIR);
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == 5, "Could not cut curds with an empty hand");
            use(test, VAT, player, Fromage.CLOTH.get());
            test.assertTrue(test.getBlockState(VAT).getValue(Fromage.Station.STAGE) == Fromage.Station.WHEY_IN_VAT,
                "Lifting curds did not leave whey");
            use(test, VAT, player, Items.GLASS_BOTTLE);
            test.assertTrue(player.getInventory().countItem(batch.whey()) == 1, "Vat did not yield whey");
            moveToHand(player, batch.curds());
            test.useBlock(TABLE, player);
            test.assertTrue(player.getMainHandItem().isEmpty(), "Table did not take the curds");
            test.runAfterDelay(64, () -> {
                test.assertTrue(test.getBlockState(TABLE).getValue(Fromage.Station.STAGE) == 3, "Curds did not finish draining");
                if (batch != DairyBatch.CHEDDAR) use(test, TABLE, player, Items.AIR);
                drained.run();
            });
        });
    }

    private static void moveToHand(Player player, Item item) {
        int slot = player.getInventory().findSlotMatchingItem(item.getDefaultInstance());
        if (slot < 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(item)) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot < 0) throw new IllegalStateException("Missing " + item);
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && player.getInventory().selected != slot) {
            player.getInventory().setItem(player.getInventory().getFreeSlot(), held);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().removeItem(slot, 1));
    }

    private static void use(GameTestHelper test, BlockPos pos, Player player, Item item) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) player.getInventory().setItem(player.getInventory().getFreeSlot(), held);
        player.setItemInHand(InteractionHand.MAIN_HAND, item.getDefaultInstance());
        if (pos.equals(VAT) && test.getBlockState(VAT).is(Fromage.FIREBOX.get())) {
            BlockPos absolute = test.absolutePos(VAT);
            var hit = new BlockHitResult(Vec3.atBottomCenterOf(absolute.above()), Direction.UP, absolute, false);
            test.useBlock(VAT, player, hit);
        } else {
            test.useBlock(pos, player);
        }
    }

    private static void useShelf(GameTestHelper test, Player player, boolean upper, boolean sneak) {
        player.setShiftKeyDown(sneak);
        BlockPos pos = test.absolutePos(SHELF);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, upper ? .57 : .13, .37), Direction.SOUTH, pos, false);
        test.useBlock(SHELF, player, hit);
        player.setShiftKeyDown(false);
    }
}
