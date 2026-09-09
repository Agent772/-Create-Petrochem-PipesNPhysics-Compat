package com.agent772.petrochem_pnp_compat;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;

@Mod(PetrochemPnPCompat.MODID)
public class PetrochemPnPCompat {
    public static final String MODID = "petrochem_pnp_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PetrochemPnPCompat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        LOGGER.info("Create: Petrochem - Pipes n' Physics Compat loaded");
    }
}
