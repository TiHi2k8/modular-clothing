package com.example.examplemod.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class ClothingInventory implements IClothingInventory {
    // List of layers, each layer is ItemStack[8]
    private final List<ItemStack[]> layers = new ArrayList<>();

    public ClothingInventory() {
        addLayer(); // Default layer
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int layer = slot / 8;
        int subSlot = slot % 8;
        if (layer < layers.size()) {
            layers.get(layer)[subSlot] = stack;
        }
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int layer = slot / 8;
        int subSlot = slot % 8;
        if (layer < layers.size()) {
            return layers.get(layer)[subSlot];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlots() {
        return layers.size() * 8;
    }

    // Layer implementation
    @Override
    public int getLayerCount() {
        return layers.size();
    }

    @Override
    public void addLayer() {
        ItemStack[] newLayer = new ItemStack[8];
        for (int i = 0; i < 8; i++) {
            newLayer[i] = ItemStack.EMPTY;
        }
        layers.add(newLayer);
    }

    @Override
    public void removeLayer() {
        if (layers.size() > 1) {
            layers.remove(layers.size() - 1);
        }
    }

    @Override
    public ItemStack getStackInLayer(int layer, int slot) {
        if (layer >= 0 && layer < layers.size() && slot >= 0 && slot < 8) {
            return layers.get(layer)[slot];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInLayer(int layer, int slot, ItemStack stack) {
        if (layer >= 0 && layer < layers.size() && slot >= 0 && slot < 8) {
            layers.get(layer)[slot] = stack;
        }
    }

    @Override
    public void copyFrom(IClothingInventory other) {
        // Clear existing
        layers.clear();
        // Add layers from other
        int count = other.getLayerCount();
        for (int l = 0; l < count; l++) {
            addLayer();
            for (int s = 0; s < 8; s++) {
                setStackInLayer(l, s, other.getStackInLayer(l, s).copy());
            }
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList layerList = new NBTTagList();

        for (int l = 0; l < layers.size(); l++) {
            NBTTagCompound layerTag = new NBTTagCompound();
            layerTag.setInteger("LayerIndex", l);
            NBTTagList items = new NBTTagList();
            ItemStack[] stacks = layers.get(l);
            for (int i = 0; i < stacks.length; i++) {
                if (!stacks[i].isEmpty()) {
                    NBTTagCompound itemTag = new NBTTagCompound();
                    itemTag.setByte("Slot", (byte) i);
                    stacks[i].writeToNBT(itemTag);
                    items.appendTag(itemTag);
                }
            }
            layerTag.setTag("Items", items);
            layerList.appendTag(layerTag);
        }

        compound.setTag("Layers", layerList);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        layers.clear();
        if (nbt.hasKey("Layers", Constants.NBT.TAG_LIST)) {
            NBTTagList layerList = nbt.getTagList("Layers", Constants.NBT.TAG_COMPOUND);
            for (int l = 0; l < layerList.tagCount(); l++) {
                NBTTagCompound layerTag = layerList.getCompoundTagAt(l);
                // Ensure layers exist up to this point
                addLayer();
                ItemStack[] currentLayer = layers.get(layers.size() - 1);

                NBTTagList items = layerTag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < items.tagCount(); i++) {
                    NBTTagCompound itemTag = items.getCompoundTagAt(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < currentLayer.length) {
                        currentLayer[slot] = new ItemStack(itemTag);
                    }
                }
            }
        } else if (nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            // Backwards compatibility for single layer
            addLayer();
            ItemStack[] currentLayer = layers.get(0);
            NBTTagList list = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound itemTag = list.getCompoundTagAt(i);
                int slot = itemTag.getInteger("Slot");
                if (slot >= 0 && slot < currentLayer.length) {
                    currentLayer[slot] = new ItemStack(itemTag);
                }
            }
        } else {
            // Always have at least 1
            addLayer();
        }
    }
}
