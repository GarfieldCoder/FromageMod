package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

final class CheeseTopping {
    private static final DustParticleOptions AROMA = new DustParticleOptions(new Vector3f(.44f, .69f, .19f), .75f);

    static boolean isMeal(BlockState state) {
        return state.is(Blocks.CAKE) || state.is(Fromage.CHEDDAR_CAKE.get()) || state.getBlock() instanceof PastaBowlBlock;
    }

    static boolean isTopped(BlockState state) {
        return state.is(Fromage.CHEDDAR_CAKE.get()) || state.is(Fromage.CHEDDAR_PASTA.get());
    }

    static boolean top(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.CAKE)) {
            return level.setBlockAndUpdate(pos, Fromage.CHEDDAR_CAKE.get().defaultBlockState()
                .setValue(CakeBlock.BITES, state.getValue(CakeBlock.BITES)));
        }
        if (state.is(Fromage.PASTA.get())) return level.setBlockAndUpdate(pos, Fromage.CHEDDAR_PASTA.get().defaultBlockState());
        return false;
    }

    static void keepMealInPlace(PlayerInteractEvent.RightClickBlock event) {
        if (!isMeal(event.getLevel().getBlockState(event.getPos()))) return;
        if (event.getHand() == InteractionHand.MAIN_HAND && (event.getItemStack().is(Fromage.GRATED_CHEDDAR.get())
                || event.getItemStack().is(Fromage.GRATER.get()))) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    static void showAroma(Level level, BlockPos pos, RandomSource random, double height) {
        if (random.nextInt(4) != 0) return;
        level.addParticle(AROMA, pos.getX() + .30 + random.nextDouble() * .40,
            pos.getY() + height, pos.getZ() + .30 + random.nextDouble() * .40, 0, .03, 0);
    }

    private CheeseTopping() {}
}
