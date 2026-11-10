package fromage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;


import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

final class CheeseMoldItem extends BlockItem {
    private static final int PACK_TICKS = 20;
    private final Map<LivingEntity, GlobalPos> targets = new WeakHashMap<>();

    CheeseMoldItem(Block block, Properties properties) {
        super(block, properties);
    }

    void startPacking(Player player, InteractionHand hand, BlockPos pos) {
        if (player.isUsingItem()) return;
        player.startUsingItem(hand);
        if (!player.level().isClientSide && player.isUsingItem()) {
            targets.put(player, GlobalPos.of(player.level().dimension(), pos.immutable()));
        }
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return PACK_TICKS;
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof Player player && packingTarget(player) == null) {
            entity.stopUsingItem();
        }
    }

    @Override public void onStopUsing(ItemStack stack, LivingEntity entity, int remaining) {
        if (!entity.level().isClientSide) targets.remove(entity);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) return stack;
        BlockPos pos = packingTarget(player);
        targets.remove(entity);
        if (pos == null) return stack;

        DairyBatch batch = Fromage.Station.batch(level.getBlockState(pos));
        Fromage.TABLE.get().packCheese(level, pos);
        ItemStack packed = (batch == DairyBatch.CHEDDAR ? Fromage.PACKED_CHEDDAR_MOLD.get()
            : Fromage.PACKED_BLUE_MOLD.get()).getDefaultInstance();
        if (stack.getCount() == 1) return packed;

        // Older saves may still contain stacks of empty molds.
        stack.shrink(1);
        player.getInventory().placeItemBackInInventory(packed);
        return stack;
    }

    private BlockPos packingTarget(Player player) {
        GlobalPos target = targets.get(player);
        Level level = player.level();
        if (target == null || !target.dimension().equals(level.dimension()) || !level.hasChunkAt(target.pos())) {
            return null;
        }
        if (!(player.pick(player.blockInteractionRange(), 1f, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(target.pos())) return null;
        var state = level.getBlockState(target.pos());
        return state.is(Fromage.TABLE.get()) && Fromage.Station.canPack(state) ? target.pos() : null;
    }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                    ItemStack stack, float partialTick, float equipProgress, float swingProgress) {
                HumanoidArm activeArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? player.getMainArm() : player.getMainArm().getOpposite();
                if (!player.isUsingItem() || !player.getUseItem().is(CheeseMoldItem.this) || arm != activeArm) return false;
                float elapsed = PACK_TICKS - player.getUseItemRemainingTicks() + partialTick;
                float progress = Mth.clamp(elapsed / PACK_TICKS, 0f, 1f);
                float reach = Mth.sin(progress * Mth.PI);
                float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
                pose.translate(-side * .28f * reach, -.17f * reach, -.24f * reach);
                pose.mulPose(Axis.XP.rotationDegrees(32f * reach));
                pose.mulPose(Axis.ZP.rotationDegrees(side * 8f * reach));
                return false;
            }
        });
    }

    static final class Packed extends BlockItem {
        private final DairyBatch batch;

        Packed(Block block, Properties properties, DairyBatch batch) {
            super(block, properties);
            this.batch = batch;
        }

        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            BlockState state = super.getPlacementState(context);
            return state == null ? null : state.setValue(CheeseMoldBlock.PORTIONS, 2).setValue(CheeseMoldBlock.BLUE, batch == DairyBatch.BLUE)
                .setValue(CheeseMoldBlock.CHEDDAR, batch == DairyBatch.CHEDDAR);
        }

        @Override public String getDescriptionId() {
            return getOrCreateDescriptionId();
        }

        @Override public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
            // The empty mold remains the canonical item for the placed block.
        }

        @Override public void removeFromBlockToItemMap(Map<Block, Item> blockToItemMap, Item item) {}
    }
}



