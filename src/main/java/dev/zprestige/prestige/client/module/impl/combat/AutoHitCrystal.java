package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.IntSetting;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.PojavInput;
import dev.zprestige.prestige.client.util.impl.PojavPacketSafety;
import dev.zprestige.prestige.client.util.impl.RandomUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** Argon's direct Auto Hit Crystal sequence, adapted to Prestige's API. */
public final class AutoHitCrystal extends Module {
    public BooleanSetting checkPlace;
    public IntSetting switchDelay;
    public IntSetting switchChance;
    public IntSetting placeDelay;
    public IntSetting placeChance;
    public BooleanSetting workWithTotem;
    public BooleanSetting workWithCrystal;
    public BooleanSetting clickSimulation;
    public BooleanSetting swordSwap;

    private int placeClock;
    private int switchClock;
    private boolean active;
    private boolean crystalling;
    private boolean crystalSelected;

    public AutoHitCrystal() {
        super("Auto Hit Crystal", Category.Combat, "Original Argon Auto Hit Crystal flow");
        checkPlace = setting("Check Place", false);
        switchDelay = setting("Switch Delay", 0, 0, 20);
        switchChance = setting("Switch Chance", 100, 0, 100);
        placeDelay = setting("Place Delay", 0, 0, 20);
        placeChance = setting("Place Chance", 100, 0, 100);
        workWithTotem = setting("Work With Totem", false);
        workWithCrystal = setting("Work With Crystal", false);
        clickSimulation = setting("Click Simulation", false).description("No Android mouse simulation");
        swordSwap = setting("Sword Swap", true);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().world == null || getMc().interactionManager == null || getMc().currentScreen != null) return;

        // Argon uses GLFW right mouse. Pojav's binding is fallback only.
        if (!(PojavInput.isArgonMousePressed(1) || PojavInput.isMousePressed(1))) {
            reset();
            return;
        }

        if (getMc().crosshairTarget instanceof BlockHitResult hit
                && !active
                && checkPlace.getObject()
                && !getMc().world.getBlockState(hit.getBlockPos()).isReplaceable()) return;

        ItemStack mainHand = getMc().player.getMainHandStack();
        boolean permitted = mainHand.getItem() instanceof SwordItem
                || workWithTotem.getObject() && mainHand.isOf(Items.TOTEM_OF_UNDYING)
                || workWithCrystal.getObject() && mainHand.isOf(Items.END_CRYSTAL);
        if (!permitted && !active) return;

        if (!active && swordSwap.getObject() && getMc().crosshairTarget instanceof BlockHitResult hit) {
            Block block = getMc().world.getBlockState(hit.getBlockPos()).getBlock();
            crystalling = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
        }
        active = true;

        if (!crystalling) placeObsidian();
        if (crystalling) placeCrystal();
    }

    private void placeObsidian() {
        if (!(getMc().crosshairTarget instanceof BlockHitResult hit) || hit.getType() == HitResult.Type.MISS) return;
        Block block = getMc().world.getBlockState(hit.getBlockPos()).getBlock();
        if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
            crystalling = true;
            return;
        }
        if (block == Blocks.RESPAWN_ANCHOR
                && getMc().world.getBlockState(hit.getBlockPos()).contains(net.minecraft.state.property.Properties.CHARGES)
                && getMc().world.getBlockState(hit.getBlockPos()).get(net.minecraft.state.property.Properties.CHARGES) > 0) return;

        getMc().options.useKey.setPressed(false);
        if (!holdOrSwap(Items.OBSIDIAN) || !consumePlaceClock() || !roll(placeChance)) return;
        interact(hit);
        placeClock = placeDelay.getObject();
        crystalling = true;
    }

    private void placeCrystal() {
        if (!holdOrSwap(Items.END_CRYSTAL)) return;
        if (!(getMc().crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        Block block = getMc().world.getBlockState(hit.getBlockPos()).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return;

        // Argon's AutoCrystal onTick placement: same crosshair hit, normal
        // client slot sync, no confirmation-state pause.
        getMc().options.useKey.setPressed(false);
        interact(hit);
        crystalSelected = true;
    }

    private boolean holdOrSwap(Item item) {
        if (getMc().player.getMainHandStack().isOf(item)) return true;
        if (switchClock > 0) {
            switchClock--;
            return false;
        }
        if (!roll(switchChance)) return false;
        Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(item);
        if (slot == null) return false;
        // Argon changes this locally; interactBlock performs the vanilla sync.
        getMc().player.getInventory().selectedSlot = slot;
        switchClock = switchDelay.getObject();
        return getMc().player.getMainHandStack().isOf(item);
    }

    private boolean consumePlaceClock() {
        if (placeClock <= 0) return true;
        placeClock--;
        return false;
    }

    private boolean roll(IntSetting chance) {
        return RandomUtil.INSTANCE.randomInRange(1, 100) <= chance.getObject();
    }

    private void interact(BlockHitResult hit) {
        PojavPacketSafety.runTrustedBlockAction(() -> {
            ActionResult result = getMc().interactionManager.interactBlock(getMc().player, Hand.MAIN_HAND, hit);
            if (result.isAccepted() && result.shouldSwingHand()) getMc().player.swingHand(Hand.MAIN_HAND);
        });
    }

    private void reset() {
        placeClock = placeDelay == null ? 0 : placeDelay.getObject();
        switchClock = switchDelay == null ? 0 : switchDelay.getObject();
        active = false;
        crystalling = false;
        crystalSelected = false;
    }
}
