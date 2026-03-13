package com.example.examplemod.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class ClothingInventory implements IClothingInventory {
    private ItemStack[] stacks = new ItemStack[8];

    public ClothingInventory() {
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < stacks.length) {
            stacks[slot] = stack;
        }
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < stacks.length) {
            return stacks[slot];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlots() {
        return stacks.length;
    }

    @Override
    public void copyFrom(IClothingInventory other) {
        for (int i = 0; i < getSlots(); i++) {
            setStackInSlot(i, other.getStackInSlot(i).copy());
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < stacks.length; i++) {
            if (!stacks[i].isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                stacks[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        compound.setTag("Items", list);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
        NBTTagList list = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound itemTag = list.getCompoundTagAt(i);
            int slot = itemTag.getInteger("Slot");
            if (slot >= 0 && slot < stacks.length) {
                stacks[slot] = new ItemStack(itemTag);
            }
        }
    }
}

