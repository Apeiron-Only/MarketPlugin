package tr.apeiron.market;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tr.apeiron.market.market.MarketCommand;
import tr.apeiron.market.menu.MenuListener;
import tr.apeiron.market.menu.MenuService;

public final class ApeironMarketPlugin extends JavaPlugin {

    private Economy economy; // Vault
    private MenuService menuService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupVaultEconomy();

        this.menuService = new MenuService(this);
        this.menuService.reloadAll();

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        registerCommands();
    }

    @Override
    public void onDisable() {
    }

    public Economy getEconomy() {
        return economy;
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public String prefix() {
        return Color.t(getConfig().getString("prefix", "&8[&bMarket&8] &7"));
    }

    private void registerCommands() {
        PluginCommand market = getCommand("market");
        if (market != null) {
            market.setExecutor(new MarketCommand(this));
        }
    }

    private void setupVaultEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault bulunamadı. Para ile alış/satış devre dışı.");
            this.economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Vault var ama Economy provider yok. Para ile alış/satış devre dışı.");
            this.economy = null;
            return;
        }
        this.economy = rsp.getProvider();
    }

}

