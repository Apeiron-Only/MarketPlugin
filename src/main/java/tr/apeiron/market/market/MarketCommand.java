package tr.apeiron.market.market;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tr.apeiron.market.ApeironMarketPlugin;
import tr.apeiron.market.Color;

public final class MarketCommand implements CommandExecutor {

    private final ApeironMarketPlugin plugin;

    public MarketCommand(ApeironMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("apeironmarket.reload")) {
                sender.sendMessage(plugin.prefix() + Color.t(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getMenuService().reloadAll();
            sender.sendMessage(plugin.prefix() + Color.t(plugin.getConfig().getString("messages.reloaded")));
            return true;
        }

        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.prefix() + Color.t(plugin.getConfig().getString("messages.only-player")));
            return true;
        }

        plugin.getMenuService().openMainMenu(p);
        return true;
    }
}

