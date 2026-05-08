package tr.apeiron.market.kristal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Locale;

public final class ExternalKristal {
    private ExternalKristal() {}

    public static long balance(Player p, String placeholder) {
        String out = applyPlaceholders(p, placeholder);
        if (out == null) return -1;
        out = out.replace(",", "").trim();
        try {
            return Long.parseLong(out);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void take(Player p, String commandTemplate, long amount) {
        String cmd = commandTemplate
                .replace("%player%", p.getName())
                .replace("%amount%", String.valueOf(amount));
        dispatchConsole(cmd);
    }

    public static void give(Player p, String commandTemplate, long amount) {
        String cmd = commandTemplate
                .replace("%player%", p.getName())
                .replace("%amount%", String.valueOf(amount));
        dispatchConsole(cmd);
    }

    private static void dispatchConsole(String cmd) {
        String c = cmd.trim();
        if (c.startsWith("/")) c = c.substring(1);
        if (c.isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    // PlaceholderAPI varsa kullanir; yoksa stringi aynen dondurur.
    private static String applyPlaceholders(Player p, String text) {
        if (text == null) return null;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method m = papi.getMethod("setPlaceholders", Player.class, String.class);
            Object r = m.invoke(null, p, text);
            return (r == null) ? null : String.valueOf(r);
        } catch (Throwable t) {
            return text;
        }
    }

    public static boolean isKristalCurrency(String s) {
        if (s == null) return false;
        return s.toUpperCase(Locale.ROOT).equals("KRISTAL");
    }
}

