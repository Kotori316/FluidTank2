package com.kotori316.fluidtank.fluids;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;

public enum PotionType {
    NORMAL((PotionItem) Items.POTION),
    SPLASH((PotionItem) Items.SPLASH_POTION),
    LINGERING((PotionItem) Items.LINGERING_POTION);

    private final PotionItem item;

    PotionType(PotionItem item) {
        this.item = item;
    }

    public PotionItem getItem() {
        return item;
    }

    /**
     * @throws IllegalArgumentException if the item is not a potion item.
     */
    public static PotionType fromItemUnsafe(Item item) {
        if (item == Items.POTION) return NORMAL;
        if (item == Items.SPLASH_POTION) return SPLASH;
        if (item == Items.LINGERING_POTION) return LINGERING;
        throw new IllegalArgumentException("Unknown potion item, " + item);
    }
}
