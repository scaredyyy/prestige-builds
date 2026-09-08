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
import dev.zprestige.prestige.client.util.impl.RandomUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Anchor macro synchronized with the local vanilla world state. */
public final class AnchorMacro extends Module {
    public BooleanSetting whileUse;
    public BooleanSetting stopOnKill;
    public IntSetting switchDelay;
    public IntSetting switchChance;
    public IntSetting placeChance;
    public IntSetting glowstoneDelay;
    public IntSetting glowstoneChance;
    public IntSetting explodeDelay;
    public IntSetting explodeChance;
    public IntSetting explodeSlot;
    public BooleanSetting onlyOwn;
    public BooleanSetting onlyCharge;

    private int switchClock;
    private int glowstoneClock;
    private int explodeClock;
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private final Set<BlockPos> pendingOwnedAnchors = new HashSet<>();
    private BlockPos armedAnchor;
    private BlockPos pendingActionPos;
    private PendingAction pendingAction = PendingAction.NONE;
    private int pendingCharge;

    public AnchorMacro() {
        super("Anchor Macro", Category.Combat, "Anchor macro synchronized with current vanilla block state");
        whileUse = setting("While Use", true).description("Trigger while eating or shielding");
        stopOnKill = setting("Stop On Kill", false).description("Do not anchor near dead players");
        switchDelay = setting("Switch Delay", 0, 0, 20);
        switchChance = setting("Switch Chance", 100, 0, 100);
        placeChance = setting("Place Chance", 100, 0, 100);
        glowstoneDelay = setting("Glowstone Delay", 0, 0, 20);
        glowstoneChance = setting("Glowstone Chance", 100, 0, 100);
        explodeDelay = setting("Explode Delay", 0, 0, 20);
        explodeChance = setting("Explode Chance", 100, 0, 100);
        explodeSlot = setting("Explode Slot", 1, 1, 9);
        onlyOwn = setting("Only Own", false);
        onlyCharge = setting("Only Charge", false);
    }

    @Override
    public void onEnable() {
        resetClocks();
        resetCycle();
        pendingOwnedAnchors.clear();
    }

    @Override
    public void onDisable() {
        ownedAnchors.clear();
        pendingOwnedAnchors.clear();
        resetCycle();
        resetClocks();
    }

    @EventListener
    public void event(TickEvent event) {
        if (!isClientReady()) {
            resetCycle();
            return;
        }
        confirmOwnedAnchors();
        updatePendingAction();

        if (!(getMc().crosshairTarget instanceof BlockHitResult hit)) {
            resetCycle();
            return;
        }
        BlockPos anchorPos = hit.getBlockPos();
        BlockState state = getMc().world.getBlockState(anchorPos);
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            resetCycle();
            return;
        }

        boolean physicalUse = isPhysicalUsePressed();
        if (!canRunWhileUsing(physicalUse)) return;
        if (physicalUse) arm(anchorPos);
        if (!isArmedFor(anchorPos, physicalUse)) return;

        getMc().options.useKey.setPressed(false);
        if (onlyOwn.getObject() && !ownedAnchors.contains(anchorPos)) {
            resetCycle();
            return;
        }
        if (isPendingAt(anchorPos)) return;

        int charges = state.get(Properties.CHARGES);
        if (charges == 0) {
            charge(hit, charges);
            return;
        }
        if (onlyCharge.getObject()) {
            resetCycle();
            return;
        }
        explode(hit, charges);
    }

    @EventListener
    public void event(PacketSendEvent event) {
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)) return;
        if (getMc().player == null || getMc().world == null) return;
        if (!getMc().player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;

        BlockHitResult hit = packet.getBlockHitResult();
        BlockPos clickedPos = hit.getBlockPos();
        BlockState clickedState = getMc().world.getBlockState(clickedPos);
        BlockPos placementPos = clickedState.isReplaceable() ? clickedPos : clickedPos.offset(hit.getSide());
        pendingOwnedAnchors.add(placementPos.toImmutable());
    }

    private void charge(BlockHitResult hit, int currentCharges) {
        if (!roll(placeChance)) return;
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (!switchReady()) return;
            if (!roll(switchChance)) {
                resetSwitchClock();
                return;
            }
            Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.GLOWSTONE);
            if (slot == null) {
                resetSwitchClock();
                return;
            }
            selectSlot(slot);
            resetSwitchClock();
        }
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) return;
        if (!glowstoneReady()) return;
        if (!roll(glowstoneChance)) {
            glowstoneClock = 0;
            return;
        }
        glowstoneClock = 0;
        if (useAnchor(hit)) beginPendingAction(PendingAction.CHARGE, hit.getBlockPos(), currentCharges);
    }

    private void explode(BlockHitResult hit, int currentCharges) {
        if (currentCharges <= 0) return;
        int slot = explodeSlot.getObject() - 1;
        if (slot < 0 || slot > 8) {
            resetCycle();
            return;
        }
        if (getMc().player.getInventory().selectedSlot != slot) {
            if (!switchReady()) return;
            if (!roll(switchChance)) {
                resetSwitchClock();
                return;
            }
            selectSlot(slot);
            resetSwitchClock();
        }
        if (getMc().player.getInventory().selectedSlot != slot) return;
        if (getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            resetCycle();
            return;
        }
        if (!explodeReady()) return;
        if (!roll(explodeChance)) {
            explodeClock = 0;
            return;
        }
        explodeClock = 0;
        if (useAnchor(hit)) beginPendingAction(PendingAction.EXPLODE, hit.getBlockPos(), currentCharges);
    }

    private boolean useAnchor(BlockHitResult hit) {
        if (!isCurrentAnchorHit(hit)) return false;
        final ActionResult[] result = {ActionResult.PASS};
        PojavPacketSafety.runTrustedBlockAction(() -> result[0] = getMc().interactionManager.interactBlock(getMc().player, Hand.MAIN_HAND, hit));
        if (!result[0].isAccepted()) return false;
        if (result[0].shouldSwingHand()) getMc().player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean isCurrentAnchorHit(BlockHitResult hit) {
        if (hit == null || getMc().world == null) return false;
        if (!(getMc().crosshairTarget instanceof BlockHitResult currentHit)) return false;
        if (!currentHit.getBlockPos().equals(hit.getBlockPos())) return false;
        return getMc().world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
    }

    private void beginPendingAction(PendingAction action, BlockPos pos, int charge) {
        pendingAction = action;
        pendingActionPos = pos.toImmutable();
        pendingCharge = charge;
    }

    private void updatePendingAction() {
        if (pendingAction == PendingAction.NONE || pendingActionPos == null) return;
        BlockState state = getMc().world.getBlockState(pendingActionPos);
        if (pendingAction == PendingAction.CHARGE) {
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
                resetPendingAction();
                resetCycle();
                return;
            }
            if (state.get(Properties.CHARGES) > pendingCharge) resetPendingAction();
            return;
        }
        if (pendingAction == PendingAction.EXPLODE) {
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
                ownedAnchors.remove(pendingActionPos);
                resetPendingAction();
                resetCycle();
                return;
            }
            if (state.get(Properties.CHARGES) != pendingCharge) {
                resetPendingAction();
                resetCycle();
            }
        }
    }

    private void confirmOwnedAnchors() {
        Iterator<BlockPos> iterator = pendingOwnedAnchors.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = getMc().world.getBlockState(pos);
            if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                ownedAnchors.add(pos);
                iterator.remove();
                continue;
            }
            if (!(getMc().crosshairTarget instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) iterator.remove();
        }
    }

    private boolean isPendingAt(BlockPos pos) {
        return pendingAction != PendingAction.NONE && pendingActionPos != null && pendingActionPos.equals(pos);
    }

    private boolean isClientReady() {
        return getMc().player != null && getMc().world != null
                && getMc().interactionManager != null && getMc().currentScreen == null;
    }

    private boolean isPhysicalUsePressed() {
        return PojavInput.isArgonMousePressed(1)
                || PojavInput.isTrackedMousePressed(1)
                || PojavInput.isMousePressed(1);
    }

    private boolean canRunWhileUsing(boolean physicalUse) {
        if (!physicalUse) return true;
        boolean eatingOrShielding = getMc().player.getMainHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getMainHandStack().getItem() instanceof ShieldItem
                || getMc().player.getOffHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getOffHandStack().getItem() instanceof ShieldItem;
        return !eatingOrShielding || whileUse.getObject();
    }

    private void arm(BlockPos pos) {
        BlockPos immutable = pos.toImmutable();
        if (!immutable.equals(armedAnchor)) {
            resetPendingAction();
            resetClocks();
        }
        armedAnchor = immutable;
    }

    private boolean isArmedFor(BlockPos pos, boolean physicalUse) {
        return physicalUse || armedAnchor != null && armedAnchor.equals(pos);
    }

    private boolean switchReady() {
        int delay = switchDelay.getObject();
        if (delay <= 0) return true;
        if (switchClock < delay) {
            switchClock++;
            return false;
        }
        return true;
    }

    private boolean glowstoneReady() {
        int delay = glowstoneDelay.getObject();
        if (delay <= 0) return true;
        if (glowstoneClock < delay) {
            glowstoneClock++;
            return false;
        }
        return true;
    }

    private boolean explodeReady() {
        int delay = explodeDelay.getObject();
        if (delay <= 0) return true;
        if (explodeClock < delay) {
            explodeClock++;
            return false;
        }
        return true;
    }

    private boolean roll(IntSetting chance) {
        return RandomUtil.INSTANCE.randomInRange(1, 100) <= chance.getObject();
    }

    private void selectSlot(int slot) {
        InventoryUtil.INSTANCE.setCurrentSlot(slot);
    }

    private void resetSwitchClock() {
        switchClock = 0;
    }

    private void resetClocks() {
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
    }

    private void resetPendingAction() {
        pendingAction = PendingAction.NONE;
        pendingActionPos = null;
        pendingCharge = 0;
    }

    private void resetCycle() {
        armedAnchor = null;
        resetPendingAction();
    }

    private enum PendingAction {
        NONE,
        CHARGE,
        EXPLODE
    }
}
