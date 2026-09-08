package dev.zprestige.prestige.client.util.impl;

import dev.zprestige.prestige.client.util.MC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class PacketUtil implements MC {

    public static PacketUtil INSTANCE = new PacketUtil();

    public void sendPacket(Packet packet) {
        // Legacy modules forge movement packets. Android servers reject them;
        // standard movement still goes through Minecraft's normal sender.
        if (PojavInput.isPojav() && packet instanceof PlayerMoveC2SPacket) {
            return;
        }
        this.getMc().getNetworkHandler().sendPacket(packet);
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }

}
