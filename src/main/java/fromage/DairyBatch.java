package fromage;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

enum DairyBatch implements StringRepresentable {
    PLAIN, CULTURED, BLUE, CHEDDAR;

    @Override public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    Item curds() {
        return switch (this) {
            case PLAIN -> Fromage.CURDS.get();
            case CULTURED -> Fromage.CULTURED_CURDS.get();
            case BLUE -> Fromage.BLUE_CURDS.get();
            case CHEDDAR -> Fromage.CHEDDAR_CURDS.get();
        };
    }

    Item whey() {
        return this == PLAIN ? Fromage.WHEY.get() : Fromage.CULTURED_WHEY.get();
    }

    static DairyBatch forCurds(ItemStack stack) {
        if (stack.is(Fromage.CURDS.get())) return PLAIN;
        if (stack.is(Fromage.CULTURED_CURDS.get())) return CULTURED;
        if (stack.is(Fromage.BLUE_CURDS.get())) return BLUE;
        if (stack.is(Fromage.CHEDDAR_CURDS.get())) return CHEDDAR;
        return null;
    }
}

