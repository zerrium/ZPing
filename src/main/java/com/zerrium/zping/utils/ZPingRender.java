//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zerrium.zping.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.zerrium.zping.utils.ZPingGeneralUtils.logInfo;

public class ZPingRender {
    public static MinecraftClient currentClient = null;
    private static final ConcurrentHashMap<BlockPos, Integer> pingList = new ConcurrentHashMap<>();
    public static final int PING_LIFE = 80;
    private static float tick = 0;

    // MatrixStack might later be used for rendering
    public static void renderPing(MatrixStack matrixStack, float tickDelta) {
        tick += tickDelta;
        if (currentClient != null && tick >= 1) {
            tick = 0;
            BlockPos pingPos;
            int pingLife;
            for(Entry<BlockPos, Integer> entry : pingList.entrySet()) {
                pingPos = entry.getKey();
                pingLife = entry.getValue();
                if (pingLife > 0) {
                    if (pingLife == PING_LIFE) {
                        logInfo(pingPos.toShortString() + " added!");
                        // Start ping render
                    }
                    pingLife--;
                } else {
                    // Stop ping render
                    pingList.remove(pingPos);
                    logInfo(pingPos.toShortString() + " removed!");
                }
                pingList.replace(pingPos, pingLife);
            }
        }

    }

    public static void addPing(BlockPos pingPos) {
        pingList.put(pingPos, 80);
    }
}
