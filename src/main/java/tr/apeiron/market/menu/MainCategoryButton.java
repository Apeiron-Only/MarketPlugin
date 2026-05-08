package tr.apeiron.market.menu;

import java.util.List;

public final class MainCategoryButton {
    public String id;
    public int slot; // 1-27
    public String material;
    public String name;
    public List<String> lore;
    public String openMenu;

    public int slotIndex() {
        return Math.max(0, slot - 1);
    }
}

