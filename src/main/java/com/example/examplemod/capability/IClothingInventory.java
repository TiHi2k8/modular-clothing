package com.example.examplemod.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Capability interface for the modular clothing inventory.
 * Slots:
 * 0: Head
 * 1: Right Arm
 * 2: Left Arm
 * 3: Chest
 * 4: Right Leg
 * 5: Left Leg
 * 6: Right Foot
 * 7: Left Foot
 */
public interface IClothingInventory {

    int SLOT_HEAD = 0;
    int SLOT_RIGHT_ARM = 1;
    int SLOT_LEFT_ARM = 2;
    int SLOT_CHEST = 3;
    int SLOT_RIGHT_LEG = 4;
    int SLOT_LEFT_LEG = 5;
    int SLOT_RIGHT_FOOT = 6;
    int SLOT_LEFT_FOOT = 7;
    int SLOT_COUNT = 8;

    /**
     * Get the number of clothing slots.
     */
    int getSlots();

    /**
     * Get the ItemStack in the given slot.
     */
    ItemStack getStackInSlot(int slot);

    /**
     * Set the ItemStack in the given slot.
     */
    void setStackInSlot(int slot, ItemStack stack);

    /**
     * Check if the given item is valid for the given slot.
     */
    boolean isItemValidForSlot(int slot, ItemStack stack);

    /**
     * Serialize the inventory to NBT.
     */
    NBTTagCompound serializeNBT();

    /**
     * Deserialize the inventory from NBT.
     */
    void deserializeNBT(NBTTagCompound nbt);
}
