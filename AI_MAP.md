# Project Structure — Modular Clothing Mod

This file is a table of contents for AI agents. It lists **all** key classes and files of the project with a short description.

**🚨 CRITICAL INSTRUCTIONS FOR AI AGENTS:**
1. **Always read this file first** to find the correct file for a task — do not open unnecessary files.
2. **If you create, rename, or delete a class/file, update this `AI_Map.md` immediately!** It is your responsibility to keep this map strictly up to date.
3. **Context:** This is a Minecraft Forge 1.12.2 mod providing a per-limb modular clothing system with network synchronization and specific rendering fallbacks (e.g., DynamX OBJ compatibility).

---

## ⚙️ Core / Setup (`com.example.examplemod`)
- **`ExampleMod.java`**: Main mod class. Handles `@Mod` declaration, initialization events (`preInit`, `init`), registers capabilities, network, and the GUI handler.
- **`CommonProxy.java` / `ClientProxy.java`**: Sided proxies for client/server specific registrations (like EventHandlers and Keybinds).

---

## 🎒 Capability System (`com.example.examplemod.capability`)
- **`IClothingInventory.java`**: Interface defining the custom clothing inventory (supports layers, slots, and per-slot transforms). Added `getSlotTransform` / `setSlotTransform` returning `float[4]{scale, offsetX, offsetY, offsetZ}`.
- **`ClothingInventory.java`**: Default implementation of `IClothingInventory`. Handles NBT serialization/deserialization, layer management (max 10 layers), and per-slot transform storage.
- **`ClothingInventorySlot.java`**: Enum mapping internal slot indices (0-7) to physical body parts (`HEAD`, `CHEST`, `RIGHT_ARM`, etc.) and Vanilla `EntityEquipmentSlot`s.
- **`ClothingProvider.java` / `ClothingStorage.java`**: Forge capability provider and storage classes to attach the inventory to the `EntityPlayer`.
- **`CapabilityHandler.java`**: Event subscriber that attaches the `CLOTHING_CAPABILITY` to players on `AttachCapabilitiesEvent`.

---

## 📡 Network & Sync (`com.example.examplemod.network`)
- **`ClothingNetworkHandler.java`**: Sets up the `SimpleNetworkWrapper` and registers all packets. Contains helper methods to sync data to tracking players.
- **`PacketUpdateClothingSlot.java`**: Client → Server. Sent when a player modifies a slot in the custom GUI.
- **`PacketSyncClothingInventory.java`**: Server → Client. Full inventory sync packet (used on join, respawn, dimension change, or slot/transform updates).
- **`PacketChangeClothingLayer.java`**: Client → Server. Requests changing the active clothing layer. Enforces the 10-layer cap server-side.
- **`PacketOpenClothingGUI.java`**: Client → Server. Requests opening the clothing inventory GUI via keybind.
- **`PacketUpdateClothingTransform.java`**: Client → Server. Sent by `GuiClothingTransform` when the player confirms new Scale/XYZ offset values for a specific layer+slot.

---

## 🖼️ GUI System (`com.example.examplemod.gui`)
- **`ClothingGuiHandler.java`**: `IGuiHandler` implementation returning the container or GUI screen based on side.
- **`ClothingContainer.java`**: Server-side inventory logic. Handles slot layout (clothing slots + player inventory + hotbar) and `Shift-Click` transfer logic. `setCurrentLayer` clamps to 0–9.
- **`ClothingGui.java`**: Client-side screen. Renders the GUI texture, the player model preview, and layer control buttons (+ / −) positioned **above** the player preview. Right-clicking any of the 8 clothing slots opens `GuiClothingTransform`. Remembers the last open layer across GUI sessions via a static field.
- **`ClothingInventorySlotHandler.java`**: Custom `Slot` implementation linking the GUI to the capability, handling valid armor checks and empty-slot background textures.
- **`GuiClothingTransform.java`**: Sub-screen opened by right-clicking a clothing slot. Presents four text fields (Scale, X/Y/Z offset). On "Apply" sends `PacketUpdateClothingTransform`; on "Cancel" returns to `ClothingGui`.

---

## 🎨 Rendering Engine (`com.example.examplemod.render`)
- **`ClothingRenderLayer.java`**: **CRITICAL FILE.** Implements `LayerRenderer<AbstractClientPlayer>`. Handles per-limb visibility mapping (`ModelBiped.showModel` flags), animation state synchronization, and texture binding to prevent Z-fighting and render clothes directly on limbs.
- **`DynamXHelper.java`**: Handles rendering compatibility with DynamX OBJ models. Uses reflection to classify custom `ModelRenderer` fields (e.g., matching "arm", "leg") and toggles visibility based on the clothing slot.
- **`ClothingRenderHelper.java`**: Client-side helper utility.

---

## 🛠️ Core & Events (`com.example.examplemod.core` & `client`)
- **`core/CommonEventHandler.java`**: Server-side events. Handles data persistence (`PlayerEvent.Clone`) and network syncing on `PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent`, and `StartTracking`.
- **`core/ModConfig.java`**: Forge configuration setup (currently holds debug rendering flags).
- **`client/ClientEventHandler.java`**: Injects `ClothingRenderLayer` into all player renderers on `RenderPlayerEvent.Post`.
- **`client/KeybindHandler.java`**: Registers the 'K' key to open the clothing GUI.

---

## 🧬 Mixins (`com.example.examplemod.mixin`)
- **`MixinLayerArmorBase.java` / `MixinRenderPlayer.java`**: Placeholders for potential deep-level render injections if standard Forge `LayerRenderer` proves insufficient in the future.

---

## 📦 Resources & Documentation
- **`src/main/resources/assets/examplemod/`**: Contains textures (`textures/gui/clothing_gui.png`) and localization files (`lang/en_us.lang`, `lang/de_de.lang`).
- **`src/main/resources/mcmod.info`**: Mod metadata.
- **`PROJECT_SUMMARY.txt`**: High-level overview of implemented features and mod status.
- **`RENDERING_BUG_FIX.md`**: Documentation explaining how the DynamX rendering conflict was resolved without breaking vanilla armor rendering.
- **`MAP_CHANGES.md`**: Documentation of map-view modifications (isometric view, block textures).