package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.PacketSendEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Read-only outbound packet diagnostic logger. It observes PacketSendEvent
 * and never cancels, mutates, resends, queues, or otherwise changes packets.
 */
public final class PacketDebug extends Module {
    private static final int FLUSH_EVERY = 64;

    private final Object writerLock = new Object();
    private final AtomicLong packetIndex = new AtomicLong();
    private BufferedWriter writer;
    private int entriesSinceFlush;
    private boolean writerUnavailable;

    public PacketDebug() {
        super("Packet Debug", Category.Misc, "Writes read-only outbound packet diagnostics to a log file");
    }

    @Override
    public void onEnable() {
        packetIndex.set(0L);
        entriesSinceFlush = 0;
        writerUnavailable = false;
        openWriter();
    }

    @Override
    public void onDisable() {
        closeWriter();
    }

    @EventListener
    public void event(PacketSendEvent event) {
        if (event == null) return;
        Packet<?> packet = event.getPacket();
        if (!isObservedPacket(packet)) return;
        writeLine(describe(packet));
    }

    private boolean isObservedPacket(Packet<?> packet) {
        return packet instanceof UpdateSelectedSlotC2SPacket
                || packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerActionC2SPacket
                || packet instanceof HandSwingC2SPacket
                || packet instanceof ClickSlotC2SPacket
                || packet instanceof PlayerMoveC2SPacket;
    }

    private String describe(Packet<?> packet) {
        MinecraftClient client = getMc();
        PlayerEntity player = client.player;
        long index = packetIndex.incrementAndGet();
        StringBuilder line = new StringBuilder(384);

        line.append("index=").append(index)
                .append(" wallMs=").append(System.currentTimeMillis())
                .append(" monoNs=").append(System.nanoTime())
                .append(" thread=").append(Thread.currentThread().getName())
                .append(" packet=").append(packet.getClass().getSimpleName());
        appendPlayerState(line, player);

        if (packet instanceof UpdateSelectedSlotC2SPacket selectedSlot) {
            line.append(" selectedSlotPacket=").append(selectedSlot.getSelectedSlot());
        } else if (packet instanceof PlayerInteractBlockC2SPacket interactBlock) {
            appendBlockInteraction(line, client, interactBlock);
        } else if (packet instanceof PlayerInteractItemC2SPacket interactItem) {
            line.append(" hand=").append(interactItem.getHand())
                    .append(" sequence=").append(interactItem.getSequence())
                    .append(" packetYaw=").append(interactItem.getYaw())
                    .append(" packetPitch=").append(interactItem.getPitch());
        } else if (packet instanceof PlayerActionC2SPacket action) {
            appendAction(line, client, action);
        } else if (packet instanceof HandSwingC2SPacket swing) {
            line.append(" hand=").append(swing.getHand());
        } else if (packet instanceof ClickSlotC2SPacket click) {
            line.append(" syncId=").append(click.getSyncId())
                    .append(" revision=").append(click.getRevision())
                    .append(" slot=").append(click.getSlot())
                    .append(" button=").append(click.getButton())
                    .append(" actionType=").append(click.getActionType())
                    .append(" stack=").append(itemName(click.getStack()))
                    .append(" modifiedStacks=").append(click.getModifiedStacks().size());
        } else if (packet instanceof PlayerMoveC2SPacket move) {
            appendMove(line, player, move);
        }
        return line.toString();
    }

    private void appendPlayerState(StringBuilder line, PlayerEntity player) {
        if (player == null) {
            line.append(" player=none");
            return;
        }
        line.append(" age=").append(player.age)
                .append(" heldSlot=").append(player.getInventory().selectedSlot)
                .append(" mainHand=").append(itemName(player.getMainHandStack()))
                .append(" offHand=").append(itemName(player.getOffHandStack()))
                .append(" playerPos=").append(player.getX()).append(',').append(player.getY()).append(',').append(player.getZ())
                .append(" playerYaw=").append(player.getYaw())
                .append(" playerPitch=").append(player.getPitch())
                .append(" playerOnGround=").append(player.isOnGround());
    }

    private void appendBlockInteraction(StringBuilder line, MinecraftClient client, PlayerInteractBlockC2SPacket packet) {
        BlockHitResult hit = packet.getBlockHitResult();
        line.append(" hand=").append(packet.getHand())
                .append(" sequence=").append(packet.getSequence());
        appendHit(line, client, hit);
    }

    private void appendAction(StringBuilder line, MinecraftClient client, PlayerActionC2SPacket packet) {
        line.append(" action=").append(packet.getAction())
                .append(" sequence=").append(packet.getSequence())
                .append(" blockPos=").append(packet.getPos())
                .append(" direction=").append(packet.getDirection());
        appendBlockState(line, client, packet.getPos());
    }

    private void appendMove(StringBuilder line, PlayerEntity player, PlayerMoveC2SPacket packet) {
        if (player == null) {
            line.append(" moveState=playerUnavailable");
            return;
        }
        line.append(" changesPosition=").append(packet.changesPosition())
                .append(" changesLook=").append(packet.changesLook())
                .append(" movePos=").append(packet.getX(player.getX())).append(',')
                .append(packet.getY(player.getY())).append(',')
                .append(packet.getZ(player.getZ()))
                .append(" moveYaw=").append(packet.getYaw(player.getYaw()))
                .append(" movePitch=").append(packet.getPitch(player.getPitch()))
                .append(" moveOnGround=").append(packet.isOnGround());
    }

    private void appendHit(StringBuilder line, MinecraftClient client, BlockHitResult hit) {
        if (hit == null) {
            line.append(" hit=none");
            return;
        }
        line.append(" blockPos=").append(hit.getBlockPos())
                .append(" side=").append(hit.getSide())
                .append(" hitVec=").append(hit.getPos())
                .append(" insideBlock=").append(hit.isInsideBlock());
        appendBlockState(line, client, hit.getBlockPos());
    }

    private void appendBlockState(StringBuilder line, MinecraftClient client, net.minecraft.util.math.BlockPos pos) {
        if (client.world == null || pos == null) {
            line.append(" blockState=worldUnavailable");
            return;
        }
        BlockState state = client.world.getBlockState(pos);
        line.append(" blockState=").append(state)
                .append(" replaceable=").append(state.isReplaceable());
    }

    private String itemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        return String.valueOf(stack.getItem());
    }

    private void openWriter() {
        synchronized (writerLock) {
            closeWriterLocked();
            try {
                File runDirectory = MinecraftClient.getInstance().runDirectory;
                Path directory = new File(runDirectory, "prestige-packet-debug").toPath();
                Files.createDirectories(directory);
                String name = "packet-debug-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".log";
                writer = Files.newBufferedWriter(directory.resolve(name), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                writer.write("PacketDebug observes ClientConnection.send order before ClientConnection dispatch. It does not observe wire order.");
                writer.newLine();
            } catch (IOException | SecurityException exception) {
                writerUnavailable = true;
                writer = null;
            }
        }
    }

    private void writeLine(String line) {
        synchronized (writerLock) {
            if (writer == null || writerUnavailable) return;
            try {
                writer.write(line);
                writer.newLine();
                entriesSinceFlush++;
                if (entriesSinceFlush >= FLUSH_EVERY) {
                    writer.flush();
                    entriesSinceFlush = 0;
                }
            } catch (IOException exception) {
                writerUnavailable = true;
                closeWriterLocked();
            }
        }
    }

    private void closeWriter() {
        synchronized (writerLock) {
            closeWriterLocked();
        }
    }

    private void closeWriterLocked() {
        if (writer == null) return;
        try {
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
            // Diagnostics must never interfere with packet dispatch.
        } finally {
            writer = null;
            entriesSinceFlush = 0;
        }
    }
}
