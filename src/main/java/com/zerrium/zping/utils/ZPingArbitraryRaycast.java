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

    public ZPingArbitraryRaycast(final MinecraftClient client, final float tickDelta, final double reachMultiplier) {
        ZPingArbitraryRaycast.reachMultiplier = reachMultiplier;
        setup(client, tickDelta);
    }

    private void setup(final MinecraftClient client, final float tickDelta) {
        if(client == null || client.cameraEntity == null)
            return;
        this.client = client;
        width = client.getWindow().getScaledWidth();
        height = client.getWindow().getScaledHeight();
        cameraDirection = client.cameraEntity.getRotationVec(tickDelta);
        final double fov = client.options.fov;
        final double angleSize = fov/height;
        Vec3f verticalRotationAxis = new Vec3f(cameraDirection);
        verticalRotationAxis.cross(Vec3f.POSITIVE_Y);
        if(!verticalRotationAxis.normalize()) {
            return;//The camera is pointing directly up or down, you'll have to fix this one
        }

        final Vec3f horizontalRotationAxis = new Vec3f(cameraDirection);
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();

        verticalRotationAxis = new Vec3f(cameraDirection);
        verticalRotationAxis.cross(horizontalRotationAxis);

        final Vec3d direction = map(
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

    private static Vec3d map(final float anglePerPixel, final Vec3d center, final Vec3f horizontalRotationAxis,
                             final Vec3f verticalRotationAxis, final int x, final int y, final int width, final int height) {
        final float horizontalRotation = (x - width/2f) * anglePerPixel;
        final float verticalRotation = (y - height/2f) * anglePerPixel;

        final Vec3f temp2 = new Vec3f(center);
        temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation));
        temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation));
        return new Vec3d(temp2);
    }

    private static HitResult raycastInDirection(final MinecraftClient client, final float tickDelta, final Vec3d direction) {
        final Entity entity = client.getCameraEntity();
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

        final Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

        extendedReach = extendedReach * extendedReach;
        if (target != null) {
            extendedReach = target.getPos().squaredDistanceTo(cameraPos);
        }

        final Vec3d vec3d3 = cameraPos.add(direction.multiply(reachDistance));
        final Box box = entity
                .getBoundingBox()
                .stretch(entity.getRotationVec(1.0F).multiply(reachDistance))
                .expand(1.0D, 1.0D, 1.0D);
        final EntityHitResult entityHitResult = ProjectileUtil.raycast(
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

        final Entity entity2 = entityHitResult.getEntity();
        final Vec3d vec3d4 = entityHitResult.getPos();
        final double g = cameraPos.squaredDistanceTo(vec3d4);
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
            final Entity entity,
            final double maxDistance,
            final float tickDelta,
            final boolean includeFluids,
            final Vec3d direction
    ) {
        final Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
        return entity.world.raycast(new RaycastContext(
                entity.getCameraPosVec(tickDelta),
                end,
                RaycastContext.ShapeType.OUTLINE,
                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                entity
        ));
    }

}
