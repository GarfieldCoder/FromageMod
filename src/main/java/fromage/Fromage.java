package fromage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.*;

@Mod("fromage")
public final class Fromage {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fromage");
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fromage");
    static final String FARMERS_DELIGHT = "farmersdelight";
    static final String COOKERY = "kaleidoscope_cookery";
    static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("fromage", "curd_cutters"));
    private static final TagKey<Item> SALTS = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("c", "salt"));
    static final TagKey<Item> MILK_BUCKETS = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("c", "buckets/milk"));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "fromage");
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "fromage");
    static final DeferredHolder<MobEffect, PungentEffect> PUNGENT = EFFECTS.register("pungent", PungentEffect::new);

    private static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, "fromage");
    static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHEY_DROP =
        PARTICLES.register("whey_drop", () -> new SimpleParticleType(false));

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fromage");
    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, "fromage");

    static final DeferredBlock<CheeseMoldBlock> MOLD = BLOCKS.register("cheese_mold",
        () -> new CheeseMoldBlock(BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion()));
    static final DeferredBlock<CheeseBlock.Young> YOUNG_CHEESE = BLOCKS.register("young_cheese_block",
        () -> new CheeseBlock.Young(cheeseProperties().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    static final DeferredBlock<CheeseBlock.Sliceable> AGED_CHEESE = BLOCKS.register("aged_cheese_block",
        () -> new CheeseBlock.Sliceable(cheeseProperties(), Fromage.MILD_WEDGE::get));
    static final DeferredBlock<CheeseBlock.Sliceable> AGED_BLUE = BLOCKS.register("aged_blue_cheese_block",
        () -> new CheeseBlock.Sliceable(cheeseProperties(), Fromage.BLUE_WEDGE::get));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CheeseBlock.Batch>> RIPENING =
        BLOCK_ENTITIES.register("ripening_cheese",
            () -> BlockEntityType.Builder.of(CheeseBlock.Batch::new, YOUNG_CHEESE.get(), MOLD.get()).build(null));

    static final DeferredItem<CheeseMoldItem> MOLD_ITEM = ITEMS.register("cheese_mold",
        () -> new CheeseMoldItem(MOLD.get(), new Item.Properties().stacksTo(1)));
    static final DeferredItem<CheeseMoldItem.Packed> PACKED_MOLD = ITEMS.register("packed_cheese_mold",
        () -> new CheeseMoldItem.Packed(MOLD.get(), new Item.Properties().stacksTo(1), DairyBatch.PLAIN));
    static final DeferredItem<CheeseMoldItem.Packed> PACKED_BLUE_MOLD = ITEMS.register("packed_blue_cheese_mold",
        () -> new CheeseMoldItem.Packed(MOLD.get(), new Item.Properties().stacksTo(1), DairyBatch.BLUE));
    static final DeferredItem<CheeseMoldItem.Packed> PACKED_CHEDDAR_MOLD = ITEMS.register("packed_cheddar_mold",
        () -> new CheeseMoldItem.Packed(MOLD.get(), new Item.Properties().stacksTo(1), DairyBatch.CHEDDAR));
    static final DeferredItem<BlockItem> AGED_BLUE_ITEM = ITEMS.registerSimpleBlockItem(AGED_BLUE);
    static final DeferredItem<BlockItem> YOUNG_CHEESE_ITEM = ITEMS.registerSimpleBlockItem(YOUNG_CHEESE);
    static final DeferredItem<BlockItem> AGED_CHEESE_ITEM = ITEMS.registerSimpleBlockItem(AGED_CHEESE);

    static final DeferredBlock<CheesePressBlock> PRESS = BLOCKS.register("cheese_press",
        () -> new CheesePressBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD)
            .pushReaction(PushReaction.BLOCK).noOcclusion()));
    static final DeferredBlock<AgingShelfBlock> SHELF = BLOCKS.register("aging_shelf",
        () -> new AgingShelfBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD)
            .pushReaction(PushReaction.BLOCK).noOcclusion().dynamicShape()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AgingShelfBlock.Slots>> SHELF_ENTITY =
        BLOCK_ENTITIES.register("shelf_slots",
            () -> BlockEntityType.Builder.of(AgingShelfBlock.Slots::new, SHELF.get()).build(null));
    static final DeferredBlock<CheeseBlock.Sliceable> CHEDDAR_BLOCK = BLOCKS.register("aged_cheddar_block",
        () -> new CheeseBlock.Sliceable(cheeseProperties(), Fromage.CHEDDAR_WEDGE::get));
    static final DeferredItem<BlockItem> CHEDDAR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(CHEDDAR_BLOCK);
    static final DeferredBlock<CheeseBlock.Sliceable> EXTRA_CHEDDAR_BLOCK = BLOCKS.register("extra_aged_cheddar_block",
        () -> new CheeseBlock.Sliceable(cheeseProperties(), Fromage.CHEDDAR_WEDGE::get));
    static final DeferredBlock<CheeseBlock.Sliceable> EXTRA_BLUE_BLOCK = BLOCKS.register("extra_aged_blue_cheese_block",
        () -> new CheeseBlock.Sliceable(cheeseProperties(), Fromage.BLUE_WEDGE::get));
    static final DeferredItem<BlockItem> EXTRA_CHEDDAR_ITEM = ITEMS.registerSimpleBlockItem(EXTRA_CHEDDAR_BLOCK);
    static final DeferredItem<BlockItem> EXTRA_BLUE_ITEM = ITEMS.registerSimpleBlockItem(EXTRA_BLUE_BLOCK);
    static final DeferredItem<BlockItem> PRESS_ITEM = ITEMS.registerSimpleBlockItem(PRESS);
    static final DeferredItem<BlockItem> SHELF_ITEM = ITEMS.registerSimpleBlockItem(SHELF);

    static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.register("firebox_vat",
        () -> new FireboxBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.STONE)
            .pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    static final DeferredBlock<FireboxBasinBlock> FIREBOX_BASIN = BLOCKS.register("firebox_basin",
        () -> new FireboxBasinBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.COPPER)
            .pushReaction(PushReaction.BLOCK).noOcclusion()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireboxBlock.Fuel>> FIREBOX_FUEL =
        BLOCK_ENTITIES.register("firebox_fuel",
            () -> BlockEntityType.Builder.of(FireboxBlock.Fuel::new, FIREBOX.get()).build(null));
    static final DeferredItem<BlockItem> FIREBOX_ITEM = ITEMS.registerSimpleBlockItem(FIREBOX);

    static final DeferredBlock<Station> VAT = station("vat", true);
    static final DeferredBlock<Station> TABLE = station("draining_table", false);
    static final DeferredItem<BlockItem> VAT_ITEM = ITEMS.registerSimpleBlockItem(VAT);
    static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem(TABLE);
    static final DeferredItem<Item> BLUE_STARTER = ITEMS.registerSimpleItem("blue_starter");
    static final DeferredItem<Item> BLUE_CURDS = ITEMS.registerSimpleItem("blue_curds");
    static final DeferredItem<Item> BLUE_FRESH = food("fresh_blue_cheese", 4, .45f);
    static final DeferredItem<Item> MILD_WEDGE = food("mild_cheese_wedge", 2, .45f);
    static final DeferredItem<Item> BLUE_WEDGE = food("blue_cheese_wedge", 2, .45f);
    static final DeferredItem<Item> CULTURE = ITEMS.registerSimpleItem("acid_culture");
    static final DeferredItem<Item> VAT_STARTER = ITEMS.registerSimpleItem("vat_starter", new Item.Properties().stacksTo(16));
    static final DeferredItem<Item> KNIFE = ITEMS.registerSimpleItem("curd_knife", new Item.Properties().stacksTo(1));
    static final DeferredItem<Item> CLOTH = ITEMS.registerSimpleItem("cheesecloth");
    static final DeferredItem<Item> CURDS = ITEMS.registerSimpleItem("curds");
    static final DeferredItem<Item> WHEY = ITEMS.registerSimpleItem("whey", new Item.Properties().stacksTo(16));
    static final DeferredItem<Item> CULTURED_WHEY = ITEMS.registerSimpleItem("cultured_whey", new Item.Properties().stacksTo(16));
    static final DeferredItem<Item> CULTURED_CURDS = ITEMS.registerSimpleItem("cultured_curds");
    static final DeferredItem<Item> CHEDDAR_CURDS = ITEMS.registerSimpleItem("cheddar_curds");
    static final DeferredItem<Item> SALTED_CURDS = ITEMS.registerSimpleItem("salted_cheddar_curds");
    static final DeferredItem<Item> DRAINED_FARMER = ITEMS.registerSimpleItem("drained_farmer_curds");
    static final DeferredItem<Item> DRAINED_BLUE = ITEMS.registerSimpleItem("drained_blue_curds");
    static final DeferredItem<Item> RENNET = ITEMS.registerSimpleItem("vegetable_rennet");
    static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");
    static final DeferredBlock<YoungWheelBlock> CHEDDAR_YOUNG_BLOCK = BLOCKS.register("pressed_cheddar",
        () -> new YoungWheelBlock(cheeseProperties().pushReaction(PushReaction.BLOCK), false));
    static final DeferredBlock<YoungWheelBlock> BLUE_YOUNG_BLOCK = BLOCKS.register("young_blue_cheese",
        () -> new YoungWheelBlock(cheeseProperties().pushReaction(PushReaction.BLOCK), true));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<YoungWheelBlock.WheelData>> WHEEL_ENTITY =
        BLOCK_ENTITIES.register("young_wheel",
            () -> BlockEntityType.Builder.of(YoungWheelBlock.WheelData::new, CHEDDAR_YOUNG_BLOCK.get(), BLUE_YOUNG_BLOCK.get()).build(null));
    static final DeferredItem<BlockItem> YOUNG_CHEDDAR = ITEMS.registerSimpleBlockItem(CHEDDAR_YOUNG_BLOCK);
    static final DeferredItem<BlockItem> YOUNG_BLUE = ITEMS.registerSimpleBlockItem(BLUE_YOUNG_BLOCK);
    static final DeferredItem<Item> CHEDDAR_WEDGE = food("cheddar_wedge", 3, .65f);
    static final DeferredItem<CheeseGraterItem> GRATER = ITEMS.register("cheese_grater",
        () -> new CheeseGraterItem(new Item.Properties().durability(128)));
    static final DeferredItem<GratedCheddarItem> GRATED_CHEDDAR = ITEMS.register("grated_cheddar",
        () -> new GratedCheddarItem(new Item.Properties()));
    static final DeferredItem<Item> CHEDDAR_POTATO = food("cheddar_potato", 6, .6f);
    static final DeferredBlock<PastaBowlBlock> PASTA = BLOCKS.register("pasta_bowl",
        () -> new PastaBowlBlock(mealProperties(), false));
    static final DeferredBlock<PastaBowlBlock> CHEDDAR_PASTA = BLOCKS.register("cheddar_pasta",
        () -> new PastaBowlBlock(mealProperties(), true));
    static final DeferredBlock<CheddarCakeBlock> CHEDDAR_CAKE = BLOCKS.register("cheddar_cake",
        () -> new CheddarCakeBlock(mealProperties()));
    static final DeferredItem<BlockItem> PASTA_ITEM = ITEMS.registerSimpleBlockItem(PASTA);
    static final DeferredItem<BlockItem> CHEDDAR_PASTA_ITEM = ITEMS.registerSimpleBlockItem(CHEDDAR_PASTA);
    static final DeferredItem<Item> FRESH = ITEMS.registerSimpleItem("farmers_cheese",
        new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(.45f).build()));
    static final DeferredItem<Item> UNTOASTED = ITEMS.registerSimpleItem("cheese_toast");
    static final DeferredItem<Item> SANDWICH = ITEMS.registerSimpleItem("cheese_sandwich",
        new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(.55f).build()));

    static final DeferredItem<Item> CRUMBLES = food("cheese_crumbles", 4, .45f);
    static final DeferredItem<Item> DUMPLINGS = food("cheese_dumplings", 8, .65f);
    static final DeferredItem<Item> FLATBREAD = food("cheese_flatbread", 8, .60f);
    static final DeferredHolder<EntityType<?>, EntityType<CheeseGolem>> CHEESE_GOLEM = ENTITIES.register(
        "cheese_golem",
        () -> EntityType.Builder.of(CheeseGolem::new, MobCategory.CREATURE)
            .sized(1.6f, 3.2f)
            .clientTrackingRange(8)
            .build("cheese_golem"));
    static final DeferredItem<DeferredSpawnEggItem> CHEESE_GOLEM_SPAWN_EGG = ITEMS.register(
        "cheese_golem_spawn_egg",
        () -> new DeferredSpawnEggItem(CHEESE_GOLEM, 0xF4D684, 0xE88818, new Item.Properties()));

    static {
        TABS.register("dairy", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fromage.dairy"))
            .icon(() -> FRESH.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                boolean delight = ModList.get().isLoaded(FARMERS_DELIGHT);
                boolean cookery = ModList.get().isLoaded(COOKERY);

                output.accept(VAT_ITEM.get());
                output.accept(FIREBOX_ITEM.get());
                output.accept(TABLE_ITEM.get());
                output.accept(MOLD_ITEM.get());
                output.accept(SHELF_ITEM.get());

                output.accept(CLOTH.get());
                if (!delight && !cookery) output.accept(KNIFE.get());
                output.accept(SALT.get());
                output.accept(GRATER.get());

                output.accept(CULTURE.get());
                output.accept(VAT_STARTER.get());
                output.accept(BLUE_STARTER.get());
                output.accept(RENNET.get());

                output.accept(Station.curdStack(DairyBatch.PLAIN, true, true));
                output.accept(WHEY.get());
                output.accept(DRAINED_FARMER.get());
                output.accept(FRESH.get());
                output.accept(AGED_CHEESE_ITEM.get());
                output.accept(MILD_WEDGE.get());

                output.accept(CULTURED_WHEY.get());
                output.accept(Station.curdStack(DairyBatch.CULTURED, true, true));

                output.accept(Station.curdStack(DairyBatch.BLUE, true, true));
                output.accept(DRAINED_BLUE.get());
                output.accept(BLUE_FRESH.get());
                output.accept(YOUNG_BLUE.get());
                output.accept(AGED_BLUE_ITEM.get());
                output.accept(EXTRA_BLUE_ITEM.get());
                output.accept(BLUE_WEDGE.get());

                output.accept(Station.curdStack(DairyBatch.CHEDDAR, true, true));
                output.accept(SALTED_CURDS.get());
                output.accept(YOUNG_CHEDDAR.get());
                output.accept(CHEDDAR_BLOCK_ITEM.get());
                output.accept(EXTRA_CHEDDAR_ITEM.get());
                output.accept(CHEDDAR_WEDGE.get());
                output.accept(GRATED_CHEDDAR.get());
                output.accept(PASTA_ITEM.get());
                output.accept(CHEDDAR_PASTA_ITEM.get());
                output.accept(CHEDDAR_POTATO.get());

                output.accept(UNTOASTED.get());
                output.accept(SANDWICH.get());
                if (delight) {
                    output.accept(CRUMBLES.get());
                    output.accept(DUMPLINGS.get());
                }
                if (cookery) output.accept(FLATBREAD.get());
                output.accept(CHEESE_GOLEM_SPAWN_EGG.get());
            })
            .build());
    }

    public Fromage(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        PARTICLES.register(bus);
        EFFECTS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        NeoForge.EVENT_BUS.addListener(Fromage::addTooltip);
        NeoForge.EVENT_BUS.addListener(CheeseTopping::keepMealInPlace);
        NeoForge.EVENT_BUS.addListener(PungentEffect::addAvoidance);
    }

    private static void addTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("fromage")) return;

        String key = stack.getDescriptionId() + ".tooltip";
        event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (stack.is(FRESH.get()) && ModList.get().isLoaded(FARMERS_DELIGHT)) {
            event.getToolTip().add(Component.translatable(key + ".delight").withStyle(ChatFormatting.GRAY));
        }
        if ((stack.is(FRESH.get()) || stack.is(CRUMBLES.get())) && ModList.get().isLoaded(COOKERY)) {
            event.getToolTip().add(Component.translatable("item.fromage.cooking_cheese.tooltip.cookery")
                .withStyle(ChatFormatting.GRAY));
        }
    }

    static Item softCheese(boolean blue) {
        return blue ? BLUE_FRESH.get() : FRESH.get();
    }

    static BlockItem agedCheese(boolean blue) {
        return blue ? AGED_BLUE_ITEM.get() : AGED_CHEESE_ITEM.get();
    }

    private static BlockBehaviour.Properties cheeseProperties() {
        return BlockBehaviour.Properties.of().strength(.45f).sound(SoundType.WOOL).noOcclusion();
    }

    private static BlockBehaviour.Properties mealProperties() {
        return BlockBehaviour.Properties.of().instabreak().sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    private static DeferredItem<Item> food(String id, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(id, new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredBlock<Station> station(String name, boolean vat) {
        return BLOCKS.register(name, () -> new Station(BlockBehaviour.Properties.of()
            .strength(2).sound(vat ? SoundType.COPPER : SoundType.WOOD).noOcclusion(), vat));
    }

    static class Station extends Block {
        static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 8);
        static final int WHEY_IN_VAT = 8;
        private static final String WHEY_LEFT_IN_VAT = "FromageWheyLeftInVat";
        private static final String CLOTH_BUNDLE = "FromageClothBundle";
        static final int STARTER_TICKS = 100;
        static final net.minecraft.world.level.block.state.properties.BooleanProperty BLUE = CheeseMoldBlock.BLUE;
        static final EnumProperty<DairyBatch> BATCH = EnumProperty.create("batch", DairyBatch.class);
        static final net.minecraft.world.level.block.state.properties.BooleanProperty WHEY_PENDING =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("whey_pending");
        static final net.minecraft.world.level.block.state.properties.BooleanProperty BUNDLED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("bundled");
        static final IntegerProperty DRAIN_PROGRESS = IntegerProperty.create("drain_progress", 0, 5);
        static final int DRAIN_INTERVAL = 10;
        static final int DRAIN_TICKS = 6 * DRAIN_INTERVAL;
        static final int WARM_TICKS = 40;
        static final int COAGULATE_TICKS = 60;
        private static final VoxelShape VAT_SHAPE = Shapes.or(
            box(2, 0, 2, 14, 3, 14),
            box(1, 3, 1, 15, 13, 3), box(1, 3, 13, 15, 13, 15),
            box(1, 3, 3, 3, 13, 13), box(13, 3, 3, 15, 13, 13),
            box(0, 13, 0, 16, 14, 3), box(0, 13, 13, 16, 14, 16),
            box(0, 13, 3, 3, 14, 13), box(13, 13, 3, 16, 14, 13));
        private static final VoxelShape TABLE_SHAPE = Shapes.or(
            box(0, 12, 0, 16, 14, 2), box(0, 12, 14, 16, 14, 16),
            box(0, 12, 2, 2, 14, 14), box(14, 12, 2, 16, 14, 14),
            box(3, 12, 2, 5, 13, 14), box(7, 12, 2, 9, 13, 14),
            box(11, 12, 2, 13, 13, 14),
            box(1, 0, 1, 4, 12, 4), box(12, 0, 1, 15, 12, 4),
            box(1, 0, 12, 4, 12, 15), box(12, 0, 12, 15, 12, 15),
            box(4, 3, 3, 12, 5, 13));
        private static final VoxelShape LINED_TABLE_SHAPE = Shapes.or(TABLE_SHAPE, box(3, 13, 4, 13, 14, 12));
        private static final VoxelShape LOADED_TABLE_SHAPE = Shapes.or(LINED_TABLE_SHAPE, box(4, 14, 5, 12, 15.5, 12));
        final boolean vat;

        Station(Properties properties, boolean vat) {
            super(properties);
            this.vat = vat;
            registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(DRAIN_PROGRESS, 0)
                .setValue(BLUE, false).setValue(BATCH, DairyBatch.PLAIN).setValue(WHEY_PENDING, true).setValue(BUNDLED, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAGE, DRAIN_PROGRESS, BLUE, BATCH, WHEY_PENDING, BUNDLED);
        }

        @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (vat) return VAT_SHAPE;
            return switch (state.getValue(STAGE)) {
                case 0 -> TABLE_SHAPE;
                case 2, 3 -> LOADED_TABLE_SHAPE;
                default -> LINED_TABLE_SHAPE;
            };
        }

        static DairyBatch batch(BlockState state) {
            DairyBatch batch = state.getValue(BATCH);
            // Older blue batches only have the blue flag.
            return batch == DairyBatch.PLAIN && state.getValue(BLUE) ? DairyBatch.BLUE : batch;
        }

        static BlockState withBatch(BlockState state, DairyBatch batch) {
            return state.setValue(BATCH, batch).setValue(BLUE, batch == DairyBatch.BLUE);
        }

        static boolean canPack(BlockState state) {
            return !state.getValue(BUNDLED) && state.getValue(STAGE) == 3 && (batch(state) == DairyBatch.BLUE
                || batch(state) == DairyBatch.CHEDDAR && state.getValue(DRAIN_PROGRESS) == 2);
        }

        @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            int stage = state.getValue(STAGE);
            if (!vat && stack.is(MOLD_ITEM.get())) {
                if (canPack(state)) {
                    MOLD_ITEM.get().startPacking(player, hand, pos);
                } else if (!level.isClientSide) {
                    resumeDrain(level, pos, stage);
                }
                return ItemInteractionResult.CONSUME;
            }
            if (vat && stage == 0 && stack.is(MILK_BUCKETS) && stack.getCraftingRemainingItem().is(Items.BUCKET)) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                    advance(state, level, pos, 1);
                }
            } else if (vat && stage == 2 && (stack.is(CULTURE.get()) || stack.is(BLUE_STARTER.get())
                    || stack.is(CULTURED_WHEY.get()) || stack.is(VAT_STARTER.get()))
                    && heated(level, pos)) {
                if (!level.isClientSide) {
                    DairyBatch chosen = stack.is(BLUE_STARTER.get()) ? DairyBatch.BLUE
                        : stack.is(CULTURED_WHEY.get()) || stack.is(VAT_STARTER.get()) ? DairyBatch.CULTURED : DairyBatch.PLAIN;
                    DairyBatch current = batch(state);
                    if (current == DairyBatch.CHEDDAR && chosen == DairyBatch.BLUE) {
                        player.displayClientMessage(Component.translatable("message.fromage.vat.blue_after_rennet"), true);
                    } else {
                        if (stack.is(VAT_STARTER.get()) || stack.is(CULTURED_WHEY.get())) {
                            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                        } else {
                            stack.consume(1, player);
                        }
                        DairyBatch next = current == DairyBatch.CHEDDAR ? DairyBatch.CHEDDAR : chosen;
                        advance(withBatch(state, next), level, pos, 3);
                    }
                }
            } else if (vat && stage == 7 && stack.is(Items.GLASS_BOTTLE)) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, VAT_STARTER.get().getDefaultInstance()));
                    advance(state, level, pos, 0);
                }
            } else if (vat && stage == 5 && stack.is(CLOTH.get())) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, curdStack(batch(state), true, true)));
                    advance(state, level, pos, WHEY_IN_VAT);
                    animate(level, pos, VatMotion.LIFT);
                }
            } else if (vat && stage == WHEY_IN_VAT && stack.is(MILK_BUCKETS)
                    && stack.getCraftingRemainingItem().is(Items.BUCKET)) {
                if (!level.isClientSide) {
                    if (batch(state) != DairyBatch.PLAIN) {
                        player.displayClientMessage(Component.translatable("message.fromage.vat.milk_after_cultured"), true);
                    } else {
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                        advance(withBatch(state, DairyBatch.PLAIN), level, pos, 1);
                    }
                }
            } else if ((vat && stage == WHEY_IN_VAT || !vat && stage == 4) && stack.is(Items.GLASS_BOTTLE)) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, batch(state).whey().getDefaultInstance()));
                    advance(state, level, pos, vat || state.getValue(BUNDLED) ? 0 : 1);
                }
            } else if (vat && stack.is(RENNET.get())) {
                if (!level.isClientSide) {
                    DairyBatch current = batch(state);
                    if (stage < 2 || stage > 4) {
                        player.displayClientMessage(Component.translatable("message.fromage.vat.rennet_not_now"), true);
                    } else if (current == DairyBatch.BLUE) {
                        player.displayClientMessage(Component.translatable("message.fromage.vat.rennet_refused_blue"), true);
                    } else if (current == DairyBatch.CHEDDAR) {
                        player.displayClientMessage(Component.translatable("message.fromage.vat.rennet_already"), true);
                    } else {
                        stack.consume(1, player);
                        level.setBlock(pos, withBatch(state, DairyBatch.CHEDDAR), Block.UPDATE_ALL);
                    }
                }
            } else if (!vat && stage == 3 && batch(state) == DairyBatch.CHEDDAR
                    && state.getValue(DRAIN_PROGRESS) == 1 && stack.is(SALTS)) {
                if (!level.isClientSide) {
                    stack.consume(1, player);
                    setCheddaringStep(state, level, pos, 2);
                }
            } else if (vat && stage == 4 && stack.is(KNIVES)) {
                if (!level.isClientSide) {
                    advance(state, level, pos, stage + 1);
                    animate(level, pos, VatMotion.CUT);
                    stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                }
            } else if (!vat && stage == 0 && stack.is(CLOTH.get())) {
                // Keeps loose curds from old inventories usable.
                if (!level.isClientSide) {
                    stack.consume(1, player);
                    advance(state, level, pos, 1);
                }
            } else if (!vat && (stage == 1 || stage == 0 && isBundle(stack)) && DairyBatch.forCurds(stack) != null) {
                if (!level.isClientSide) {
                    DairyBatch batch = DairyBatch.forCurds(stack);
                    var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    boolean bundled = data.getBoolean(CLOTH_BUNDLE);
                    if (stage == 1 && bundled) player.getInventory().placeItemBackInInventory(CLOTH.get().getDefaultInstance());
                    stack.consume(1, player);
                    advance(withBatch(state, batch).setValue(WHEY_PENDING, !data.getBoolean(WHEY_LEFT_IN_VAT))
                        .setValue(BUNDLED, bundled), level, pos, 2);
                    resumeDrain(level, pos, 2);
                }
            } else {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                Player player, BlockHitResult hit) {
            int stage = state.getValue(STAGE);
            if (vat && stage == WHEY_IN_VAT) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (vat && stage == 3 && state.getValue(DRAIN_PROGRESS) == 0 && heated(level, pos)
                    && player.getMainHandItem().isEmpty()) {
                level.setBlock(pos, state.setValue(DRAIN_PROGRESS, 1), Block.UPDATE_ALL);
                level.scheduleTick(pos, this, COAGULATE_TICKS);
                animate(level, pos, VatMotion.STIR);
                level.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, .35f, 1.15f);
            } else if (vat && stage == 2 && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                    && heated(level, pos)) {
                if (batch(state) == DairyBatch.CHEDDAR) {
                    player.displayClientMessage(Component.translatable("message.fromage.vat.rennet_ferment"), true);
                } else {
                    advance(state, level, pos, 6);
                    level.scheduleTick(pos, this, STARTER_TICKS);
                }
            } else if (vat && stage == 4 && player.getMainHandItem().isEmpty()) {
                advance(state, level, pos, 5);
                animate(level, pos, VatMotion.CUT);
            } else if (!vat && stage == 3 && player.getMainHandItem().isEmpty()) {
                DairyBatch batch = batch(state);
                if (batch == DairyBatch.CHEDDAR && state.getValue(DRAIN_PROGRESS) < 2) {
                    if (state.getValue(DRAIN_PROGRESS) == 0) setCheddaringStep(state, level, pos, 1);
                    return InteractionResult.CONSUME;
                }
                boolean bundled = state.getValue(BUNDLED);
                advance(state, level, pos, state.getValue(WHEY_PENDING) ? 4 : bundled ? 0 : 1);
                if (bundled) player.getInventory().placeItemBackInInventory(CLOTH.get().getDefaultInstance());
                player.getInventory().placeItemBackInInventory(drainedOutput(batch, bundled));
            } else {
                if (!vat) resumeDrain(level, pos, stage);
            }
            return InteractionResult.CONSUME;
        }

        boolean heated(Level level, BlockPos pos) {
            if (this instanceof FireboxBlock) return level.getBlockState(pos).getValue(BlockStateProperties.LIT);
            BlockState below = level.getBlockState(pos.below());
            return below.is(FIREBOX.get()) && below.getValue(BlockStateProperties.LIT)
                || below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
        }

        private void advance(BlockState state, Level level, BlockPos pos, int stage) {
            BlockState nextState = state.setValue(STAGE, stage).setValue(DRAIN_PROGRESS, 0);
            if (stage == 0 || !vat && stage == 1) {
                nextState = withBatch(nextState, DairyBatch.PLAIN).setValue(WHEY_PENDING, true).setValue(BUNDLED, false);
            }
            level.setBlock(pos, nextState, Block.UPDATE_ALL);
            if (vat && stage == 1) level.scheduleTick(pos, this, WARM_TICKS);
            var sound = vat ? switch (stage) {
                case 0 -> SoundEvents.ITEM_PICKUP;
                case 1 -> SoundEvents.BUCKET_EMPTY;
                case 5 -> SoundEvents.SHEEP_SHEAR;
                default -> SoundEvents.BOTTLE_FILL;
            } : switch (stage) {
                case 1 -> state.getValue(STAGE) == 0 ? SoundEvents.WOOL_PLACE : SoundEvents.ITEM_PICKUP;
                case 4 -> SoundEvents.WOOL_PLACE;
                default -> SoundEvents.WOOL_PLACE;
            };
            level.playSound(null, pos, sound, SoundSource.BLOCKS, .55f, 1f);
        }

        private void setCheddaringStep(BlockState state, Level level, BlockPos pos, int step) {
            BlockState next = state.setValue(DRAIN_PROGRESS, step);
            level.setBlockAndUpdate(pos, next);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, .55f, .85f + step * .15f);
        }

        void packCheese(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            advance(state, level, pos, state.getValue(WHEY_PENDING) ? 4 : 1);
        }

        private void animate(Level level, BlockPos pos, VatMotion motion) {
            level.blockEvent(pos, this, VatMotion.BLOCK_EVENT, motion.ordinal());
        }

        private static ItemStack drainedOutput(DairyBatch batch, boolean bundled) {
            if (batch == DairyBatch.CHEDDAR) return new ItemStack(SALTED_CURDS.get(), 2);
            if (!bundled) return new ItemStack(softCheese(batch == DairyBatch.BLUE), 2);
            return (batch == DairyBatch.BLUE ? DRAINED_BLUE.get() : DRAINED_FARMER.get()).getDefaultInstance();
        }

        private static ItemStack curdStack(DairyBatch batch, boolean wheyLeftInVat, boolean bundled) {
            ItemStack curds = new ItemStack(batch.curds());
            if (wheyLeftInVat || bundled) {
                CompoundTag data = new CompoundTag();
                data.putBoolean(WHEY_LEFT_IN_VAT, wheyLeftInVat);
                data.putBoolean(CLOTH_BUNDLE, bundled);
                curds.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
            return curds;
        }

        static boolean isBundle(ItemStack stack) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(CLOTH_BUNDLE);
        }

        @Override public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int event, int value) {
            if (event != VatMotion.BLOCK_EVENT || !vat) return super.triggerEvent(state, level, pos, event, value);
            if (level.isClientSide) VatMotion.receive(level, pos, value);
            return true;
        }

        private void resumeDrain(Level level, BlockPos pos, int stage) {
            if (stage == 2) level.scheduleTick(pos, this, DRAIN_INTERVAL);
        }

        @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                boolean moved) {
            super.onPlace(state, level, pos, oldState, moved);
            if (!vat && !level.isClientSide) resumeDrain(level, pos, state.getValue(STAGE));
        }

        @Override protected boolean isRandomlyTicking(BlockState state) {
            int stage = state.getValue(STAGE);
            if (!vat) return stage == 2;
            return stage == 1 || stage == 3 && state.getValue(DRAIN_PROGRESS) > 0;
        }

        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (vat) {
                if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                    level.scheduleTick(pos, this, heated(level, pos) ? 1 : 20);
                }
                return;
            }
            // Old worlds can have an unprocessed table without a scheduled tick.
            resumeDrain(level, pos, state.getValue(STAGE));
        }

        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int stage = state.getValue(STAGE);
            if (vat && stage == 1) {
                if (heated(level, pos)) advance(state, level, pos, 2);
                else level.scheduleTick(pos, this, 20);
                return;
            }
            if (vat && stage == 3 && state.getValue(DRAIN_PROGRESS) > 0) {
                if (heated(level, pos)) {
                    level.setBlock(pos, state.setValue(STAGE, 4).setValue(DRAIN_PROGRESS, 0), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, .45f, 1.05f);
                } else {
                    level.scheduleTick(pos, this, 20);
                }
                return;
            }
            if (vat && stage == 6) {
                level.setBlockAndUpdate(pos, state.setValue(STAGE, 7));
                return;
            }
            if (vat || stage != 2) return;

            int progress = state.getValue(DRAIN_PROGRESS);
            if (progress == 5) {
                level.setBlockAndUpdate(pos, state.setValue(STAGE, 3).setValue(DRAIN_PROGRESS, 0));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, .38f, 1.25f);
            } else {
                double x = pos.getX() + (progress % 2 == 0 ? .375 : .625);
                level.sendParticles(WHEY_DROP.get(), x, pos.getY() + .72, pos.getZ() + .5,
                    progress < 3 ? 2 : 1, .018, .012, .12, 0);
                level.setBlockAndUpdate(pos, state.setValue(DRAIN_PROGRESS, progress + 1));
                resumeDrain(level, pos, 2);
            }
        }

        @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState replacement, boolean moved) {
            if (!level.isClientSide && !moved && !state.is(replacement.getBlock())) {
                int stage = state.getValue(STAGE);
                if (vat && stage == 5) {
                    popResource(level, pos, new ItemStack(batch(state).curds()));
                } else if (!vat) {
                    boolean bundled = state.getValue(BUNDLED);
                    if (stage >= 1 && !bundled) popResource(level, pos, new ItemStack(CLOTH.get()));
                    if (stage == 3 && batch(state) == DairyBatch.CHEDDAR && state.getValue(DRAIN_PROGRESS) == 2) {
                        popResource(level, pos, drainedOutput(DairyBatch.CHEDDAR, bundled));
                        if (bundled) popResource(level, pos, new ItemStack(CLOTH.get()));
                    } else if (stage == 2 || stage == 3) {
                        popResource(level, pos, curdStack(batch(state), !state.getValue(WHEY_PENDING), bundled));
                    }
                }
            }
            super.onRemove(state, level, pos, replacement, moved);
        }
    }
}


















