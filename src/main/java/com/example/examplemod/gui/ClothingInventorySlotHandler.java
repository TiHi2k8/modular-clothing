package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketSyncClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ClothingInventorySlotHandler extends Slot {
    private final IClothingInventory itemHandler;
    private final int index;
    private final ClothingInventorySlot slotType;
    private final EntityPlayer player;

    public ClothingInventorySlotHandler(IClothingInventory itemHandler, int index, int xPosition, int yPosition, ClothingInventorySlot slotType, EntityPlayer player) {
        super(null, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
        this.slotType = slotType;
        this.player = player;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Accept any item that is valid for this slot type
        return stack.getItem().isValidArmor(stack, slotType.getVanillaSlot(), this.player);
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
        if (player != null && !player.world.isRemote && player instanceof EntityPlayerMP) {
             EntityPlayerMP mpPlayer = (EntityPlayerMP) player;
             ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(mpPlayer), mpPlayer);
             ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(mpPlayer), mpPlayer);
        }
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
