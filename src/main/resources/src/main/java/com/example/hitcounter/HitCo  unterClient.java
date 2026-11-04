package com.example.hitcounter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HitCounterClient implements ClientModInitializer {

    private static final TagKey<EntityType<?>> SMITE_TARGETS =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("sensitive_to_smite"));

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int AQUA = 0xFF55FFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int RED = 0xFFFF5555;

    private record Weapon(String name, float baseDamage, boolean enchanted, boolean smite) {}

    private static final float SHARPNESS_5 = 0.5f * 5 + 0.5f;
    private static final float SMITE_5 = 2.5f * 5;

    private static final List<Weapon> WEAPONS = List.of(
            new Weapon("Wooden Sword", 4f, false, false),
            new Weapon("Stone Sword", 5f, false, false),
            new Weapon("Iron Sword", 6f, false, false),
            new Weapon("Golden Sword", 4f, false, false),
            new Weapon("Diamond Sword", 7f, false, false),
            new Weapon("Diamond + Sharpness V", 7f + SHARPNESS_5, true, false),
            new Weapon("Diamond + Smite V", 7f + SMITE_5, true, true),
            new Weapon("Netherite Sword", 8f, false, false),
            new Weapon("Netherite + Sharpness V", 8f + SHARPNESS_5, true, false),
            new Weapon("Netherite + Smite V", 8f + SMITE_5, true, true)
    );

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(HitCounterClient::render);
    }

    private static float afterArmor(float damage, float armor, float toughness) {
        float f = 2.0f + toughness / 4.0f;
        float g = Mth.clamp(armor - damage / f, armor * 0.2f, 20.0f);
        return damage * (1.0f - g / 25.0f);
    }

    private static int hitsToKill(float health, float damage) {
        if (damage <= 0f) return Integer.MAX_VALUE;
        return Math.max(1, (int) Math.ceil(health / damage - 0.0001f));
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target) || target instanceof ArmorStand || !target.isAlive()) return;

        float health = target.getHealth();
        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        boolean undead = target.getType().is(SMITE_TARGETS);

        String header = String.format(Locale.ROOT, "%s  HP %.1f/%.1f  Armor %.0f",
                target.getName().getString(), health, target.getMaxHealth(), armor);

        List<String> names = new ArrayList<>();
        List<String> counts = new ArrayList<>();
        List<Integer> nameColors = new ArrayList<>();
        List<Integer> countColors = new ArrayList<>();

        for (Weapon w : WEAPONS) {
            if (w.smite() && !undead) continue;
            int hits = hitsToKill(health, afterArmor(w.baseDamage(), armor, toughness));
            names.add(w.name());
            counts.add(hits == 1 ? "1 hit" : hits + " hits");
            nameColors.add(w.enchanted() ? AQUA : WHITE);
            countColors.add(hits <= 3 ? GREEN : hits <= 8 ? YELLOW : RED);
        }

        int pad = 4, lineH = 10;
        int nameW = 0, countW = 0;
        for (String s : names) nameW = Math.max(nameW, mc.font.width(s));
        for (String s : counts) countW = Math.max(countW, mc.font.width(s));
        int boxW = Math.max(mc.font.width(header), nameW + 12 + countW) + pad * 2;
        int boxH = pad * 2 + lineH * (names.size() + 1) + 2;

        int x = mc.getWindow().getGuiScaledWidth() / 2 + 16;
        int y = mc.getWindow().getGuiScaledHeight() / 2 - boxH / 2;
        if (x + boxW > mc.getWindow().getGuiScaledWidth()) x = mc.getWindow().getGuiScaledWidth() - boxW - 2;

        g.fill(x, y, x + boxW, y + boxH, 0xA0000000);
        g.drawString(mc.font, header, x + pad, y + pad, GRAY, true);
        int ly = y + pad + lineH + 2;
        for (int i = 0; i < names.size(); i++) {
            g.drawString(mc.font, names.get(i), x + pad, ly, nameColors.get(i), true);
            g.drawString(mc.font, counts.get(i), x + pad + nameW + 12, ly, countColors.get(i), true);
            ly += lineH;
        }
    }
}
