package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketChangeClothingLayer;
import com.example.examplemod.network.PacketSetPartMode;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class ClothingGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/clothing_gui.png");
    private static final ResourceLocation TEXTURE_NA =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/clothing_gui_na.png");

    /** Max layer index (0-based), mirrors the 10-layer cap in ClothingInventory. */
    private static final int MAX_LAYER_INDEX = 9;

    // Custom Icons
    private static final ResourceLocation ICON_CHEST      = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_chest.png");
    private static final ResourceLocation ICON_RIGHT_ARM  = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_right_arm.png");
    private static final ResourceLocation ICON_LEFT_ARM   = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_left_arm.png");
    private static final ResourceLocation ICON_RIGHT_LEG  = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_right_leg.png");
    private static final ResourceLocation ICON_LEFT_LEG   = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_left_leg.png");
    private static final ResourceLocation ICON_RIGHT_SHOE = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_right_shoe.png");
    private static final ResourceLocation ICON_LEFT_SHOE  = new ResourceLocation(ExampleMod.MODID, "textures/gui/icons/clothing_icon_left_shoe.png");
    private static final ResourceLocation ICON_HELMET     = new ResourceLocation("minecraft", "textures/items/empty_armor_slot_helmet.png");
    private static final ResourceLocation ICON_CHESTPLATE = new ResourceLocation("minecraft", "textures/items/empty_armor_slot_chestplate.png");
    private static final ResourceLocation ICON_LEGGINGS   = new ResourceLocation("minecraft", "textures/items/empty_armor_slot_leggings.png");
    private static final ResourceLocation ICON_BOOTS      = new ResourceLocation("minecraft", "textures/items/empty_armor_slot_boots.png");

    /** Remembered across GUI open/close (client-side only). */
    private static int lastSelectedLayer = 0;

    private float oldMouseX;
    private float oldMouseY;

    public ClothingGui(ClothingContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = this.guiLeft;
        int j = this.guiTop;

        this.buttonList.add(new GuiButton(1, i + 28, j + 5, 20, 20, "-"));
        this.buttonList.add(new GuiButton(2, i + 52, j + 5, 20, 20, "+"));

        if (lastSelectedLayer > 0) {
            IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            int maxExisting = (inv != null) ? inv.getLayerCount() - 1 : 0;
            int targetLayer = Math.min(lastSelectedLayer, Math.min(maxExisting, MAX_LAYER_INDEX));
            if (targetLayer > 0) {
                ((ClothingContainer) this.inventorySlots).setCurrentLayer(targetLayer);
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(targetLayer));
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        int currentLayer = container.getCurrentLayer();

        if (button.id == 1) {
            if (currentLayer > 0) {
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer - 1));
                container.setCurrentLayer(currentLayer - 1);
            }
        } else if (button.id == 2) {
            if (currentLayer < MAX_LAYER_INDEX) {
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer + 1));
                container.setCurrentLayer(currentLayer + 1);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;

        // Shift+left-click on slots to toggle rendering modes (Single vs Double)
        if (mouseButton == 0 && isShiftKeyDown() && this.mc.player.inventory.getItemStack().isEmpty()) {
            for (int slotIdx = 0; slotIdx < 8; slotIdx++) {
                Slot slot = this.inventorySlots.getSlot(slotIdx);
                if (isMouseOverSlot(slot, mouseX, mouseY) && slot instanceof ClothingInventorySlotHandler) {
                    ClothingInventorySlotHandler cSlot = (ClothingInventorySlotHandler) slot;
                    int capSlotIndex = cSlot.getCapabilitySlotIndex();
                    int layer = container.getCurrentLayer();
                    IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);

                    if (inv != null) {
                        // Chest: Arms Mode
                        if (capSlotIndex == ClothingInventorySlot.CHEST.getIndex()) {
                            boolean newMode = !inv.getChestArmsMode(layer);
                            inv.setChestArmsMode(layer, newMode);
                            // Use generic packet for all mode switches
                            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketSetPartMode(layer, 0, newMode));
                            container.updateSlotPositions(); // Update container slots (hides arms if necessary)
                            return;
                        }
                        // Pants: Legs Mode (Double vs Single)
                        else if (capSlotIndex == ClothingInventorySlot.RIGHT_LEG.getIndex() ||
                                 capSlotIndex == ClothingInventorySlot.LEFT_LEG.getIndex()) {
                            boolean newMode = !inv.getPantsLegsMode(layer);
                            inv.setPantsLegsMode(layer, newMode);
                            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketSetPartMode(layer, 1, newMode));
                            container.updateSlotPositions();
                            return;
                        }
                        // Shoes: Feet Mode (Double vs Single)
                        else if (capSlotIndex == ClothingInventorySlot.RIGHT_FOOT.getIndex() ||
                                 capSlotIndex == ClothingInventorySlot.LEFT_FOOT.getIndex()) {
                            boolean newMode = !inv.getShoesFeetMode(layer);
                            inv.setShoesFeetMode(layer, newMode);
                            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketSetPartMode(layer, 2, newMode));
                            container.updateSlotPositions();
                            return;
                        }
                    }
                }
            }
        }

        // Right-click on any clothing slot opens the transform sub-screen
        if (mouseButton == 1) {
            for (int slotIdx = 0; slotIdx < 8; slotIdx++) {
                Slot slot = this.inventorySlots.getSlot(slotIdx);
                if (isMouseOverSlot(slot, mouseX, mouseY) && slot instanceof ClothingInventorySlotHandler) {
                    int capSlot = ((ClothingInventorySlotHandler) slot).getCapabilitySlotIndex();
                    this.mc.displayGuiScreen(new GuiClothingTransform(this, capSlot, container.getCurrentLayer()));
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int sx = this.guiLeft + slot.xPos;
        int sy = this.guiTop  + slot.yPos;
        return mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16;
    }

    @Override
    public void onGuiClosed() {
        lastSelectedLayer = ((ClothingContainer) this.inventorySlots).getCurrentLayer();
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.oldMouseX = (float) mouseX;
        this.oldMouseY = (float) mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        String hint = "Right-click: transform  |  Shift+click part: toggle mode"; // Should validly update this hint
        int hintWidth = this.fontRenderer.getStringWidth(hint);
        this.fontRenderer.drawString(hint, (this.width / 2) - hintWidth / 2, this.guiTop + this.ySize + 4, 0x888888);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        String layerText = "Layer " + (container.getCurrentLayer() + 1);
        int textWidth = this.fontRenderer.getStringWidth(layerText);
        this.fontRenderer.drawString(layerText, 51 - textWidth / 2, 28, 4210752);

        // Removed default text indicators as requested by user ("Arms", "Legs", etc.)
        // Instead, we rely on the visual slot arrangement and background icons.
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        ClothingContainer container2 = (ClothingContainer) this.inventorySlots;
        IClothingInventory inv2 = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);

        ResourceLocation currentTexture = TEXTURE;

        if (inv2 != null) {
            int layer = container2.getCurrentLayer();
            boolean chestArms = inv2.getChestArmsMode(layer);
            boolean pantsLegs = inv2.getPantsLegsMode(layer);
            boolean shoesFeet = inv2.getShoesFeetMode(layer);

            // Construct texture name based on flags:
            // Base: clothing_gui
            // Suffix parts:
            // _n (if any is true)
            // a (if chestArms)
            // l (if pantsLegs)
            // b (if shoesFeet)

            if (chestArms || pantsLegs || shoesFeet) {
                StringBuilder suffix = new StringBuilder("_n");
                if (chestArms) suffix.append("a");
                if (pantsLegs) suffix.append("l");
                if (shoesFeet) suffix.append("b");

                String path = "textures/gui/clothing_gui" + suffix.toString() + ".png";
                currentTexture = new ResourceLocation(ExampleMod.MODID, path);
            }
        }

        this.mc.getTextureManager().bindTexture(currentTexture);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        GuiInventory.drawEntityOnScreen(i + 51, j + 75, 30,
                (float)(i + 51) - this.oldMouseX,
                (float)(j + 75 - 50) - this.oldMouseY,
                this.mc.player);

        // Draw custom slot icons for empty slots
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (slot instanceof ClothingInventorySlotHandler && !slot.getHasStack()) {
                ClothingInventorySlotHandler cSlot = (ClothingInventorySlotHandler) slot;
                // Skip if slot is hidden (off-screen)
                if (cSlot.xPos < 0 || cSlot.yPos < 0) continue;

                ResourceLocation icon = null;
                switch (cSlot.getCapabilitySlotIndex()) {
                    case 0: icon = ICON_HELMET; break; // HEAD
                    case 1: icon = ICON_RIGHT_ARM; break; // RIGHT_ARM
                    case 2: icon = ICON_LEFT_ARM; break; // LEFT_ARM
                    case 3:
                        // CHEST: in merged chest+arms mode, show vanilla chestplate slot texture.
                        if (inv2 != null && inv2.getChestArmsMode(container2.getCurrentLayer())) {
                            icon = ICON_CHESTPLATE;
                        } else {
                            icon = ICON_CHEST;
                        }
                        break;
                    case 4:
                        // RIGHT_LEG / PANTS
                        // If merged mode, use standard Minecraft icon. Else use custom icon.
                        if (inv2 != null && inv2.getPantsLegsMode(container2.getCurrentLayer())) {
                            icon = ICON_LEGGINGS;
                        } else {
                            icon = ICON_RIGHT_LEG;
                        }
                        break;
                    case 5: icon = ICON_LEFT_LEG; break; // LEFT_LEG
                    case 6:
                        // RIGHT_FOOT / SHOES
                        // If merged mode, use standard Minecraft icon. Else use custom icon.
                        if (inv2 != null && inv2.getShoesFeetMode(container2.getCurrentLayer())) {
                            icon = ICON_BOOTS;
                        } else {
                            icon = ICON_RIGHT_SHOE;
                        }
                        break;
                    case 7: icon = ICON_LEFT_SHOE; break; // LEFT_FOOT
                }

                if (icon != null) {
                    this.mc.getTextureManager().bindTexture(icon);
                    GlStateManager.enableBlend(); // Enable blending for transparency
                    // Draw icon (16x16) at slot pos
                    // drawModalRectWithCustomSizedTexture assumes full texture is drawn
                    drawModalRectWithCustomSizedTexture(i + slot.xPos, j + slot.yPos, 0, 0, 16, 16, 16, 16);
                    GlStateManager.disableBlend();
                }
            }
        }
    }
}
