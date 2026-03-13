package com.example.examplemod.capability;

import net.minecraft.inventory.EntityEquipmentSlot;

public enum ClothingInventorySlot {
    HEAD(0, EntityEquipmentSlot.HEAD),
    RIGHT_ARM(1, EntityEquipmentSlot.CHEST),
    LEFT_ARM(2, EntityEquipmentSlot.CHEST),
    CHEST(3, EntityEquipmentSlot.CHEST),
    RIGHT_LEG(4, EntityEquipmentSlot.LEGS),
    LEFT_LEG(5, EntityEquipmentSlot.LEGS),
    RIGHT_FOOT(6, EntityEquipmentSlot.FEET),
    LEFT_FOOT(7, EntityEquipmentSlot.FEET);

    private final int index;
    private final EntityEquipmentSlot vanillaSlot;

    ClothingInventorySlot(int index, EntityEquipmentSlot vanillaSlot) {
        this.index = index;
        this.vanillaSlot = vanillaSlot;
    }

    public int getIndex() {
        return index;
    }

    public EntityEquipmentSlot getVanillaSlot() {
        return vanillaSlot;
    }

    public static ClothingInventorySlot fromIndex(int index) {
        for (ClothingInventorySlot slot : values()) {
            if (slot.index == index) return slot;
        }
        return HEAD;
    }
}

