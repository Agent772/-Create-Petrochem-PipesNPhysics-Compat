package com.agent772.petrochem_pnp_compat.mixin;

import com.agent772.petrochem_pnp_compat.fluid.InputSlotHandler;
import com.agent772.petrochem_pnp_compat.fluid.OutputSlotHandler;
import com.agent772.petrochem_pnp_compat.fluid.SidedFluidAccess;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlock;
import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlockEntity;
import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlockEntity.DistilMode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DistillationControllerBlockEntity.class)
public abstract class DistillationControllerBlockEntityMixin implements SidedFluidAccess {

    @Shadow
    public ScrollOptionBehaviour<DistilMode> distilMode;

    @Shadow
    public SmartFluidTankBehaviour inputTank;

    @Shadow
    public SmartFluidTankBehaviour outputTank;

    @Shadow
    public IFluidHandler fluidCapability;

    @Unique
    private IFluidHandler petrochemPnpCompat$positiveInputHandler;

    @Unique
    private IFluidHandler petrochemPnpCompat$positiveVacuumHandler;

    @Unique
    private IFluidHandler petrochemPnpCompat$negativeFlashHandler;

    @Unique
    private IFluidHandler petrochemPnpCompat$negativeAtmosphericHandler;

    @Unique
    private IFluidHandler petrochemPnpCompat$negativeVacuumHandler;

    @SuppressWarnings("unchecked")
    @Redirect(
            method = "registerCapabilities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent;registerBlockEntity(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/neoforged/neoforge/capabilities/ICapabilityProvider;)V"
            ),
            remap = false
    )
    private static void petrochemPnpCompat$registerPerSideCapability(RegisterCapabilitiesEvent event,
            BlockCapability<?, ?> capability, BlockEntityType<?> type, ICapabilityProvider<?, ?, ?> provider) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                (BlockEntityType<DistillationControllerBlockEntity>) type,
                (be, side) -> ((SidedFluidAccess) be).petrochemPnpCompat$sidedFluidHandler(side));
    }

    @Override
    public IFluidHandler petrochemPnpCompat$sidedFluidHandler(Direction side) {
        if (side == null) {
            return fluidCapability;
        }
        BlockEntity be = (BlockEntity) (Object) this;
        if (side.getAxis() != DistillationControllerBlock.getAxis(be.getBlockState())) {
            return null;
        }
        DistilMode mode = (DistilMode) distilMode.get();
        if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            if (mode == DistilMode.DISTIL_VACUUM) {
                if (petrochemPnpCompat$positiveVacuumHandler == null) {
                    petrochemPnpCompat$positiveVacuumHandler = new OutputSlotHandler(
                            ((TankSegmentAccessor) outputTank.getTanks()[0]).getTank());
                }
                return petrochemPnpCompat$positiveVacuumHandler;
            }
            if (petrochemPnpCompat$positiveInputHandler == null) {
                petrochemPnpCompat$positiveInputHandler = new InputSlotHandler(
                        ((TankSegmentAccessor) inputTank.getTanks()[0]).getTank());
            }
            return petrochemPnpCompat$positiveInputHandler;
        }
        return switch (mode) {
            case DISTIL_FLASH -> {
                if (petrochemPnpCompat$negativeFlashHandler == null) {
                    petrochemPnpCompat$negativeFlashHandler = new InputSlotHandler(
                            ((TankSegmentAccessor) inputTank.getTanks()[1]).getTank());
                }
                yield petrochemPnpCompat$negativeFlashHandler;
            }
            case DISTIL_ATMOSPHERIC -> {
                if (petrochemPnpCompat$negativeAtmosphericHandler == null) {
                    petrochemPnpCompat$negativeAtmosphericHandler = new InputSlotHandler(
                            ((TankSegmentAccessor) inputTank.getTanks()[0]).getTank());
                }
                yield petrochemPnpCompat$negativeAtmosphericHandler;
            }
            case DISTIL_VACUUM -> {
                if (petrochemPnpCompat$negativeVacuumHandler == null) {
                    petrochemPnpCompat$negativeVacuumHandler = new InputSlotHandler(
                            ((TankSegmentAccessor) inputTank.getTanks()[0]).getTank());
                }
                yield petrochemPnpCompat$negativeVacuumHandler;
            }
        };
    }
}
