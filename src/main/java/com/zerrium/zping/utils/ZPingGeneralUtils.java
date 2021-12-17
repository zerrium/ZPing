package com.zerrium.zping.utils;

import static com.zerrium.zping.models.ZPingGeneral.*;

public class ZPingGeneralUtils {
    public static void logInfo(final String str) {
        LOGGER.info("["+ MOD_NAME +"] " +  str);
    }
    public static void logWarn(final String str) {
        LOGGER.warn("["+ MOD_NAME +"] " +  str);
    }
}
