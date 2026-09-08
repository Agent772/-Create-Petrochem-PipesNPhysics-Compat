package com.agent772.petrochem_pnp_compat.mixin;

import com.agent772.petrochem_pnp_compat.fluid.InputSlotHandler;
import com.agent772.petrochem_pnp_compat.fluid.SidedFluidAccess;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlock;
import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlockEntity;

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
    public SmartFluidTankBehaviour inputTank;

    @Shadow
    public IFluidHandler fluidCapability;

    @Unique
    private IFluidHandler petrochemPnpCompat$positiveSideInput;

    @Unique
    private IFluidHandler petrochemPnpCompat$negativeSideInput;

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
        // A null context is the tower's own lookup in getSteam()/tick() and the goggle tooltip, which
        // need to see every tank. Only the two exposed faces get a narrowed handler.
        if (side == null) {
            return fluidCapability;
        }
        BlockEntity be = (BlockEntity) (Object) this;
        if (side.getAxis() != DistillationControllerBlock.getAxis(be.getBlockState())) {
            return null;
        }
        if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            if (petrochemPnpCompat$positiveSideInput == null) {
                petrochemPnpCompat$positiveSideInput = petrochemPnpCompat$inputSlot(0);
            }
            return petrochemPnpCompat$positiveSideInput;
        }
        if (petrochemPnpCompat$negativeSideInput == null) {
            petrochemPnpCompat$negativeSideInput = petrochemPnpCompat$inputSlot(1);
        }
        return petrochemPnpCompat$negativeSideInput;
    }

    @Unique
    private IFluidHandler petrochemPnpCompat$inputSlot(int slot) {
        return new InputSlotHandler(((TankSegmentAccessor) inputTank.getTanks()[slot]).getTank());
    }
}
