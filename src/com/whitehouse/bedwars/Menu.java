package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class Menu {
    private final BedWars plugin;

    public Menu(BedWars plugin){
        this.plugin = plugin;
    }

    public ChatColor getColorOfNthTeam(int nth){
        if(nth==0) return ChatColor.getByChar('c');
        if(nth==1) return ChatColor.getByChar('9');
        if(nth==2) return ChatColor.getByChar('a');
        if(nth==3) return ChatColor.getByChar('e');
        if(nth==4) return ChatColor.getByChar('5');
        if(nth==5) return ChatColor.getByChar('6');
        if(nth==6) return ChatColor.getByChar('d');
        if(nth==7) return ChatColor.getByChar('3');
        return ChatColor.WHITE;
    }

    public Material getWoolOfNthTeam(int nth){
        if(nth==0) return Material.RED_WOOL;
        if(nth==1) return Material.BLUE_WOOL;
        if(nth==2) return Material.LIME_WOOL;
        if(nth==3) return Material.YELLOW_WOOL;
        if(nth==4) return Material.PURPLE_WOOL;
        if(nth==5) return Material.ORANGE_WOOL;
        if(nth==6) return Material.PINK_WOOL;
        if(nth==7) return Material.CYAN_WOOL;
        return Material.WHITE_WOOL;
    }

    public String getNameOfNthTeam(int nth){
        if(nth==0) return "§cCerveny tym";
        if(nth==1) return "§9Modry tym";
        if(nth==2) return "§aZeleny tym";
        if(nth==3) return "§eZluty tym";
        if(nth==4) return "§5Fialovy tym";
        if(nth==5) return "§6Oranzovy tym";
        if(nth==6) return "§dRuzovy tym";
        if(nth==7) return "§3Tyrkysovy tym";
        return "Bezbarvy tym";
    }

    public void openTeamSelectMenu(Player player){
        Inventory inv = Bukkit.createInventory(null, 9, this.plugin.getConfig().getString("main.teamSelectMenuName"));
        ItemMeta im = null;
        //Vytvorit jednotlive itemy pro tymy
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        for(int i=0; i<teamCount; i++){
            ItemStack wool = new ItemStack(getWoolOfNthTeam(i));
            im = wool.getItemMeta();
            ArrayList<Player> playersInTeam = this.plugin.getPlayersInTeam(i);
            int playersInTeamCount = playersInTeam.size();
            im.setDisplayName(getNameOfNthTeam(i)+" "+playersInTeamCount+"/"+playersPerTeam);
            ArrayList<String> lore = new ArrayList<String>();
            for(Player p : playersInTeam){
                lore.add("§f- "+p.getName());
            }
            if(playersInTeamCount < playersPerTeam) lore.add(plugin.getConfig().getString("main.teamClickToJoin"));
            else lore.add(plugin.getConfig().getString("main.teamIsFull"));
            im.setLore(lore);
            wool.setItemMeta(im);
            inv.setItem(i, wool);
        }
        player.openInventory(inv);
    }
}
