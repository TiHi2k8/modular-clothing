package com.example.examplemod.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provider that attaches the clothing capability to players.
 * Implements ICapabilitySerializable so it auto-saves/loads with the player.
 */
public class ClothingCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(IClothingInventory.class)
    public static Capability<IClothingInventory> CLOTHING_CAP = null;

    private final ClothingInventory inventory = new ClothingInventory();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CLOTHING_CAP;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CLOTHING_CAP) {
            return CLOTHING_CAP.cast(inventory);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        inventory.deserializeNBT(nbt);
    }
}

