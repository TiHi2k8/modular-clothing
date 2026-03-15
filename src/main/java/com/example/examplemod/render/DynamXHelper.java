package com.example.examplemod.render;

import com.example.examplemod.capability.ClothingInventorySlot;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class DynamXHelper {

    // -------------------------------------------------------------------------
    // DynamX ModelObjArmor selective rendering
    //
    // ModelObjArmor.render() ignores standard ModelBiped showModel flags.
    // It renders all geometry for a given EntityEquipmentSlot (e.g. CHEST →
    // body + both arms). RIGHT_ARM and LEFT_ARM both map to vanillaSlot=CHEST,
    // so naively both would render body+both arms.
    //
    // Fix: temporarily set showModel=false on the unwanted ArmorRenderer parts
    // (which DO respect showModel in their own render() override), then call
    // model.render() normally. This keeps the scene-graph / Matrix4f code path
    // intact so animation from setModelAttributes(mainModel) is preserved.
    //
    // DO NOT use renderRightArm()/renderLeftArm() — those call
    // setModelAttributes(this) internally which resets to the default pose,
    // losing the player's live walking/swing animation.
    //
    // Mirroring: from the camera's perspective (looking at the player):
    //   arms[0]  = bipedLeftArm  (player's left)  = appears on SCREEN RIGHT
    //   arms[1]  = bipedRightArm (player's right)  = appears on SCREEN LEFT
    //   legs[0]  = bipedLeftLeg  (player's left)   = appears on SCREEN RIGHT
    //   legs[1]  = bipedRightLeg (player's right)  = appears on SCREEN LEFT
    //   foot[0]  = left foot (mirror=true)          = appears on SCREEN RIGHT
    //   foot[1]  = right foot                       = appears on SCREEN LEFT
    // So RIGHT_ARM/RIGHT_LEG/RIGHT_FOOT slots → [0] index (screen right),
    //    LEFT_ARM/LEFT_LEG/LEFT_FOOT slots   → [1] index (screen left).
    // -------------------------------------------------------------------------

    private static final String MODEL_OBJ_ARMOR_CLASS = "fr.dynamx.client.renders.model.ModelObjArmor";

    // Cached reflected fields for ModelObjArmor
    private static boolean fieldsResolved = false;
    private static boolean fieldsAvailable = false;
    private static Field fieldBody = null;
    private static Field fieldArms = null;
    private static Field fieldLegs = null;
    private static Field fieldFoot = null;

    private static void resolveFields(Class<?> clazz) {
        if (fieldsResolved) return;
        fieldsResolved = true;
        try {
            fieldBody = clazz.getDeclaredField("body");
            fieldBody.setAccessible(true);
            fieldArms = clazz.getDeclaredField("arms");
            fieldArms.setAccessible(true);
            fieldLegs = clazz.getDeclaredField("legs");
            fieldLegs.setAccessible(true);
            fieldsAvailable = true;
            // foot is optional — present only when the armor has foot parts
            try {
                fieldFoot = clazz.getDeclaredField("foot");
                fieldFoot.setAccessible(true);
            } catch (Exception ignored) { /* foot field absent */ }
        } catch (Exception e) {
            fieldsAvailable = false;
        }
    }

    /** Returns true if this model is a DynamX ModelObjArmor (manages its own textures). */
    public static boolean isDynamXModel(ModelBiped model) {
        return model != null && model.getClass().getName().equals(MODEL_OBJ_ARMOR_CLASS);
    }

    /**
     * If the model is a DynamX {@code ModelObjArmor}, hides the ArmorRenderer
     * parts that do not belong to {@code slot}, then calls {@code model.render()}
     * (the scene-graph path), then restores visibility.
     *
     * @param chestShowArms when slot==CHEST and true, also render arms alongside body
     * @param pantsLegsMode when slot==RIGHT_LEG and true, also render both legs + body
     * @param shoesFeetMode when slot==RIGHT_FOOT and true, also render both feet
     * @return true  — model was handled, caller must NOT call model.render() again
     *         false — not a DynamX armor model, caller must call model.render()
     */
    public static boolean renderDynamXArmorPart(
            ModelBiped model, ModelBiped defaultModel,
            ClothingInventorySlot slot,
            boolean chestShowArms, boolean pantsLegsMode, boolean shoesFeetMode,
            float scale, float[] transform,
            Entity player, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {

        if (model == null) return false;
        if (!model.getClass().getName().equals(MODEL_OBJ_ARMOR_CLASS)) return false;

        resolveFields(model.getClass());
        if (!fieldsAvailable) return false;

        // HEAD activePart already renders only the head part — no intervention needed
        if (slot == ClothingInventorySlot.HEAD) {
            return false;
        }

        // Ensure animations are updated since we don't call model.render()
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);

        try {
            // Replicate ModelBiped.render() sneaking offset
            if (player.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            ModelRenderer body = safeGet(fieldBody, model);

            Object armsArr = fieldArms.get(model);
            // arms[0] = bipedLeftArm  (screen RIGHT) → RIGHT_ARM slot
            // arms[1] = bipedRightArm (screen LEFT)  → LEFT_ARM slot
            ModelRenderer rightArm = getElement(armsArr, 0); // screen right
            ModelRenderer leftArm  = getElement(armsArr, 1); // screen left

            Object legsArr = fieldLegs.get(model);
            // legs[0] = bipedLeftLeg  (screen RIGHT) → RIGHT_LEG slot
            // legs[1] = bipedRightLeg (screen LEFT)  → LEFT_LEG slot
            ModelRenderer rightLeg = getElement(legsArr, 0); // screen right
            ModelRenderer leftLeg  = getElement(legsArr, 1); // screen left

            // foot is optional
            Object footArr = null;
            if (fieldFoot != null) {
                try { footArr = fieldFoot.get(model); } catch (Exception ignored) {}
            }
            // foot[0] = left foot (mirror=true, screen RIGHT) → RIGHT_FOOT slot
            // foot[1] = right foot (screen LEFT)              → LEFT_FOOT slot
            ModelRenderer rightFoot = getElement(footArr, 0); // screen right
            ModelRenderer leftFoot  = getElement(footArr, 1); // screen left

            // Adjust for sneaking: Vanilla renders the whole model 0.2F lower when sneaking.
            // We pass this offset to renderPart to adjust the pivot point Y.
            float sneakY = player.isSneaking() ? 0.2F : 0.0F;
            float leftArmSneak = player.isSneaking() ? -0.2F : 0.0F;
            float rightArmSneak = player.isSneaking() ? -0.2F : 0.0F;
            float bodySneak = player.isSneaking() ? -0.4F : 0.0F;
            float leftLegSneak = player.isSneaking() ? -0.21F : 0.0F;
            float leftBothLegSneak = player.isSneaking() ? -0.011F : 0.0F;
            float rightLegSneak = player.isSneaking() ? -0.007F : 0.0F;
            float leftFootSneak = player.isSneaking() ? -1.0F : 0.0F;
            float rightFootSneak = player.isSneaking() ? -1.2F : 0.0F;

            switch (slot) {
                case CHEST:
                    if (chestShowArms) {
                        renderPart(rightArm, defaultModel.bipedLeftArm, transform, scale, 0.02F, 0.01F, 0.0F, 0.0F);
                        renderPart(leftArm,  defaultModel.bipedRightArm, transform, scale, -0.02F, 0.01F, 0.0F, 0.0F);
                        renderPart(body, defaultModel.bipedBody, transform, scale, 0.0F, 0.0F, 0.0F, 0.0F);
                    } else {
                        renderPart(body, defaultModel.bipedBody, transform, scale, 0.0F, 0.0F, 0.0F, bodySneak);
                    }
                    break;
                case RIGHT_ARM:
                    renderPart(rightArm, defaultModel.bipedLeftArm, transform, scale, 0.02F, 0.01F, 0.0F, 0.0F);
                    break;
                case LEFT_ARM:
                    renderPart(leftArm, defaultModel.bipedRightArm, transform, scale, -0.02F, 0.01F, 0.0F, leftArmSneak);
                    break;
                case RIGHT_LEG:
                    renderPart(rightLeg, defaultModel.bipedLeftLeg, transform, scale, 0.0F, 0.0475F, 0.0F, rightLegSneak);
                    if (pantsLegsMode) {
                        renderPart(leftLeg, defaultModel.bipedRightLeg, transform, scale, 0.0F, 0.05F, 0.0F, leftBothLegSneak);
                    }
                    break;
                case LEFT_LEG:
                    if (!pantsLegsMode) {
                        renderPart(leftLeg, defaultModel.bipedRightLeg, transform, scale, 0.0F, 0.0475F, 0.0F, leftLegSneak);
                    }
                    break;
                case RIGHT_FOOT:
                    renderPart(rightFoot, defaultModel.bipedLeftLeg, transform, scale, 0.005F, 0.045F, 0.0F, leftFootSneak);
                    if (shoesFeetMode) {
                        renderPart(leftFoot, defaultModel.bipedRightLeg, transform, scale, -0.005F, 0.09F, 0.0F, rightFootSneak);
                    }
                    break;
                case LEFT_FOOT:
                     if (!shoesFeetMode) {
                        renderPart(leftFoot, defaultModel.bipedRightLeg, transform, scale, -0.005F, 0.045F, 0.0F, rightFootSneak);
                    }
                    break;
                default:
                    break;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void renderPart(ModelRenderer part, ModelRenderer targetPart, float[] transform, 
                                   float scale, float adX, float adY, float adZ, float sneakOffset) {
        if (part == null || !part.showModel || part.isHidden) return;
        
        // Transform: {scaleX, scaleY, scaleZ, offX, offY, offZ}
        float scaleX = transform[0];
        float scaleY = transform[1];
        float scaleZ = transform[2];
        float ox     = transform[3] + adX;
        float oy     = transform[4] + adY;
        float oz     = transform[5] + adZ;

        // Target Pivot (Standard ModelBiped limb pivot)
        float tx = (targetPart != null ? targetPart.rotationPointX : part.rotationPointX);
        float ty = (targetPart != null ? targetPart.rotationPointY : part.rotationPointY);
        float tz = (targetPart != null ? targetPart.rotationPointZ : part.rotationPointZ);

        GlStateManager.pushMatrix();

        // 1. Move to the Pivot Point + Sneak Offset
        //    (ty * scale) converts model units to world units. sneakOffset is in world units (0.2F).
        GlStateManager.translate(tx * scale, (ty * scale) + sneakOffset, tz * scale);

        // 2. Apply Rotation (Standard ModelRenderer angles)
        float rx = (targetPart != null ? targetPart.rotateAngleX : part.rotateAngleX);
        float ry = (targetPart != null ? targetPart.rotateAngleY : part.rotateAngleY);
        float rz = (targetPart != null ? targetPart.rotateAngleZ : part.rotateAngleZ);

        if (rz != 0.0F) GlStateManager.rotate(rz * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
        if (ry != 0.0F) GlStateManager.rotate(ry * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
        if (rx != 0.0F) GlStateManager.rotate(rx * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);

        // 3. Apply User Transforms
        if (ox != 0.0f || oy != 0.0f || oz != 0.0f) {
             GlStateManager.translate(ox, oy, oz);
        }
        if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
            GlStateManager.scale(scaleX, scaleY, scaleZ);
        }

        // 4. Move Back to the Geometry Origin
        float px = part.rotationPointX;
        float py = part.rotationPointY;
        float pz = part.rotationPointZ;

        if (px != 0.0f || py != 0.0f || pz != 0.0f) {
             GlStateManager.translate(-px * scale, -py * scale, -pz * scale);
        }

        // 5. Render Part
        // Temporarily zero transforms
        float savedX = part.rotationPointX;
        float savedY = part.rotationPointY;
        float savedZ = part.rotationPointZ;
        float savedRotX = part.rotateAngleX;
        float savedRotY = part.rotateAngleY;
        float savedRotZ = part.rotateAngleZ;

        part.rotationPointX = 0;
        part.rotationPointY = 0;
        part.rotationPointZ = 0;
        part.rotateAngleX = 0;
        part.rotateAngleY = 0;
        part.rotateAngleZ = 0;
        
        part.render(scale);

        // Restore
        part.rotationPointX = savedX;
        part.rotationPointY = savedY;
        part.rotationPointZ = savedZ;
        part.rotateAngleX = savedRotX;
        part.rotateAngleY = savedRotY;
        part.rotateAngleZ = savedRotZ;

        GlStateManager.popMatrix();
    }

    private static ModelRenderer safeGet(Field f, Object obj) {
        try { return (ModelRenderer) f.get(obj); } catch (Exception e) { return null; }
    }

    private static ModelRenderer getElement(Object array, int index) {
        if (array == null) return null;
        try {
            Object elem = Array.get(array, index);
            return elem instanceof ModelRenderer ? (ModelRenderer) elem : null;
        } catch (Exception e) { return null; }
    }

    // -------------------------------------------------------------------------
    // Generic custom model support (showModel flag toggling via reflection)
    // Used for non-DynamX custom armor models that DO respect showModel flags.
    // -------------------------------------------------------------------------

    private static boolean isPartForSlot(String fieldName, ClothingInventorySlot slot) {
        String lower = fieldName.toLowerCase();
        boolean isArm = (lower.contains("arm") || lower.contains("hand") || lower.contains("sleeve") || lower.contains("shoulder"))
                        && !lower.contains("leg") && !lower.contains("foot") && !lower.contains("boot") && !lower.contains("shoe");
        boolean isFoot = lower.contains("foot") || lower.contains("boot") || lower.contains("shoe") || lower.contains("sock");
        boolean isLegProper = (lower.contains("leg") || lower.contains("pants") || lower.contains("thigh") || lower.contains("shin") || lower.contains("knee"))
                              && !isFoot;
        boolean isRight = lower.contains("right");
        boolean isLeft  = lower.contains("left");
        switch (slot) {
            case HEAD:
                return lower.contains("head") || lower.contains("helmet") || lower.contains("hat")
                    || lower.contains("mask") || lower.contains("face") || lower.contains("hood");
            case CHEST: {
                boolean isBody = lower.contains("body") || lower.contains("chest") || lower.contains("torso")
                              || lower.contains("jacket") || lower.contains("shirt") || lower.contains("vest");
                return (isBody || isArm) && !isLegProper && !isFoot;
            }
            case RIGHT_ARM:  return isArm && isRight;
            case LEFT_ARM:   return isArm && isLeft;
            case RIGHT_LEG:  return isLegProper && isRight;
            case LEFT_LEG:   return isLegProper && isLeft;
            case RIGHT_FOOT: return isFoot && isRight;
            case LEFT_FOOT:  return isFoot && isLeft;
            default:         return false;
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
        if (model.getClass() == ModelBiped.class
                || model.getClass().getName().startsWith("net.minecraft.")
                || model.getClass().getName().equals(MODEL_OBJ_ARMOR_CLASS)) {
            return originalState;
        }
        try {
            Class<?> clazz = model.getClass();
            while (clazz != null && clazz != ModelBase.class && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (ModelRenderer.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        ModelRenderer renderer = (ModelRenderer) field.get(model);
                        if (renderer != null && isKnownPart(field.getName())) {
                            originalState.put(renderer, renderer.showModel);
                            renderer.showModel = isPartForSlot(field.getName(), slot);
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) { /* ignore */ }
        return originalState;
    }

    public static void restoreDynamXModel(Map<ModelRenderer, Boolean> state) {
        if (state == null) return;
        try {
            for (Map.Entry<ModelRenderer, Boolean> entry : state.entrySet()) {
                if (entry.getKey() != null) entry.getKey().showModel = entry.getValue();
            }
        } catch (Exception e) { /* ignore */ }
    }
}
