package com.whitehouse.bedwars;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;

public class Events implements Listener {
    private final BedWars plugin;

    public Events(BedWars plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event){
        if(event.getEntityType() != EntityType.VILLAGER) event.setCancelled(true);
    }

    @EventHandler
    public void beforeJoin(AsyncPlayerPreLoginEvent event){
        if(!this.plugin.getGameState().isJoinable()){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getConfig().getString("main.cannotJoinMessage"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.sendMessage(this.plugin.getPrefix()+plugin.getConfig().getString("main.joinMessage"));
        player.setGameMode(GameMode.SURVIVAL);
        //Vlozit itemy do inventare
        player.getInventory().clear();
        ItemStack teamSelector = new ItemStack(Material.RED_BED);
        ItemMeta im = teamSelector.getItemMeta();
        im.setDisplayName(plugin.getConfig().getString("main.teamSelectorName"));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(plugin.getConfig().getString("main.unDroppableLore"));
        im.setLore(lore);
        teamSelector.setItemMeta(im);
        player.getInventory().addItem(teamSelector);
        //Zkontrolovat pocet online hracu
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if(onlinePlayers >= plugin.getConfig().getInt("game.minPlayers")){
            //Mela by zacit startovat hra
            if(plugin.getGameState() != GameState.STARTING){
                plugin.setGameStarting(true);
            }
        }
    }

}
