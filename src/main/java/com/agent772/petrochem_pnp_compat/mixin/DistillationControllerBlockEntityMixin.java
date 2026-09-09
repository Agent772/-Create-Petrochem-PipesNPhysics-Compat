package com.agent772.petrochem_pnp_compat.mixin;

import com.agent772.petrochem_pnp_compat.Config;
import com.agent772.petrochem_pnp_compat.PetrochemPnPCompat;
import com.agent772.petrochem_pnp_compat.fluid.InputSlotHandler;
import com.agent772.petrochem_pnp_compat.fluid.OutputSlotHandler;
import com.agent772.petrochem_pnp_compat.fluid.SidedFluidAccess;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;

import java.util.List;

import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.lang.Lang;

import de.devin.pipesnphysics.engine.graph.Graph;
import de.devin.pipesnphysics.engine.graph.GraphBuilder;
import de.devin.pipesnphysics.engine.graph.GraphCache;
import de.devin.pipesnphysics.engine.graph.Node;
import de.devin.pipesnphysics.engine.pump.Pumps;

import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlock;
import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlockEntity;
import io.github.hadron13.petrochem.blocks.distillation_tower.DistillationControllerBlockEntity.DistilMode;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Shadow
    public abstract int getAir();

    @Shadow
    public abstract boolean hasVacuum();

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

    @Unique
    private static final int PETROCHEM_PNP_COMPAT$SUCTION_RECHECK_TICKS = 20;

    @Unique
    private long petrochemPnpCompat$suctionCheckedAt = Long.MIN_VALUE;

    @Unique
    private boolean petrochemPnpCompat$hasSuction;

    @Unique
    private boolean petrochemPnpCompat$starved;

    @Unique
    private int petrochemPnpCompat$graphNodes;

    @Unique
    private int petrochemPnpCompat$graphPumps;

    @Unique
    private double petrochemPnpCompat$bestPumpRpm;

    @Unique
    private int petrochemPnpCompat$predictedAir = -1;

    @Unique
    private int petrochemPnpCompat$drainedMb;

    @Unique
    private int petrochemPnpCompat$sampledTicks;

    @Unique
    private long petrochemPnpCompat$lastSampleTick = Long.MIN_VALUE;

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
                            ((TankSegmentAccessor) outputTank.getTanks()[0]).getTank(),
                            () -> petrochemPnpCompat$hasSuction ? Integer.MAX_VALUE
                                    : Config.VACUUM_PASSIVE_DRAIN_MB.get());
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

    @Unique
    private static double petrochemPnpCompat$minPumpRpm() {
        return Math.max(Config.VACUUM_MIN_PUMP_RPM.get(), 0.01);
    }

    // Pipes n' Physics caps a tank's draw at the pipe lip unless a pump pulls, but it skips
    // that rule for gases, so a bare pipe emptied the tower at the full endpoint rate. This only
    // ever runs from the tick path (never from inside a drain call), so the graph scan cannot
    // re-enter Pipes n' Physics while it is solving the network this endpoint belongs to.
    @Unique
    private boolean petrochemPnpCompat$hasPumpSuction() {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        long now = level.getGameTime();
        // The initial stamp is Long.MIN_VALUE, so the recheck comparison must add to the stamp
        // rather than subtract from now, otherwise the first check underflows into "recent".
        if (now < petrochemPnpCompat$suctionCheckedAt + PETROCHEM_PNP_COMPAT$SUCTION_RECHECK_TICKS) {
            return petrochemPnpCompat$hasSuction;
        }
        petrochemPnpCompat$suctionCheckedAt = now;
        petrochemPnpCompat$hasSuction = petrochemPnpCompat$scanForPump(level, be, now);
        return petrochemPnpCompat$hasSuction;
    }

    @Unique
    private boolean petrochemPnpCompat$scanForPump(Level level, BlockEntity be, long now) {
        Direction outlet = Direction.get(Direction.AxisDirection.POSITIVE,
                DistillationControllerBlock.getAxis(be.getBlockState()));
        BlockPos pipePos = be.getBlockPos().relative(outlet);
        Graph graph = GraphCache.get(level, pipePos, now);
        if (graph == null) {
            graph = GraphBuilder.build(level, pipePos);
        }
        if (graph == null) {
            // No pipe at the outlet (mode switched to vacuum before the pipe was placed, or the
            // pipe was broken mid-run). Report no suction rather than dereferencing a null graph.
            petrochemPnpCompat$graphNodes = 0;
            petrochemPnpCompat$graphPumps = 0;
            petrochemPnpCompat$bestPumpRpm = 0.0;
            return false;
        }
        double minRpm = petrochemPnpCompat$minPumpRpm();
        double best = 0.0;
        int pumps = 0;
        // Any pump on the graph counts, wherever it sits and whichever way it faces.
        for (Node node : graph.nodes()) {
            if (!node.isPump()) {
                continue;
            }
            pumps++;
            best = Math.max(best, Pumps.strength(level, node.pos()));
        }
        petrochemPnpCompat$graphNodes = graph.nodes().size();
        petrochemPnpCompat$graphPumps = pumps;
        petrochemPnpCompat$bestPumpRpm = best;
        return best >= minRpm;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void petrochemPnpCompat$writeSuction(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) {
            tag.putInt("PetrochemPnpCompatPumps", petrochemPnpCompat$graphPumps);
            tag.putDouble("PetrochemPnpCompatPumpRpm", petrochemPnpCompat$bestPumpRpm);
            tag.putBoolean("PetrochemPnpCompatStarved", petrochemPnpCompat$starved);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void petrochemPnpCompat$readSuction(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) {
            petrochemPnpCompat$graphPumps = tag.getInt("PetrochemPnpCompatPumps");
            petrochemPnpCompat$bestPumpRpm = tag.getDouble("PetrochemPnpCompatPumpRpm");
            petrochemPnpCompat$starved = tag.getBoolean("PetrochemPnpCompatStarved");
        }
    }

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void petrochemPnpCompat$explainMissingSuction(List<Component> tooltip, boolean isPlayerSneaking,
            CallbackInfoReturnable<Boolean> cir) {
        if ((DistilMode) distilMode.get() != DistilMode.DISTIL_VACUUM || hasVacuum()
                || !petrochemPnpCompat$starved) {
            return;
        }
        double minRpm = petrochemPnpCompat$minPumpRpm();
        MutableComponent reason;
        if (petrochemPnpCompat$graphPumps == 0) {
            reason = Lang.builder(PetrochemPnPCompat.MODID).translate("hint.vacuum_suction.no_pump").component();
        } else if (petrochemPnpCompat$bestPumpRpm < minRpm) {
            reason = Lang.builder(PetrochemPnPCompat.MODID)
                    .translate("hint.vacuum_suction.slow_pump",
                            Math.max(1L, Math.round(petrochemPnpCompat$bestPumpRpm)),
                            Math.max(1, (int) Math.ceil(minRpm)))
                    .component();
        } else {
            reason = Lang.builder(PetrochemPnPCompat.MODID).translate("hint.vacuum_suction.weak_pump").component();
        }
        Lang.builder(PetrochemPnPCompat.MODID).text("").forGoggles(tooltip);
        Lang.builder(PetrochemPnPCompat.MODID).translate("hint.vacuum_suction.title")
                .style(ChatFormatting.GOLD).forGoggles(tooltip);
        for (Component line : TooltipHelper.cutTextComponent(reason, FontHelper.Palette.GRAY_AND_WHITE)) {
            Lang.builder(PetrochemPnPCompat.MODID).add(line).forGoggles(tooltip);
        }
        // Create's goggle overlay only renders when addToGoggleTooltip returns true; force it so the
        // hint survives even if Petrochem returns false for an unformed or otherwise empty tower.
        cir.setReturnValue(true);
    }

    // Reads (does not change) the air a vacuum tower gains this tick so we can measure how much
    // Pipes n' Physics actually drains. Petrochem does not expose that amount any other way, so we
    // capture it at the one FluidStack(Fluid, int) construction in tick(). petrochemPnpCompat$sampleVacuum
    // guards itself to run at most once per game tick, so a future second construction would not
    // double sample; the arg is returned untouched, so it can never clobber an unrelated stack.
    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/FluidStack;<init>(Lnet/minecraft/world/level/material/Fluid;I)V"
            ),
            index = 1
    )
    private int petrochemPnpCompat$sampleVacuumAirGain(int gain) {
        petrochemPnpCompat$sampleVacuum(gain);
        return gain;
    }

    @Unique
    private void petrochemPnpCompat$sampleVacuum(int gain) {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (now == petrochemPnpCompat$lastSampleTick) {
            return;
        }
        boolean contiguous = now == petrochemPnpCompat$lastSampleTick + 1;
        petrochemPnpCompat$lastSampleTick = now;

        petrochemPnpCompat$hasPumpSuction();

        int air = getAir();
        int capacity = outputTank.getPrimaryHandler().getTankCapacity(0);
        if (!contiguous) {
            // The prediction is only valid one tick ahead; a skipped fill would fold the whole gap
            // into a single spurious drain sample, so discard the window and start fresh.
            petrochemPnpCompat$predictedAir = -1;
            petrochemPnpCompat$drainedMb = 0;
            petrochemPnpCompat$sampledTicks = 0;
        }
        if (petrochemPnpCompat$predictedAir >= 0) {
            petrochemPnpCompat$drainedMb += Math.max(0, petrochemPnpCompat$predictedAir - air);
            petrochemPnpCompat$sampledTicks++;
        }
        petrochemPnpCompat$predictedAir = Math.min(capacity, air + gain);

        if (petrochemPnpCompat$sampledTicks >= 20) {
            petrochemPnpCompat$starved = petrochemPnpCompat$drainedMb < gain * petrochemPnpCompat$sampledTicks;
            if (Config.DEBUG_VACUUM_LOGGING.get()) {
                petrochemPnpCompat$logSample(air, capacity, gain);
            }
            petrochemPnpCompat$drainedMb = 0;
            petrochemPnpCompat$sampledTicks = 0;
        }
    }

    @Unique
    private void petrochemPnpCompat$logSample(int air, int capacity, int gain) {
        PetrochemPnPCompat.LOGGER.debug(
                "[vacuum] {} air={}/{} vacuum={} starved={} gain={} pump={} rpm={} pumps={}/{} nodes"
                        + " allowance={} drain={} mB/t over {}t",
                ((BlockEntity) (Object) this).getBlockPos().toShortString(), air, capacity, hasVacuum(),
                petrochemPnpCompat$starved, gain, petrochemPnpCompat$hasSuction, petrochemPnpCompat$bestPumpRpm,
                petrochemPnpCompat$graphPumps, petrochemPnpCompat$graphNodes,
                petrochemPnpCompat$hasSuction ? "unlimited" : Config.VACUUM_PASSIVE_DRAIN_MB.get(),
                petrochemPnpCompat$drainedMb / (float) petrochemPnpCompat$sampledTicks,
                petrochemPnpCompat$sampledTicks);
    }
}
