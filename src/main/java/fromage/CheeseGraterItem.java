package fromage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

final class CheeseGraterItem extends Item {
    private static final int GRATE_TICKS = 30;
    private final Map<Player, GlobalPos> targets = new WeakHashMap<>();

    CheeseGraterItem(Properties properties) {
        super(properties);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND
                || !CheeseTopping.isMeal(level.getBlockState(pos))) return InteractionResult.PASS;
        if (!player.getOffhandItem().is(Fromage.CHEDDAR_WEDGE.get())) return InteractionResult.FAIL;
        if (!player.isUsingItem()) {
            player.startUsingItem(InteractionHand.MAIN_HAND);
            if (!level.isClientSide && player.isUsingItem()) {
                targets.put(player, GlobalPos.of(level.dimension(), pos.immutable()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return GRATE_TICKS;
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        BlockPos pos = target(player);
        if (pos == null) {
            player.stopUsingItem();
            targets.remove(player);
        } else if (remaining % 6 == 0) {
            server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, .30f, 1.55f);
            var cheese = new ItemParticleOption(ParticleTypes.ITEM, Fromage.GRATED_CHEDDAR.get().getDefaultInstance());
            server.sendParticles(cheese, pos.getX() + .5, pos.getY() + .65, pos.getZ() + .5, 4, .14, .07, .14, .025);
        }
    }

    @Override public void onStopUsing(ItemStack stack, LivingEntity entity, int remaining) {
        if (!entity.level().isClientSide && entity instanceof Player player) targets.remove(player);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) return stack;
        BlockPos pos = target(player);
        targets.remove(player);
        if (pos == null) return stack;

        boolean topped = CheeseTopping.top(level, pos);
        player.getOffhandItem().consume(1, player);
        player.getInventory().placeItemBackInInventory(new ItemStack(Fromage.GRATED_CHEDDAR.get(), topped ? 1 : 2));
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .55f, 1.25f);
        return stack;
    }

    private BlockPos target(Player player) {
        GlobalPos target = targets.get(player);
        Level level = player.level();
        if (target == null || !target.dimension().equals(level.dimension())
                || !level.hasChunkAt(target.pos()) || !player.getOffhandItem().is(Fromage.CHEDDAR_WEDGE.get())) return null;
        if (!(player.pick(player.blockInteractionRange(), 1f, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(target.pos())) return null;
        return CheeseTopping.isMeal(level.getBlockState(target.pos())) ? target.pos() : null;
    }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                    ItemStack stack, float partialTick, float equipProgress, float swingProgress) {
                if (!player.isUsingItem() || !player.getUseItem().is(CheeseGraterItem.this)
                        || player.getUsedItemHand() != InteractionHand.MAIN_HAND || arm != player.getMainArm()) return false;
                float elapsed = GRATE_TICKS - player.getUseItemRemainingTicks() + partialTick;
                float stroke = Mth.sin(elapsed * Mth.TWO_PI / 6);
                float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                pose.translate(-side * .26f, -.12f + .065f * stroke, -.22f);
                pose.mulPose(Axis.XP.rotationDegrees(25 + 11 * stroke));
                pose.mulPose(Axis.ZP.rotationDegrees(side * 12));
                return false;
            }
        });
    }
}
