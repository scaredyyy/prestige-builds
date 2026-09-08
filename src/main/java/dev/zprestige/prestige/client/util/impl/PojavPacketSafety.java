package dev.zprestige.prestige.client.util.impl;

import net.minecraft.network.packet.Packet;
import java.util.Objects;

/** Validates packet data without timing, queueing, or rate limiting. */
public final class PojavPacketSafety {
    private PojavPacketSafety() { }

    public static boolean allow(Packet<?> packet) {
        return PacketStateValidator.canSend(packet);
    }

    public static void runTrustedBlockAction(Runnable action) {
        Objects.requireNonNull(action, "action").run();
    }
}
