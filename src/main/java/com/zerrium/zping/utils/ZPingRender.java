//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zerrium.zping.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.zerrium.zping.utils.ZPingGeneralUtils.logInfo;

public class ZPingRender {
    public static MinecraftClient currentClient = null;
    private static final ConcurrentHashMap<BlockPos, Integer> pingList = new ConcurrentHashMap<>();
    public static final int PING_LIFE = 80;
    private static float tick = 0;

    public static void renderPing(MatrixStack matrixStack, float tickDelta) {
        updatePings(tickDelta);
        drawPings(matrixStack);
    }

    private static void updatePings(float tickDelta) {
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
                    }
                    pingLife--;
                } else {
                    pingList.remove(pingPos);
                    logInfo(pingPos.toShortString() + " removed!");
                }
                pingList.replace(pingPos, pingLife);
            }
        }
    }

    private static void drawPings(MatrixStack matrixStack) {
        if(!pingList.isEmpty()) {
            for (Entry<BlockPos, Integer> entry : pingList.entrySet()) {
                BlockPos pingPos = entry.getKey();
                int pingLife = entry.getValue();
                if (pingLife > 0) {
                    Vec2f screenPos = blockPosToScreenPos(pingPos, matrixStack);
                    drawPing(matrixStack, (int) screenPos.x, (int) screenPos.y, 1,1,1,0xffff0000);
                }
            }
        }
    }

    private static void drawPing(MatrixStack matrixStack, int x, int y, int width, int height, int stroke, int color) {
        matrixStack.push();
        matrixStack.translate(x-stroke, y-stroke, 0);
        width += stroke *2;
        height += stroke *2;
        DrawableHelper.fill(matrixStack, 0, 0, width, stroke, color);
        DrawableHelper.fill(matrixStack, width - stroke, 0, width, height, color);
        DrawableHelper.fill(matrixStack, 0, height - stroke, width, height, color);
        DrawableHelper.fill(matrixStack, 0, 0, stroke, height, color);
        matrixStack.pop();
    }

    // Need a method of translating 3d world space coordinates to screen space
    private static Vec2f blockPosToScreenPos(BlockPos blockPos, MatrixStack matrixStack) {
        return new Vec2f(0,0);
    }

    public static void addPing(BlockPos pingPos) {
        pingList.put(pingPos, 80);
    }
}
