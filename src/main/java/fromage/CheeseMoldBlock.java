package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class CheeseMoldBlock extends Block implements EntityBlock {
    static final IntegerProperty PORTIONS = IntegerProperty.create("portions", 0, 2);
    static final BooleanProperty BLUE = BooleanProperty.create("blue");
    static final BooleanProperty CHEDDAR = BooleanProperty.create("cheddar");
    static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
    static final BooleanProperty FORMING = BooleanProperty.create("forming");
    static final IntegerProperty PRESS_DEPTH = IntegerProperty.create("press_depth", 0, 3);
    static final int BEAT_TICKS = 40;
    private static final VoxelShape SHAPE = Shapes.or(
        box(0, 0, 0, 16, 1, 16),
        box(0, 1, 0, 16, 8, 2), box(0, 1, 14, 16, 8, 16),
        box(0, 1, 2, 2, 8, 14), box(14, 1, 2, 16, 8, 14));

    CheeseMoldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PORTIONS, 0).setValue(BLUE, false)
            .setValue(CHEDDAR, false).setValue(AGE, 0).setValue(FORMING, false).setValue(PRESS_DEPTH, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORTIONS, BLUE, CHEDDAR, AGE, FORMING, PRESS_DEPTH);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CHEDDAR) && state.getValue(FORMING) && state.getValue(AGE) == 1) {
            int plateY = 7 - state.getValue(PRESS_DEPTH);
            return Shapes.or(SHAPE, box(2.75, plateY, 2.75, 13.25, plateY + 1, 13.25),
                box(6, plateY + 1, 7, 10, plateY + 3, 9));
        }
        return SHAPE;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CheeseBlock.Batch(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return state.getValue(PORTIONS) == 2 && state.getValue(AGE) < 2
            && (state.getValue(FORMING) ? state.getValue(AGE) == 1 : !state.getValue(CHEDDAR))
            ? CheeseBlock.Batch.serverTicker(level, type) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
            ItemStack stack) {
        if (state.getValue(PORTIONS) == 2 && !state.getValue(CHEDDAR)) startRipening(level, pos);
    }

    private static void startRipening(Level level, BlockPos pos) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CheeseBlock.Batch batch) {
            batch.start(level.getGameTime());
        }
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int portions = state.getValue(PORTIONS);
        boolean wholeBatch = stack.is(Fromage.DRAINED_FARMER.get()) || stack.is(Fromage.DRAINED_BLUE.get());
        boolean blue = stack.is(Fromage.DRAINED_BLUE.get());
        boolean cheddar = stack.is(Fromage.SALTED_CURDS.get());
        if (stack.is(Fromage.FRESH.get()) || stack.is(Fromage.BLUE_FRESH.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(stack.is(Fromage.BLUE_FRESH.get())
                    ? "message.fromage.mold.blue_fresh" : "message.fromage.mold.soft"), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if ((!wholeBatch && !blue && !cheddar) || portions == 2) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (wholeBatch && portions != 0) return ItemInteractionResult.FAIL;
        if (portions == 1 && (state.getValue(BLUE) != blue || state.getValue(CHEDDAR) != cheddar)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.fromage.mold.mixed"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            int amount = cheddar ? Math.min(2 - portions, stack.getCount()) : 1;
            boolean forming = wholeBatch || cheddar;
            int loaded = wholeBatch ? 2 : portions + amount;
            BlockState packed = state.setValue(PORTIONS, loaded).setValue(BLUE, blue)
                .setValue(CHEDDAR, cheddar).setValue(FORMING, forming).setValue(AGE, 0);
            level.setBlockAndUpdate(pos, packed);
            stack.consume(amount, player);
            if (loaded == 2 && forming && !cheddar) startForming(level, pos, packed);
            else if (loaded == 2 && !forming) startRipening(level, pos);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, .65f, .85f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (state.getValue(PORTIONS) == 2 && player.getMainHandItem().isEmpty()) {
                if (state.getValue(CHEDDAR) && state.getValue(AGE) < 2) {
                    press(level, pos, state);
                    return InteractionResult.CONSUME;
                }
                if (state.getValue(AGE) < 2) return InteractionResult.CONSUME;
                ItemStack output = finished(state);
                level.setBlockAndUpdate(pos, defaultBlockState());
                player.getInventory().placeItemBackInInventory(output);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .55f, .9f);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void startForming(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CheeseBlock.Batch batch) {
            batch.beginForming(level.getGameTime());
            level.setBlockAndUpdate(pos, state.setValue(FORMING, true).setValue(AGE, 1).setValue(PRESS_DEPTH, 0));
        }
    }

    private static void press(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(AGE) == 1) return;
        startForming(level, pos, state);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .45f, .78f);
    }

    static void updateForming(ServerLevel level, BlockPos pos, BlockState state, long elapsed) {
        if (state.getValue(CHEDDAR)) {
            if (elapsed >= BEAT_TICKS) {
                level.setBlockAndUpdate(pos, state.setValue(PRESS_DEPTH, 3).setValue(AGE, 2));
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .65f, 1.2f);
                level.sendParticles(Fromage.WHEY_DROP.get(), pos.getX() + .5, pos.getY() + .10, pos.getZ() + .82,
                    3, .06, .02, .07, 0);
            } else {
                int depth = elapsed >= 30 ? 3 : elapsed >= 20 ? 2 : elapsed >= 10 ? 1 : 0;
                if (state.getValue(PRESS_DEPTH) != depth) {
                    level.setBlockAndUpdate(pos, state.setValue(PRESS_DEPTH, depth));
                    if (depth == 3) level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .5f, 1.1f);
                }
            }
        } else if (elapsed >= (state.getValue(BLUE) ? 35 : 18)) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, 2));
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, .45f, 1.2f);
        }
    }

    private static ItemStack finished(BlockState state) {
        if (state.getValue(CHEDDAR)) return Fromage.YOUNG_CHEDDAR.get().getDefaultInstance();
        if (!state.getValue(FORMING)) return Fromage.agedCheese(state.getValue(BLUE)).getDefaultInstance();
        if (state.getValue(BLUE)) return YoungWheelBlock.asItem(true, true, 0);
        return new ItemStack(Fromage.FRESH.get(), 2);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
            boolean moved) {
        int portions = state.getValue(PORTIONS);
        if (!level.isClientSide && !moved && !state.is(replacement.getBlock()) && portions > 0) {
            boolean blue = state.getValue(BLUE);
            ItemStack drop;
            if (state.getValue(AGE) == 2) drop = finished(state);
            else if (state.getValue(CHEDDAR)) drop = new ItemStack(Fromage.SALTED_CURDS.get(), portions);
            else if (state.getValue(FORMING)) drop = (blue ? Fromage.DRAINED_BLUE.get() : Fromage.DRAINED_FARMER.get()).getDefaultInstance();
            else drop = new ItemStack(Fromage.softCheese(blue), portions);
            popResource(level, pos, drop);
        }
        super.onRemove(state, level, pos, replacement, moved);
    }
}


