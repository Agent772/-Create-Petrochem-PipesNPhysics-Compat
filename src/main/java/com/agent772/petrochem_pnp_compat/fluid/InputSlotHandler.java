package com.agent772.petrochem_pnp_compat.fluid;

import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class InputSlotHandler implements IFluidHandler {

    private final SmartFluidTank tank;

    public InputSlotHandler(SmartFluidTank tank) {
        this.tank = tank;
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
        return tank.fill(resource, action);
    }

    // Mirrors Petrochem's inputTank.forbidExtraction(): pipes may only push into the tower.
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
