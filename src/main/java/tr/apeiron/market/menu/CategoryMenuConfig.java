package tr.apeiron.market.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CategoryMenuConfig {
    public String id;
    public String title;
    public int size;
    public CurrencyType currency;

    public boolean fillerEnabled;
    public String fillerMaterial;
    public String fillerName;

    public int backSlot;
    public String backMaterial;
    public String backName;
    public List<String> backLore;

    public final Map<String, CategoryItemConfig> items = new HashMap<>();

    public int backSlotIndex() {
        return Math.max(0, backSlot - 1);
    }
}

