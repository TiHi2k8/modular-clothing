package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ClothingContainer extends Container {
    private final IClothingInventory clothingInventory;
    private final EntityPlayer player;

    public ClothingContainer(InventoryPlayer playerInventory, IClothingInventory clothingInventory, EntityPlayer player) {
        this.clothingInventory = clothingInventory;
        this.player = player;

        // Add Clothing Slots (Custom positions)
        // Layout:
        // L-Leg(5)  L-Arm(2)  Head(0)  R-Arm(1)  R-Leg(4)
        //           Chest(3)
        // Just an example layout, I will use a vertical column or something standard.
        // Let's emulate 3x2 grid or similar near player model.

        // Head
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 0, 80, 8, ClothingInventorySlot.HEAD, player));
        // Chest
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 3, 80, 26, ClothingInventorySlot.CHEST, player));

        // Right Arm
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 1, 98, 26, ClothingInventorySlot.RIGHT_ARM, player));
        // Left Arm
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 2, 62, 26, ClothingInventorySlot.LEFT_ARM, player));

        // Right Leg
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 4, 98, 44, ClothingInventorySlot.RIGHT_LEG, player));
        // Left Leg
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 5, 62, 44, ClothingInventorySlot.LEFT_LEG, player));

        // Right Foot
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 6, 98, 62, ClothingInventorySlot.RIGHT_FOOT, player));
        // Left Foot
        this.addSlotToContainer(new ClothingInventorySlotHandler(clothingInventory, 7, 62, 62, ClothingInventorySlot.LEFT_FOOT, player));


        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 8) { // Clothing slots to Inventory
                if (!this.mergeItemStack(itemstack1, 8, 44, true)) {
                   return ItemStack.EMPTY;
                }
            } else { // Inventory to Clothing slots
                // Try to merge into appropriate clothing slot
                Item item = itemstack1.getItem();

                // Check if valid for HEAD
                if (item.isValidArmor(itemstack1, net.minecraft.inventory.EntityEquipmentSlot.HEAD, playerIn)) {
                    if (!mergeItemStack(itemstack1, 0, 1, false)); // Try Head
                }

                // Check if valid for CHEST (if not fully merged)
                if (!itemstack1.isEmpty() && item.isValidArmor(itemstack1, net.minecraft.inventory.EntityEquipmentSlot.CHEST, playerIn)) {
                    // Try chest first, then arms
                    if (!mergeItemStack(itemstack1, 3, 4, false)) {
                         if (!mergeItemStack(itemstack1, 1, 3, false)); // Try arms (1 and 2)
                    }
                }

                // Check if valid for LEGS
                if (!itemstack1.isEmpty() && item.isValidArmor(itemstack1, net.minecraft.inventory.EntityEquipmentSlot.LEGS, playerIn)) {
                    // Try legs
                    if (!mergeItemStack(itemstack1, 4, 6, false)); // Try legs (4 and 5)
                }

                // Check if valid for FEET
                if (!itemstack1.isEmpty() && item.isValidArmor(itemstack1, net.minecraft.inventory.EntityEquipmentSlot.FEET, playerIn)) {
                     // Try feet
                    if (!mergeItemStack(itemstack1, 6, 8, false)); // Try feet (6 and 7)
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }
}
