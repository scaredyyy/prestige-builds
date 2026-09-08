package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.DragSetting;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.hit.BlockHitResult;
import dev.zprestige.prestige.client.util.impl.PojavInput;

public class AutoPickaxe extends Module {

    public DragSetting delay;
    public TimerUtil timer;
    public int originalSlot = -1;


    public AutoPickaxe() {
        super("Auto Pickaxe", Category.Misc, "Automatically switches to a pickaxe in your hotbar when mining the block below you");
        delay = setting("Delay", 30.0f, 50.0f, 0.0f, 300.0f).description("Delay between each action");
        timer = new TimerUtil();
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().world == null) return;
        boolean miningBlock = getMc().crosshairTarget instanceof BlockHitResult hit
                && !getMc().world.getBlockState(hit.getBlockPos()).isReplaceable()
                && getMc().world.getBlockState(hit.getBlockPos()).getBlock() != Blocks.BEDROCK
                && PojavInput.isMousePressed(0);
        if (!miningBlock) {
            restoreSlot();
            return;
        }
        if (getMc().player.getMainHandStack().getItem() instanceof PickaxeItem) return;
        if (!timer.delay(delay)) return;

        Integer slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.NETHERITE_PICKAXE);
        if (slot == null) slot = InventoryUtil.INSTANCE.findItemInHotbar(Items.DIAMOND_PICKAXE);
        if (slot == null) return;
        if (originalSlot == -1) originalSlot = getMc().player.getInventory().selectedSlot;
        getMc().player.getInventory().selectedSlot = slot;
        timer.reset();
    }

    private void restoreSlot() {
        if (originalSlot != -1) {
            getMc().player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
    }

    @Override
    public void onDisable() {
        restoreSlot();
    }
}
