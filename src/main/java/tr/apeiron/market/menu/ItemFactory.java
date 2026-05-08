package tr.apeiron.market.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tr.apeiron.market.ApeironMarketPlugin;
import tr.apeiron.market.Color;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.*;

public final class ItemFactory {
    private ItemFactory() {}

    public static ItemStack simple(Material mat, String name, List<String> lore) {
        Material m = (mat == null) ? Material.STONE : mat;
        ItemStack it = new ItemStack(m);
        return withNameAndLore(it, name, lore);
    }

    public static ItemStack withNameAndLore(ItemStack it, String name, List<String> lore) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(Color.t(name));
            if (lore != null) meta.setLore(Color.tl(lore));
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack fromConfig(String material, String name, List<String> lore, Map<String, String> replacements) {
        Material mat = Material.matchMaterial(material == null ? "STONE" : material);
        if (mat == null) mat = Material.STONE;

        String n = apply(name, replacements);
        List<String> l = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) l.add(apply(line, replacements));
        }
        return withNameAndLore(new ItemStack(mat), n, l);
    }

    public static ItemStack headButton(ApeironMarketPlugin plugin, HeadColor color, String name, List<String> lore) {
        String basePath = "heads." + (color == HeadColor.RED ? "red" : "green");
        String material = plugin.getConfig().getString(basePath + ".material", "PLAYER_HEAD");
        String texture = plugin.getConfig().getString(basePath + ".texture", "");

        Material mat = Material.matchMaterial(material);
        if (mat == null) mat = Material.PLAYER_HEAD;
        ItemStack it = new ItemStack(mat);
        withNameAndLore(it, name, lore);

        if (it.getItemMeta() instanceof SkullMeta skull && texture != null && !texture.isBlank()) {
            applyTexture(skull, texture);
            it.setItemMeta(skull);
        }
        return it;
    }

    private static String apply(String s, Map<String, String> repl) {
        if (s == null) return "";
        String out = s;
        if (repl != null) {
            for (Map.Entry<String, String> e : repl.entrySet()) {
                out = out.replace(e.getKey(), e.getValue());
            }
        }
        return Color.t(out);
    }

    // Spigot uyumlu texture basma (reflection)
    private static void applyTexture(SkullMeta meta, String base64) {
        try {
            String val = normalizeTextureValue(base64);
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", val));
            meta.setPlayerProfile(profile);
        } catch (Throwable ignored) {
        }
    }

    // Kabul ettigimiz formatlar:
    // - Direkt base64 value
    // - Direkt url (http/https)
    // - JSON (textures->SKIN->url) -> base64'a cevirir
    private static String normalizeTextureValue(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.isEmpty()) return "";

        if (s.startsWith("http://") || s.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + s + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        if (s.startsWith("{") && s.contains("\"textures\"")) {
            return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        }
        return s; // base64 varsay
    }
}

