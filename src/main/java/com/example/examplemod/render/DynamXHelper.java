package com.example.examplemod.render;

import com.example.examplemod.capability.ClothingInventorySlot;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
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
    // -------------------------------------------------------------------------

    private static final String MODEL_OBJ_ARMOR_CLASS = "fr.dynamx.client.renders.model.ModelObjArmor";

    // Cached reflected fields for ModelObjArmor
    private static boolean fieldsResolved = false;
    private static boolean fieldsAvailable = false;
    private static Field fieldBody = null;
    private static Field fieldArms = null;
    private static Field fieldLegs = null;

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
        } catch (Exception e) {
            fieldsAvailable = false;
        }
    }

    /**
     * If the model is a DynamX {@code ModelObjArmor}, hides the ArmorRenderer
     * parts that do not belong to {@code slot}, then calls {@code model.render()}
     * (the scene-graph path), then restores visibility.
     *
     * @return true  — model was handled, caller must NOT call model.render() again
     *         false — not a DynamX armor model, caller must call model.render()
     */
    public static boolean renderDynamXArmorPart(
            ModelBiped model, ClothingInventorySlot slot, float scale,
            Entity player, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {

        if (model == null) return false;
        if (!model.getClass().getName().equals(MODEL_OBJ_ARMOR_CLASS)) return false;

        resolveFields(model.getClass());
        if (!fieldsAvailable) return false;

        // Slots whose vanilla activePart already renders the right geometry — no hiding needed
        if (slot == ClothingInventorySlot.CHEST
                || slot == ClothingInventorySlot.HEAD
                || slot == ClothingInventorySlot.RIGHT_FOOT
                || slot == ClothingInventorySlot.LEFT_FOOT) {
            return false; // let model.render() handle normally
        }

        try {
            ModelRenderer body    = safeGet(fieldBody, model);
            Object        armsArr = fieldArms.get(model);
            Object        legsArr = fieldLegs.get(model);

            ModelRenderer leftArm  = getElement(armsArr, 0);
            ModelRenderer rightArm = getElement(armsArr, 1);
            ModelRenderer leftLeg  = getElement(legsArr, 0);
            ModelRenderer rightLeg = getElement(legsArr, 1);

            // Save original showModel states
            boolean savedBody     = body     != null && body.showModel;
            boolean savedLeftArm  = leftArm  != null && leftArm.showModel;
            boolean savedRightArm = rightArm != null && rightArm.showModel;
            boolean savedLeftLeg  = leftLeg  != null && leftLeg.showModel;
            boolean savedRightLeg = rightLeg != null && rightLeg.showModel;

            // Hide everything by default, then show only what belongs to this slot
            if (body     != null) body.showModel     = false;
            if (leftArm  != null) leftArm.showModel  = false;
            if (rightArm != null) rightArm.showModel = false;
            if (leftLeg  != null) leftLeg.showModel  = false;
            if (rightLeg != null) rightLeg.showModel = false;

            switch (slot) {
                case RIGHT_ARM:
                    if (rightArm != null) rightArm.showModel = true;
                    break;
                case LEFT_ARM:
                    if (leftArm != null) leftArm.showModel = true;
                    break;
                case RIGHT_LEG:
                    if (rightLeg != null) rightLeg.showModel = true;
                    break;
                case LEFT_LEG:
                    if (leftLeg != null) leftLeg.showModel = true;
                    break;
                default:
                    break;
            }

            model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

            // Restore
            if (body     != null) body.showModel     = savedBody;
            if (leftArm  != null) leftArm.showModel  = savedLeftArm;
            if (rightArm != null) rightArm.showModel = savedRightArm;
            if (leftLeg  != null) leftLeg.showModel  = savedLeftLeg;
            if (rightLeg != null) rightLeg.showModel = savedRightLeg;

            return true;
        } catch (Exception e) {
            return false;
        }
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
