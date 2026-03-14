package com.example.examplemod.render;

import com.example.examplemod.capability.ClothingInventorySlot;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class DynamXHelper {

    private static boolean isPartForSlot(String fieldName, ClothingInventorySlot slot) {
        String lower = fieldName.toLowerCase();

        // Arm: hand, sleeve, shoulder — but NOT leg/foot parts
        boolean isArm = (lower.contains("arm") || lower.contains("hand") || lower.contains("sleeve") || lower.contains("shoulder"))
                        && !lower.contains("leg") && !lower.contains("foot") && !lower.contains("boot") && !lower.contains("shoe");

        // Foot: only explicit foot/boot/shoe/sock names
        boolean isFoot = lower.contains("foot") || lower.contains("boot") || lower.contains("shoe") || lower.contains("sock");

        // Leg-proper: leg/pants/thigh/shin/knee — but NOT foot parts
        boolean isLegProper = (lower.contains("leg") || lower.contains("pants") || lower.contains("thigh") || lower.contains("shin") || lower.contains("knee"))
                              && !isFoot;

        boolean isRight = lower.contains("right");
        boolean isLeft  = lower.contains("left");

        switch (slot) {
            case HEAD:
                return lower.contains("head") || lower.contains("helmet") || lower.contains("hat")
                    || lower.contains("mask") || lower.contains("face") || lower.contains("hood");

            case CHEST:
                // Body + arms for shirts/jackets; exclude all leg/foot geometry
                boolean isBody = lower.contains("body") || lower.contains("chest") || lower.contains("torso")
                              || lower.contains("jacket") || lower.contains("shirt") || lower.contains("vest");
                return (isBody || isArm) && !isLegProper && !isFoot;

            case RIGHT_ARM:
                // Must be explicitly arm and on the right side; no leg/foot
                return isArm && isRight;

            case LEFT_ARM:
                return isArm && isLeft;

            case RIGHT_LEG:
                // Leg-proper geometry on the right — excludes boot/foot parts
                return isLegProper && isRight;

            case LEFT_LEG:
                return isLegProper && isLeft;

            case RIGHT_FOOT:
                // Foot/boot geometry on the right
                return isFoot && isRight;

            case LEFT_FOOT:
                return isFoot && isLeft;

            default:
                return false;
        }
    }

    private static boolean isKnownPart(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("head") || lower.contains("helmet") || lower.contains("hat") || lower.contains("mask") || lower.contains("hood")
            || lower.contains("body") || lower.contains("chest") || lower.contains("torso") || lower.contains("jacket") || lower.contains("shirt") || lower.contains("vest")
            || lower.contains("arm") || lower.contains("sleeve") || lower.contains("shoulder") || lower.contains("hand")
            || lower.contains("leg") || lower.contains("thigh") || lower.contains("shin") || lower.contains("knee") || lower.contains("pants")
            || lower.contains("foot") || lower.contains("feet") || lower.contains("boot") || lower.contains("shoe") || lower.contains("sock");
    }

    public static Map<ModelRenderer, Boolean> updateDynamXModel(ModelBiped model, ClothingInventorySlot slot) {
        Map<ModelRenderer, Boolean> originalState = new HashMap<>();
        if (model == null) return originalState;

        // Only process if it looks likely to be a custom model
        if (model.getClass() == ModelBiped.class || model.getClass().getName().startsWith("net.minecraft.")) {
           return originalState;
        }

        try {
            Class<?> clazz = model.getClass();
            while (clazz != null && clazz != ModelBase.class && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (ModelRenderer.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        ModelRenderer renderer = (ModelRenderer) field.get(model);
                        if (renderer != null) {
                            String fieldName = field.getName();
                            
                            // Check if this field name looks like ANY known part
                            if (isKnownPart(fieldName)) {
                                // Save original state
                                originalState.put(renderer, renderer.showModel);

                                // Determine if we should show it
                                renderer.showModel = isPartForSlot(fieldName, slot);
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }

        return originalState;
    }

    public static void restoreDynamXModel(Map<ModelRenderer, Boolean> state) {
        if (state == null) return;
        try {
            for (Map.Entry<ModelRenderer, Boolean> entry : state.entrySet()) {
                if (entry.getKey() != null) {
                    entry.getKey().showModel = entry.getValue();
                }
            }
        } catch (Exception e) {
            // Ignore errors during restore
        }
    }
}

