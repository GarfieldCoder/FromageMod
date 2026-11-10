package fromage;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

final class GratedCheddarItem extends Item {
    GratedCheddarItem(Properties properties) {
        super(properties);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = level.getBlockState(pos);
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND || !CheeseTopping.isMeal(state)) {
            return InteractionResult.PASS;
        }
        if (CheeseTopping.isTopped(state)) return InteractionResult.FAIL;
        if (!level.isClientSide && CheeseTopping.top(level, pos)) {
            context.getItemInHand().consume(1, player);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .4f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
