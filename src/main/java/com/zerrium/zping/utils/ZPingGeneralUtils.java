package com.zerrium.zping.utils;

import static com.zerrium.zping.models.ZPingGeneral.*;

public class ZPingGeneralUtils {
    public static void logInfo(String str) {
        LOGGER.info("["+ MOD_NAME +"] " +  str);
    }
    public static void logWarn(String str) {
        LOGGER.warn("["+ MOD_NAME +"] " +  str);
    }
}
