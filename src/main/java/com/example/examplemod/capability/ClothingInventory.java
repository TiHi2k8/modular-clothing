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
    // Per-layer per-slot transforms: transforms[layer][slot] = float[6]{scaleX, scaleY, scaleZ, offsetX, offsetY, offsetZ}
    private final List<float[][]> transforms = new ArrayList<>();
    // Per-layer chest arms mode: true = render arms alongside body for CHEST slot
    private final List<Boolean> chestArmsModes = new ArrayList<>();

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
        float[][] layerTransforms = new float[8][];
        for (int i = 0; i < 8; i++) {
            layerTransforms[i] = new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        }
        transforms.add(layerTransforms);
        chestArmsModes.add(false);
    }

    @Override
    public void removeLayer() {
        if (layers.size() > 1) {
            layers.remove(layers.size() - 1);
            if (!transforms.isEmpty()) transforms.remove(transforms.size() - 1);
            if (!chestArmsModes.isEmpty()) chestArmsModes.remove(chestArmsModes.size() - 1);
        }
    }

    @Override
    public float[] getSlotTransform(int layer, int slot) {
        if (layer >= 0 && layer < transforms.size() && slot >= 0 && slot < 8) {
            float[] t = transforms.get(layer)[slot];
            return new float[]{t[0], t[1], t[2], t[3], t[4], t[5]};
        }
        return new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
    }

    @Override
    public void setSlotTransform(int layer, int slot, float scaleX, float scaleY, float scaleZ, float offsetX, float offsetY, float offsetZ) {
        if (layer >= 0 && layer < transforms.size() && slot >= 0 && slot < 8) {
            transforms.get(layer)[slot] = new float[]{scaleX, scaleY, scaleZ, offsetX, offsetY, offsetZ};
        }
    }

    @Override
    public boolean getChestArmsMode(int layer) {
        if (layer >= 0 && layer < chestArmsModes.size()) {
            return chestArmsModes.get(layer);
        }
        return false;
    }

    @Override
    public void setChestArmsMode(int layer, boolean showArms) {
        if (layer >= 0 && layer < chestArmsModes.size()) {
            chestArmsModes.set(layer, showArms);
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
        chestArmsModes.clear();
        int count = other.getLayerCount();
        for (int l = 0; l < count; l++) {
            addLayer();
            for (int s = 0; s < 8; s++) {
                setStackInLayer(l, s, other.getStackInLayer(l, s).copy());
                float[] t = other.getSlotTransform(l, s);
                setSlotTransform(l, s, t[0], t[1], t[2], t[3], t[4], t[5]);
            }
            setChestArmsMode(l, other.getChestArmsMode(l));
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList layerList = new NBTTagList();

        for (int l = 0; l < layers.size(); l++) {
            NBTTagCompound layerTag = new NBTTagCompound();
            layerTag.setInteger("LayerIndex", l);

            // Items
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

            // Transforms (only write non-default values)
            if (l < transforms.size()) {
                NBTTagList transformList = new NBTTagList();
                float[][] lt = transforms.get(l);
                for (int s = 0; s < 8; s++) {
                    float[] t = lt[s];
                    if (t[0] != 1.0f || t[1] != 1.0f || t[2] != 1.0f || t[3] != 0.0f || t[4] != 0.0f || t[5] != 0.0f) {
                        NBTTagCompound transformTag = new NBTTagCompound();
                        transformTag.setByte("Slot", (byte) s);
                        transformTag.setFloat("SX", t[0]);
                        transformTag.setFloat("SY", t[1]);
                        transformTag.setFloat("SZ", t[2]);
                        transformTag.setFloat("OX", t[3]);
                        transformTag.setFloat("OY", t[4]);
                        transformTag.setFloat("OZ", t[5]);
                        transformList.appendTag(transformTag);
                    }
                }
                if (transformList.tagCount() > 0) {
                    layerTag.setTag("Transforms", transformList);
                }
            }

            // Chest arms mode (only write when true)
            if (l < chestArmsModes.size() && chestArmsModes.get(l)) {
                layerTag.setBoolean("ChestArms", true);
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
        chestArmsModes.clear();

        if (nbt.hasKey("Layers", Constants.NBT.TAG_LIST)) {
            NBTTagList layerList = nbt.getTagList("Layers", Constants.NBT.TAG_COMPOUND);
            for (int l = 0; l < layerList.tagCount() && l < MAX_LAYERS; l++) {
                NBTTagCompound layerTag = layerList.getCompoundTagAt(l);
                addLayer();
                int layerIdx = layers.size() - 1;
                ItemStack[] currentLayer = layers.get(layerIdx);

                // Items
                NBTTagList items = layerTag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < items.tagCount(); i++) {
                    NBTTagCompound itemTag = items.getCompoundTagAt(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot < currentLayer.length) {
                        currentLayer[slot] = new ItemStack(itemTag);
                    }
                }

                // Transforms
                if (layerTag.hasKey("Transforms", Constants.NBT.TAG_LIST)) {
                    NBTTagList transformList = layerTag.getTagList("Transforms", Constants.NBT.TAG_COMPOUND);
                    float[][] lt = transforms.get(layerIdx);
                    for (int t = 0; t < transformList.tagCount(); t++) {
                        NBTTagCompound tag = transformList.getCompoundTagAt(t);
                        int s = tag.getByte("Slot") & 255;
                        if (s < 8) {
                            if (tag.hasKey("SX")) {
                                // New format: per-axis scale
                                lt[s][0] = tag.getFloat("SX");
                                lt[s][1] = tag.getFloat("SY");
                                lt[s][2] = tag.getFloat("SZ");
                            } else if (tag.hasKey("Scale")) {
                                // Backward compat: old uniform scale
                                float scale = tag.getFloat("Scale");
                                lt[s][0] = scale;
                                lt[s][1] = scale;
                                lt[s][2] = scale;
                            }
                            lt[s][3] = tag.getFloat("OX");
                            lt[s][4] = tag.getFloat("OY");
                            lt[s][5] = tag.getFloat("OZ");
                        }
                    }
                }

                // Chest arms mode
                if (layerTag.getBoolean("ChestArms")) {
                    chestArmsModes.set(layerIdx, true);
                }
            }
        } else if (nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            // Backward compat: single layer, no transforms
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
