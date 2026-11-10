package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class CheesePressBlock extends Block {
    static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final int PRESS_TICKS = 80;
    private static final VoxelShape FRAME = Shapes.or(
        box(0, 0, 2, 16, 2, 14), box(0, 2, 3, 2, 13, 13), box(14, 2, 3, 16, 13, 13),
        box(0, 12, 3, 16, 14, 13), box(4, 14, 7, 12, 16, 9));
    private static final VoxelShape FRAME_SIDE = Shapes.or(
        box(2, 0, 0, 14, 2, 16), box(3, 2, 0, 13, 13, 2), box(3, 2, 14, 13, 13, 16),
        box(3, 12, 0, 13, 14, 16), box(7, 14, 4, 9, 16, 12));
    private static final VoxelShape MOLD = box(3, 2, 3, 13, 5, 13);

    CheesePressBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(FACING, Direction.SOUTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, FACING);
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
        VoxelShape frame = state.getValue(FACING).getAxis() == Direction.Axis.X ? FRAME_SIDE : FRAME;
        return state.getValue(STAGE) == 0 ? frame : Shapes.or(frame, MOLD);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Fromage.PACKED_CHEDDAR_MOLD.get()) || state.getValue(STAGE) != 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            stack.consume(1, player);
            level.setBlockAndUpdate(pos, state.setValue(STAGE, 1));
            sound(level, pos, .8f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int stage = state.getValue(STAGE);
        if (player.getMainHandItem().isEmpty() && stage == 1) {
            level.setBlockAndUpdate(pos, state.setValue(STAGE, 2));
            level.scheduleTick(pos, this, 8);
            sound(level, pos, .7f);
        } else if (player.getMainHandItem().isEmpty() && stage == 5) {
            level.setBlockAndUpdate(pos, state.setValue(STAGE, 0));
            player.getInventory().placeItemBackInInventory(Fromage.YOUNG_CHEDDAR.get().getDefaultInstance());
            player.getInventory().placeItemBackInInventory(Fromage.MOLD_ITEM.get().getDefaultInstance());
            sound(level, pos, 1.15f);
        }
        return InteractionResult.CONSUME;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage < 2 || stage >= 5) return;
        level.setBlockAndUpdate(pos, state.setValue(STAGE, stage + 1));
        if (stage < 4) {
            Direction facing = state.getValue(FACING);
            level.sendParticles(Fromage.WHEY_DROP.get(), pos.getX() + .5 + .39 * facing.getStepX(),
                pos.getY() + .19, pos.getZ() + .5 + .39 * facing.getStepZ(), 2, .035, .01, .035, 0);
            level.scheduleTick(pos, this, stage == 2 ? PRESS_TICKS - 16 : 8);
        }
        sound(level, pos, stage == 4 ? 1.2f : .8f);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            int stage = state.getValue(STAGE);
            if (stage == 5) {
                popResource(level, pos, Fromage.YOUNG_CHEDDAR.get().getDefaultInstance());
                popResource(level, pos, Fromage.MOLD_ITEM.get().getDefaultInstance());
            } else if (stage > 0) {
                popResource(level, pos, Fromage.PACKED_CHEDDAR_MOLD.get().getDefaultInstance());
            }
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    private static void sound(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .55f, pitch);
    }
}

