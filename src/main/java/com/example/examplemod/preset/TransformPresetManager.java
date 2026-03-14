package com.example.examplemod.preset;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side manager for transform presets.
 * Presets are stored in <game_directory>/clothing_presets.json and shared across all players.
 * Deletion requires OP permission (enforced by the packet handler, not here).
 */
public class TransformPresetManager {

    public static class Preset {
        public final String name;
        public final float scaleX, scaleY, scaleZ;
        public final float offsetX, offsetY, offsetZ;

        public Preset(String name, float sx, float sy, float sz, float ox, float oy, float oz) {
            this.name    = name;
            this.scaleX  = sx; this.scaleY = sy; this.scaleZ = sz;
            this.offsetX = ox; this.offsetY = oy; this.offsetZ = oz;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getFile(MinecraftServer server) {
        return new File(server.getDataDirectory(), "clothing_presets.json");
    }

    /** Load all presets from disk. Returns an empty list on error or if the file doesn't exist. */
    public static List<Preset> load(MinecraftServer server) {
        File file = getFile(server);
        if (!file.exists()) return new ArrayList<>();
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr == null) return new ArrayList<>();
            List<Preset> list = new ArrayList<>();
            for (JsonElement el : arr) {
                try {
                    JsonObject obj = el.getAsJsonObject();
                    list.add(new Preset(
                            obj.get("name").getAsString(),
                            obj.get("scaleX").getAsFloat(),
                            obj.get("scaleY").getAsFloat(),
                            obj.get("scaleZ").getAsFloat(),
                            obj.get("offsetX").getAsFloat(),
                            obj.get("offsetY").getAsFloat(),
                            obj.get("offsetZ").getAsFloat()
                    ));
                } catch (Exception ignored) {}
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(MinecraftServer server, List<Preset> presets) {
        File file = getFile(server);
        JsonArray arr = new JsonArray();
        for (Preset p : presets) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name",    p.name);
            obj.addProperty("scaleX",  p.scaleX);
            obj.addProperty("scaleY",  p.scaleY);
            obj.addProperty("scaleZ",  p.scaleZ);
            obj.addProperty("offsetX", p.offsetX);
            obj.addProperty("offsetY", p.offsetY);
            obj.addProperty("offsetZ", p.offsetZ);
            arr.add(obj);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(arr, w);
        } catch (Exception ignored) {}
    }

    /**
     * Add or overwrite a preset by name (case-insensitive match for overwrite,
     * but keeps original casing of the new name).
     */
    public static void addOrUpdate(MinecraftServer server, String name,
                                   float sx, float sy, float sz,
                                   float ox, float oy, float oz) {
        List<Preset> list = load(server);
        list.removeIf(p -> p.name.equalsIgnoreCase(name));
        list.add(new Preset(name, sx, sy, sz, ox, oy, oz));
        save(server, list);
    }

    /**
     * Delete a preset by name (case-insensitive). Returns true if something was removed.
     */
    public static boolean delete(MinecraftServer server, String name) {
        List<Preset> list = load(server);
        int before = list.size();
        list.removeIf(p -> p.name.equalsIgnoreCase(name));
        if (list.size() < before) {
            save(server, list);
            return true;
        }
        return false;
    }
}
