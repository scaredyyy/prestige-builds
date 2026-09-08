package dev.zprestige.prestige.client.util.impl;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/** Direct 1.21 port of Argon's non-interactive inventory screen. */
public final class FakeInvScreen extends InventoryScreen {
    public FakeInvScreen(PlayerEntity player) {
        super(player);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}
