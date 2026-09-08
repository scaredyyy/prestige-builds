package dev.zprestige.prestige.client.util.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** Rejects objectively malformed outbound movement data without rewriting it. */
public final class PacketStateValidator {
    private PacketStateValidator() { }

    public static boolean canSend(Packet<?> packet) {
        if (packet == null) return false;
        if (packet instanceof PlayerMoveC2SPacket movePacket) return isValidMovement(movePacket);
        return true;
    }

    private static boolean isValidMovement(PlayerMoveC2SPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        double x = packet.getX(client.player.getX());
        double y = packet.getY(client.player.getY());
        double z = packet.getZ(client.player.getZ());
        float yaw = packet.getYaw(client.player.getYaw());
        float pitch = packet.getPitch(client.player.getPitch());

        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z)
                && Float.isFinite(yaw)
                && Float.isFinite(pitch)
                && pitch >= -90.0F
                && pitch <= 90.0F;
    }
}
