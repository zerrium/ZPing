package com.zerrium.zping.models;

import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZPingGeneral {
    public static final String MOD_NAME = "ZPing";
    public static final String MOD_VERSION = "0.1.0-SNAPSHOT.render";
    public static final String MOD_ID = "zping";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final Identifier PING_PACKET_ID = new Identifier("zping", "ping");
}
