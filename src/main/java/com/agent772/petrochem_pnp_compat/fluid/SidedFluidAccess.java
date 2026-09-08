package com.agent772.petrochem_pnp_compat.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface SidedFluidAccess {

    IFluidHandler petrochemPnpCompat$sidedFluidHandler(Direction side);
}
