package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.MoveEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.util.impl.BlockUtil;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import dev.zprestige.prestige.client.util.impl.PojavInput;

public class GhostObby extends Module {
    public TimerUtil field559 = new TimerUtil();

    public GhostObby() {
        super("Ghost Obby", Category.Combat, "Automatically places obsidian when looking at a block while holding a totem");
    }

    @EventListener
    public void event(MoveEvent event) {
        if (getMc().currentScreen != null || !PojavInput.isGameFocused()) {
            return;
        }
        if (!field559.delay(200)) {
            return;
        }
        if (getMc().player.getMainHandStack().getItem().getComponents().get(net.minecraft.component.DataComponentTypes.FOOD) != null || getMc().player.getMainHandStack().isStackable()) {
            return;
        }
        if (!PojavInput.isMousePressed(1)) {
            return;
        }
        HitResult hitResult = getMc().crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
        if (getMc().world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN && getMc().world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && !BlockUtil.INSTANCE.isCollidesEntity(blockPos)) {
            Integer obsidianSlot = InventoryUtil.INSTANCE.findBlockSlot(Blocks.OBSIDIAN);
            if (obsidianSlot == null) return;
            // Slot update must reach server before placement packet.  Old code
            // switched, placed, and switched back in one event.
            if (getMc().player.getInventory().selectedSlot != obsidianSlot) {
                InventoryUtil.INSTANCE.setCurrentSlot(obsidianSlot);
                field559.reset();
                return;
            }
            getMc().interactionManager.interactBlock(getMc().player, getMc().player.getActiveHand(), (BlockHitResult) hitResult);
            getMc().player.swingHand(getMc().player.getActiveHand());
            field559.reset();
        }
    }
}
