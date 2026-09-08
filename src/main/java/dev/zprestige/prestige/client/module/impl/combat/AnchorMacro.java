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

/** Exact Argon Anchor Macro sequence, translated only to Prestige's event/settings API. */
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
    // Pojav may clear the held use binding when vanilla consumes the
    // glowstone action. Keep exactly one already-started anchor cycle alive
    // while the player is still aiming at that same anchor.
    private BlockPos armedAnchor;

    public AnchorMacro() {
        super("Anchor Macro", Category.Combat, "Original Argon Anchor Macro");
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
        switchClock = glowstoneClock = explodeClock = 0;
        armedAnchor = null;
    }

    @Override
    public void onDisable() {
        ownedAnchors.clear();
        armedAnchor = null;
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().world == null || getMc().interactionManager == null || getMc().currentScreen != null) return;
        boolean physicalUse = PojavInput.isArgonMousePressed(1)
                || PojavInput.isTrackedMousePressed(1)
                || PojavInput.isMousePressed(1);
        boolean eatingOrShielding = getMc().player.getMainHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getMainHandStack().getItem() instanceof ShieldItem
                || getMc().player.getOffHandStack().contains(DataComponentTypes.FOOD)
                || getMc().player.getOffHandStack().getItem() instanceof ShieldItem;
        if (!(getMc().crosshairTarget instanceof BlockHitResult hit)
                || getMc().world.getBlockState(hit.getBlockPos()).getBlock() != Blocks.RESPAWN_ANCHOR) {
            armedAnchor = null;
            return;
        }
        if (eatingOrShielding && physicalUse && !whileUse.getObject()) return;
        if (physicalUse) armedAnchor = hit.getBlockPos().toImmutable();
        boolean use = physicalUse || hit.getBlockPos().equals(armedAnchor);
        if (!use) return;
        // Stop vanilla's second click without losing the tracked physical hold.
        getMc().options.useKey.setPressed(false);
        if (onlyOwn.getObject() && !ownedAnchors.contains(hit.getBlockPos())) return;

        int charges = getMc().world.getBlockState(hit.getBlockPos()).get(Properties.CHARGES);
        if (charges == 0) charge(hit);
        else if (!onlyCharge.getObject()) explode(hit);
        else armedAnchor = null;
    }

    @EventListener
    public void event(PacketSendEvent event) {
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)
                || getMc().player == null || getMc().world == null
                || !getMc().player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
        BlockHitResult hit = packet.getBlockHitResult();
        ownedAnchors.add(getMc().world.getBlockState(hit.getBlockPos()).isReplaceable()
                ? hit.getBlockPos() : hit.getBlockPos().offset(hit.getSide()));
    }

    private void charge(BlockHitResult hit) {
        if (!roll(placeChance)) return;
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (switchClock++ != switchDelay.getObject()) return;
            if (roll(switchChance)) {
                switchClock = 0;
                Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.GLOWSTONE);
                if (slot != null) selectSlot(slot);
            }
        }
        // Argon continues immediately after a local hotbar swap.  Returning
        // here added an unnecessary 50 ms Pojav tick before every charge.
        if (!getMc().player.getMainHandStack().isOf(Items.GLOWSTONE)) return;
        if (glowstoneClock++ != glowstoneDelay.getObject() || !roll(glowstoneChance)) return;
        glowstoneClock = 0;
        useAnchor(hit);
    }

    private void explode(BlockHitResult hit) {
        int slot = explodeSlot.getObject() - 1;
        if (getMc().player.getInventory().selectedSlot != slot) {
            if (switchClock++ != switchDelay.getObject()) return;
            if (roll(switchChance)) {
                switchClock = 0;
                selectSlot(slot);
            }
        }
        // Same Argon behavior: swap and use during this very tick.
        if (getMc().player.getInventory().selectedSlot != slot) return;
        if (explodeClock++ != explodeDelay.getObject() || !roll(explodeChance)) return;
        explodeClock = 0;
        useAnchor(hit);
        ownedAnchors.remove(hit.getBlockPos());
        armedAnchor = null;
    }

    private boolean roll(IntSetting chance) {
        return RandomUtil.INSTANCE.randomInRange(1, 100) <= chance.getObject();
    }

    private void selectSlot(int slot) {
        // Match Argon exactly. interactBlock performs Minecraft's normal
        // selected-slot synchronisation immediately before it sends the use
        // packet. Sending a second manual update here can make Pojav/server
        // state disagree after the glowstone swap.
        getMc().player.getInventory().selectedSlot = slot;
    }

    private void useAnchor(BlockHitResult hit) {
        // Prestige's global Android throttle is not part of Argon.  Let this
        // direct Argon interaction reach Minecraft unchanged.
        PojavPacketSafety.runTrustedBlockAction(() -> {
            var result = getMc().interactionManager.interactBlock(getMc().player, Hand.MAIN_HAND, hit);
            if (result.isAccepted() && result.shouldSwingHand()) getMc().player.swingHand(Hand.MAIN_HAND);
        });
    }
}
