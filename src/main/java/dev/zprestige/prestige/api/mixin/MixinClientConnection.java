package dev.zprestige.prestige.api.mixin;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.impl.PacketReceiveEvent;
import dev.zprestige.prestige.client.event.impl.PacketSendEvent;
import dev.zprestige.prestige.client.util.impl.PojavInput;
import dev.zprestige.prestige.client.util.impl.PojavPacketSafety;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientConnection.class})
public class MixinClientConnection {
    @Inject(at={@At(value="HEAD")}, method={"handlePacket"}, cancellable=true)
    private static void handlePacket(Packet packet, PacketListener packetListener, CallbackInfo callbackInfo) {
        if (Prestige.Companion.getSelfDestructed()) {
            return;
        }
        // Login/configuration packets are not gameplay packets.  Let Fabric and
        // Minecraft handle them untouched on Pojav; dispatching them through a
        // client-module event bus can corrupt the join state on Android.
        if (isPojavPreWorld()) return;
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        if (event.invoke()) {
            callbackInfo.cancel();
        }
    }

    @Inject(at={@At(value="HEAD")}, method={"send(Lnet/minecraft/network/packet/Packet;)V"}, cancellable=true)
    void onPacketSend(Packet packet, CallbackInfo callbackInfo) {
        if (Prestige.Companion.getSelfDestructed()) {
            return;
        }
        if (isPojavPreWorld()) return;
        if (!PojavPacketSafety.allow(packet)) {
            callbackInfo.cancel();
            return;
        }
        PacketSendEvent event = new PacketSendEvent(packet);
        if (event.invoke()) {
            callbackInfo.cancel();
        }
    }

    private static boolean isPojavPreWorld() {
        if (!PojavInput.isPojav()) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null || client.world == null;
    }
}
