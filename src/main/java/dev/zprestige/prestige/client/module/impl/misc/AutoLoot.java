package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BindSetting;
import dev.zprestige.prestige.client.setting.impl.DragSetting;
import dev.zprestige.prestige.client.setting.impl.IntSetting;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.TimerUtil;

import java.util.List;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import dev.zprestige.prestige.client.util.impl.PojavInput;

public class AutoLoot extends Module {

    public DragSetting delay;
    public IntSetting minTotems;
    public IntSetting minPearls;
    public BindSetting keyBind;
    public TimerUtil timer;

    public AutoLoot() {
        super("AutoLoot", Category.Misc, "Helps with looting items from the ground");
        delay = setting("Delay", 30, 50, 0, 300).description("Delay between each action");
        minTotems = setting("Min Totems", 0, 1, 20).description("Minimum amount of totems");
        minPearls = setting("Min Pearls", 5, 1, 10).description("Minimum amount of pearls (stacks)");
        keyBind = setting("Key", -1);
        timer = new TimerUtil();
    }

    @EventListener
    public void event(TickEvent event) {
        if (!(getMc().currentScreen instanceof InventoryScreen screen)
                || getMc().player == null || getMc().interactionManager == null
                || keyBind.getObject() == -1 || !PojavInput.isKeyPressed(keyBind.getObject())
                || !timer.delay(delay)) return;

        List<Integer> totems = InventoryUtil.INSTANCE.findItemSlot(Items.TOTEM_OF_UNDYING, true);
        if (totems.size() > minTotems.getObject()) {
            dropStack(screen, totems.get(0));
            return;
        }
        List<Integer> pearls = InventoryUtil.INSTANCE.findItemSlot(Items.ENDER_PEARL, true);
        if (pearls.size() > minPearls.getObject()) dropStack(screen, pearls.get(0));
    }

    private void dropStack(InventoryScreen screen, int inventorySlot) {
        int handlerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        getMc().interactionManager.clickSlot(screen.getScreenHandler().syncId, handlerSlot, 1,
                SlotActionType.THROW, getMc().player);
        delay.setValue();
        timer.reset();
    }
}
