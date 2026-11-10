package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class FireboxBlock extends Fromage.Station implements EntityBlock {
    static final int COAL_TICKS = 1600;
    private static final int CAPACITY = 4 * COAL_TICKS;

    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int HEARTH_HEIGHT = 10;
    private static final VoxelShape SHAPE = Shapes.or(
        box(0, 0, 0, 16, 11, 16),
        box(1, 11, 1, 15, 16, 3), box(1, 11, 13, 15, 16, 15),
        box(1, 11, 3, 3, 16, 13), box(13, 11, 3, 15, 16, 13));

    FireboxBlock(Properties properties) {
        super(properties, true);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
            .setValue(BlockStateProperties.LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, BlockStateProperties.LIT);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(migrateBasin(state, level, pos), level, pos, random);
    }

    private static BlockState migrateBasin(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) return state;
        BlockPos above = pos.above();
        BlockState oldBasin = level.getBlockState(above);
        if (!oldBasin.is(Fromage.FIREBOX_BASIN.get())) return state;

        if (state.getValue(STAGE) == 0 && oldBasin.getValue(STAGE) != 0) {
            state = withBatch(state.setValue(STAGE, oldBasin.getValue(STAGE)), batch(oldBasin));
            level.setBlockAndUpdate(pos, state);
            // Clear the old contents before removal so cut curds cannot be recovered twice.
            level.setBlock(above, oldBasin.setValue(STAGE, 0), Block.UPDATE_CLIENTS);
        }
        level.removeBlock(above, false);
        return state;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean coal = stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        boolean shovel = stack.is(ItemTags.SHOVELS);
        state = migrateBasin(state, level, pos);
        if (!coal && !flint && !shovel) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Fuel fuel) {
            long now = level.getGameTime();
            boolean lit = state.getValue(BlockStateProperties.LIT);
            int left = fuel.remaining(now);
            if (coal && left <= CAPACITY - COAL_TICKS) {
                stack.consume(1, player);
                fuel.light(now, left + COAL_TICKS);
                setLit(level, pos, state, true);
                if (!lit) igniteSound(level, pos);
            } else if (flint && !lit && left > 0) {
                fuel.light(now, left);
                setLit(level, pos, state, true);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                igniteSound(level, pos);
            } else if (shovel && lit) {
                fuel.bank(left);
                setLit(level, pos, state, false);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .55f, 1f);
            }
            showFuel(player, level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        state = migrateBasin(state, level, pos);
        if (state.getValue(STAGE) == WHEY_IN_VAT || aimedAtBasin(pos, hit)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide) showFuel(player, level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean aimedAtBasin(BlockPos pos, BlockHitResult hit) {
        return hit.getLocation().y - pos.getY() >= HEARTH_HEIGHT / 16.0;
    }

    private static void showFuel(Player player, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Fuel fuel)) return;
        int remaining = fuel.remaining(level.getGameTime());
        String key = remaining == 0 ? "empty" :
            level.getBlockState(pos).getValue(BlockStateProperties.LIT) ? "lit" : "banked";
        player.displayClientMessage(Component.translatable("message.fromage.firebox." + key,
            (remaining + 19) / 20), true);
    }

    private static void igniteSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, .65f, 1f);
    }

    private static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, lit));
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
            boolean moved) {
        if (!level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockState(pos.above()).is(Fromage.FIREBOX_BASIN.get())) {
            level.removeBlock(pos.above(), false);
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Fuel(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != Fromage.FIREBOX_FUEL.get() || !state.getValue(BlockStateProperties.LIT)) {
            return null;
        }
        return (world, pos, blockState, entity) -> {
            Fuel fuel = (Fuel) entity;
            if (fuel.remaining(world.getGameTime()) == 0) {
                fuel.bank(0);
                setLit(world, pos, blockState, false);
            }
        };
    }

    static final class Fuel extends BlockEntity {
        private long burnsUntil = -1;
        private int bankedTicks;

        Fuel(BlockPos pos, BlockState state) {
            super(Fromage.FIREBOX_FUEL.get(), pos, state);
        }

        @Override public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server && level.getBlockState(worldPosition.above()).is(Fromage.FIREBOX_BASIN.get())) {
                server.scheduleTick(worldPosition, Fromage.FIREBOX.get(), 1);
            }
        }

        int remaining(long now) {
            return burnsUntil < 0 ? bankedTicks : (int) Math.clamp(burnsUntil - now, 0, CAPACITY);
        }

        void light(long now, int ticks) {
            burnsUntil = now + ticks;
            bankedTicks = 0;
            setChanged();
        }

        void bank(int ticks) {
            burnsUntil = -1;
            bankedTicks = ticks;
            setChanged();
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putLong("BurnsUntil", burnsUntil);
            tag.putInt("BankedTicks", bankedTicks);
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            burnsUntil = tag.contains("BurnsUntil") ? tag.getLong("BurnsUntil") : -1;
            bankedTicks = Math.clamp(tag.getInt("BankedTicks"), 0, CAPACITY);
        }
    }
}




