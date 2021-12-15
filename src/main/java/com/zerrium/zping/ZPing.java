package com.zerrium.zping;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZPing implements ModInitializer {

    public static final String MOD_NAME = "ZPing";
    public static final String MOD_VERSION = "0.1.0-SNAPSHOT";
    public static final String MOD_ID = "zping";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final Identifier PING_PACKET_ID = new Identifier("zping", "ping");

    @Override
    public void onInitialize() {
        LOGGER.info("["+ MOD_NAME +"] Initializing "+ MOD_NAME +" "+ MOD_VERSION +"!");
    }

    public static void logInfo(String str) {
        LOGGER.info("["+ MOD_NAME +"] " +  str);
    }
    public static void logWarn(String str) {
        LOGGER.warn("["+ MOD_NAME +"] " +  str);
    }
}
