package tr.apeiron.market.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tr.apeiron.market.ApeironMarketPlugin;
import tr.apeiron.market.Color;

import java.io.File;
import java.util.*;

public final class MenuService {

    private final ApeironMarketPlugin plugin;

    private MainMenuConfig mainMenu;
    private final Map<String, CategoryMenuConfig> categories = new HashMap<>();
    private final Map<UUID, MenuSession> sessions = new HashMap<>();

    public MenuService(ApeironMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadAll() {
        ensureDefaults();
        this.mainMenu = loadMain();
        this.categories.clear();
        loadCategory("end");
        loadCategory("nether");
        loadCategory("totem");
        loadCategory("biftek");
        loadCategory("ametist");
    }

    public void openMainMenu(Player p) {
        MenuSession s = sessions.computeIfAbsent(p.getUniqueId(), id -> new MenuSession());
        s.lastCategoryId = null;
        Inventory inv = buildMainInventory(p);
        p.openInventory(inv);
    }

    public void openCategory(Player p, String id) {
        CategoryMenuConfig cat = categories.get(id.toLowerCase());
        if (cat == null) return;
        MenuSession s = sessions.computeIfAbsent(p.getUniqueId(), u -> new MenuSession());
        s.lastCategoryId = cat.id;
        Inventory inv = buildCategoryInventory(p, cat);
        p.openInventory(inv);
    }

    public void openPurchase(Player p, String categoryId, String itemKey) {
        CategoryMenuConfig cat = categories.get(categoryId.toLowerCase());
        if (cat == null) return;
        CategoryItemConfig item = cat.items.get(itemKey);
        if (item == null) return;

        MenuSession s = sessions.computeIfAbsent(p.getUniqueId(), u -> new MenuSession());
        s.lastCategoryId = cat.id;
        s.selectedCategoryId = cat.id;
        s.selectedItemKey = itemKey;
        s.quantity = 1;

        p.openInventory(buildPurchaseInventory(p, s));
    }

    public void openPurchase(Player p, MenuSession s) {
        p.openInventory(buildPurchaseInventory(p, s));
    }

    public void openStackMenu(Player p, MenuSession s) {
        p.openInventory(buildStackInventory(p, s));
    }

    public MenuSession session(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), u -> new MenuSession());
    }

    public CategoryMenuConfig category(String id) {
        if (id == null) return null;
        return categories.get(id.toLowerCase());
    }

    public List<MainCategoryButton> mainButtons() {
        return mainMenu == null ? List.of() : mainMenu.buttons;
    }

    // ----------------- Build inventories -----------------

    private Inventory buildMainInventory(Player p) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, null), mainMenu.size, Color.t(mainMenu.title));
        if (mainMenu.fillerEnabled) {
            ItemStack filler = ItemFactory.simple(Material.matchMaterial(mainMenu.fillerMaterial), Color.t(mainMenu.fillerName), List.of());
            // main.yml filler mode (ALL veya SLOTS)
            YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), plugin.getConfig().getString("menus.main", "menus/main.yml")));
            String mode = y.getString("filler.mode", "ALL").toUpperCase(Locale.ROOT);
            if (mode.equals("SLOTS")) {
                for (int s : y.getIntegerList("filler.slots")) {
                    int idx = Math.max(0, s - 1);
                    if (idx < mainMenu.size) inv.setItem(idx, filler);
                }
            } else {
                for (int i = 0; i < mainMenu.size; i++) inv.setItem(i, filler);
            }
        }

        for (MainCategoryButton b : mainMenu.buttons) {
            ItemStack it = ItemFactory.fromConfig(b.material, b.name, b.lore, Map.of());
            inv.setItem(b.slotIndex(), it);
        }
        return inv;
    }

    private Inventory buildCategoryInventory(Player p, CategoryMenuConfig cat) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.CATEGORY, cat.id), cat.size, Color.t(cat.title));
        if (cat.fillerEnabled) {
            ItemStack filler = ItemFactory.simple(Material.matchMaterial(cat.fillerMaterial), Color.t(cat.fillerName), List.of());
            // kategori yml filler mode (ALL veya SLOTS)
            File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("menus." + cat.id, "menus/" + cat.id + ".yml"));
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            String mode = y.getString("filler.mode", "ALL").toUpperCase(Locale.ROOT);
            if (mode.equals("SLOTS")) {
                for (int s : y.getIntegerList("filler.slots")) {
                    int idx = Math.max(0, s - 1);
                    if (idx < cat.size) inv.setItem(idx, filler);
                }
            } else {
                for (int i = 0; i < cat.size; i++) inv.setItem(i, filler);
            }
        }

        for (CategoryItemConfig item : cat.items.values()) {
            Map<String, String> repl = new HashMap<>();
            repl.put("%amount%", String.valueOf(item.amount));
            repl.put("%buy%", Format.money(item.buy));
            repl.put("%sell%", Format.money(item.sell));
            ItemStack it = ItemFactory.fromConfig(item.material, item.name, item.lore, repl);
            inv.setItem(item.slotIndex(), it);
        }

        ItemStack back = ItemFactory.fromConfig(cat.backMaterial, cat.backName, cat.backLore, Map.of());
        inv.setItem(cat.backSlotIndex(), back);
        return inv;
    }

    private Inventory buildPurchaseInventory(Player p, MenuSession s) {
        CategoryMenuConfig cat = category(s.selectedCategoryId);
        if (cat == null) return Bukkit.createInventory(new MenuHolder(MenuType.PURCHASE, null), 54, "Market");
        CategoryItemConfig item = cat.items.get(s.selectedItemKey);
        if (item == null) return Bukkit.createInventory(new MenuHolder(MenuType.PURCHASE, null), 54, "Market");

        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.PURCHASE, cat.id), 54, Color.t("&0&l ɪsʟᴇᴍ ᴍᴇɴᴜ"));

        // filler
        if (plugin.getConfig().getBoolean("ui.purchase.filler.enabled", false)) {
            Material fm = Material.matchMaterial(plugin.getConfig().getString("ui.purchase.filler.material", "GRAY_STAINED_GLASS_PANE"));
            String fn = Color.t(plugin.getConfig().getString("ui.purchase.filler.name", " "));
            ItemStack filler = ItemFactory.simple(fm == null ? Material.GRAY_STAINED_GLASS_PANE : fm, fn, List.of());
            for (int i = 0; i < 54; i++) inv.setItem(i, filler);
        }

        // quantity controls
        inv.setItem(10, ItemFactory.headButton(plugin, HeadColor.RED, "&c ᴄɪᴋᴀʀᴛ &f10", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));
        inv.setItem(11, ItemFactory.headButton(plugin, HeadColor.RED, "&c ᴄɪᴋᴀʀᴛ &f1", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));
        inv.setItem(19, ItemFactory.headButton(plugin, HeadColor.RED, "&c ᴄɪᴋᴀʀᴛ &f32", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));

        inv.setItem(15, ItemFactory.headButton(plugin, HeadColor.GREEN, "&a ᴇᴋʟᴇ &f1", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));
        inv.setItem(16, ItemFactory.headButton(plugin, HeadColor.GREEN, "&a ᴇᴋʟᴇ &f10", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));
        inv.setItem(25, ItemFactory.headButton(plugin, HeadColor.GREEN, "&a ᴇᴋʟᴇ &f32", List.of("&7 ᴍɪᴋᴛᴀʀ: &f" + s.quantity)));

        // back + stack + confirm
        inv.setItem(45, ItemFactory.headButton(plugin, HeadColor.RED, "&c ɢᴇʀɪ ᴅᴏɴ", List.of("&7 ᴋᴀᴛᴇɢᴏʀɪ ᴍᴇɴᴜsᴜɴᴇ ᴅᴏɴ.")));

        if (plugin.getConfig().getBoolean("stack-menu.enabled", true)) {
            inv.setItem(49, ItemFactory.simple(
                    Material.EMERALD,
                    Color.t("&a sᴛᴀᴄᴋ ᴍᴇɴᴜ"),
                    Color.tl(List.of("&7 sᴛᴀᴄᴋ ɪʟᴇ ᴍɪᴋᴛᴀʀ sᴇᴄ"))
            ));
        }

        ItemStack itemShow = new ItemStack(Material.matchMaterial(item.material) == null ? Material.STONE : Material.matchMaterial(item.material));
        itemShow.setAmount(Math.min(64, Math.max(1, s.quantity)));
        inv.setItem(13, itemShow);

        double buyTotal = item.buy * s.quantity;
        double sellTotal = item.sell * s.quantity;
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(Color.t("&7Eşya Miktarı: &f" + s.quantity));
        confirmLore.add(Color.t("&7Satın Alma Fiyatı: &f" + Format.money(buyTotal)));
        confirmLore.add(Color.t("&7Satış Fiyatı: &f" + Format.money(sellTotal)));
        confirmLore.add("");
        confirmLore.add(Color.t("&8&m------------------------"));
        confirmLore.add(Color.t("&a sᴏʟ ᴛɪᴋ: &fᴀʟ"));
        confirmLore.add(Color.t("&c sᴀɢ ᴛɪᴋ: &fsᴀᴛ"));
        confirmLore.add(Color.t("&e sʜɪғᴛ+sᴏʟ: &fᴍᴀᴋs ᴀʟ"));
        confirmLore.add(Color.t("&e sʜɪғᴛ+sᴀɢ: &fʜᴇᴘsɪɴɪ sᴀᴛ"));
        confirmLore.add(Color.t("&8&m------------------------"));

        inv.setItem(53, ItemFactory.headButton(plugin, HeadColor.GREEN, "&a ᴏɴᴀʏʟᴀ", confirmLore));

        // clear some slots around center for nicer look
        inv.setItem(14, null); // slot 15 (index14) free (visual)
        return inv;
    }

    private Inventory buildStackInventory(Player p, MenuSession s) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.STACK, s.selectedCategoryId), 27, Color.t("&0&l sᴛᴀᴄᴋ sᴇᴄ"));

        Material frameMat = Material.matchMaterial(plugin.getConfig().getString("stack-menu.frame.material", "GRAY_STAINED_GLASS_PANE"));
        String frameName = Color.t(plugin.getConfig().getString("stack-menu.frame.name", " "));
        ItemStack frame = ItemFactory.simple(frameMat == null ? Material.GRAY_STAINED_GLASS_PANE : frameMat, frameName, List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, frame);

        CategoryMenuConfig cat = category(s.selectedCategoryId);
        CategoryItemConfig item = (cat == null) ? null : cat.items.get(s.selectedItemKey);
        Material mat = (item == null) ? Material.STONE : (Material.matchMaterial(item.material) == null ? Material.STONE : Material.matchMaterial(item.material));

        String mode = plugin.getConfig().getString("stack-menu.mode", "STACKS").toUpperCase(Locale.ROOT);
        for (int i = 0; i < 7; i++) {
            int human = i + 1;
            int qty = mode.equals("ITEMS") ? human : (human * 64);
            ItemStack it = new ItemStack(mat);
            it.setAmount(Math.min(64, human)); // görünüm için
            inv.setItem(10 + i, ItemFactory.withNameAndLore(it,
                    Color.t("&bSeç: &f" + human + (mode.equals("ITEMS") ? " adet" : " stack")),
                    Color.tl(List.of("&7Seçilen miktar: &f" + qty, "", "&eTıkla &7→ &fSatın alma menüsü"))));
        }

        inv.setItem(22, ItemFactory.headButton(plugin, HeadColor.RED, "&c ɢᴇʀɪ ᴅᴏɴ", List.of("&7 sᴀᴛɪɴ ᴀʟᴍᴀ ᴍᴇɴᴜsᴜɴᴇ ᴅᴏɴ.")));
        inv.setItem(23, null);
        return inv;
    }

    // ----------------- Load config files -----------------

    private void ensureDefaults() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        // menus folder
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists()) menusDir.mkdirs();

        // copy default resources if missing
        copyIfMissing("menus/main.yml");
        copyIfMissing("menus/end.yml");
        copyIfMissing("menus/nether.yml");
        copyIfMissing("menus/totem.yml");
        copyIfMissing("menus/biftek.yml");
        copyIfMissing("menus/ametist.yml");
    }

    private void copyIfMissing(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (f.exists()) return;
        plugin.saveResource(path, false);
    }

    private MainMenuConfig loadMain() {
        String path = plugin.getConfig().getString("menus.main", "menus/main.yml");
        File file = new File(plugin.getDataFolder(), path);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        MainMenuConfig m = new MainMenuConfig();
        m.title = y.getString("title", "&8Market");
        m.size = clampSize(y.getInt("size", 27));
        m.fillerEnabled = y.getBoolean("filler.enabled", true);
        m.fillerMaterial = y.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        m.fillerName = y.getString("filler.name", " ");

        ConfigurationSection cats = y.getConfigurationSection("categories");
        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                ConfigurationSection c = cats.getConfigurationSection(key);
                if (c == null) continue;
                MainCategoryButton b = new MainCategoryButton();
                b.id = key;
                b.slot = c.getInt("slot", 1);
                b.material = c.getString("material", "STONE");
                b.name = c.getString("name", key);
                b.lore = c.getStringList("lore");
                b.openMenu = c.getString("open-menu", key);
                m.buttons.add(b);
            }
        }
        return m;
    }

    private void loadCategory(String id) {
        String path = plugin.getConfig().getString("menus." + id, "menus/" + id + ".yml");
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        CategoryMenuConfig cat = new CategoryMenuConfig();
        cat.id = y.getString("id", id);
        cat.title = y.getString("title", id);
        cat.size = clampSize(y.getInt("size", 27));
        cat.currency = CurrencyType.from(y.getString("currency", "MONEY"));

        cat.fillerEnabled = y.getBoolean("filler.enabled", true);
        cat.fillerMaterial = y.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        cat.fillerName = y.getString("filler.name", " ");

        cat.backSlot = y.getInt("back-button.slot", 18);
        cat.backMaterial = y.getString("back-button.material", "RED_STAINED_GLASS_PANE");
        cat.backName = y.getString("back-button.name", "&cGeri Dön");
        cat.backLore = y.getStringList("back-button.lore");

        ConfigurationSection items = y.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection i = items.getConfigurationSection(key);
                if (i == null) continue;
                CategoryItemConfig item = new CategoryItemConfig();
                item.key = key;
                item.slot = i.getInt("slot", 1);
                item.material = i.getString("material", "STONE");
                item.name = i.getString("name", key);
                item.lore = i.getStringList("lore");
                item.amount = i.getInt("amount", 1);
                item.buy = i.getDouble("buy", 0);
                item.sell = i.getDouble("sell", 0);
                item.commands = i.getStringList("commands");
                cat.items.put(key, item);
            }
        }

        categories.put(cat.id.toLowerCase(), cat);
    }

    private int clampSize(int s) {
        int v = Math.max(9, Math.min(54, s));
        return (v / 9) * 9;
    }
}

