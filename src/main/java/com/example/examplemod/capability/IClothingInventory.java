package com.example.examplemod.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

public interface IClothingInventory extends INBTSerializable<NBTTagCompound> {
    void setStackInSlot(int slot, ItemStack stack);
    ItemStack getStackInSlot(int slot);
    int getSlots();
    void copyFrom(IClothingInventory other);

    // Layer support
    int getLayerCount();
    void addLayer();
    void removeLayer();
    ItemStack getStackInLayer(int layer, int slot);
    void setStackInLayer(int layer, int slot, ItemStack stack);

    // Per-slot transform: float[6] = { scaleX, scaleY, scaleZ, offsetX, offsetY, offsetZ }
    // Defaults: all scales=1.0, all offsets=0.0
    float[] getSlotTransform(int layer, int slot);
    void setSlotTransform(int layer, int slot, float scaleX, float scaleY, float scaleZ, float offsetX, float offsetY, float offsetZ);

    // CHEST slot: if true, the chest piece also renders both arms in addition to the body
    boolean getChestArmsMode(int layer);
    void setChestArmsMode(int layer, boolean showArms);
}
