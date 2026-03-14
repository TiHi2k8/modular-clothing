package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketSyncClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

public class ClothingInventorySlotHandler extends Slot {
    private final IClothingInventory itemHandler;
    private final int index;
    private final ClothingInventorySlot slotType;
    private final EntityPlayer player;
    private final Supplier<Integer> layerSupplier;

    public ClothingInventorySlotHandler(IClothingInventory itemHandler, int index, int xPosition, int yPosition, ClothingInventorySlot slotType, EntityPlayer player, Supplier<Integer> layerSupplier) {
        super(null, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
        this.slotType = slotType;
        this.player = player;
        this.layerSupplier = layerSupplier;
        this.setSlotBackground();
    }

    // Legacy constructor just for smooth migration (optional)
    public ClothingInventorySlotHandler(IClothingInventory itemHandler, int index, int xPosition, int yPosition, ClothingInventorySlot slotType, EntityPlayer player) {
        this(itemHandler, index, xPosition, yPosition, slotType, player, () -> 0);
    }

    private void setSlotBackground() {
        switch (slotType) {
            case HEAD:
                this.setBackgroundName("minecraft:items/empty_armor_slot_helmet");
                break;
            case CHEST:
            case RIGHT_ARM:
            case LEFT_ARM:
                this.setBackgroundName("minecraft:items/empty_armor_slot_chestplate");
                break;
            case RIGHT_LEG:
            case LEFT_LEG:
                this.setBackgroundName("minecraft:items/empty_armor_slot_leggings");
                break;
            case RIGHT_FOOT:
            case LEFT_FOOT:
                this.setBackgroundName("minecraft:items/empty_armor_slot_boots");
                break;
        }
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Accept any item that is valid for this slot type
        return stack.getItem().isValidArmor(stack, slotType.getVanillaSlot(), this.player);
    }

    @Override
    public ItemStack getStack() {
        return itemHandler.getStackInLayer(getLayer(), index);
    }

    @Override
    public void putStack(ItemStack stack) {
        itemHandler.setStackInLayer(getLayer(), index, stack);
        this.onSlotChanged();
    }

    // Note: getSlotStackLimit / decrStackSize must also use layer
    @Override
    public ItemStack decrStackSize(int amount) {
        ItemStack stack = getStack();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.splitStack(amount);
        if (stack.isEmpty()) {
            putStack(ItemStack.EMPTY);
        }
        return taken;
    }

    /** Returns the capability-side slot index (0-7) this handler targets. */
    public int getCapabilitySlotIndex() {
        return index;
    }

    private int getLayer() {
        return layerSupplier.get();
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

    // Override this to prevent NPE since we pass null inventory to super
    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof ClothingInventorySlotHandler && ((ClothingInventorySlotHandler)other).itemHandler == this.itemHandler;
    }
}
