package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fromage")
@PrefixGameTestTemplate(false)
public final class FireboxAndSliceTests {
    @GameTest(template = "kitchen", timeoutTicks = 120)
    public static void fireboxPlacesFuelsAndRelights(GameTestHelper test) {
        BlockPos floor = new BlockPos(3, 1, 3);
        BlockPos base = floor.above();
        BlockPos basin = base.above();
        test.setBlock(floor, Blocks.COBBLESTONE);
        test.setBlock(basin, Blocks.COBBLESTONE);

        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, Fromage.FIREBOX_ITEM.get().getDefaultInstance());
        BlockPos clicked = test.absolutePos(floor);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(clicked.above()), Direction.UP, clicked, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        player.getMainHandItem().useOn(context);
        test.assertBlockPresent(Fromage.FIREBOX.get(), base);
        test.assertBlockPresent(Blocks.COBBLESTONE, basin);
        test.assertTrue(player.getMainHandItem().isEmpty(), "Placement did not consume one vat");
        BlockPos absoluteBase = test.absolutePos(base);
        var upperHit = new BlockHitResult(Vec3.atCenterOf(absoluteBase).add(0, .45, 0), Direction.UP,
            absoluteBase, false);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        test.useBlock(base, player);
        test.assertTrue(!test.getBlockState(base).getValue(BlockStateProperties.LIT), "An empty firebox lit for free");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 2));
        test.useBlock(base, player);
        test.assertTrue(player.getMainHandItem().getCount() == 1, "Firebox did not consume one coal");
        test.assertTrue(test.getBlockState(base).getValue(BlockStateProperties.LIT), "Basin did not reflect lit state");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        test.useBlock(base, player, upperHit);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.useBlock(base, player, upperHit);
        test.assertTrue(test.getBlockState(base).getValue(Fromage.Station.STAGE) == 1,
            "Empty hand should not be required to warm milk");
        test.runAfterDelay(Fromage.Station.WARM_TICKS + 2, () -> {
            test.assertTrue(test.getBlockState(base).getValue(Fromage.Station.STAGE) == 2, "Lit firebox did not heat milk");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        test.useBlock(base, player);
        test.assertTrue(!test.getBlockState(base).getValue(BlockStateProperties.LIT), "Basin stayed heated after banking");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        test.useBlock(base, player);
        test.assertTrue(test.getBlockState(base).getValue(BlockStateProperties.LIT), "Flint did not relight banked fuel");
        test.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Relighting did not consume flint durability");

        var fuel = (FireboxBlock.Fuel) test.getBlockEntity(base);
        fuel.light(test.getLevel().getGameTime(), 2);
        test.runAfterDelay(4, () -> {
            test.assertTrue(!test.getBlockState(base).getValue(BlockStateProperties.LIT), "Firebox did not burn out");
            test.getLevel().destroyBlock(test.absolutePos(base), true);
            test.assertBlockPresent(Blocks.AIR, base);
            test.assertBlockPresent(Blocks.COBBLESTONE, basin);
            test.assertItemEntityCountIs(Fromage.FIREBOX_ITEM.get(), base, 2, 1);
            test.succeed();
        });
        });
    }

    @GameTest(template = "kitchen")
    public static void legacyBasinMigratesWithoutDuplicatingCurds(GameTestHelper test) {
        BlockPos base = new BlockPos(3, 2, 3);
        test.setBlock(base, Fromage.FIREBOX.get());
        test.setBlock(base.above(), Fromage.FIREBOX_BASIN.get().defaultBlockState()
            .setValue(Fromage.Station.STAGE, 5).setValue(Fromage.Station.BLUE, true));
        test.runAfterDelay(5, () -> {
            test.assertBlockPresent(Blocks.AIR, base.above());
            var state = test.getBlockState(base);
            test.assertTrue(state.getValue(Fromage.Station.STAGE) == 5 && state.getValue(Fromage.Station.BLUE),
                "Compact vat lost the old batch");
            test.assertItemEntityNotPresent(Fromage.BLUE_CURDS.get(), base, 2);
            test.getLevel().destroyBlock(test.absolutePos(base), true);
            test.assertItemEntityCountIs(Fromage.FIREBOX_ITEM.get(), base, 2, 1);
            test.assertItemEntityCountIs(Fromage.BLUE_CURDS.get(), base, 2, 1);
            test.succeed();
        });
    }

    @GameTest(template = "kitchen")
    public static void blueCheeseSlicesAndRecovers(GameTestHelper test) {
        BlockPos pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, Fromage.AGED_BLUE.get());
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        test.useBlock(pos, player);
        test.assertTrue(test.getBlockState(pos).getValue(CheeseBlock.Sliceable.SLICES) == 3, "First slice did not change shape");
        test.assertItemEntityCountIs(Fromage.BLUE_WEDGE.get(), pos, 2, 1);
        test.getLevel().destroyBlock(test.absolutePos(pos), true);
        test.assertItemEntityCountIs(Fromage.BLUE_WEDGE.get(), pos, 2, 4);
        test.assertItemEntityNotPresent(Fromage.AGED_BLUE_ITEM.get(), pos, 2);
        verifyCrafting(test, Fromage.AGED_BLUE_ITEM.get().getDefaultInstance(), Fromage.BLUE_WEDGE.get().getDefaultInstance());
        test.succeed();
    }

    @GameTest(template = "kitchen")
    public static void mildCheeseYieldsExactlyFourSlices(GameTestHelper test) {
        BlockPos pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, Fromage.AGED_CHEESE.get());
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        for (int count = 3; count >= 1; count--) {
            test.useBlock(pos, player);
            test.assertTrue(test.getBlockState(pos).getValue(CheeseBlock.Sliceable.SLICES) == count,
                "Unexpected remaining slice count");
        }
        test.useBlock(pos, player);
        test.assertBlockPresent(Blocks.AIR, pos);
        test.assertItemEntityCountIs(Fromage.MILD_WEDGE.get(), pos, 2, 4);
        verifyCrafting(test, Fromage.AGED_CHEESE_ITEM.get().getDefaultInstance(), Fromage.MILD_WEDGE.get().getDefaultInstance());
        test.succeed();
    }

    private static void verifyCrafting(GameTestHelper test, ItemStack block, ItemStack expected) {
        var input = CraftingInput.of(1, 1, java.util.List.of(block));
        var recipe = test.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel());
        test.assertTrue(recipe.isPresent(), "Missing inventory slicing recipe");
        var result = recipe.orElseThrow().value().assemble(input, test.getLevel().registryAccess());
        test.assertTrue(result.is(expected.getItem()) && result.getCount() == 4, "Crafting did not yield four matching wedges");
    }
}



