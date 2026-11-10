package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fromage")
@PrefixGameTestTemplate(false)
public final class ShelfMigrationTests {
    private static final BlockPos SHELF = new BlockPos(4, 2, 6);

    @GameTest(template = "kitchen")
    public static void legacyCodesRoundTrip(GameTestHelper test) {
        for (int slot = 0; slot <= 16; slot++) {
            int restored = AgingShelfBlock.Wheel.fromLegacy(slot).legacy();
            test.assertTrue(restored == slot, "Shelf code " + slot + " came back as " + restored);
        }
        test.succeed();
    }

    @GameTest(template = "kitchen")
    public static void occupiedShelfSurvivesReload(GameTestHelper test) {
        // 10 is a turned developing cheddar. 16 is a turned extra-aged blue wheel.
        BlockState legacy = Fromage.SHELF.get().defaultBlockState()
            .setValue(AgingShelfBlock.LOWER, 10)
            .setValue(AgingShelfBlock.UPPER, 16);
        test.setBlock(SHELF, legacy);
        ServerLevel level = test.getLevel();
        AgingShelfBlock.Slots slots = slots(test, SHELF);
        test.assertTrue("cheddar".equals(slots.kind(false)) && slots.phase(false) == 2 && slots.turned(false),
            "Old lower slot was not read as turned developing cheddar");
        test.assertTrue("blue".equals(slots.kind(true)) && slots.phase(true) == 4 && slots.turned(true),
            "Old upper slot was not read as turned extra-aged blue");
        assertMirror(test, SHELF, 10, 16);

        long now = level.getGameTime();
        CompoundTag oldSave = new CompoundTag();
        oldSave.putLong("LowerReadyAt", now + 250);
        oldSave.putLong("UpperReadyAt", now + 10);
        oldSave.putLong("LowerBonusAt", now + 90);
        oldSave.putLong("UpperBonusAt", now + 40);
        oldSave.putLong("LowerLastTurn", now - 3);
        oldSave.putLong("UpperLastTurn", now - 1);
        slots.loadAdditional(oldSave, level.registryAccess());
        slots.onLoad();
        test.assertTrue("cheddar".equals(slots.kind(false)) && slots.phase(false) == 2 && slots.turned(false),
            "Importing an old save changed the lower cheese");
        test.assertTrue("blue".equals(slots.kind(true)) && slots.phase(true) == 4 && slots.turned(true),
            "Importing an old save changed the upper cheese");
        test.assertTrue(slots.readyAt(false) == now + 250 && slots.readyAt(true) == now + 10,
            "Old aging progress was dropped");
        test.assertTrue(slots.bonusAt(false) == now + 90 && slots.bonusAt(true) == now + 40,
            "Old turn cooldown was dropped");
        test.assertTrue(slots.lastTurn(false) == now - 3 && slots.lastTurn(true) == now - 1,
            "Old turn lock was dropped");
        assertMirror(test, SHELF, 10, 16);

        CompoundTag saved = slots.saveWithoutMetadata(level.registryAccess());
        BlockPos copy = SHELF.east();
        test.setBlock(copy, Fromage.SHELF.get());
        AgingShelfBlock.Slots copied = slots(test, copy);
        copied.loadWithComponents(saved, level.registryAccess());
        copied.onLoad();
        assertSameShelf(test, slots, copied, "Save and reload");
        assertMirror(test, copy, 10, 16);

        BlockPos absolute = test.absolutePos(copy);
        BlockState visible = level.getBlockState(absolute);
        CompoundTag chunkTag = copied.saveWithId(level.registryAccess());
        level.getChunkSource().save(true);
        LevelChunk chunk = level.getChunkAt(absolute);
        chunk.removeBlockEntity(absolute);
        BlockEntity reloaded = BlockEntity.loadStatic(absolute, visible, chunkTag, level.registryAccess());
        test.assertTrue(reloaded instanceof AgingShelfBlock.Slots, "Chunk reload did not restore the shelf");
        reloaded.setLevel(level);
        chunk.setBlockEntity(reloaded);
        reloaded.onLoad();
        assertSameShelf(test, slots, (AgingShelfBlock.Slots) reloaded, "Chunk unload");
        assertMirror(test, copy, 10, 16);
        test.succeed();
    }

    private static AgingShelfBlock.Slots slots(GameTestHelper test, BlockPos pos) {
        return (AgingShelfBlock.Slots) test.getBlockEntity(pos);
    }

    private static void assertMirror(GameTestHelper test, BlockPos pos, int lower, int upper) {
        BlockState state = test.getBlockState(pos);
        test.assertTrue(state.getValue(AgingShelfBlock.LOWER) == lower
                && state.getValue(AgingShelfBlock.UPPER) == upper,
            "Visible shelf changed from " + lower + "/" + upper + " to "
                + state.getValue(AgingShelfBlock.LOWER) + "/" + state.getValue(AgingShelfBlock.UPPER));
    }

    private static void assertSameShelf(GameTestHelper test, AgingShelfBlock.Slots expected,
            AgingShelfBlock.Slots actual, String where) {
        test.assertTrue(expected.kind(false).equals(actual.kind(false))
                && expected.phase(false) == actual.phase(false)
                && expected.turned(false) == actual.turned(false)
                && expected.kind(true).equals(actual.kind(true))
                && expected.phase(true) == actual.phase(true)
                && expected.turned(true) == actual.turned(true),
            where + " lost a cheese");
        test.assertTrue(expected.readyAt(false) == actual.readyAt(false)
                && expected.readyAt(true) == actual.readyAt(true),
            where + " lost aging progress");
        test.assertTrue(expected.bonusAt(false) == actual.bonusAt(false)
                && expected.bonusAt(true) == actual.bonusAt(true),
            where + " lost the turn cooldown");
        test.assertTrue(expected.lastTurn(false) == actual.lastTurn(false)
                && expected.lastTurn(true) == actual.lastTurn(true),
            where + " lost the turn lock");
    }
}
