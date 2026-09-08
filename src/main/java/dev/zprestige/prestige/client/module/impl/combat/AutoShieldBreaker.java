package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.MoveEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.util.impl.InventoryUtil;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoShieldBreaker extends Module {
    private final TimerUtil timer = new TimerUtil();

    public AutoShieldBreaker() {
        super("Auto Shield Breaker", Category.Combat, "Automatically breaks shields by silently attacking them");
    }

    @EventListener
    public void event(MoveEvent event) {
        if (getMc().player != null && getMc().targetedEntity != null && usingShield() && timer.delay(250)) {
            int axeSlot = getAxe();
            if (axeSlot < 0) {
                return;
            }
            if (getMc().player.getInventory().selectedSlot != axeSlot) {
                InventoryUtil.INSTANCE.setCurrentSlot(axeSlot);
                timer.reset();
                return;
            }
            if (getMc().player.getAttackCooldownProgress(0.5f) < 0.9f) return;
            getMc().interactionManager.attackEntity(getMc().player, getMc().targetedEntity);
            getMc().player.swingHand(Hand.MAIN_HAND);
            timer.reset();
        }
    }

   private int getAxe() {
        for (int i = 0; i < 9; ++i) {
            if (getMc().player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean usingShield() {
        return getMc().targetedEntity instanceof PlayerEntity player && player.isUsingItem() && player.getActiveItem().getItem() == Items.SHIELD;
    }
}
