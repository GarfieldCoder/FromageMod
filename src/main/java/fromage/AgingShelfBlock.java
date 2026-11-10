package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class AgingShelfBlock extends Block implements EntityBlock {
    static final int AGE_TICKS = 300;
    private static final int EXTRA_TICKS = 300;
    static final int TURN_BONUS = 12;
    private static final int TURN_INTERVAL = 100;
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final BooleanProperty LEFT = BooleanProperty.create("left");
    static final BooleanProperty RIGHT = BooleanProperty.create("right");
    static final BooleanProperty ABOVE = BooleanProperty.create("above");
    static final BooleanProperty BELOW = BooleanProperty.create("below");
    static final BooleanProperty WIDE = BooleanProperty.create("wide");
    static final IntegerProperty LOWER = IntegerProperty.create("lower", 0, 16);
    static final IntegerProperty UPPER = IntegerProperty.create("upper", 0, 16);

    AgingShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH)
            .setValue(LEFT, false).setValue(RIGHT, false).setValue(ABOVE, false).setValue(BELOW, false)
            .setValue(WIDE, false).setValue(LOWER, 0).setValue(UPPER, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, ABOVE, BELOW, WIDE, LOWER, UPPER);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState facing = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return connected(facing, context.getLevel(), context.getClickedPos());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING)))
            .setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connected(state, level, pos);
    }

    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        boolean left = matches(level, pos.relative(facing.getClockWise()), facing);
        boolean right = matches(level, pos.relative(facing.getCounterClockWise()), facing);
        boolean above = matches(level, pos.above(), facing);
        boolean below = matches(level, pos.below(), facing);
        return state.setValue(LEFT, left).setValue(RIGHT, right)
            .setValue(WIDE, left || right || above || below).setValue(ABOVE, above).setValue(BELOW, below);
    }

    private boolean matches(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.is(this) && neighbor.getValue(FACING) == facing;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = Shapes.or(orientedBox(facing, 0, state.getValue(BELOW) ? 0 : 1, 2.25, 16, 2, 13.75),
            orientedBox(facing, 0, 8, 2.25, 16, 9, 13.75));
        if (!state.getValue(ABOVE)) {
            shape = Shapes.or(shape, orientedBox(facing, 0, 14, 2, 16, 16, 13));
        }
        int end = state.getValue(WIDE) ? 3 : 2;
        if (!state.getValue(LEFT)) shape = Shapes.or(shape, endShape(state, facing, 0, end));
        if (!state.getValue(RIGHT)) shape = Shapes.or(shape, endShape(state, facing, 16 - end, 16));
        if (state.getValue(LOWER) > 0) shape = Shapes.or(shape, orientedBox(facing, 3, 2, 3, 13, 6, 13));
        if (state.getValue(UPPER) > 0) shape = Shapes.or(shape, orientedBox(facing, 3, 9, 3, 13, 13, 13));
        return shape;
    }

    private static VoxelShape endShape(BlockState state, Direction facing, int x, int x2) {
        boolean above = state.getValue(ABOVE);
        VoxelShape end = Shapes.or(orientedBox(facing, x, 0, 2, x2, above ? 16 : 14, 14),
            orientedBox(facing, x, 0, 1, x2, 2, 15));
        if (above) return end;
        int peg = x == 0 ? 0 : 14;
        return Shapes.or(end, orientedBox(facing, peg, 14, 13, peg + 2, 16, 15));
    }

    private static VoxelShape orientedBox(Direction facing, double x, double y, double z, double x2, double y2, double z2) {
        return switch (facing) {
            case NORTH -> box(16 - x2, y, 16 - z2, 16 - x, y2, 16 - z);
            case WEST -> box(16 - z2, y, x, 16 - z, y2, x2);
            case EAST -> box(z, y, 16 - x2, z2, y2, 16 - x);
            default -> box(x, y, z, x2, y2, z2);
        };
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean blue = stack.is(Fromage.YOUNG_BLUE.get());
        if (!blue && !stack.is(Fromage.YOUNG_CHEDDAR.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Slots slots) {
            boolean upper = isUpper(pos, hit);
            if (!slots.occupied(upper)) {
                slots.insert(state, stack, upper, blue);
                stack.consume(1, player);
                sound(level, pos);
            } else {
                player.displayClientMessage(Component.translatable("message.fromage.shelf.occupied"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.getMainHandItem().isEmpty() && level.getBlockEntity(pos) instanceof Slots slots) {
            boolean upper = isUpper(pos, hit);
            int stage = slots.phase(upper);
            boolean turning = stage < 3 ? !player.isShiftKeyDown() : player.isShiftKeyDown();
            if (stage > 0 && turning) {
                slots.turn(state, upper);
            } else if (stage > 0) {
                ItemStack cheese = slots.remove(state, upper);
                player.getInventory().placeItemBackInInventory(cheese);
                sound(level, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    static boolean isUpper(BlockPos pos, BlockHitResult hit) {
        return hit.getLocation().y - pos.getY() >= .5;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!level.isClientSide && !state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof Slots slots) {
            popResource(level, pos, slots.itemFor(state, false));
            popResource(level, pos, slots.itemFor(state, true));
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Slots(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != Fromage.SHELF_ENTITY.get()
                || !(isAging(state.getValue(LOWER)) || isAging(state.getValue(UPPER)))) return null;
        return (world, pos, blockState, entity) -> ((Slots) entity).update(blockState);
    }

    static int phase(int slot) {
        return Wheel.fromLegacy(slot).phase;
    }

    private static boolean isAging(int slot) {
        Wheel wheel = Wheel.fromLegacy(slot);
        return wheel.phase > 0 && wheel.phase < 4;
    }

    private static void sound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .45f, 1.1f);
    }

    /** A wheel remembered by the shelf. The 0–16 block values stay as the model selector. */
    static final class Wheel {
        static final Wheel EMPTY = new Wheel("none", 0, false);
        final String kind;
        final int phase;
        final boolean turned;

        Wheel(String kind, int phase, boolean turned) {
            this.kind = kind;
            this.phase = phase;
            this.turned = turned;
        }

        int legacy() {
            if (phase <= 0 || !("cheddar".equals(kind) || "blue".equals(kind))) return 0;
            return phase + ("blue".equals(kind) ? 4 : 0) + (turned ? 8 : 0);
        }

        static Wheel fromLegacy(int slot) {
            if (slot <= 0) return EMPTY;
            int phase = (slot - 1) % 4 + 1;
            boolean blue = (slot - 1) % 8 >= 4;
            return new Wheel(blue ? "blue" : "cheddar", phase, slot > 8);
        }

        Wheel advanced(long remaining) {
            if (phase <= 0 || phase >= 4) return this;
            int stage = remaining <= -EXTRA_TICKS ? 4 : remaining <= 0 ? 3 : remaining <= AGE_TICKS / 2 ? 2 : 1;
            return stage == phase ? this : new Wheel(kind, stage, turned);
        }

        void write(CompoundTag tag, String prefix) {
            tag.putString(prefix + "Kind", kind);
            tag.putInt(prefix + "Phase", phase);
            tag.putBoolean(prefix + "Turned", turned);
        }

        static Wheel read(CompoundTag tag, String prefix) {
            int phase = tag.getInt(prefix + "Phase");
            String kind = tag.getString(prefix + "Kind");
            if (phase <= 0 || kind.isEmpty() || "none".equals(kind)) return EMPTY;
            return new Wheel(kind, Math.clamp(phase, 1, 4), tag.getBoolean(prefix + "Turned"));
        }
    }

    static final class Slots extends BlockEntity {
        private static final String IDENTITY = "ShelfIdentity";
        private Wheel lowerWheel = Wheel.EMPTY;
        private Wheel upperWheel = Wheel.EMPTY;
        private boolean identityStored;
        private boolean mirroring;
        private long lowerReadyAt;
        private long upperReadyAt;
        private long lowerLastTurn;
        private long upperLastTurn;
        private long lowerBonusAt;
        private long upperBonusAt;

        Slots(BlockPos pos, BlockState state) {
            super(Fromage.SHELF_ENTITY.get(), pos, state);
        }

        boolean occupied(boolean upper) {
            return phase(upper) > 0;
        }

        int phase(boolean upper) {
            ensureIdentity();
            return (upper ? upperWheel : lowerWheel).phase;
        }

        String kind(boolean upper) {
            ensureIdentity();
            return (upper ? upperWheel : lowerWheel).kind;
        }

        boolean turned(boolean upper) {
            ensureIdentity();
            return (upper ? upperWheel : lowerWheel).turned;
        }

        long readyAt(boolean upper) {
            return upper ? upperReadyAt : lowerReadyAt;
        }

        long bonusAt(boolean upper) {
            return upper ? upperBonusAt : lowerBonusAt;
        }

        long lastTurn(boolean upper) {
            return upper ? upperLastTurn : lowerLastTurn;
        }

        private void ensureIdentity() {
            if (identityStored) return;
            BlockState state = getBlockState();
            lowerWheel = Wheel.fromLegacy(state.getValue(LOWER));
            upperWheel = Wheel.fromLegacy(state.getValue(UPPER));
            identityStored = true;
            if (level != null && !level.isClientSide) setChanged();
        }

        private void insert(BlockState state, ItemStack stack, boolean upper, boolean blue) {
            ensureIdentity();
            int elapsed = YoungWheelBlock.elapsed(stack);
            long now = level.getGameTime();
            Wheel wheel = new Wheel(blue ? "blue" : "cheddar", elapsed < AGE_TICKS / 2 ? 1 : 2, false);
            if (upper) {
                upperWheel = wheel;
                upperReadyAt = now + AGE_TICKS - elapsed;
                upperLastTurn = now - 10;
                upperBonusAt = Math.max(now, YoungWheelBlock.turnReady(stack));
            } else {
                lowerWheel = wheel;
                lowerReadyAt = now + AGE_TICKS - elapsed;
                lowerLastTurn = now - 10;
                lowerBonusAt = Math.max(now, YoungWheelBlock.turnReady(stack));
            }
            identityStored = true;
            mirror(state);
            setChanged();
        }

        private void turn(BlockState state, boolean upper) {
            ensureIdentity();
            long now = level.getGameTime();
            if (now - (upper ? upperLastTurn : lowerLastTurn) < 10) return;
            Wheel wheel = upper ? upperWheel : lowerWheel;
            if (wheel.phase <= 0) return;
            boolean bonus = wheel.phase < 4 && now >= (upper ? upperBonusAt : lowerBonusAt);
            Wheel turnedWheel = new Wheel(wheel.kind, wheel.phase, !wheel.turned);
            if (upper) {
                upperWheel = turnedWheel;
                upperLastTurn = now;
                if (bonus) {
                    upperReadyAt -= TURN_BONUS;
                    upperBonusAt = now + TURN_INTERVAL;
                }
            } else {
                lowerWheel = turnedWheel;
                lowerLastTurn = now;
                if (bonus) {
                    lowerReadyAt -= TURN_BONUS;
                    lowerBonusAt = now + TURN_INTERVAL;
                }
            }
            mirror(state);
            level.playSound(null, worldPosition, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, .50f, .9f);
            setChanged();
        }

        private ItemStack remove(BlockState state, boolean upper) {
            ensureIdentity();
            ItemStack cheese = itemFor(state, upper);
            if (!cheese.isEmpty()) {
                if (upper) {
                    upperWheel = Wheel.EMPTY;
                    upperReadyAt = 0;
                    upperBonusAt = 0;
                    upperLastTurn = 0;
                } else {
                    lowerWheel = Wheel.EMPTY;
                    lowerReadyAt = 0;
                    lowerBonusAt = 0;
                    lowerLastTurn = 0;
                }
                mirror(state);
                setChanged();
            }
            return cheese;
        }

        private ItemStack itemFor(BlockState state, boolean upper) {
            ensureIdentity();
            Wheel wheel = upper ? upperWheel : lowerWheel;
            if (wheel.phase <= 0) return ItemStack.EMPTY;
            long remaining = (upper ? upperReadyAt : lowerReadyAt) - level.getGameTime();
            boolean blue = "blue".equals(wheel.kind);
            if (!blue && !"cheddar".equals(wheel.kind)) return ItemStack.EMPTY;
            if (wheel.phase == 4 || remaining <= -EXTRA_TICKS) {
                return (blue ? Fromage.EXTRA_BLUE_ITEM.get() : Fromage.EXTRA_CHEDDAR_ITEM.get()).getDefaultInstance();
            }
            if (wheel.phase == 3 || remaining <= 0) {
                return (blue ? Fromage.AGED_BLUE_ITEM.get() : Fromage.CHEDDAR_BLOCK_ITEM.get()).getDefaultInstance();
            }
            return YoungWheelBlock.asItem(blue, blue,
                (int) Math.clamp(AGE_TICKS - remaining, 0, AGE_TICKS - 1),
                upper ? upperBonusAt : lowerBonusAt);
        }

        private void update(BlockState state) {
            ensureIdentity();
            lowerWheel = lowerWheel.advanced(lowerReadyAt - level.getGameTime());
            upperWheel = upperWheel.advanced(upperReadyAt - level.getGameTime());
            mirror(state);
        }

        private void mirror(BlockState state) {
            if (mirroring || level == null || level.isClientSide) return;
            int lower = lowerWheel.legacy();
            int upper = upperWheel.legacy();
            if (state.getValue(LOWER) == lower && state.getValue(UPPER) == upper) return;
            mirroring = true;
            level.setBlockAndUpdate(worldPosition, state.setValue(LOWER, lower).setValue(UPPER, upper));
            mirroring = false;
        }

        @Override public void onLoad() {
            super.onLoad();
            if (level == null) return;
            ensureIdentity();
            if (!level.isClientSide) mirror(getBlockState());
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putBoolean(IDENTITY, true);
            lowerWheel.write(tag, "Lower");
            upperWheel.write(tag, "Upper");
            tag.putLong("LowerReadyAt", lowerReadyAt);
            tag.putLong("UpperReadyAt", upperReadyAt);
            tag.putLong("LowerBonusAt", lowerBonusAt);
            tag.putLong("UpperBonusAt", upperBonusAt);
            tag.putLong("LowerLastTurn", lowerLastTurn);
            tag.putLong("UpperLastTurn", upperLastTurn);
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            identityStored = tag.getBoolean(IDENTITY);
            if (identityStored) {
                lowerWheel = Wheel.read(tag, "Lower");
                upperWheel = Wheel.read(tag, "Upper");
            }
            lowerReadyAt = tag.getLong("LowerReadyAt");
            upperReadyAt = tag.getLong("UpperReadyAt");
            lowerBonusAt = tag.getLong("LowerBonusAt");
            upperBonusAt = tag.getLong("UpperBonusAt");
            lowerLastTurn = tag.getLong("LowerLastTurn");
            upperLastTurn = tag.getLong("UpperLastTurn");
        }
    }
}

