package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

class CheeseBlock extends Block {
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 7, 14);

    CheeseBlock(Properties properties) {
        super(properties);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    static final class Sliceable extends CheeseBlock {
        static final IntegerProperty SLICES = IntegerProperty.create("slices", 1, 4);
        private static final VoxelShape ONE = box(2, 0, 8, 8, 7, 14);
        private static final VoxelShape TWO = box(2, 0, 8, 14, 7, 14);
        private static final VoxelShape THREE = Shapes.or(TWO, box(2, 0, 2, 8, 7, 8));

        private final java.util.function.Supplier<? extends Item> wedge;

        Sliceable(Properties properties, java.util.function.Supplier<? extends Item> wedge) {
            super(properties);
            this.wedge = wedge;
            registerDefaultState(stateDefinition.any().setValue(SLICES, 4));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SLICES);
        }

        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(SLICES)) {
                case 1 -> ONE;
                case 2 -> TWO;
                case 3 -> THREE;
                default -> SHAPE;
            };
        }

        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                Player player, BlockHitResult hit) {
            if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                int slices = state.getValue(SLICES);
                if (slices == 1) level.removeBlock(pos, false);
                else level.setBlockAndUpdate(pos, state.setValue(SLICES, slices - 1));


                popResource(level, pos, new ItemStack(wedge.get()));
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, .45f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    static final class Young extends CheeseBlock implements EntityBlock {
        static final int RIPENING_TICKS = 15 * 20;

        Young(Properties properties) {
            super(properties);
        }

        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Batch(pos, state);
        }

        @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                ItemStack stack) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof Batch batch) {
                batch.start(level.getGameTime());
            }
        }

        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                BlockEntityType<T> type) {
            return Batch.serverTicker(level, type);
        }
    }

    static final class Batch extends BlockEntity {
        private static final String READY_AT = "ReadyAt";
        private long readyAt = -1;

        Batch(BlockPos pos, BlockState state) {
            super(Fromage.RIPENING.get(), pos, state);
        }

        void start(long gameTime) {
            readyAt = gameTime + Young.RIPENING_TICKS;
            setChanged();
        }

        void beginForming(long gameTime) {
            readyAt = gameTime;
            setChanged();
        }

        static <T extends BlockEntity> BlockEntityTicker<T> serverTicker(Level level, BlockEntityType<T> type) {
            if (level.isClientSide || type != Fromage.RIPENING.get()) return null;
            return (world, pos, state, entity) -> ((Batch) entity).ripen((ServerLevel) world, state);
        }

        private void ripen(ServerLevel level, BlockState state) {
            boolean mold = state.is(Fromage.MOLD.get());
            if (mold && (state.getValue(CheeseMoldBlock.PORTIONS) != 2
                    || state.getValue(CheeseMoldBlock.AGE) == 2)) return;

            long now = level.getGameTime();
            if (mold && state.getValue(CheeseMoldBlock.FORMING)) {
                if (readyAt < 0 || readyAt > now) beginForming(now);
                CheeseMoldBlock.updateForming(level, worldPosition, state, now - readyAt);
                return;
            }
            if (readyAt < 0) {
                start(now);
                return;
            }
            if (now >= readyAt) {
                BlockState aged = mold ? state.setValue(CheeseMoldBlock.AGE, 2)
                    : Fromage.AGED_CHEESE.get().defaultBlockState();
                level.setBlockAndUpdate(worldPosition, aged);
                level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, .45f, 1.25f);
            } else if (mold && state.getValue(CheeseMoldBlock.AGE) == 0 && readyAt - now <= Young.RIPENING_TICKS / 2) {
                level.setBlockAndUpdate(worldPosition, state.setValue(CheeseMoldBlock.AGE, 1));
            }
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            readyAt = tag.contains(READY_AT) ? tag.getLong(READY_AT) : -1;
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putLong(READY_AT, readyAt);
        }
    }
}





