package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for the modular clothing GUI.
 * Contains 8 clothing slots + the standard player inventory (27 + 9 hotbar).
 */
public class ClothingContainer extends Container {

    private final IClothingInventory clothingInventory;

    public ClothingContainer(InventoryPlayer playerInv, IClothingInventory clothingInventory) {
        this.clothingInventory = clothingInventory;

        // --- Clothing Slots (8 slots) ---
        // Arranged in a visual layout representing the body:
        //       [Head]          -> slot index 0   (gui slot 0)
        // [L.Arm][Chest][R.Arm] -> slot index 2,3,1  (gui slot 1,2,3)
        //   [L.Leg]  [R.Leg]    -> slot index 5,4    (gui slot 4,5)
        //   [L.Foot]  [R.Foot]  -> slot index 7,6    (gui slot 6,7)

        // Head - center top
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_HEAD, 80, 8));

        // Left Arm, Chest, Right Arm - middle row
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_LEFT_ARM, 50, 30));
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_CHEST, 80, 30));
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_RIGHT_ARM, 110, 30));

        // Left Leg, Right Leg - bottom row
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_LEFT_LEG, 65, 52));
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_RIGHT_LEG, 95, 52));

        // Left Foot, Right Foot - below legs
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_LEFT_FOOT, 65, 70));
        addSlotToContainer(new ClothingSlot(clothingInventory, IClothingInventory.SLOT_RIGHT_FOOT, 95, 70));

        // --- Player Inventory (main 27 slots) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }

        // --- Player Hotbar (9 slots) ---
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 160));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /**
     * Handle shift-click transfers between clothing slots and player inventory.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            returnStack = slotStack.copy();

            // Number of clothing slots
            final int clothingSlotCount = IClothingInventory.SLOT_COUNT;
            // Total player inventory slots (27 main + 9 hotbar)
            final int playerInvStart = clothingSlotCount;
            final int playerInvEnd = playerInvStart + 36;

            if (index < clothingSlotCount) {
                // Transfer from clothing slot to player inventory
                if (!mergeItemStack(slotStack, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Transfer from player inventory to clothing slot
                boolean merged = false;
                for (int i = 0; i < clothingSlotCount; i++) {
                    Slot clothingSlot = inventorySlots.get(i);
                    if (clothingSlot instanceof ClothingSlot && ((ClothingSlot) clothingSlot).isItemValid(slotStack)) {
                        if (!clothingSlot.getHasStack()) {
                            if (mergeItemStack(slotStack, i, i + 1, false)) {
                                merged = true;
                                break;
                            }
                        }
                    }
                }
                if (!merged) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return returnStack;
    }
}
