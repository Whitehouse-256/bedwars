package com.whitehouse.bedwars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {
    private static final String lobbyServerName = "lobby";

    private final BedWars plugin;

    public LobbyCommand(BedWars plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("§cNutno zadavat ze hry!");
            return true;
        }
        Player player = (Player)sender;
        plugin.getPlayerUtilsInstance().sendPlayerBungee(player, lobbyServerName);
        return true;
    }
}
