package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class FireboxBasinBlock extends Fromage.Station {
    static final BooleanProperty HEATED = BooleanProperty.create("heated");
    private static final VoxelShape SHAPE = Shapes.or(
        box(1, 0, 1, 15, 1, 15),
        box(1, 1, 1, 15, 8, 3), box(1, 1, 13, 15, 8, 15),
        box(1, 1, 3, 3, 8, 13), box(13, 1, 3, 15, 8, 13));

    FireboxBasinBlock(Properties properties) {
        super(properties, true);
        registerDefaultState(defaultBlockState().setValue(HEATED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEATED);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(HEATED, neighbor.is(Fromage.FIREBOX.get()) && neighbor.getValue(BlockStateProperties.LIT));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

}

