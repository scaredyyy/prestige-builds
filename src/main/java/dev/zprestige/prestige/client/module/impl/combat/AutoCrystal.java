package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.MoveEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.DragSetting;
import dev.zprestige.prestige.client.util.impl.BlockUtil;
import dev.zprestige.prestige.client.util.impl.EntityUtil;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.OneLineUtil;
import dev.zprestige.prestige.client.util.impl.PojavInput;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoCrystal extends Module {
    public DragSetting delay;
    public BooleanSetting silentSwap;
    public BooleanSetting headBob;
    public BooleanSetting inAir;
    public BooleanSetting switchSetting;
    public BooleanSetting damageTick;
    public BooleanSetting pauseOnKill;
    public TimerUtil timer;
    public BlockPos pos;
    private Integer restoreSlot;

    public AutoCrystal() {
        super("Auto Crystal", Category.Combat, "Automatically places and explodes crystals.");
        delay = setting("Delay", 125.0f, 175.0f, 75.0f, 500.0f).description("Delay between each action");
        silentSwap = setting("Silent Swap", false).description("Silently swaps to crystals to place allowing you to hold e.g a sword and crystal at the same time");
        headBob = setting("Head Bob", false).description("Kept for configuration compatibility; attacks remain bound to the current crosshair target.");
        inAir = setting("In Air", false).description("Place crystals while in air, otherwise will time out in air.");
        switchSetting = setting("Switch", false).description("Switches to crystals when the module is enabled.");
        damageTick = setting("Damage Tick", false).description("Only break crystals when they do damage");
        pauseOnKill = setting("Pause On Kill", false).description("Pauses when there is a dead body");
        timer = new TimerUtil();
    }

    @Override
    public void onEnable() {
        restoreSlot = null;
        pos = null;
        if (switchSetting.getObject()) {
            Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.END_CRYSTAL);
            if (slot != null) InventoryUtil.INSTANCE.setCurrentSlot(slot);
        }
        timer.reset();
    }

    @Override
    public void onDisable() {
        restoreOriginalSlot();
        restoreSlot = null;
        pos = null;
    }

    @EventListener
    public void event(MoveEvent event) {
        if (!hasValidGameState() || !PojavInput.isGameFocused()) {
            restoreOriginalSlot();
            return;
        }
        if (pauseOnKill.getObject() && OneLineUtil.isInvalidPlayer()) {
            restoreOriginalSlot();
            return;
        }
        if (getMc().player.isUsingItem()) return;
        if (!getMc().player.isOnGround() && !inAir.getObject()) return;
        if (!timer.delay(delay)) return;
        if (!hasCrystalAvailable()) {
            restoreOriginalSlot();
            return;
        }

        PlayerEntity target = EntityUtil.INSTANCE.getPlayer();
        boolean canAct = damageTick.getObject() || target == null || target.hurtTime == 0;
        if (!canAct) return;

        HitResult hitResult = getMc().crosshairTarget;
        if (hitResult == null) return;
        if (tryBreakTargetedCrystal(hitResult)) {
            finishAction();
            return;
        }
        if (tryPlaceCrystal(hitResult)) finishAction();
    }

    private boolean tryBreakTargetedCrystal(HitResult hitResult) {
        if (!(hitResult instanceof EntityHitResult entityHitResult)) return false;
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof EndCrystalEntity) && !(entity instanceof SlimeEntity) && !(entity instanceof MagmaCubeEntity)) return false;
        if (entity.isRemoved()) return false;

        ClientPlayerInteractionManager interactionManager = getMc().interactionManager;
        interactionManager.attackEntity(getMc().player, entity);
        getMc().player.swingHand(Hand.MAIN_HAND);
        pos = null;
        return true;
    }

    private boolean tryPlaceCrystal(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHitResult)) return false;
        BlockPos blockPos = blockHitResult.getBlockPos();
        var block = getMc().world.getBlockState(blockPos).getBlock();
        if (block != Blocks.BEDROCK && block != Blocks.OBSIDIAN) {
            restoreOriginalSlot();
            return false;
        }

        BlockPos crystalSpace = blockPos.up();
        if (BlockUtil.INSTANCE.isCollidesEntity(crystalSpace)) return false;
        if (!ensureCrystalSelected()) return false;
        if (!InventoryUtil.INSTANCE.isHoldingItem(Items.END_CRYSTAL)) return false;

        ActionResult result = getMc().interactionManager.interactBlock(getMc().player, Hand.MAIN_HAND, blockHitResult);
        if (!result.isAccepted()) return false;
        if (result.shouldSwingHand()) getMc().player.swingHand(Hand.MAIN_HAND);
        pos = blockPos.toImmutable();
        restoreOriginalSlot();
        return true;
    }

    private boolean ensureCrystalSelected() {
        if (InventoryUtil.INSTANCE.isHoldingItem(Items.END_CRYSTAL)) return true;
        if (!silentSwap.getObject()) return false;
        Integer crystalSlot = InventoryUtil.INSTANCE.findItemInHotbar(Items.END_CRYSTAL);
        if (crystalSlot == null) return false;
        if (restoreSlot == null) restoreSlot = getMc().player.getInventory().selectedSlot;
        InventoryUtil.INSTANCE.setCurrentSlot(crystalSlot);
        return getMc().player.getInventory().selectedSlot == crystalSlot
                && InventoryUtil.INSTANCE.isHoldingItem(Items.END_CRYSTAL);
    }

    private void restoreOriginalSlot() {
        if (restoreSlot == null || getMc().player == null) return;
        int slot = restoreSlot;
        restoreSlot = null;
        if (slot < 0 || slot > 8) return;
        InventoryUtil.INSTANCE.setCurrentSlot(slot);
    }

    private boolean hasCrystalAvailable() {
        return InventoryUtil.INSTANCE.isHoldingItem(Items.END_CRYSTAL)
                || silentSwap.getObject() && InventoryUtil.INSTANCE.findItemInHotbar(Items.END_CRYSTAL) != null;
    }

    private boolean hasValidGameState() {
        return getMc().player != null && getMc().world != null
                && getMc().interactionManager != null && getMc().currentScreen == null;
    }

    private void finishAction() {
        timer.reset();
        delay.setValue();
    }
}
