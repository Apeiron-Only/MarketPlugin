package tr.apeiron.market.menu;

import java.util.List;

public final class CategoryItemConfig {
    public String key;
    public int slot; // 1-27
    public String material;
    public String name;
    public List<String> lore;
    public int amount;
    public double buy;
    public double sell;
    public List<String> commands; // kristal menüsünde satın alımda çalıştırılabilir

    public int slotIndex() {
        return Math.max(0, slot - 1);
    }
}

