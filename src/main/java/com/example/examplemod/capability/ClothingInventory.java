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
    // Per-layer per-slot transforms: transforms[layer][slot] = float[4]{scale, offsetX, offsetY, offsetZ}
    private final List<float[][]> transforms = new ArrayList<>();

    private static final int MAX_LAYERS = 10;

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
        if (layers.size() >= MAX_LAYERS) return;
        ItemStack[] newLayer = new ItemStack[8];
        for (int i = 0; i < 8; i++) {
            newLayer[i] = ItemStack.EMPTY;
        }
        layers.add(newLayer);
        float[][] layerTransforms = new float[8][4];
        for (int i = 0; i < 8; i++) {
            layerTransforms[i] = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
        }
        transforms.add(layerTransforms);
    }

    @Override
    public void removeLayer() {
        if (layers.size() > 1) {
            layers.remove(layers.size() - 1);
            if (!transforms.isEmpty()) {
                transforms.remove(transforms.size() - 1);
            }
        }
    }

    @Override
    public float[] getSlotTransform(int layer, int slot) {
        if (layer >= 0 && layer < transforms.size() && slot >= 0 && slot < 8) {
            float[] t = transforms.get(layer)[slot];
            return new float[]{t[0], t[1], t[2], t[3]};
        }
        return new float[]{1.0f, 0.0f, 0.0f, 0.0f};
    }

    @Override
    public void setSlotTransform(int layer, int slot, float scale, float offsetX, float offsetY, float offsetZ) {
        if (layer >= 0 && layer < transforms.size() && slot >= 0 && slot < 8) {
            transforms.get(layer)[slot] = new float[]{scale, offsetX, offsetY, offsetZ};
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
        layers.clear();
        transforms.clear();
        int count = other.getLayerCount();
        for (int l = 0; l < count; l++) {
            addLayer();
            for (int s = 0; s < 8; s++) {
                setStackInLayer(l, s, other.getStackInLayer(l, s).copy());
                float[] t = other.getSlotTransform(l, s);
                setSlotTransform(l, s, t[0], t[1], t[2], t[3]);
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

            // Serialize transforms for this layer
            if (l < transforms.size()) {
                NBTTagList transformList = new NBTTagList();
                float[][] lt = transforms.get(l);
                for (int s = 0; s < 8; s++) {
                    // Only write non-default transforms to save space
                    float[] t = lt[s];
                    if (t[0] != 1.0f || t[1] != 0.0f || t[2] != 0.0f || t[3] != 0.0f) {
                        NBTTagCompound transformTag = new NBTTagCompound();
                        transformTag.setByte("Slot", (byte) s);
                        transformTag.setFloat("Scale", t[0]);
                        transformTag.setFloat("OX", t[1]);
                        transformTag.setFloat("OY", t[2]);
                        transformTag.setFloat("OZ", t[3]);
                        transformList.appendTag(transformTag);
                    }
                }
                if (transformList.tagCount() > 0) {
                    layerTag.setTag("Transforms", transformList);
                }
            }

            layerList.appendTag(layerTag);
        }

        compound.setTag("Layers", layerList);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        layers.clear();
        transforms.clear();
        if (nbt.hasKey("Layers", Constants.NBT.TAG_LIST)) {
            NBTTagList layerList = nbt.getTagList("Layers", Constants.NBT.TAG_COMPOUND);
            for (int l = 0; l < layerList.tagCount() && l < MAX_LAYERS; l++) {
                NBTTagCompound layerTag = layerList.getCompoundTagAt(l);
                addLayer();
                ItemStack[] currentLayer = layers.get(layers.size() - 1);

                NBTTagList items = layerTag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < items.tagCount(); i++) {
                    NBTTagCompound itemTag = items.getCompoundTagAt(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot < currentLayer.length) {
                        currentLayer[slot] = new ItemStack(itemTag);
                    }
                }

                // Deserialize transforms
                if (layerTag.hasKey("Transforms", Constants.NBT.TAG_LIST)) {
                    NBTTagList transformList = layerTag.getTagList("Transforms", Constants.NBT.TAG_COMPOUND);
                    float[][] lt = transforms.get(transforms.size() - 1);
                    for (int t = 0; t < transformList.tagCount(); t++) {
                        NBTTagCompound transformTag = transformList.getCompoundTagAt(t);
                        int s = transformTag.getByte("Slot") & 255;
                        if (s < 8) {
                            lt[s][0] = transformTag.getFloat("Scale");
                            lt[s][1] = transformTag.getFloat("OX");
                            lt[s][2] = transformTag.getFloat("OY");
                            lt[s][3] = transformTag.getFloat("OZ");
                        }
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
            addLayer();
        }
    }
}
