package com.zerrium.zping.server;

import com.zerrium.zping.ZPing;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static com.zerrium.zping.models.ZPingGeneral.*;
import static com.zerrium.zping.utils.ZPingGeneralUtils.*;

@Environment(EnvType.SERVER)
public class ZPingServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        LogInfo("Server initialized!");
        ServerPlayNetworking.registerGlobalReceiver(PING_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            BlockPos hitPos = buf.readBlockPos();
            String msg = buf.readString();
            server.execute(() -> {
                // Everything in this lambda is run on the render thread
                PacketByteBuf sendBuf = PacketByteBufs.create();
                sendBuf.writeBlockPos(hitPos);
                sendBuf.writeString(msg);
                for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking((ServerWorld) player.world, hitPos)) {
                    if(otherPlayer != player)
                        ServerPlayNetworking.send(otherPlayer, PING_PACKET_ID, sendBuf);
                }
            });
        });
    }


}
