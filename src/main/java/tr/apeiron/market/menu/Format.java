package tr.apeiron.market.menu;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Format {
    private static final DecimalFormat DF;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("tr-TR"));
        sym.setDecimalSeparator('.');
        sym.setGroupingSeparator(',');
        DF = new DecimalFormat("#,##0.##", sym);
    }

    private Format() {}

    public static String money(double d) {
        return DF.format(d);
    }
}

