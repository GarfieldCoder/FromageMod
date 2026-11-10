package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class YoungWheelBlock extends Block implements EntityBlock {
    private static final String AGE_TICKS = "FromageAgeTicks";
    private static final String TURN_READY = "FromageTurnReady";
    static final BooleanProperty PIERCED = BooleanProperty.create("pierced");
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 6, 14);
    final boolean blue;

    YoungWheelBlock(Properties properties, boolean blue) {
        super(properties);
        this.blue = blue;
        registerDefaultState(stateDefinition.any().setValue(PIERCED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIERCED);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    static int elapsed(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(AGE_TICKS),
            0, AgingShelfBlock.AGE_TICKS - 1);
    }

    static long turnReady(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.contains(TURN_READY) ? data.getLong(TURN_READY) : 0;
    }

    static ItemStack asItem(boolean blue, boolean pierced, int elapsed) {
        return asItem(blue, pierced, elapsed, 0);
    }

    static ItemStack asItem(boolean blue, boolean pierced, int elapsed, long turnReady) {
        ItemStack stack = (blue ? Fromage.YOUNG_BLUE.get() : Fromage.YOUNG_CHEDDAR.get()).getDefaultInstance();
        if (pierced) stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(PIERCED, true));
        if (elapsed > 0 || turnReady > 0) {
            CompoundTag data = new CompoundTag();
            if (elapsed > 0) data.putInt(AGE_TICKS, elapsed);
            if (turnReady > 0) data.putLong(TURN_READY, turnReady);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WheelData(pos, state);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof WheelData data) {
            data.elapsed = elapsed(stack);
            data.turnReady = turnReady(stack);
            data.setChanged();
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WheelData data) {
            data.pickedUp = true;
            if (level.removeBlock(pos, false)) {
                player.getInventory().placeItemBackInInventory(asItem(blue, state.getValue(PIERCED), data.elapsed, data.turnReady));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .55f, .95f);
            } else {
                data.pickedUp = false;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!level.isClientSide && !moved && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof WheelData data && !data.pickedUp) {
            popResource(level, pos, asItem(blue, state.getValue(PIERCED), data.elapsed, data.turnReady));
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    static final class WheelData extends BlockEntity {
        private int elapsed;
        private long turnReady;
        private boolean pickedUp;

        WheelData(BlockPos pos, BlockState state) {
            super(Fromage.WHEEL_ENTITY.get(), pos, state);
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            elapsed = Math.clamp(tag.getInt(AGE_TICKS), 0, AgingShelfBlock.AGE_TICKS - 1);
            turnReady = tag.contains(TURN_READY) ? tag.getLong(TURN_READY) : 0;
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt(AGE_TICKS, elapsed);
            tag.putLong(TURN_READY, turnReady);
        }
    }
}
