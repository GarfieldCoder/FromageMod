package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class CheddarCakeBlock extends CakeBlock {
    CheddarCakeBlock(Properties properties) {
        super(properties);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ItemTags.CANDLES)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide && result.consumesAction()) PungentEffect.apply(player);
        return result;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        CheeseTopping.showAroma(level, pos, random, .56);
    }
}
