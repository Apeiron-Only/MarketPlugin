package tr.apeiron.market.menu;

import java.util.Locale;

public enum CurrencyType {
    MONEY,
    KRISTAL;

    public static CurrencyType from(String s) {
        if (s == null) return MONEY;
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "KRISTAL" -> KRISTAL;
            default -> MONEY;
        };
    }
}

