package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.PacketSendEvent;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.IntSetting;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.PojavInput;
import dev.zprestige.prestige.client.util.impl.PojavPacketSafety;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/** Deterministic respawn-anchor macro using normal client block interactions. */
public final class AnchorMacro extends Module {
    public BooleanSetting whileUse;
    public BooleanSetting stopOnKill;
    public IntSetting switchDelay;
    public IntSetting glowstoneDelay;
    public IntSetting explodeDelay;
    public IntSetting explodeSlot;
    public BooleanSetting onlyOwn;
    public BooleanSetting onlyCharge;

    private int switchClock;
    private int glowstoneClock;
    private int explodeClock;
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private BlockPos armedAnchor;

    public AnchorMacro() {
        super("Anchor Macro", Category.Combat, "Deterministic respawn-anchor macro using normal interactions");
        whileUse = setting("While Use", true).description("Allows the macro while eating or shielding.");
        stopOnKill = setting("Stop On Kill", false).description("Reserved for the module's existing kill-stop behavior.");
        switchDelay = setting("Switch Delay", 0, 0, 20);
        glowstoneDelay = setting("Glowstone Delay", 0, 0, 20);
        explodeDelay = setting("Explode Delay", 0, 0, 20);
        explodeSlot = setting("Explode Slot", 1, 1, 9);
        onlyOwn = setting("Only Own", false);
        onlyCharge = setting("Only Charge", false);
    }

    @Override
    public void onEnable() {
        resetTimers();
        armedAnchor = null;
    }

    @Override
    public void onDisable() {
        resetTimers();
        ownedAnchors.clear();
        armedAnchor = null;
    }

    @EventListener
    public void event(TickEvent event) {
        if (!hasValidGameState()) {
            resetActiveCycle();
            return;
        }

        boolean physicalUse = isPhysicalUsePressed();
        boolean eatingOrShielding = getMc().player.getMainHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getMainHandStack().getItem() instanceof ShieldItem
                || getMc().player.getOffHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getOffHandStack().getItem() instanceof ShieldItem;

        if (!(getMc().crosshairTarget instanceof BlockHitResult hit)
                || getMc().world.getBlockState(hit.getBlockPos()).getBlock() != Blocks.RESPAWN_ANCHOR) {
            resetActiveCycle();
            return;
        }
        if (eatingOrShielding && physicalUse && !whileUse.getObject()) return;

        if (physicalUse) armedAnchor = hit.getBlockPos().toImmutable();
        boolean active = physicalUse || hit.getBlockPos().equals(armedAnchor);
        if (!active) return;

        getMc().options.useKey.setPressed(false);
        if (onlyOwn.getObject() && !ownedAnchors.contains(hit.getBlockPos())) {
            resetActiveCycle();
            return;
        }

        int charges = getMc().world.getBlockState(hit.getBlockPos()).get(Properties.CHARGES);
        if (charges == 0) {
            charge(hit);
            return;
        }
        if (onlyCharge.getObject()) {
            resetActiveCycle();
            return;
        }
        explode(hit);
    }

    @EventListener
    public void event(PacketSendEvent event) {
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)
                || getMc().player == null || getMc().world == null
                || !getMc().player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;

        BlockHitResult hit = packet.getBlockHitResult();
        BlockPos placedPos = getMc().world.getBlockState(hit.getBlockPos()).isReplaceable()
                ? hit.getBlockPos() : hit.getBlockPos().offset(hit.getSide());
        ownedAnchors.add(placedPos.toImmutable());
    }

    private void charge(BlockHitResult hit) {
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (!isTimerReadyForSwitch()) return;
            Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.GLOWSTONE);
            if (slot == null || !selectSlot(slot)) {
                resetSwitchTimer();
                return;
            }
            resetSwitchTimer();
        }
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) return;
        if (!isTimerReady(glowstoneClock, glowstoneDelay.getObject())) {
            glowstoneClock++;
            return;
        }
        glowstoneClock = 0;
        useAnchor(hit);
    }

    private void explode(BlockHitResult hit) {
        int slot = explodeSlot.getObject() - 1;
        if (!isValidHotbarSlot(slot)) {
            resetActiveCycle();
            return;
        }
        if (getMc().player.getInventory().selectedSlot != slot) {
            if (!isTimerReadyForSwitch()) return;
            if (!selectSlot(slot)) {
                resetSwitchTimer();
                return;
            }
            resetSwitchTimer();
        }
        if (getMc().player.getInventory().selectedSlot != slot) return;
        if (!isTimerReady(explodeClock, explodeDelay.getObject())) {
            explodeClock++;
            return;
        }
        explodeClock = 0;
        useAnchor(hit);
        ownedAnchors.remove(hit.getBlockPos());
        armedAnchor = null;
    }

    private boolean hasValidGameState() {
        return getMc().player != null && getMc().world != null
                && getMc().interactionManager != null && getMc().currentScreen == null;
    }

    private boolean isPhysicalUsePressed() {
        return PojavInput.isArgonMousePressed(1)
                || PojavInput.isTrackedMousePressed(1)
                || PojavInput.isMousePressed(1);
    }

    private boolean isTimerReadyForSwitch() {
        if (switchClock >= switchDelay.getObject()) return true;
        switchClock++;
        return false;
    }

    private boolean isTimerReady(int clock, int configuredDelay) {
        return clock >= configuredDelay;
    }

    private boolean selectSlot(int slot) {
        if (!isValidHotbarSlot(slot) || getMc().player == null) return false;
        getMc().player.getInventory().selectedSlot = slot;
        return getMc().player.getInventory().selectedSlot == slot;
    }

    private boolean isValidHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8;
    }

    private void useAnchor(BlockHitResult hit) {
        if (!hasValidGameState() || hit == null) return;
        if (!hit.getBlockPos().equals(armedAnchor) && !isPhysicalUsePressed()) return;
        PojavPacketSafety.runTrustedBlockAction(() -> {
            var result = getMc().interactionManager.interactBlock(getMc().player, Hand.MAIN_HAND, hit);
            if (result.isAccepted() && result.shouldSwingHand()) getMc().player.swingHand(Hand.MAIN_HAND);
        });
    }

    private void resetTimers() {
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
    }

    private void resetSwitchTimer() {
        switchClock = 0;
    }

    private void resetActiveCycle() {
        armedAnchor = null;
        resetTimers();
    }
}
