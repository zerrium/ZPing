package com.zerrium.zping.utils;

import static com.zerrium.zping.models.ZPingGeneral.*;

public class ZPingGeneralUtils {
    public static void LogInfo(String str) {
        LOGGER.info("["+ MOD_NAME +"] " +  str);
    }
    public static void LogWarn(String str) {
        LOGGER.warn("["+ MOD_NAME +"] " +  str);
    }
}
