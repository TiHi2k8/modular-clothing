package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ClothingInventorySlotHandler extends Slot {
    private final IClothingInventory itemHandler;
    private final int index;
    private final ClothingInventorySlot slotType;

    public ClothingInventorySlotHandler(IClothingInventory itemHandler, int index, int xPosition, int yPosition, ClothingInventorySlot slotType) {
        super(null, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
        this.slotType = slotType;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Accept any item that is valid for this slot type
        return stack.getItem().isValidArmor(stack, slotType.getVanillaSlot(), null);
    }

    @Override
    public ItemStack getStack() {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public void putStack(ItemStack stack) {
        itemHandler.setStackInSlot(index, stack);
        this.onSlotChanged();
    }

    @Override
    public void onSlotChanged() {
        // Logic to notify changes if needed
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        ItemStack stack = itemHandler.getStackInSlot(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.splitStack(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(index, ItemStack.EMPTY);
        }
        return taken;
    }

    // Override this to prevent NPE since we pass null inventory to super
    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof ClothingInventorySlotHandler && ((ClothingInventorySlotHandler)other).itemHandler == this.itemHandler;
    }
}

