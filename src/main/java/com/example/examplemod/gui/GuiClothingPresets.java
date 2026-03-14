package com.example.examplemod.gui;

import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketDeletePreset;
import com.example.examplemod.network.PacketRequestPresetList;
import com.example.examplemod.network.PacketSavePreset;
import com.example.examplemod.preset.TransformPresetManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Preset browser opened from GuiClothingTransform via the "Presets..." button.
 *
 * Layout:
 *   Title
 *   [Search field]
 *   [Preset rows: name  [Load] [Delete]]  — up to VISIBLE_ROWS shown at once
 *   [▲] / [▼] scroll buttons (right side)
 *   [Save Current]  [Close]
 *
 * Save mode replaces the bottom row with: [Name field] [Save] [Cancel]
 *
 * Delete requires OP on the server; the request is silently rejected otherwise.
 * All players can save presets.
 */
@SideOnly(Side.CLIENT)
public class GuiClothingPresets extends GuiScreen {

    // ── Static callback used by PacketPresetList.Handler ──────────────────────
    /** Set by the open GUI; cleared after delivery. Thread-safe via mc.addScheduledTask. */
    public static GuiClothingPresets openInstance = null;

    public static void deliverPresets(List<TransformPresetManager.Preset> presets) {
        if (openInstance != null) openInstance.receivePresets(presets);
    }
    // ─────────────────────────────────────────────────────────────────────────

    private static final int VISIBLE_ROWS = 7;
    private static final int ROW_H        = 18;

    // Button IDs
    private static final int BTN_CLOSE        = 0;
    private static final int BTN_SAVE_CURRENT = 1;
    private static final int BTN_SCROLL_UP    = 2;
    private static final int BTN_SCROLL_DOWN  = 3;
    private static final int BTN_CONFIRM_SAVE = 4;
    private static final int BTN_CANCEL_SAVE  = 5;
    private static final int BTN_LOAD_BASE    = 100; // 100 + filteredIndex
    private static final int BTN_DELETE_BASE  = 200; // 200 + filteredIndex

    private final GuiClothingTransform parent;

    /** Current transform values passed in from the transform screen. */
    private final float curSx, curSy, curSz, curOx, curOy, curOz;

    private List<TransformPresetManager.Preset> allPresets      = new ArrayList<>();
    private List<TransformPresetManager.Preset> filteredPresets = new ArrayList<>();

    private GuiTextField searchField;
    private GuiTextField saveNameField;

    private int     scrollOffset = 0;
    private boolean saveMode     = false;
    private boolean loading      = true;

    // Geometry (computed in initGui / buildLayout)
    private int panelLeft, panelTop, panelW;

    public GuiClothingPresets(GuiClothingTransform parent,
                               float sx, float sy, float sz,
                               float ox, float oy, float oz) {
        this.parent = parent;
        this.curSx  = sx; this.curSy = sy; this.curSz = sz;
        this.curOx  = ox; this.curOy = oy; this.curOz = oz;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        openInstance = this;
        panelW    = 260;
        panelLeft = this.width  / 2 - panelW / 2;
        panelTop  = this.height / 2 - (VISIBLE_ROWS * ROW_H / 2) - 50;

        super.initGui();
        buildLayout();
        // Ask server for the list
        ClothingNetworkHandler.INSTANCE.sendToServer(new PacketRequestPresetList());
    }

    @Override
    public void onGuiClosed() {
        openInstance = null;
        super.onGuiClosed();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildLayout() {
        this.buttonList.clear();

        int x      = panelLeft;
        int y      = panelTop;
        int right  = x + panelW;

        // Search field  (y+20 = title row, y+32 = field)
        searchField = new GuiTextField(0, this.fontRenderer, x + 5, y + 32, panelW - 10, 16);
        searchField.setMaxStringLength(50);
        searchField.setFocused(true);

        // Preset row buttons
        rebuildPresetButtons();

        // Scroll buttons (right of panel)
        int midRow = y + 52 + (VISIBLE_ROWS * ROW_H) / 2 - ROW_H;
        this.buttonList.add(new GuiButton(BTN_SCROLL_UP,   right + 4, midRow,       20, ROW_H - 2, "▲"));
        this.buttonList.add(new GuiButton(BTN_SCROLL_DOWN, right + 4, midRow + ROW_H, 20, ROW_H - 2, "▼"));

        // Bottom bar
        int bottomY = y + 52 + VISIBLE_ROWS * ROW_H + 6;

        if (!saveMode) {
            this.buttonList.add(new GuiButton(BTN_SAVE_CURRENT, x + 5,          bottomY, 120, 20, "Save Current"));
            this.buttonList.add(new GuiButton(BTN_CLOSE,        right - 125,     bottomY, 120, 20, "Close"));
        } else {
            saveNameField = new GuiTextField(1, this.fontRenderer, x + 5, bottomY + 1, panelW - 120, 18);
            saveNameField.setMaxStringLength(40);
            saveNameField.setFocused(true);
            int bx = x + 5 + panelW - 115;
            this.buttonList.add(new GuiButton(BTN_CONFIRM_SAVE, bx,      bottomY, 55, 20, "Save"));
            this.buttonList.add(new GuiButton(BTN_CANCEL_SAVE,  bx + 58, bottomY, 57, 20, "Cancel"));
        }
    }

    /** Removes and recreates only the Load/Delete buttons for the current page. */
    private void rebuildPresetButtons() {
        this.buttonList.removeIf(b -> b.id >= BTN_LOAD_BASE);

        int rowY = panelTop + 52;
        int end  = Math.min(scrollOffset + VISIBLE_ROWS, filteredPresets.size());
        for (int i = scrollOffset; i < end; i++) {
            int ry = rowY + (i - scrollOffset) * ROW_H;
            this.buttonList.add(new GuiButton(BTN_LOAD_BASE   + i, panelLeft + panelW - 100, ry, 46, ROW_H - 2, "Load"));
            this.buttonList.add(new GuiButton(BTN_DELETE_BASE + i, panelLeft + panelW -  51, ry, 46, ROW_H - 2, "Delete"));
        }
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    /** Called from the packet handler on the main thread. */
    public void receivePresets(List<TransformPresetManager.Preset> presets) {
        this.allPresets = presets;
        this.loading    = false;
        applySearch();
    }

    private void applySearch() {
        String q = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        filteredPresets.clear();
        for (TransformPresetManager.Preset p : allPresets) {
            if (q.isEmpty() || p.name.toLowerCase().contains(q)) {
                filteredPresets.add(p);
            }
        }
        // Clamp scroll
        int maxScroll = Math.max(0, filteredPresets.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        rebuildPresetButtons();
    }

    // ── Input handling ────────────────────────────────────────────────────────

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {

        if (button.id == BTN_CLOSE) {
            this.mc.displayGuiScreen(parent);
            return;
        }

        if (button.id == BTN_SAVE_CURRENT) {
            saveMode = true;
            buildLayout();
            return;
        }

        if (button.id == BTN_CANCEL_SAVE) {
            saveMode = false;
            buildLayout();
            return;
        }

        if (button.id == BTN_CONFIRM_SAVE) {
            String name = saveNameField != null ? saveNameField.getText().trim() : "";
            if (!name.isEmpty()) {
                sendSave(name);
            }
            return;
        }

        if (button.id == BTN_SCROLL_UP) {
            if (scrollOffset > 0) { scrollOffset--; rebuildPresetButtons(); }
            return;
        }
        if (button.id == BTN_SCROLL_DOWN) {
            if (scrollOffset < filteredPresets.size() - VISIBLE_ROWS) { scrollOffset++; rebuildPresetButtons(); }
            return;
        }

        if (button.id >= BTN_LOAD_BASE && button.id < BTN_DELETE_BASE) {
            int idx = button.id - BTN_LOAD_BASE;
            if (idx < filteredPresets.size()) {
                TransformPresetManager.Preset p = filteredPresets.get(idx);
                parent.applyPreset(p.scaleX, p.scaleY, p.scaleZ, p.offsetX, p.offsetY, p.offsetZ);
                this.mc.displayGuiScreen(parent);
            }
            return;
        }

        if (button.id >= BTN_DELETE_BASE) {
            int idx = button.id - BTN_DELETE_BASE;
            if (idx < filteredPresets.size()) {
                ClothingNetworkHandler.INSTANCE.sendToServer(
                        new PacketDeletePreset(filteredPresets.get(idx).name));
                loading = true;
                rebuildPresetButtons(); // clear rows until updated list arrives
            }
        }
    }

    private void sendSave(String name) {
        ClothingNetworkHandler.INSTANCE.sendToServer(
                new PacketSavePreset(name, curSx, curSy, curSz, curOx, curOy, curOz));
        saveMode = false;
        loading  = true;
        buildLayout();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // Escape
            if (saveMode) { saveMode = false; buildLayout(); return; }
            this.mc.displayGuiScreen(parent);
            return;
        }
        if (saveMode && saveNameField != null) {
            saveNameField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) { // Enter
                String name = saveNameField.getText().trim();
                if (!name.isEmpty()) sendSave(name);
            }
        } else if (searchField != null) {
            searchField.textboxKeyTyped(typedChar, keyCode);
            applySearch();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (searchField   != null) searchField.mouseClicked(mouseX, mouseY, mouseButton);
        if (saveNameField != null) saveNameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel > 0 && scrollOffset > 0) {
            scrollOffset--;
            rebuildPresetButtons();
        } else if (wheel < 0 && scrollOffset < filteredPresets.size() - VISIBLE_ROWS) {
            scrollOffset++;
            rebuildPresetButtons();
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int x      = panelLeft;
        int y      = panelTop;
        int right  = x + panelW;
        int bottom = y + 52 + VISIBLE_ROWS * ROW_H + 30;

        // Panel background
        drawRect(x, y, right, bottom, 0xBB000000);
        drawRect(x, y, right, y + 1,  0xFF555555); // top border
        drawRect(x, y, x + 1, bottom, 0xFF555555); // left border
        drawRect(right - 1, y, right, bottom, 0xFF555555); // right border
        drawRect(x, bottom - 1, right, bottom, 0xFF555555); // bottom border

        // Title
        String title = "Transform Presets";
        int tw = this.fontRenderer.getStringWidth(title);
        this.fontRenderer.drawStringWithShadow(title, x + panelW / 2 - tw / 2, y + 8, 0xFFFFFF);

        // Search label
        this.fontRenderer.drawStringWithShadow("Search:", x + 5, y + 20, 0xAAAAAA);

        // Content
        if (loading) {
            String txt = "Loading...";
            this.fontRenderer.drawStringWithShadow(txt,
                    x + panelW / 2 - this.fontRenderer.getStringWidth(txt) / 2,
                    y + 60, 0x888888);
        } else if (filteredPresets.isEmpty()) {
            String txt = allPresets.isEmpty() ? "No presets saved yet" : "No results";
            this.fontRenderer.drawStringWithShadow(txt,
                    x + panelW / 2 - this.fontRenderer.getStringWidth(txt) / 2,
                    y + 60, 0x888888);
        } else {
            int rowY = y + 52;
            int end  = Math.min(scrollOffset + VISIBLE_ROWS, filteredPresets.size());
            for (int i = scrollOffset; i < end; i++) {
                int ry   = rowY + (i - scrollOffset) * ROW_H;
                String name = filteredPresets.get(i).name;
                // Truncate name so it doesn't overlap Load button
                int maxW = panelW - 110;
                if (this.fontRenderer.getStringWidth(name) > maxW) {
                    while (name.length() > 1 && this.fontRenderer.getStringWidth(name + "…") > maxW) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "…";
                }
                this.fontRenderer.drawString(name, x + 6, ry + (ROW_H - 8) / 2, 0xDDDDDD);
            }

            // Scroll position indicator
            if (filteredPresets.size() > VISIBLE_ROWS) {
                String info = (scrollOffset + 1) + "–"
                        + Math.min(scrollOffset + VISIBLE_ROWS, filteredPresets.size())
                        + " / " + filteredPresets.size();
                this.fontRenderer.drawStringWithShadow(info,
                        x + panelW / 2 - this.fontRenderer.getStringWidth(info) / 2,
                        y + 52 + VISIBLE_ROWS * ROW_H + 1, 0x666666);
            }
        }

        // Save-mode label
        if (saveMode) {
            int bottomY = y + 52 + VISIBLE_ROWS * ROW_H + 6;
            this.fontRenderer.drawStringWithShadow("Name:", x + 5, bottomY + 5, 0xAAAAAA);
        }

        // Draw text fields
        if (searchField   != null) searchField.drawTextBox();
        if (saveNameField != null) saveNameField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
