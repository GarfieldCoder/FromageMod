package fromage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class WorkshopStep {
    static Component forTarget(Level level, BlockState state, Player player, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if (CheeseTopping.isMeal(state)) {
            ItemStack main = player.getMainHandItem();
            String mealStep = null;
            if (main.is(Fromage.GRATER.get())) {
                if (player.isUsingItem()) mealStep = "grater.grating";
                else mealStep = player.getOffhandItem().is(Fromage.CHEDDAR_WEDGE.get()) ? "grater.use" : "grater.wedge";
            } else if (main.is(Fromage.GRATED_CHEDDAR.get())) {
                mealStep = CheeseTopping.isTopped(state) ? "meal.topped" : "meal.top";
            } else if (state.getBlock() instanceof PastaBowlBlock) {
                mealStep = "meal.eat";
            }
            return mealStep == null ? null : Component.translatable("message.fromage." + mealStep);
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) held = player.getOffhandItem();
        String step;
        if (state.getBlock() instanceof Fromage.Station station) {
            if (state.is(Fromage.FIREBOX.get()) && !FireboxBlock.aimedAtBasin(pos, hit)
                    && state.getValue(Fromage.Station.STAGE) != Fromage.Station.WHEY_IN_VAT) {
                step = state.getValue(BlockStateProperties.LIT) ? "firebox.burning" : fireboxIgnition(held);
            } else if (station.vat) {
                step = vat(station, state, level, pos, held, player.isShiftKeyDown());
            } else {
                step = table(state, held);
            }
        } else if (state.is(Fromage.MOLD.get())) {
            step = mold(state, held);
        } else if (state.is(Fromage.PRESS.get())) {
            step = "press." + state.getValue(CheesePressBlock.STAGE);
        } else if (state.is(Fromage.SHELF.get())) {
            var slot = AgingShelfBlock.isUpper(pos, hit) ? AgingShelfBlock.UPPER : AgingShelfBlock.LOWER;
            step = switch (AgingShelfBlock.phase(state.getValue(slot))) {
                case 0 -> "shelf.empty";
                case 1 -> player.isShiftKeyDown() ? "shelf.lift_young" : "shelf.fresh";
                case 2 -> player.isShiftKeyDown() ? "shelf.lift_young" : "shelf.rind";
                case 3 -> player.isShiftKeyDown() ? "shelf.turn_mature" : "shelf.ready";
                default -> player.isShiftKeyDown() ? "shelf.turn_mature" : "shelf.extra";
            };
        } else if (state.getBlock() instanceof YoungWheelBlock) {
            step = "wheel.pick_up";
        } else if (state.getBlock() instanceof CheeseBlock.Sliceable) {
            step = "cheese.slice";
        } else {
            return null;
        }
        return Component.translatable("message.fromage." + step);
    }

    private static String vat(Fromage.Station station, BlockState state, Level level, BlockPos pos,
            ItemStack held, boolean sneaking) {
        int stage = state.getValue(Fromage.Station.STAGE);
        if (stage >= 1 && stage <= 3 && !station.heated(level, pos)) {
            return state.is(Fromage.FIREBOX.get()) || state.is(Fromage.FIREBOX_BASIN.get())
                ? fireboxIgnition(held) : "vat.cold";
        }
        DairyBatch batch = Fromage.Station.batch(state);
        if (stage == 2) {
            if (held.is(Fromage.VAT_STARTER.get())) return "vat.add_starter";
            if (held.is(Fromage.CULTURED_WHEY.get())) return "vat.add_whey";
            if (held.is(Fromage.BLUE_STARTER.get())) return "vat.add_blue";
            if (held.is(Fromage.RENNET.get()) && (batch == DairyBatch.PLAIN || batch == DairyBatch.CULTURED)) {
                return "vat.add_rennet";
            }
            if (sneaking && held.isEmpty() && batch != DairyBatch.CHEDDAR) return "vat.start_fermentation";
            if (batch == DairyBatch.CHEDDAR) return "vat.culture_after_rennet";
        }
        if (stage == Fromage.Station.WHEY_IN_VAT && batch == DairyBatch.PLAIN) return "vat.pour_next";
        if (stage == Fromage.Station.WHEY_IN_VAT) return "vat.collect_cultured_whey";
        if (stage == 1) return "vat.warming";
        if (stage == 3) {
            if (held.is(Fromage.RENNET.get()) && (batch == DairyBatch.PLAIN || batch == DairyBatch.CULTURED)) {
                return "vat.add_rennet";
            }
            return state.getValue(Fromage.Station.DRAIN_PROGRESS) == 0 ? "vat.stir" : "vat.coagulating";
        }
        if (stage == 4 && (batch == DairyBatch.PLAIN || batch == DairyBatch.CULTURED)) return "vat.rennet_or_cut";
        return "vat." + stage;
    }

    private static String fireboxIgnition(ItemStack held) {
        return held.is(Items.FLINT_AND_STEEL) ? "firebox.relight" : "firebox.add_fuel";
    }

    private static String table(BlockState state, ItemStack held) {
        int stage = state.getValue(Fromage.Station.STAGE);
        if (stage == 3 && held.is(Fromage.MOLD_ITEM.get()) && Fromage.Station.canPack(state)) {
            return "mold.packing";
        }
        DairyBatch batch = Fromage.Station.batch(state);
        if (stage == 3 && batch == DairyBatch.CHEDDAR) {
            return "table.cheddar." + Math.min(state.getValue(Fromage.Station.DRAIN_PROGRESS), 2);
        }
        if (stage == 3 && state.getValue(Fromage.Station.BUNDLED)) return "table.bundle_ready";
        if (stage == 3 && batch != DairyBatch.BLUE) return "table.collect_soft";
        return "table." + stage;
    }

    private static String mold(BlockState state, ItemStack held) {
        int portions = state.getValue(CheeseMoldBlock.PORTIONS);
        if (portions == 2) {
            int phase = state.getValue(CheeseMoldBlock.AGE);
            if (state.getValue(CheeseMoldBlock.CHEDDAR)) {
                if (phase == 0) return "mold.start_press";
                if (phase == 2) return "mold.lift_cheddar";
                return "mold.pressing";
            }
            if (state.getValue(CheeseMoldBlock.FORMING)) {
                if (phase != 2) return state.getValue(CheeseMoldBlock.BLUE) ? "mold.blue_draining" : "mold.soft_settling";
                return state.getValue(CheeseMoldBlock.BLUE) ? "mold.lift_blue" : "mold.lift_soft";
            }
            return state.getValue(CheeseMoldBlock.AGE) == 2 ? "mold.ready" : "mold.2";
        }
        if (held.is(Fromage.FRESH.get())) return "mold.soft";
        if (held.is(Fromage.BLUE_FRESH.get())) return "mold.blue_fresh";
        if (portions == 1) return state.getValue(CheeseMoldBlock.BLUE) ? "mold.more_blue" : "mold.more_cheddar";
        if (held.is(Fromage.DRAINED_BLUE.get())) return "mold.pack_blue";
        if (held.is(Fromage.SALTED_CURDS.get())) return "mold.0";
        return "mold.pack_soft";
    }

    private WorkshopStep() {}
}
