package com.whitehouse.bedwars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class PlayerUtils {
    private final BedWars plugin;

    public PlayerUtils(BedWars plugin){
        this.plugin = plugin;
    }

    public void handlePlayerJoin(Player player){
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
            im.setDisplayName(this.plugin.getConfig().getString("main.teamSelectorName"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(this.plugin.getConfig().getString("main.unDroppableLore"));
            im.setLore(lore);
            teamSelector.setItemMeta(im);
        }catch(NullPointerException e){e.printStackTrace();}
        player.getInventory().addItem(teamSelector);
        //Teleportovat hrace do herniho lobby
        String lobbyLoc = this.plugin.getConfig().getString("arena.lobby", null);
        if(lobbyLoc != null){
            String[] split = lobbyLoc.split(";");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            float yaw = Float.parseFloat(split[3]);
            float pitch = Float.parseFloat(split[4]);
            Location lobby = new Location(Bukkit.getWorld("world"), x, y, z, yaw, pitch);
            player.teleport(lobby);
        }
        //Zkontrolovat pocet online hracu
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if(onlinePlayers >= this.plugin.getConfig().getInt("game.minPlayers")){
            //Mela by zacit startovat hra
            if(this.plugin.getGameState() != GameState.STARTING){
                this.plugin.setGameStarting(true);
            }
        }
        player.setScoreboard(this.plugin.getMyScoreboardInstance().getGlobalSidebarScoreboard());
    }

    public String getResourceById(int id){
        if(id==0) return "iron";
        if(id==1) return "gold";
        if(id==2) return "diamond";
        return "-";
    }

    public int getMaxInTeam(){
        int onlinePlayerCount = Bukkit.getOnlinePlayers().size();
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int max = (int) Math.ceil(((double)onlinePlayerCount)/teamCount);
        if(max > playersPerTeam) return playersPerTeam;
        return max;
    }

    public Material getMaterialFromResourceName(String resource){
        if(resource.equalsIgnoreCase("IRON")){
            return Material.IRON_INGOT;
        }else if(resource.equalsIgnoreCase("GOLD")){
            return Material.GOLD_INGOT;
        }else if(resource.equalsIgnoreCase("DIAMOND")){
            return Material.DIAMOND;
        }else if(resource.equalsIgnoreCase("EMERALD")){
            return Material.EMERALD;
        }
        return null;
    }

    public void setPlayersArmor(Player player){
        int armor = this.plugin.getPlayerArmor(player);
        PlayerInventory playerInventory = player.getInventory();
        int team = plugin.getTeamOfPlayer(player);

        ItemMeta im = null;
        LeatherArmorMeta lam = null;
        try {
            ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
            im = chestplate.getItemMeta();
            lam = (LeatherArmorMeta) im;
            lam.setColor(this.getDyeColorOfNthTeam(team));
            lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
            chestplate.setItemMeta(lam);
            playerInventory.setChestplate(chestplate);

            if(armor == 1){
                //Leather brneni
                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                im = helmet.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.getDyeColorOfNthTeam(team));
                lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                helmet.setItemMeta(lam);
                playerInventory.setHelmet(helmet);

                ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
                im = leggings.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.getDyeColorOfNthTeam(team));
                lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                leggings.setItemMeta(lam);
                playerInventory.setLeggings(leggings);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                im = boots.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.getDyeColorOfNthTeam(team));
                lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                boots.setItemMeta(lam);
                playerInventory.setBoots(boots);
            }else if(armor == 2){
                //Iron brneni
                ItemStack helmet = new ItemStack(Material.IRON_HELMET);
                im = helmet.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                helmet.setItemMeta(im);
                playerInventory.setHelmet(helmet);

                ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
                im = leggings.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                leggings.setItemMeta(im);
                playerInventory.setLeggings(leggings);

                ItemStack boots = new ItemStack(Material.IRON_BOOTS);
                im = boots.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                boots.setItemMeta(im);
                playerInventory.setBoots(boots);
            }else if(armor == 3){
                //Diamond brneni
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                im = helmet.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                helmet.setItemMeta(im);
                playerInventory.setHelmet(helmet);

                ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
                im = leggings.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                leggings.setItemMeta(im);
                playerInventory.setLeggings(leggings);

                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                im = boots.getItemMeta();
                im.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                boots.setItemMeta(im);
                playerInventory.setBoots(boots);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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

    public Color getDyeColorOfNthTeam(int nth){
        if(nth==0) return Color.fromRGB(255, 0, 0);
        if(nth==1) return Color.fromRGB(50, 0, 255);
        if(nth==2) return Color.fromRGB(0, 255, 0);
        if(nth==3) return Color.fromRGB(255, 255, 0);
        if(nth==4) return Color.fromRGB(128, 0, 128);
        if(nth==5) return Color.fromRGB(255, 160, 0);
        if(nth==6) return Color.fromRGB(255, 128, 150);
        if(nth==7) return Color.fromRGB(0, 160, 160);
        return Color.WHITE;
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

    public void shootFireball(Player player){
        Location eye = player.getEyeLocation();
        Location loc = eye.add(eye.getDirection().multiply(1.2));
        Fireball fireball = (Fireball) loc.getWorld().spawnEntity(loc, EntityType.FIREBALL);
        fireball.setVelocity(loc.getDirection().multiply(2));
        fireball.setShooter(player);
        fireball.setIsIncendiary(false);
        fireball.setYield(0F);
    }

    public int getTeamByBedBlock(Block block){
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        for(int i=0; i<teamCount; i++) {
            try {
                String bedCoords = this.plugin.getConfig().getString("arena.beds." + i);
                String[] coordsXYZ = Objects.requireNonNull(bedCoords).split(";");
                int x = Integer.parseInt(coordsXYZ[0]);
                int y = Integer.parseInt(coordsXYZ[1]);
                int z = Integer.parseInt(coordsXYZ[2]);
                int distSq = (block.getX()-x)*(block.getX()-x) + (block.getY()-y)*(block.getY()-y) + (block.getZ()-z)*(block.getZ()-z);
                if(distSq <= 1){
                    //je to postel daneho tymu
                    return i;
                }
            }catch (Exception e){e.printStackTrace();}
        }
        return -1;
    }

}
