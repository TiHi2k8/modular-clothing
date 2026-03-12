package com.example.examplemod.capability;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/**
 * Default implementation of the modular clothing inventory.
 * Stores ItemStacks for each body part slot.
 */
public class ClothingInventory implements IClothingInventory {

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    public ClothingInventory() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        return slots[slot];
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        slots[slot] = stack == null ? ItemStack.EMPTY : stack;
    }

    /**
     * Validates which armor types are allowed in each slot:
     * - Head slot: helmets only
     * - Chest slot: chestplates only
     * - Right/Left Arm slots: chestplates (renders only the arm part)
     * - Right/Left Leg slots: leggings (renders only the leg part)
     * - Right/Left Foot slots: boots (renders only the foot part)
     *
     * Also accepts DynamX clothing items and similar modded items.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;

        // Accept ItemArmor with strict validation
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            EntityEquipmentSlot armorSlot = armor.armorType;

            switch (slot) {
                case SLOT_HEAD:
                    return armorSlot == EntityEquipmentSlot.HEAD;
                case SLOT_RIGHT_ARM:
                case SLOT_LEFT_ARM:
                case SLOT_CHEST:
                    return armorSlot == EntityEquipmentSlot.CHEST;
                case SLOT_RIGHT_LEG:
                case SLOT_LEFT_LEG:
                    return armorSlot == EntityEquipmentSlot.LEGS;
                case SLOT_RIGHT_FOOT:
                case SLOT_LEFT_FOOT:
                    return armorSlot == EntityEquipmentSlot.FEET;
                default:
                    return false;
            }
        }

        // Accept DynamX and other modded clothing items
        String className = stack.getItem().getClass().getName().toLowerCase();
        if (className.contains("dynamx") || className.contains("clothing")) {
            // DynamX items can go in any clothing slot
            return slot >= SLOT_HEAD && slot <= SLOT_LEFT_FOOT;
        }

        return false;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setByte("Slot", (byte) i);
            if (!slots[i].isEmpty()) {
                slots[i].writeToNBT(slotTag);
            }
            list.appendTag(slotTag);
        }
        nbt.setTag("ClothingSlots", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        // Clear all slots first
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = ItemStack.EMPTY;
        }
        NBTTagList list = nbt.getTagList("ClothingSlots", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound slotTag = list.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < SLOT_COUNT) {
                slots[slot] = new ItemStack(slotTag);
            }
        }
    }

    /**
     * Copy all data from another inventory (used for player clone events).
     */
    public void copyFrom(IClothingInventory other) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = other.getStackInSlot(i).copy();
        }
    }
}

