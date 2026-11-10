package fromage;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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
public final class CheeseMoldTests {
    @GameTest(template = "kitchen")
    public static void farmerCheeseStaysSoft(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        BlockPos mold = new BlockPos(2, 2, 2);
        test.setBlock(mold, Fromage.MOLD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fromage.FRESH.get()));
        test.useBlock(mold, player);
        test.assertTrue(player.getMainHandItem().is(Fromage.FRESH.get()), "Mold consumed soft farmer's cheese");
        test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.PORTIONS) == 0, "Farmer's cheese entered a mold");
        test.succeed();
    }

    @GameTest(template = "kitchen", timeoutTicks = 400)
    public static void moldAndRipenBlueCheese(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        BlockPos mold = new BlockPos(2, 2, 2);
        test.setBlock(mold, Fromage.MOLD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fromage.BLUE_FRESH.get()));
        test.useBlock(mold, player);
        test.assertTrue(player.getMainHandItem().is(Fromage.BLUE_FRESH.get()), "Mold consumed soft blue cheese");
        test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.PORTIONS) == 0, "Soft blue cheese entered a mold");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fromage.DRAINED_BLUE.get()));
        test.useBlock(mold, player);
        test.assertTrue(player.getMainHandItem().isEmpty(), "Mold did not take the drained blue curds");
        test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.PORTIONS) == 2, "Drained blue curds did not fill the mold");
        test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.FORMING), "Drained blue curds did not start forming");
        test.runAfterDelay(37, () -> {
            test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.AGE) == 2, "Blue wheel did not finish forming");
            test.useBlock(mold, player);
            test.assertTrue(player.getInventory().countItem(Fromage.YOUNG_BLUE.get()) == 1, "Mold did not return a young blue wheel");
            test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.PORTIONS) == 0, "Mold did not empty");

            test.setBlock(mold, Fromage.MOLD.get().defaultBlockState()
                .setValue(CheeseMoldBlock.PORTIONS, 2)
                .setValue(CheeseMoldBlock.BLUE, true)
                .setValue(CheeseMoldBlock.FORMING, false)
                .setValue(CheeseMoldBlock.AGE, 0));
            test.runAfterDelay(304, () -> {
                test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.AGE) == 2,
                    "A mold already ripening soft blue did not finish");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                test.useBlock(mold, player);
                test.assertTrue(player.getInventory().countItem(Fromage.AGED_BLUE_ITEM.get()) == 1,
                    "Legacy mold did not return aged blue cheese");
                test.succeed();
            });
        });
    }

    @GameTest(template = "kitchen", timeoutTicks = 340)
    public static void handheldBlueMoldPacksAndPlaces(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        BlockPos table = new BlockPos(2, 2, 2);
        test.setBlock(table, Fromage.TABLE.get().defaultBlockState().setValue(Fromage.Station.STAGE, 3)
            .setValue(Fromage.Station.BLUE, true));
        BlockPos target = test.absolutePos(table);
        Vec3 origin = Vec3.atBottomCenterOf(target.south(2));
        player.setPos(origin.x, origin.y, origin.z);
        player.setNoGravity(true);
        player.noPhysics = true;
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target).add(0, .4, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.MOLD_ITEM.get().getDefaultInstance());
        test.useBlock(table, player);
        test.assertTrue(player.isUsingItem(), "Mold did not start a held action");
        player.releaseUsingItem();
        test.assertTrue(test.getBlockState(table).getValue(Fromage.Station.STAGE) == 3, "Releasing early took the cheese");

        test.useBlock(table, player);
        for (int tick = 0; tick < 22; tick++) player.tick();
        test.assertTrue(player.getMainHandItem().is(Fromage.PACKED_BLUE_MOLD.get()), "Hold did not pack blue cheese");
        test.assertTrue(test.getBlockState(table).getValue(Fromage.Station.STAGE) == 4, "Packed table should retain only whey");

        BlockPos base = new BlockPos(4, 1, 2);
        test.setBlock(base, Blocks.OAK_PLANKS);
        BlockPos clicked = test.absolutePos(base);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(clicked.above()), Direction.UP, clicked, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        BlockPos mold = base.above();
        test.assertBlockPresent(Fromage.MOLD.get(), mold);
        test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.BLUE), "Placed mold lost its variety");
        player.setItemInHand(InteractionHand.MAIN_HAND, Items.GLASS_BOTTLE.getDefaultInstance());
        test.useBlock(table, player);
        test.assertTrue(player.getInventory().countItem(Fromage.CULTURED_WHEY.get()) == 1, "Packing took the whey");
        test.assertTrue(player.getInventory().countItem(Fromage.BLUE_FRESH.get()) == 0, "Packing duplicated cheese");
        test.runAfterDelay(302, () -> {
            test.assertTrue(test.getBlockState(mold).getValue(CheeseMoldBlock.AGE) == 2, "Placed packed mold did not age");
            test.getLevel().destroyBlock(test.absolutePos(mold), true);
            test.assertItemEntityCountIs(Fromage.AGED_BLUE_ITEM.get(), mold, 1, 1);
            test.assertItemEntityCountIs(Fromage.MOLD_ITEM.get(), mold, 1, 1);
            test.succeed();
        });
    }

    @GameTest(template = "kitchen")
    public static void breakingMoldRecoversItsContents(GameTestHelper test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        test.setBlock(pos, Fromage.MOLD.get().defaultBlockState().setValue(CheeseMoldBlock.PORTIONS, 1));
        test.getLevel().destroyBlock(test.absolutePos(pos), true);
        test.assertItemEntityCountIs(Fromage.FRESH.get(), pos, 1, 1);
        test.assertItemEntityCountIs(Fromage.MOLD_ITEM.get(), pos, 1, 1);
        test.succeed();
    }
}



