package dev.zprestige.prestige.client.util.impl;

import dev.zprestige.prestige.client.util.MC;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;

public class InventoryUtil implements MC {
    public static final InventoryUtil INSTANCE = new InventoryUtil();

    public ArrayList<Integer> findItemSlot(Item item, boolean includeHotbar) {
        ArrayList<Integer> slots = new ArrayList<>();

        if (getMc().player == null || item == null) {
            return slots;
        }

        int startSlot = includeHotbar ? 0 : 9;

        for (int slot = startSlot; slot < 36; slot++) {
            if (getMc().player.getInventory().getStack(slot).isOf(item)) {
                slots.add(slot);
            }
        }

        return slots;
    }

    /**
     * Selects a valid hotbar slot immediately.
     *
     * @return true if the requested slot was already selected before this call.
     */
    public boolean setCurrentSlot(int slot) {
        if (!isValidHotbarSlot(slot) || getMc().player == null) {
            return false;
        }

        if (getMc().player.getInventory().selectedSlot == slot) {
            return true;
        }

        getMc().player.getInventory().selectedSlot = slot;
        return false;
    }

    public boolean isCurrentSlot(int slot) {
        return getMc().player != null
                && isValidHotbarSlot(slot)
                && getMc().player.getInventory().selectedSlot == slot;
    }

    public boolean isHoldingItem(Item item) {
        return getMc().player != null
                && item != null
                && getMc().player.getMainHandStack().isOf(item);
    }

    public boolean isHoldingOffhandItem(Item item) {
        return getMc().player != null
                && item != null
                && getMc().player.getOffHandStack().isOf(item);
    }

    public Integer findBlockSlot(Block block) {
        if (getMc().player == null || block == null) {
            return null;
        }

        Item item = block.asItem();

        for (int slot = 0; slot < 9; slot++) {
            if (getMc().player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }

        return null;
    }

    public Integer findItemInHotbar(Item item) {
        if (getMc().player == null || item == null) {
            return null;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (getMc().player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }

        return null;
    }

    public Integer findPotion(
            int startSlot,
            int endSlot,
            RegistryEntry<StatusEffect> statusEffect
    ) {
        if (getMc().player == null) {
            return null;
        }

        int start = Math.max(0, startSlot);
        int end = Math.min(36, endSlot);

        if (start >= end) {
            return null;
        }

        for (int slot = start; slot < end; slot++) {
            ItemStack stack = getMc().player.getInventory().getStack(slot);

            if (!stack.isOf(Items.SPLASH_POTION)) {
                continue;
            }

            if (statusEffect == null || hasStatusEffect(stack, statusEffect)) {
                return slot;
            }
        }

        return null;
    }

    public boolean hasStatusEffect(
            ItemStack itemStack,
            RegistryEntry<StatusEffect> statusEffect
    ) {
        if (itemStack == null || statusEffect == null) {
            return false;
        }

        PotionContentsComponent contents = itemStack.getOrDefault(
                DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT
        );

        for (StatusEffectInstance effect : contents.getEffects()) {
            if (effect.getEffectType().equals(statusEffect)) {
                return true;
            }
        }

        return false;
    }

    public boolean performSlotAction(
            int slot,
            int button,
            SlotActionType actionType
    ) {
        if (getMc().player == null
                || getMc().interactionManager == null
                || actionType == null) {
            return false;
        }

        if (!(getMc().currentScreen instanceof InventoryScreen screen)) {
            return false;
        }

        if (slot < 0 || slot >= screen.getScreenHandler().slots.size()) {
            return false;
        }

        getMc().interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slot,
                button,
                actionType,
                getMc().player
        );

        return true;
    }

    private boolean isValidHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8;
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }
}
