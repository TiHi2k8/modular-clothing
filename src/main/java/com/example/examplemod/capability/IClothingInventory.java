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
}

