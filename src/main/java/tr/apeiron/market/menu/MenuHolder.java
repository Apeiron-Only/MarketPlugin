package tr.apeiron.market.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {

    public final MenuType type;
    public final String id; // category id for CATEGORY/PURCHASE/STACK

    public MenuHolder(MenuType type, String id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("Holder sadece kimlik taşır.");
    }
}

