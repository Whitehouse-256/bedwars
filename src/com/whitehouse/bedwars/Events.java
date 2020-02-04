package com.whitehouse.bedwars;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
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
        player.setFoodLevel(20);
        if(this.plugin.getConfig().getBoolean("main.runSetup")){
            for(int i=0; i<3; i++){
                player.sendMessage(this.plugin.getPrefix()+"§cHra neni nastavena! Pouzij /bw-setup!");
            }
        }
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
        }catch(NullPointerException e){e.printStackTrace();}
        player.getInventory().addItem(teamSelector);
        //Zkontrolovat pocet online hracu
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if(onlinePlayers >= plugin.getConfig().getInt("game.minPlayers")){
            //Mela by zacit startovat hra
            if(plugin.getGameState() != GameState.STARTING){
                plugin.setGameStarting(true);
            }
        }
        player.setScoreboard(plugin.getMyScoreboardInstance().getGlobalScoreboard());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        Player player = event.getPlayer();
        for (int i = 0; i < teamCount; i++) {
            this.plugin.removePlayerFromTeam(i, player);
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

    private String getResourceById(int id){
        if(id==0) return "iron";
        if(id==1) return "gold";
        if(id==2) return "diamond";
        return "-";
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
        if(this.plugin.getGameState().isTeamSelectable()){ //vyber tymu
            if(item.getType() == Material.RED_BED){
                event.setCancelled(true);
                this.plugin.getMenuInstance().openTeamSelectMenu(player);
            }
        }
        if(this.plugin.getGameState() == GameState.SETUP){
            event.setCancelled(true);
            if(player.getInventory().getHeldItemSlot() == 8){
                //prepnout na dalsi nabidku
                player.performCommand("bw-setup next");
            }else{
                //zjistit, co se ma nastavit
                ItemStack[] invCont = player.getInventory().getStorageContents();
                if(invCont[8] != null){
                    try{
                        String name = invCont[8].getItemMeta().getDisplayName();
                        int slot = player.getInventory().getHeldItemSlot();
                        if(name.contains("Nastaveni postele")){
                            //Nastavit postel pro tym (slot)
                            Block clickedBlock = event.getClickedBlock();
                            if(clickedBlock == null){
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.clickOnBlock"));
                                return;
                            } else if (!clickedBlock.getType().toString().contains("BED")){
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.clickOnBed"));
                                return;
                            }
                            //Kliknul na postel
                            String loc = clickedBlock.getX()+";"+clickedBlock.getY()+";"+clickedBlock.getZ();
                            this.plugin.getConfig().set("arena.beds."+slot, loc);
                            this.plugin.saveConfig();
                            player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successBedSet")+this.plugin.getMenuInstance().getNameOfNthTeam(slot));
                        }
                        else if(name.contains("Nastaveni spawnu")){
                            //Nastavit spawn tymu (slot)
                            Location location = player.getLocation();
                            String loc = location.getX()+";"+location.getY()+";"+location.getZ()+";"+location.getYaw()+";"+location.getPitch();
                            this.plugin.getConfig().set("arena.spawn."+slot, loc);
                            this.plugin.saveConfig();
                            player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successSpawnSet")+this.plugin.getMenuInstance().getNameOfNthTeam(slot));
                        }
                        else if(name.contains("Ostatni nastaveni")){
                            if(slot <= 2){
                                //Nastavit spawner na itemy
                                Location location = player.getLocation();
                                String loc = location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
                                List<String> list = this.plugin.getConfig().getStringList("arena.resources." + getResourceById(slot));
                                list.add(loc);
                                this.plugin.getConfig().set("arena.resources." + getResourceById(slot), list);
                                this.plugin.saveConfig();
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successResourceSpawnerAdded")+getResourceById(slot));
                            }
                            else if(slot == 3){
                                //Nastavit global spawn (lobby)
                                Location location = player.getLocation();
                                String loc = location.getX()+";"+location.getY()+";"+location.getZ()+";"+location.getYaw()+";"+location.getPitch();
                                this.plugin.getConfig().set("arena.lobby", loc);
                                this.plugin.saveConfig();
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successLobbySet")+this.plugin.getMenuInstance().getNameOfNthTeam(slot));
                            }
                            else event.setCancelled(false);
                        }
                    }catch(NullPointerException e){e.printStackTrace();}
                }
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

    public int getMaxInTeam(){
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
        if(event.getView().getTitle().equals(this.plugin.getConfig().getString("main.teamSelectMenuName"))) {
            //Je kliknuto v menu vyberu teamu
            int teamCount = this.plugin.getConfig().getInt("arena.teams");
            int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
            if(slot >= teamCount) return; //kliknuto mimo vlny - ignorovat
            event.setCancelled(true);
            player.closeInventory();
            //Neni tym plny? Tym balancer
            int maxInTeam = getMaxInTeam();
            if(this.plugin.getPlayersInTeam(slot).size() >= maxInTeam){
                player.sendMessage(this.plugin.getPrefix()+this.plugin.getConfig().getString("main.teamIsFull"));
            }else {
                plugin.addPlayerToTeamAndRemoveFromOthers(slot, player);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        Player player = event.getPlayer();
        int team = plugin.getTeamOfPlayer(player);
        if(this.plugin.getGameState() == GameState.INGAME){
            Location respawnLoc = plugin.teamSpawns.get(team);
            event.setRespawnLocation(respawnLoc);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){ //dropovat pri smrti pouze dia a emeraldy
        Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        for(int i=0; i < drops.size(); i++){
            if(drops.get(i) == null) continue;
            if(drops.get(i).getType() != Material.DIAMOND && drops.get(i).getType() != Material.EMERALD){
                drops.set(i, null);
            }
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            Entity attacker = event.getDamager();
            if(attacker.getType() == EntityType.PLAYER){ //utoci hrac na hrace
                Player attackerPlayer = (Player) attacker;
                if(player.getHealth()<=0){ //zabil ho
                    attackerPlayer.getInventory().addItem(new ItemStack(Material.EMERALD));
                }
            }
        }
    }
}
