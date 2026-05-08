package tr.apeiron.market.menu;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import tr.apeiron.market.ApeironMarketPlugin;
import tr.apeiron.market.Color;
import tr.apeiron.market.kristal.ExternalKristal;

import java.util.*;

public final class MenuListener implements Listener {

    private final ApeironMarketPlugin plugin;

    public MenuListener(ApeironMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof MenuHolder holder)) return;

        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory() instanceof PlayerInventory) return; // menü açıkken oyuncu envanterine tıklamasın

        int slot = e.getSlot();

        switch (holder.type) {
            case MAIN -> handleMain(p, slot);
            case CATEGORY -> handleCategory(p, holder.id, slot);
            case PURCHASE -> handlePurchase(p, slot, e.getClick());
            case STACK -> handleStack(p, slot);
        }
    }

    private void handleMain(Player p, int slot) {
        for (MainCategoryButton b : plugin.getMenuService().mainButtons()) {
            if (b.slotIndex() == slot) {
                plugin.getMenuService().openCategory(p, b.openMenu);
                return;
            }
        }
    }

    private void handleCategory(Player p, String categoryId, int slot) {
        CategoryMenuConfig cat = plugin.getMenuService().category(categoryId);
        if (cat == null) return;
        if (cat.backSlotIndex() == slot) {
            plugin.getMenuService().openMainMenu(p);
            return;
        }
        for (CategoryItemConfig item : cat.items.values()) {
            if (item.slotIndex() == slot) {
                plugin.getMenuService().openPurchase(p, cat.id, item.key);
                return;
            }
        }
    }

    private void handlePurchase(Player p, int slot, ClickType click) {
        MenuSession s = plugin.getMenuService().session(p);
        CategoryMenuConfig cat = plugin.getMenuService().category(s.selectedCategoryId);
        if (cat == null) return;
        CategoryItemConfig item = cat.items.get(s.selectedItemKey);
        if (item == null) return;

        if (slot == 10) changeQty(s, -10);
        else if (slot == 11) changeQty(s, -1);
        else if (slot == 19) changeQty(s, -32);
        else if (slot == 15) changeQty(s, +1);
        else if (slot == 16) changeQty(s, +10);
        else if (slot == 25) changeQty(s, +32);
        else if (slot == 45) {
            if (s.lastCategoryId != null) plugin.getMenuService().openCategory(p, s.lastCategoryId);
            else plugin.getMenuService().openMainMenu(p);
            return;
        } else if (slot == 49 && plugin.getConfig().getBoolean("stack-menu.enabled", true)) {
            plugin.getMenuService().openStackMenu(p, s);
            return;
        } else if (slot == 53) {
            boolean isShift = click.isShiftClick();
            boolean isLeft = click.isLeftClick();
            boolean isRight = click.isRightClick();

            if (isShift && isLeft) {
                buyMax(p, cat, item, s);
            } else if (isShift && isRight) {
                sellAll(p, cat, item, s);
            } else if (isLeft) {
                buy(p, cat, item, s, s.quantity);
            } else if (isRight) {
                sell(p, cat, item, s, s.quantity);
            }
        }

        // menüyü tazele
        plugin.getMenuService().openPurchase(p, s);
    }

    private void handleStack(Player p, int slot) {
        MenuSession s = plugin.getMenuService().session(p);
        if (slot == 22) { // geri dön
            plugin.getMenuService().openPurchase(p, s);
            return;
        }
        if (slot < 10 || slot > 16) return;

        String mode = plugin.getConfig().getString("stack-menu.mode", "STACKS").toUpperCase(Locale.ROOT);
        int human = (slot - 10) + 1;
        int qty = mode.equals("ITEMS") ? human : (human * 64);
        s.quantity = Math.max(1, qty);
        plugin.getMenuService().openPurchase(p, s);
    }

    private void changeQty(MenuSession s, int delta) {
        int next = s.quantity + delta;
        if (next < 1) next = 1;
        s.quantity = next;
    }

    // ----------------- Economy operations -----------------

    private void buy(Player p, CategoryMenuConfig cat, CategoryItemConfig item, MenuSession s, int qty) {
        if (qty <= 0) return;
        double total = item.buy * qty;
        if (total <= 0) return;

        if (cat.currency == CurrencyType.MONEY) {
            Economy eco = plugin.getEconomy();
            if (eco == null) {
                msg(p, "messages.vault-missing");
                return;
            }
            if (eco.getBalance(p) < total) {
                msg(p, "messages.not-enough-money");
                return;
            }
            if (plugin.getConfig().getBoolean("purchase.shift-buy-check-inventory", true)) {
                if (!canFit(p.getInventory(), Material.matchMaterial(item.material), qty)) {
                    msg(p, "messages.inventory-full");
                    return;
                }
            }
            eco.withdrawPlayer(p, total);
            giveProduct(p, item, qty);
            msg(p, "messages.bought", Map.of(
                    "%amount%", String.valueOf(qty),
                    "%item%", strip(item.name),
                    "%price%", Format.money(total)
            ));
            return;
        }

        // KRISTAL
        long kristalTotal = (long) Math.ceil(total);
        String placeholder = plugin.getConfig().getString("kristal.placeholder.balance", "%kristal_balance%");
        long bal = ExternalKristal.balance(p, placeholder);
        if (bal >= 0 && bal < kristalTotal) {
            msg(p, "messages.not-enough-kristal");
            return;
        }
        ExternalKristal.take(p, plugin.getConfig().getString("kristal.commands.take", "kristal take %player% %amount%"), kristalTotal);
        giveProduct(p, item, qty);
        msg(p, "messages.bought", Map.of(
                "%amount%", String.valueOf(qty),
                "%item%", strip(item.name),
                "%price%", String.valueOf(kristalTotal) + " kristal"
        ));
    }

    private void buyMax(Player p, CategoryMenuConfig cat, CategoryItemConfig item, MenuSession s) {
        if (item.buy <= 0) return;
        int maxByMoney;

        if (cat.currency == CurrencyType.MONEY) {
            Economy eco = plugin.getEconomy();
            if (eco == null) {
                msg(p, "messages.vault-missing");
                return;
            }
            maxByMoney = (int) Math.floor(eco.getBalance(p) / item.buy);
        } else {
            String placeholder = plugin.getConfig().getString("kristal.placeholder.balance", "%kristal_balance%");
            long bal = ExternalKristal.balance(p, placeholder);
            if (bal < 0) {
                // PlaceholderAPI yoksa max alamayiz
                msg(p, "messages.not-enough-kristal");
                return;
            }
            maxByMoney = (int) Math.floor(bal / item.buy);
        }

        if (maxByMoney < 1) {
            if (cat.currency == CurrencyType.MONEY) msg(p, "messages.not-enough-money");
            else msg(p, "messages.not-enough-kristal");
            return;
        }

        Material mat = Material.matchMaterial(item.material);
        int maxByInv = plugin.getConfig().getBoolean("purchase.shift-buy-check-inventory", true)
                ? maxFit(p.getInventory(), mat)
                : Integer.MAX_VALUE;

        int qty = Math.max(1, Math.min(maxByMoney, maxByInv));
        buy(p, cat, item, s, qty);
    }

    private void sell(Player p, CategoryMenuConfig cat, CategoryItemConfig item, MenuSession s, int qty) {
        if (qty <= 0) return;
        if (item.sell <= 0) {
            msg(p, "messages.cannot-sell");
            return;
        }
        Material mat = Material.matchMaterial(item.material);
        if (mat == null) mat = Material.STONE;

        ItemStack similarTarget = shiftSellModeSimilar() ? buildSimilarTarget(item, mat) : null;
        int removed = removeItems(p.getInventory(), mat, qty, similarTarget);
        if (removed <= 0) return;

        double total = item.sell * removed;
        if (cat.currency == CurrencyType.MONEY) {
            Economy eco = plugin.getEconomy();
            if (eco == null) {
                msg(p, "messages.vault-missing");
                return;
            }
            eco.depositPlayer(p, total);
        } else {
            long give = (long) Math.floor(total);
            ExternalKristal.give(p, plugin.getConfig().getString("kristal.commands.give", "kristal give %player% %amount%"), give);
        }

        msg(p, "messages.sold", Map.of(
                "%amount%", String.valueOf(removed),
                "%item%", strip(item.name),
                "%price%", Format.money(total)
        ));
    }

    private void sellAll(Player p, CategoryMenuConfig cat, CategoryItemConfig item, MenuSession s) {
        if (item.sell <= 0) {
            msg(p, "messages.cannot-sell");
            return;
        }
        Material mat = Material.matchMaterial(item.material);
        if (mat == null) mat = Material.STONE;

        ItemStack similarTarget = shiftSellModeSimilar() ? buildSimilarTarget(item, mat) : null;
        int totalCount = countItems(p.getInventory(), mat, similarTarget);
        if (totalCount <= 0) return;

        sell(p, cat, item, s, totalCount);
    }

    private void giveProduct(Player p, CategoryItemConfig item, int qty) {
        // Kristal menüsünde çoğunlukla komutla ürün veriliyor
        if (item.commands != null && !item.commands.isEmpty()) {
            for (String cmd : item.commands) {
                String c = cmd.replace("%player%", p.getName()).replace("%amount%", String.valueOf(qty));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            }
            return;
        }

        Material mat = Material.matchMaterial(item.material);
        if (mat == null) mat = Material.STONE;
        ItemStack stack = new ItemStack(mat);
        stack.setAmount(Math.min(64, qty));

        int left = qty;
        while (left > 0) {
            int give = Math.min(64, left);
            ItemStack s = new ItemStack(mat, give);
            Map<Integer, ItemStack> overflow = p.getInventory().addItem(s);
            if (!overflow.isEmpty()) break;
            left -= give;
        }
    }

    // ----------------- Inventory helpers -----------------

    private boolean canFit(PlayerInventory inv, Material mat, int qty) {
        if (mat == null) mat = Material.STONE;
        int space = 0;
        for (ItemStack is : inv.getStorageContents()) {
            if (is == null || is.getType().isAir()) space += 64;
            else if (is.getType() == mat && is.getAmount() < is.getMaxStackSize()) {
                space += (is.getMaxStackSize() - is.getAmount());
            }
            if (space >= qty) return true;
        }
        return space >= qty;
    }

    private int maxFit(PlayerInventory inv, Material mat) {
        if (mat == null) mat = Material.STONE;
        int space = 0;
        for (ItemStack is : inv.getStorageContents()) {
            if (is == null || is.getType().isAir()) space += 64;
            else if (is.getType() == mat && is.getAmount() < is.getMaxStackSize()) {
                space += (is.getMaxStackSize() - is.getAmount());
            }
        }
        return space;
    }

    private int countItems(PlayerInventory inv, Material mat, ItemStack similarTarget) {
        int count = 0;
        for (ItemStack is : inv.getStorageContents()) {
            if (is == null || is.getType().isAir()) continue;
            if (is.getType() != mat) continue;
            if (similarTarget != null && !is.isSimilar(similarTarget)) continue;
            count += is.getAmount();
        }
        return count;
    }

    private int removeItems(PlayerInventory inv, Material mat, int qty, ItemStack similarTarget) {
        int need = qty;
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType().isAir()) continue;
            if (is.getType() != mat) continue;
            if (similarTarget != null && !is.isSimilar(similarTarget)) continue;

            int take = Math.min(need, is.getAmount());
            is.setAmount(is.getAmount() - take);
            need -= take;
            if (is.getAmount() <= 0) contents[i] = null;
            if (need <= 0) break;
        }
        inv.setStorageContents(contents);
        return qty - need;
    }

    private ItemStack buildSimilarTarget(CategoryItemConfig item, Material mat) {
        ItemStack target = new ItemStack(mat);
        ItemMeta meta = target.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Color.t(item.name));
            meta.setLore(Color.tl(item.lore));
            target.setItemMeta(meta);
        }
        target.setAmount(1);
        return target;
    }

    private boolean shiftSellModeSimilar() {
        String mode = plugin.getConfig().getString("purchase.shift-sell-count-mode", "MATERIAL").toUpperCase(Locale.ROOT);
        return mode.equals("SIMILAR");
    }

    // ----------------- Messages -----------------

    private void msg(Player p, String path) {
        msg(p, path, Map.of());
    }

    private void msg(Player p, String path, Map<String, String> repl) {
        String raw = plugin.getConfig().getString(path, "");
        String out = raw;
        for (Map.Entry<String, String> e : repl.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        if (out.isBlank()) return;
        p.sendMessage(plugin.prefix() + Color.t(out));
    }

    private String strip(String colored) {
        if (colored == null) return "";
        return colored.replaceAll("(?i)&[0-9A-FK-OR]", "").trim();
    }
}

