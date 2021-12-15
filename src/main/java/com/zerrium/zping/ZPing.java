package com.zerrium.zping;

import net.fabricmc.api.ModInitializer;

import static com.zerrium.zping.models.ZPingGeneral.*;
import static com.zerrium.zping.utils.ZPingGeneralUtils.*;

public class ZPing implements ModInitializer {

    @Override
    public void onInitialize() {
        LogInfo("Initializing "+ MOD_NAME +" "+ MOD_VERSION +"!");
    }
}
