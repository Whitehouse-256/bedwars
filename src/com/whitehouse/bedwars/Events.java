package com.whitehouse.bedwars;

import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int maxPlayers = teamCount*playersPerTeam;
        if(Bukkit.getOnlinePlayers().size() >= maxPlayers){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, plugin.getConfig().getString("main.serverFullMessage"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.sendMessage(this.plugin.getPrefix()+plugin.getConfig().getString("main.joinMessage"));
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        //Vlozit itemy do inventare
        player.getInventory().clear();
        ItemStack teamSelector = new ItemStack(Material.RED_BED);
        try {
            ItemMeta im = teamSelector.getItemMeta();
            im.setDisplayName(plugin.getConfig().getString("main.teamSelectorName"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(plugin.getConfig().getString("main.unDroppableLore"));
            im.setLore(lore);
            teamSelector.setItemMeta(im);
        }catch(Exception e){e.printStackTrace();}
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

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event){
        if(this.plugin.getGameState().isInvincible()) event.setFoodLevel(20);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event){
        if(event.getEntityType() == EntityType.PLAYER){
            if(this.plugin.getGameState().isInvincible()) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightclick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        //kliknul nejakym itemem
        if(this.plugin.getGameState().isTeamSelectable()){
            if(item.getType() == Material.RED_BED){
                event.setCancelled(true);
                this.plugin.getMenuInstance().openTeamSelectMenu(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event){
        Player pl = event.getPlayer();
        Item it = event.getItemDrop();
        ItemStack is = it.getItemStack();
        if(!is.hasItemMeta()){it.remove(); return;}
        ItemMeta im = is.getItemMeta();
        try {
            ArrayList<String> lore = new ArrayList<String>(im.getLore());
            for (String line : lore) {
                if (line.contains(this.plugin.getConfig().getString("main.soulBoundLore"))) {
                    it.remove();
                    pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    break;
                }
                if(line.contains(this.plugin.getConfig().getString("main.unDroppableLore"))){
                    event.setCancelled(true);
                    break;
                }
            }
        }catch(Exception e){
            //Nema lore, nebo nema itemmeta - neni treba resit
        }
    }

    private int getMaxInTeam(){
        int onlinePlayerCount = Bukkit.getOnlinePlayers().size();
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int max = (int) Math.ceil(((double)onlinePlayerCount)/teamCount);
        if(max > playersPerTeam) return playersPerTeam;
        return max;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();
        Inventory open = event.getClickedInventory();
        ItemStack item = event.getCurrentItem();
        int slot = event.getSlot();
        player.closeInventory();
        if(event.getView().getTitle().equals(this.plugin.getConfig().getString("main.teamSelectMenuName"))) {
            //Je kliknuto v menu vyberu teamu
            event.setCancelled(true);
            int teamCount = this.plugin.getConfig().getInt("arena.teams");
            int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
            //Neni tym plny? Tym balancer
            int maxInTeam = getMaxInTeam();
            if(this.plugin.getPlayersInTeam(slot).size() >= maxInTeam){
                player.sendMessage(this.plugin.getPrefix()+this.plugin.getConfig().getString("main.teamIsFull"));
            }else {
                for (int i = 0; i < teamCount; i++) {
                    if (i == slot) this.plugin.addPlayerToTeam(i, player);
                    else this.plugin.removePlayerFromTeam(i, player);
                }
                player.sendMessage(this.plugin.getPrefix()+this.plugin.getConfig().getString("main.joinedTeam")+this.plugin.getMenuInstance().getNameOfNthTeam(slot));
            }
        }
    }

}
