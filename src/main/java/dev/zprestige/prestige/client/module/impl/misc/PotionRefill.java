package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.DragSetting;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class PotionRefill extends Module {

    public DragSetting delay;
    public BooleanSetting healtPot;
    public TimerUtil timer;

    public PotionRefill() {
        super("Potion Refill", Category.Misc, "Refills your hotbar with splash potions");
        delay = setting("Delay", 30, 50, 0, 300).description("Delay between each potion swap");
        healtPot = setting("Health Pot Only", false).description("Only refills health potions");
        timer = new TimerUtil();
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().interactionManager == null) return;
        if (!(getMc().currentScreen instanceof InventoryScreen screen)) {
            timer.reset();
            return;
        }
        if (hasEmptySlot() == null || !timer.delay(delay)) return;
        Integer slot = InventoryUtil.INSTANCE.findPotion(9, 36, healtPot.getObject() ? StatusEffects.INSTANT_HEALTH : null);
        if (slot != null) {
            getMc().interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.QUICK_MOVE, getMc().player);
            delay.setValue();
            timer.reset();
        }
    }

    private Integer hasEmptySlot() {
        for (int i = 0; i < 9; ++i) {
            if (getMc().player.getInventory().getStack(i).getItem() == Items.AIR) {
                return i;
            }
        }
        return null;
    }
}
