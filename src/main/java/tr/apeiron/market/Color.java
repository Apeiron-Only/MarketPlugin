package tr.apeiron.market;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public final class Color {
    private Color() {}

    public static String t(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> tl(List<String> list) {
        if (list == null) return List.of();
        return list.stream().map(Color::t).collect(Collectors.toList());
    }
}

