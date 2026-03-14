# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## CRITICAL: Always check AI_MAP.md first

**Before opening any Java file**, read [AI_MAP.md](AI_MAP.md). It lists every key class with a short description so you can find the right file immediately without browsing blindly. If you create, rename, or delete any class or file, update `AI_MAP.md` immediately to keep it accurate.

---

## Project Overview

This is a **Minecraft Forge 1.12.2 mod** (Java 8) implementing a per-limb modular clothing system. The built JAR lands at `build/libs/examplemod-1.0.jar`.

- Mod ID / package root: `com.example.examplemod`
- Forge version: `1.12.2-14.23.5.2847`
- MCP mappings: `snapshot_20171003`

---

## Build Commands

```bash
# Set up Forge workspace (first time only)
gradlew setupDecompWorkspace

# Build the mod JAR
gradlew build

# Run the Minecraft client in dev mode
gradlew runClient

# Run the Minecraft server in dev mode
gradlew runServer
```

Run these from the repo root on Windows. Output JAR: `build/libs/examplemod-1.0.jar`.

---

## Architecture

### Data Flow

1. **Capability** — Every `EntityPlayer` gets a `ClothingInventory` attached via `CapabilityHandler` on `AttachCapabilitiesEvent`. The inventory holds 6 slots (Head, Chest, Right/Left Arm, Right/Left Leg) across multiple layers.
2. **GUI** — The player presses **K** → `KeybindHandler` → `PacketOpenClothingGUI` (C→S) → server opens `ClothingContainer` → client shows `ClothingGui`. Layer change uses `PacketChangeClothingLayer`.
3. **Sync** — `PacketUpdateClothingSlot` (C→S) updates a slot; server responds with `PacketSyncClothingInventory` (S→C) to the player and all trackers. `CommonEventHandler` also triggers full syncs on login, respawn, and dimension change.
4. **Rendering** — `ClientEventHandler` injects `ClothingRenderLayer` into every player renderer. It maps clothing slots to `ModelBiped` limb visibility flags and copies animation state each frame so clothes move with the player. DynamX OBJ model compatibility is handled separately in `DynamXHelper` via reflection.

### Key Design Decisions

- **Server-authoritative**: slot writes are validated server-side before any sync is sent.
- **NBT persistence**: `ClothingInventory` serializes to NBT; `CommonEventHandler.PlayerCloneEvent` copies it on death/respawn.
- **DynamX compatibility**: `ClothingRenderLayer` wraps armor rendering in try/catch to gracefully skip non-standard models. `DynamXHelper` uses reflection to classify custom `ModelRenderer` fields by name pattern ("arm", "leg", etc.).
- **Mixin placeholders**: `mixin/` contains empty Mixin classes reserved for future deep render injections; they are **not** active.

---

## Rendering Notes (see also RENDERING_BUG_FIX.md)

`ClothingRenderLayer` is the most critical file. It:
- Sets `ModelBiped.showModel` flags to hide vanilla armor on limbs not covered by the clothing slot.
- Synchronizes `ModelBiped` rotation angles from the player renderer before each draw call.
- Manages GL state (push/pop matrix, blend) to prevent Z-fighting.
