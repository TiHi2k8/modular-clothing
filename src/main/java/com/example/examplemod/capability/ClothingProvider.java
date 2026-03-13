package com.example.examplemod.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClothingProvider implements ICapabilitySerializable<NBTTagCompound> {
    @CapabilityInject(IClothingInventory.class)
    public static final Capability<IClothingInventory> CLOTHING_CAPABILITY = null;

    private final IClothingInventory instance = new ClothingInventory();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CLOTHING_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CLOTHING_CAPABILITY ? CLOTHING_CAPABILITY.cast(instance) : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        instance.deserializeNBT(nbt);
    }
}
