package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class PastaBowlBlock extends Block {
    private static final VoxelShape BOWL = box(3, 0, 3, 13, 4, 13);
    private final boolean topped;

    PastaBowlBlock(Properties properties, boolean topped) {
        super(properties);
        this.topped = topped;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOWL;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.canEat(false)) return InteractionResult.PASS;
        if (!level.isClientSide && level.removeBlock(pos, false)) {
            player.getFoodData().eat(6, .45f);
            if (topped) PungentEffect.apply(player);
            player.getInventory().placeItemBackInInventory(Items.BOWL.getDefaultInstance());
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, .55f, 1.05f);
            level.gameEvent(player, GameEvent.EAT, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (topped) CheeseTopping.showAroma(level, pos, random, .31);
    }
}
