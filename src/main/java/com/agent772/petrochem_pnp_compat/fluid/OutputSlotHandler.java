package com.agent772.petrochem_pnp_compat.fluid;

import com.simibubi.create.foundation.fluid.SmartFluidTank;

import java.util.function.IntSupplier;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class OutputSlotHandler implements IFluidHandler {

    private final SmartFluidTank tank;
    private final IntSupplier drainAllowance;

    public OutputSlotHandler(SmartFluidTank tank, IntSupplier drainAllowance) {
        this.tank = tank;
        this.drainAllowance = drainAllowance;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.tank.getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.tank.getTankCapacity(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return this.tank.isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        int allowed = drainAllowance.getAsInt();
        return tank.drain(resource.getAmount() <= allowed ? resource : resource.copyWithAmount(allowed), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(Math.min(maxDrain, drainAllowance.getAsInt()), action);
    }
}
