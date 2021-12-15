package com.zerrium.zping.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.RaycastContext;
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
        logInfo("Client initialized!");
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
            BlockPos hitPos = buf.readBlockPos();
            String msg = buf.readString();
            client.execute(() -> {
                // Everything in this lambda is run on the render thread
                assert client.player != null;
                client.player.sendMessage(new LiteralText(msg + " (" +  hitPos.toShortString() + ")"), true);
                SoundEvent pingSound = new SoundEvent(PING_SOUND_ID);
                client.player.playSound(pingSound, 1, 2);
            });
        });
    }

    private boolean ping(MinecraftClient client) {

        if(client == null)
            return false;
        HitResult hit = new ArbitraryRaycast(client, 1, 40).hit;
        if(hit == null || client.player == null || client.world == null)
            return false;
        Type hitType = hit.getType();
        boolean isHit = false;
        String msg = null;
        BlockPos hitPos = null;
        switch (hitType) {
            case MISS:
                break;
            case BLOCK:
                isHit = true;
                BlockHitResult blockHit = (BlockHitResult) hit;
                hitPos = blockHit.getBlockPos();
                msg = client.world.getBlockState(hitPos).getBlock().getName().getString();
                break;
            case ENTITY:
                isHit = true;
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                hitPos = entity.getBlockPos();
                msg = entity.getName().getString();
                break;
            default:
                logWarn("Unexpected value: " + hitType);
        }
        if(isHit) {
            client.player.sendMessage(new LiteralText(msg + " (" +  hitPos.toShortString() + ")"), true);
            SoundEvent pingSound = new SoundEvent(PING_SOUND_ID);
            client.player.playSound(pingSound, 1, 2);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(hitPos);
            buf.writeString(msg);
            ClientPlayNetworking.send(PING_PACKET_ID, buf);
        }
        return isHit;
    }


    private static class ArbitraryRaycast {

        MinecraftClient client;
        int width;
        int height;
        Vec3d cameraDirection;
        HitResult hit;
        static double reachMultiplier = 1;

        public ArbitraryRaycast(MinecraftClient client, float tickDelta) {
            setup(client, tickDelta);
        }

        public ArbitraryRaycast(MinecraftClient client, float tickDelta, double reachMultiplier) {
            ArbitraryRaycast.reachMultiplier = reachMultiplier;
            setup(client, tickDelta);
        }

        private void setup(MinecraftClient client, float tickDelta) {
            if(client == null || client.cameraEntity == null)
                return;
            this.client = client;
            width = client.getWindow().getScaledWidth();
            height = client.getWindow().getScaledHeight();
            cameraDirection = client.cameraEntity.getRotationVec(tickDelta);
            double fov = client.options.fov;
            double angleSize = fov/height;
            Vec3f verticalRotationAxis = new Vec3f(cameraDirection);
            verticalRotationAxis.cross(Vec3f.POSITIVE_Y);
            if(!verticalRotationAxis.normalize()) {
                return;//The camera is pointing directly up or down, you'll have to fix this one
            }

            Vec3f horizontalRotationAxis = new Vec3f(cameraDirection);
            horizontalRotationAxis.cross(verticalRotationAxis);
            horizontalRotationAxis.normalize();

            verticalRotationAxis = new Vec3f(cameraDirection);
            verticalRotationAxis.cross(horizontalRotationAxis);

            Vec3d direction = map(
                    (float) angleSize,
                    cameraDirection,
                    horizontalRotationAxis,
                    verticalRotationAxis,
                    width/2,
                    height/2,
                    width,
                    height
            );
            hit = raycastInDirection(client, tickDelta, direction);
        }

        private static Vec3d map(float anglePerPixel, Vec3d center, Vec3f horizontalRotationAxis,
                                 Vec3f verticalRotationAxis, int x, int y, int width, int height) {
            float horizontalRotation = (x - width/2f) * anglePerPixel;
            float verticalRotation = (y - height/2f) * anglePerPixel;

            final Vec3f temp2 = new Vec3f(center);
            temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation));
            temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation));
            return new Vec3d(temp2);
        }

        private static HitResult raycastInDirection(MinecraftClient client, float tickDelta, Vec3d direction) {
            Entity entity = client.getCameraEntity();
            if (entity == null || client.world == null || client.interactionManager == null) {
                return null;
            }

            double reachDistance = client.interactionManager.getReachDistance()*reachMultiplier;//Change this to extend the reach
            HitResult target = raycast(entity, reachDistance, tickDelta, false, direction);
            boolean tooFar = false;
            double extendedReach = reachDistance;
            if (client.interactionManager.hasExtendedReach()) {
                extendedReach = 6.0D*reachMultiplier;//Change this to extend the reach
                reachDistance = extendedReach;
            } else {
                if (reachDistance > 3.0D) {
                    tooFar = true;
                }
            }

            Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

            extendedReach = extendedReach * extendedReach;
            if (target != null) {
                extendedReach = target.getPos().squaredDistanceTo(cameraPos);
            }

            Vec3d vec3d3 = cameraPos.add(direction.multiply(reachDistance));
            Box box = entity
                    .getBoundingBox()
                    .stretch(entity.getRotationVec(1.0F).multiply(reachDistance))
                    .expand(1.0D, 1.0D, 1.0D);
            EntityHitResult entityHitResult = ProjectileUtil.raycast(
                    entity,
                    cameraPos,
                    vec3d3,
                    box,
                    (entityx) -> !entityx.isSpectator() && entityx.collides(),
                    extendedReach
            );

            if (entityHitResult == null) {
                return target;
            }

            Entity entity2 = entityHitResult.getEntity();
            Vec3d vec3d4 = entityHitResult.getPos();
            double g = cameraPos.squaredDistanceTo(vec3d4);
            if (tooFar && g > 9.0D) {
                return null;
            } else if (g < extendedReach || target == null) {
                target = entityHitResult;
                if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrameEntity) {
                    client.targetedEntity = entity2;
                }
            }

            return target;
        }

        private static HitResult raycast(
                Entity entity,
                double maxDistance,
                float tickDelta,
                boolean includeFluids,
                Vec3d direction
        ) {
            Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
            return entity.world.raycast(new RaycastContext(
                    entity.getCameraPosVec(tickDelta),
                    end,
                    RaycastContext.ShapeType.OUTLINE,
                    includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                    entity
            ));
        }

    }
}
