package com.zerrium.zping.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.RaycastContext;

public class ZPingArbitraryRaycast {

    public MinecraftClient client;
    public int width;
    public int height;
    public Vec3d cameraDirection;
    public HitResult hit;
    static double reachMultiplier = 1;

    public ZPingArbitraryRaycast(MinecraftClient client, float tickDelta) {
        setup(client, tickDelta);
    }

    public ZPingArbitraryRaycast(MinecraftClient client, float tickDelta, double reachMultiplier) {
        ZPingArbitraryRaycast.reachMultiplier = reachMultiplier;
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
        boolean tooFar = false;
        double extendedReach = reachDistance;
        if (client.interactionManager.hasExtendedReach()) {
            extendedReach = 6.0D * reachMultiplier;//Change this to extend the reach
            reachDistance = extendedReach;
        }
        HitResult target = raycast(entity, reachDistance, tickDelta, false, direction);

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
