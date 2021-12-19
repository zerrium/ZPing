package com.zerrium.zping.client;

import com.zerrium.zping.utils.ZPingArbitraryRaycast;
import com.zerrium.zping.utils.ZPingRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import static com.zerrium.zping.models.ZPingGeneral.*;
import static com.zerrium.zping.utils.ZPingGeneralUtils.*;

@Environment(EnvType.CLIENT)
public class ZPingClient implements ClientModInitializer {

    private KeyBinding pingKeyBinding;
    public static final int DEFAULT_PING_KEY = GLFW.GLFW_KEY_Z;
    public static final int PING_LIMIT = 5;
    public static final int PING_DELAY = 40;
    public static final int PING_TIMEOUT = 80;
    public static int timeoutCount = 0;
    public static int delayCount = 0;
    public static int pingAmount = 0;
    public static boolean wasPressed = false;
    public static final Identifier PING_SOUND_ID = new Identifier("minecraft", "entity.experience_orb.pickup");

    @Override
    public void onInitializeClient() {
        pingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key."+ MOD_ID +".ping", // The translation key of the keybinding's name
                DEFAULT_PING_KEY, // The keycode of the key
                MOD_NAME // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(pingAmount < PING_LIMIT) {
                if(!wasPressed) {
                    if (pingKeyBinding.isPressed()) {
                        if(ping(client)) {
                            if(delayCount < PING_DELAY) {
                                pingAmount++;
                            }
                            else {
                                if(pingAmount>0) {
                                    pingAmount--;
                                }
                            }
                            delayCount = 0;
                            wasPressed = true;
                        }
                    }
                }
                else {
                    if(!pingKeyBinding.isPressed()) {
                        wasPressed = false;
                    }
                }
                delayCount++;
            }
            else {
                if(timeoutCount < PING_TIMEOUT) {
                    if (pingKeyBinding.isPressed()) {
                        timeoutCount = 0;
                    }
                    else {
                        timeoutCount++;
                    }
                }
                else {
                    timeoutCount = 0;
                    pingAmount = 0;
                    delayCount = 20;
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(PING_PACKET_ID, (client, handler, buf, responseSender) -> {
            final BlockPos hitPos = buf.readBlockPos();
            final String hitName = buf.readString();
            final String dimensionName = buf.readString();
            client.execute(() -> {
                // Everything in this lambda is run on the render thread
                assert client.player != null;
                ZPingRender.currentClient = client;
                ZPingRender.addPing(hitPos);
                client.player.sendMessage(new LiteralText(hitName + " (" +  hitPos.toShortString() + ", " + dimensionName + ")"), true);
                SoundEvent pingSound = new SoundEvent(PING_SOUND_ID);
                client.player.playSound(pingSound, 1, 2);
            });
        });
        HudRenderCallback.EVENT.register(ZPingRender::renderPing);
        logInfo("Client initialized!");
    }

    private boolean ping(final MinecraftClient client) {

        if(client == null)
            return false;
        final HitResult hit = new ZPingArbitraryRaycast(client, 1, 40).hit;
        if(hit == null || client.player == null || client.world == null)
            return false;
        final Type hitType = hit.getType();
        boolean isHit = false;
        String hitName = null;
        BlockPos hitPos = null;
        switch (hitType) {
            case MISS:
                break;
            case BLOCK:
                isHit = true;
                BlockHitResult blockHit = (BlockHitResult) hit;
                hitPos = blockHit.getBlockPos();
                hitName = client.world.getBlockState(hitPos).getBlock().getName().getString();
                break;
            case ENTITY:
                isHit = true;
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                hitPos = entity.getBlockPos();
                hitName = entity.getName().getString();
                break;
            default:
                logWarn("Unexpected value: " + hitType);
        }
        if(isHit) {
            String dimensionName = StringUtils.capitalize(StringUtils.replaceChars(client.world.getRegistryKey().getValue().getPath(), '_', ' '));
            client.player.sendMessage(new LiteralText(hitName + " (" +  hitPos.toShortString() + ", " + dimensionName + ")"), true);
            final SoundEvent pingSound = new SoundEvent(PING_SOUND_ID);
            client.player.playSound(pingSound, 1, 2);
            ZPingRender.currentClient = client;
            ZPingRender.addPing(hitPos);
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(hitPos);
            buf.writeString(hitName);
            buf.writeString(dimensionName);
            ClientPlayNetworking.send(PING_PACKET_ID, buf);
        }
        return isHit;
    }
}
