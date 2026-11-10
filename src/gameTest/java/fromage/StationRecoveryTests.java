package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fromage")
@PrefixGameTestTemplate(false)
public class StationRecoveryTests {
    @GameTest(template = "kitchen")
    public static void movedTableKeepsContentsWithoutDrops(GameTestHelper test) {
        BlockPos table = new BlockPos(3, 2, 3);
        test.setBlock(table, Fromage.TABLE.get().defaultBlockState().setValue(Fromage.Station.STAGE, 2));
        test.setBlock(table.west(), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        test.setBlock(table.west(2), Blocks.REDSTONE_BLOCK);
        test.runAfterDelay(8, () -> {
            var moved = test.getBlockState(table.east());
            test.assertTrue(moved.is(Fromage.TABLE.get()) && moved.getValue(Fromage.Station.STAGE) == 2,
                "Piston did not preserve loaded table state");
            test.assertItemEntityNotPresent(Fromage.CURDS.get(), table, 3);
            test.assertItemEntityNotPresent(Fromage.CLOTH.get(), table, 3);
        });
        test.runAfterDelay(Fromage.Station.DRAIN_TICKS + 12, () -> {
            test.assertTrue(test.getBlockState(table.east()).getValue(Fromage.Station.STAGE) == 3,
                "The moved table did not finish draining");
            test.succeed();
        });
    }

    @GameTest(template = "kitchen")
    public static void breakingStationsRefundsRecoverableContents(GameTestHelper test) {
        BlockPos table = new BlockPos(2, 2, 2);
        BlockPos vat = new BlockPos(5, 2, 2);
        test.setBlock(table, Fromage.TABLE.get().defaultBlockState().setValue(Fromage.Station.STAGE, 2));
        test.setBlock(vat, Fromage.VAT.get().defaultBlockState().setValue(Fromage.Station.STAGE, 5));
        test.getLevel().destroyBlock(test.absolutePos(table), true);
        test.getLevel().destroyBlock(test.absolutePos(vat), true);
        test.assertItemEntityCountIs(Fromage.CLOTH.get(), table, 1, 1);
        test.assertItemEntityCountIs(Fromage.CURDS.get(), table, 1, 1);
        test.assertItemEntityCountIs(Fromage.CURDS.get(), vat, 1, 1);
        test.succeed();
    }
}

