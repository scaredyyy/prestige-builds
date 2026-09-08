package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.DragSetting;
import dev.zprestige.prestige.client.util.impl.TimerUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import dev.zprestige.prestige.client.util.impl.PojavInput;

public class FastEXP extends Module {

    public DragSetting delay;
    public TimerUtil timer;

    public FastEXP() {
        super("Fast EXP", Category.Misc, "Spams EXP bottles");
        delay = setting("Delay", 125, 175, 75, 500).description("Delay between throwing each bottle");
        timer = new TimerUtil();
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().interactionManager == null || getMc().currentScreen != null || !PojavInput.isGameFocused()) return;
        if (PojavInput.isPojav()) {
            if (timer.delay(delay) && hasItem() && PojavInput.isMousePressed(1)) {
                getMc().interactionManager.interactItem(getMc().player, Hand.MAIN_HAND);
                getMc().player.swingHand(Hand.MAIN_HAND);
                delay.setValue();
                timer.reset();
            }
            return;
        }
        if (timer.delay(delay) && !Prestige.Companion.getClickManager().click() && hasItem() && PojavInput.isMousePressed(1)) {
            Prestige.Companion.getClickManager().setClick(1, 0.0f);
            delay.setValue();
            timer.reset();
        }
    }

    private boolean hasItem() {
        return getMc().player != null && getMc().player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
    }
}
