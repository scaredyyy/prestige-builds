package dev.zprestige.prestige.client.module.impl.combat;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.IntSetting;
import dev.zprestige.prestige.client.setting.impl.ModeSetting;
import dev.zprestige.prestige.client.util.impl.FakeInvScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/** Direct Argon Auto Inventory Totem flow, translated only to Prestige's API. */
public final class AutoTotem extends Module {
    public ModeSetting mode;
    public IntSetting delay;
    public BooleanSetting hotbar;
    public IntSetting totemSlot;
    public BooleanSetting autoSwitch;
    public BooleanSetting forceTotem;
    public BooleanSetting autoOpen;
    public IntSetting stayOpenFor;

    private int clock;
    private int closeClock;

    public AutoTotem() {
        super("Auto Inventory Totem", Category.Combat, "Original Argon Auto Inventory Totem");
        mode = setting("Mode", "Blatant", new String[]{"Blatant", "Random"});
        delay = setting("Delay", 0, 0, 20);
        hotbar = setting("Hotbar", true);
        totemSlot = setting("Totem Slot", 1, 1, 9);
        autoSwitch = setting("Auto Switch", false);
        forceTotem = setting("Force Totem", false);
        autoOpen = setting("Auto Open", false);
        stayOpenFor = setting("Stay Open For", 0, 0, 20);
    }

    @Override
    public void onEnable() {
        clock = -1;
        closeClock = -1;
    }

    @EventListener
    public void event(TickEvent event) {
        if (getMc().player == null || getMc().interactionManager == null) return;
        if (shouldOpenScreen() && autoOpen.getObject()) getMc().setScreen(new FakeInvScreen(getMc().player));

        if (!(getMc().currentScreen instanceof InventoryScreen screen)) {
            clock = -1;
            closeClock = -1;
            return;
        }
        if (clock == -1) clock = delay.getObject();
        if (closeClock == -1) closeClock = stayOpenFor.getObject();
        if (clock > 0) clock--;

        int selected = totemSlot.getObject() - 1;
        if (autoSwitch.getObject()) getMc().player.getInventory().selectedSlot = selected;
        if (clock > 0) return;

        if (!getMc().player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int source = findTotemSlot();
            if (source != -1) {
                getMc().interactionManager.clickSlot(screen.getScreenHandler().syncId, source, 40,
                        SlotActionType.SWAP, getMc().player);
                return;
            }
        }

        if (hotbar.getObject()) {
            ItemStack mainHand = getMc().player.getMainHandStack();
            if (mainHand.isEmpty() || forceTotem.getObject() && !mainHand.isOf(Items.TOTEM_OF_UNDYING)) {
                int source = findTotemSlot();
                if (source != -1) {
                    getMc().interactionManager.clickSlot(screen.getScreenHandler().syncId, source, selected,
                            SlotActionType.SWAP, getMc().player);
                    return;
                }
            }
        }

        if (shouldCloseScreen() && autoOpen.getObject()) {
            if (closeClock != 0) {
                closeClock--;
                return;
            }
            getMc().currentScreen.close();
            closeClock = stayOpenFor.getObject();
        }
    }

    private boolean shouldCloseScreen() {
        if (!(getMc().currentScreen instanceof FakeInvScreen)) return false;
        if (!getMc().player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return false;
        return !hotbar.getObject() || getMc().player.getInventory().getStack(totemSlot.getObject() - 1)
                .isOf(Items.TOTEM_OF_UNDYING);
    }

    private boolean shouldOpenScreen() {
        if (getMc().currentScreen instanceof FakeInvScreen || countTotemsOutsideHotbar() == 0) return false;
        if (!getMc().player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        return hotbar.getObject() && !getMc().player.getInventory().getStack(totemSlot.getObject() - 1)
                .isOf(Items.TOTEM_OF_UNDYING);
    }

    private int findTotemSlot() {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) {
            if (getMc().player.getInventory().getStack(slot).isOf(Items.TOTEM_OF_UNDYING)) slots.add(slot);
        }
        if (slots.isEmpty()) return -1;
        return mode.getObject().equals("Random") ? slots.get(ThreadLocalRandom.current().nextInt(slots.size())) : slots.get(0);
    }

    private int countTotemsOutsideHotbar() {
        int total = 0;
        for (int slot = 9; slot < 36; slot++) {
            if (getMc().player.getInventory().getStack(slot).isOf(Items.TOTEM_OF_UNDYING)) total++;
        }
        return total;
    }
}
