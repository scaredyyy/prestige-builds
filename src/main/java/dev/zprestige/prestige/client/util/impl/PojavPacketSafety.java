package dev.zprestige.prestige.client.util.impl;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

import java.util.HashMap;
import java.util.Map;

/** Pojav outbound guard: preserve normal gameplay, reject packet bursts. */
public final class PojavPacketSafety {
    private static final Map<Class<?>, Long> LAST_SENT = new HashMap<>();
    private static final ThreadLocal<Integer> TRUSTED_BLOCK_ACTION = ThreadLocal.withInitial(() -> 0);

    private PojavPacketSafety() { }

    public static synchronized boolean allow(Packet<?> packet) {
        if (!PojavInput.isPojav()) return true;
        if (packet instanceof PlayerInteractBlockC2SPacket && TRUSTED_BLOCK_ACTION.get() > 0) return true;

        long interval;
        if (packet instanceof PlayerInteractEntityC2SPacket) interval = 125L;
        else if (packet instanceof PlayerInteractBlockC2SPacket) interval = 150L;
        else if (packet instanceof PlayerInteractItemC2SPacket) interval = 125L;
        else if (packet instanceof ClickSlotC2SPacket) interval = 175L;
        else return true;

        long now = System.currentTimeMillis();
        Class<?> type = packet.getClass();
        long previous = LAST_SENT.getOrDefault(type, 0L);
        if (now - previous < interval) return false;
        LAST_SENT.put(type, now);
        return true;
    }

    /** Run one module-owned block interaction without racing normal touch input. */
    public static void runTrustedBlockAction(Runnable action) {
        TRUSTED_BLOCK_ACTION.set(TRUSTED_BLOCK_ACTION.get() + 1);
        try {
            action.run();
        } finally {
            int remaining = TRUSTED_BLOCK_ACTION.get() - 1;
            if (remaining == 0) TRUSTED_BLOCK_ACTION.remove();
            else TRUSTED_BLOCK_ACTION.set(remaining);
        }
    }
}
