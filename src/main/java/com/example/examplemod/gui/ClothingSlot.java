package com.example.examplemod.gui;

import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Custom slot implementation for clothing inventory slots.
 * Wraps the IClothingInventory capability so Container can interact with it.
 * Each slot validates that only appropriate armor types can be placed.
 */
public class ClothingSlot extends Slot {

    private final IClothingInventory clothingInventory;
    private final int clothingSlotIndex;

    public ClothingSlot(IClothingInventory inventory, int slotIndex, int xPos, int yPos) {
        // Pass a dummy IInventory since we override all methods
        super(new ClothingInventoryWrapper(inventory), slotIndex, xPos, yPos);
        this.clothingInventory = inventory;
        this.clothingSlotIndex = slotIndex;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return clothingInventory.isItemValidForSlot(clothingSlotIndex, stack);
    }

    @Override
    public int getSlotStackLimit() {
        return 1; // Armor is always stack size 1
    }

    @Override
    public ItemStack getStack() {
        return clothingInventory.getStackInSlot(clothingSlotIndex);
    }

    @Override
    public void putStack(ItemStack stack) {
        clothingInventory.setStackInSlot(clothingSlotIndex, stack);
        onSlotChanged();
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        ItemStack current = clothingInventory.getStackInSlot(clothingSlotIndex);
        if (current.isEmpty()) return ItemStack.EMPTY;

        ItemStack result;
        if (current.getCount() <= amount) {
            result = current.copy();
            clothingInventory.setStackInSlot(clothingSlotIndex, ItemStack.EMPTY);
        } else {
            result = current.splitStack(amount);
            if (current.isEmpty()) {
                clothingInventory.setStackInSlot(clothingSlotIndex, ItemStack.EMPTY);
            }
        }
        onSlotChanged();
        return result;
    }

    @Override
    public boolean isHere(IInventory inv, int slotIn) {
        return false; // We don't use the standard IInventory
    }

    /**
     * Minimal IInventory wrapper to satisfy Slot's constructor.
     * All real logic is handled by overriding Slot methods directly.
     */
    private static class ClothingInventoryWrapper implements IInventory {

        private final IClothingInventory inv;

        ClothingInventoryWrapper(IClothingInventory inv) {
            this.inv = inv;
        }

        @Override
        public int getSizeInventory() {
            return inv.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return inv.getStackInSlot(index);
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            ItemStack stack = inv.getStackInSlot(index);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (stack.getCount() <= count) {
                inv.setStackInSlot(index, ItemStack.EMPTY);
                return stack;
            }
            ItemStack result = stack.splitStack(count);
            if (stack.isEmpty()) inv.setStackInSlot(index, ItemStack.EMPTY);
            return result;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            ItemStack stack = inv.getStackInSlot(index);
            inv.setStackInSlot(index, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            inv.setStackInSlot(index, stack);
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return inv.isItemValidForSlot(index, stack);
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            for (int i = 0; i < inv.getSlots(); i++) {
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public String getName() {
            return "clothing";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentString(getName());
        }
    }
}

