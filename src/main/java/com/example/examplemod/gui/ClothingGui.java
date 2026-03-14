package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketChangeClothingLayer;
import com.example.examplemod.network.PacketSetChestArmsMode;
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

        // Shift+left-click on CHEST slot toggles whether arms render alongside the chest piece
        if (mouseButton == 0 && isShiftKeyDown() && this.mc.player.inventory.getItemStack().isEmpty()) {
            for (int slotIdx = 0; slotIdx < 8; slotIdx++) {
                Slot slot = this.inventorySlots.getSlot(slotIdx);
                if (isMouseOverSlot(slot, mouseX, mouseY) && slot instanceof ClothingInventorySlotHandler) {
                    ClothingInventorySlotHandler cSlot = (ClothingInventorySlotHandler) slot;
                    if (cSlot.getCapabilitySlotIndex() == ClothingInventorySlot.CHEST.getIndex()) {
                        int layer = container.getCurrentLayer();
                        IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                        if (inv != null) {
                            boolean newMode = !inv.getChestArmsMode(layer);
                            inv.setChestArmsMode(layer, newMode); // client-side immediate preview
                            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketSetChestArmsMode(layer, newMode));
                        }
                        return;
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

        String hint = "Right-click: transform  |  Shift+click chest: toggle arms";
        int hintWidth = this.fontRenderer.getStringWidth(hint);
        this.fontRenderer.drawString(hint, (this.width / 2) - hintWidth / 2, this.guiTop + this.ySize + 4, 0x888888);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        String layerText = "Layer " + (container.getCurrentLayer() + 1);
        int textWidth = this.fontRenderer.getStringWidth(layerText);
        this.fontRenderer.drawString(layerText, 51 - textWidth / 2, 28, 4210752);

        // Chest arms mode indicator — shown to the right of the CHEST slot (x=120, y=26)
        IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (inv != null) {
            boolean armsMode = inv.getChestArmsMode(container.getCurrentLayer());
            // CHEST slot is at x=120, y=26 in the container; indicator at x=138, y=30
            this.fontRenderer.drawString(armsMode ? "Arms" : "Body", 138, 30,
                    armsMode ? 0x55FF55 : 0x888888);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        ClothingContainer container2 = (ClothingContainer) this.inventorySlots;
        IClothingInventory inv2 = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        boolean chestArms = inv2 != null && inv2.getChestArmsMode(container2.getCurrentLayer());
        this.mc.getTextureManager().bindTexture(chestArms ? TEXTURE_NA : TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        GuiInventory.drawEntityOnScreen(i + 51, j + 75, 30,
                (float)(i + 51) - this.oldMouseX,
                (float)(j + 75 - 50) - this.oldMouseY,
                this.mc.player);
    }
}
