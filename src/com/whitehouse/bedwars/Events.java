package com.whitehouse.bedwars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Collections;
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
            im.setDisplayName(plugin.getConfig().getString("main.teamSelectorName"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(plugin.getConfig().getString("main.unDroppableLore"));
            im.setLore(lore);
            teamSelector.setItemMeta(im);
        }catch(NullPointerException e){e.printStackTrace();}
        player.getInventory().addItem(teamSelector);
        //Teleportovat hrace do herniho lobby
        String lobbyLoc = plugin.getConfig().getString("arena.lobby", null);
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
        if(onlinePlayers >= plugin.getConfig().getInt("game.minPlayers")){
            //Mela by zacit startovat hra
            if(plugin.getGameState() != GameState.STARTING){
                plugin.setGameStarting(true);
            }
        }
        player.setScoreboard(plugin.getMyScoreboardInstance().getGlobalSidebarScoreboard());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        handlePlayerJoin(player);
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
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successLobbySet"));
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

    @EventHandler
    public void onCraft(CraftItemEvent event){
        event.setCancelled(true);
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
            lam.setColor(this.plugin.getMenuInstance().getDyeColorOfNthTeam(team));
            lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
            chestplate.setItemMeta(lam);
            playerInventory.setChestplate(chestplate);

            if(armor == 1){
                //Leather brneni
                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                im = helmet.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.plugin.getMenuInstance().getDyeColorOfNthTeam(team));
                lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                helmet.setItemMeta(lam);
                playerInventory.setHelmet(helmet);

                ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
                im = leggings.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.plugin.getMenuInstance().getDyeColorOfNthTeam(team));
                lam.setLore(Collections.singletonList(plugin.getConfig().getString("main.unDroppableLore")));
                leggings.setItemMeta(lam);
                playerInventory.setLeggings(leggings);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                im = boots.getItemMeta();
                lam = (LeatherArmorMeta)im;
                lam.setColor(this.plugin.getMenuInstance().getDyeColorOfNthTeam(team));
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

    public void processShopBoughtItem(Player player, int category, ItemStack clickedItem){
        ItemStack itemToGive = clickedItem.clone();
        if(category == 0){
            //Armor - perma upgrade
            int boughtArmor = 0;
            if(itemToGive.getType() == Material.LEATHER_CHESTPLATE) boughtArmor = 1;
            else if(itemToGive.getType() == Material.IRON_CHESTPLATE) boughtArmor = 2;
            else if(itemToGive.getType() == Material.DIAMOND_CHESTPLATE) boughtArmor = 3;
            this.plugin.setPlayerArmor(player, boughtArmor);
            this.setPlayersArmor(player);
        }else{
            //Itemy na givnuti
            ItemMeta im = itemToGive.getItemMeta();
            im.setLore(null);
            String displayName = im.getDisplayName();
            if(displayName.contains("×")){
                displayName = displayName.replaceFirst("\\d×[ ]*", "");
                if(!displayName.substring(0, 1).equals("§")){
                    displayName = "§f"+displayName;
                }
                im.setDisplayName(displayName);
            }
            itemToGive.setItemMeta(im);
            player.getInventory().addItem(itemToGive);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();
        Inventory open = event.getClickedInventory();
        ItemStack item = event.getCurrentItem();
        int slot = event.getSlot();
        if(event.getView().getTitle().equals(this.plugin.getConfig().getString("main.teamSelectMenuName"))) {
            //Otevreno menu pro vyber teamu
            if(open == null){ //kliknuto mimo inventar
                player.closeInventory();
                return;
            }
            if(open.getType() == InventoryType.PLAYER){ //kliknuto do hracova inventare
                return;
            }
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
        else if(event.getView().getTitle().equals(this.plugin.getConfig().getString("game.shopMenuName"))) {
            //Otevren shop
            if(open == null){ //kliknuto mimo inventar
                player.closeInventory();
                return;
            }
            if(open.getType() == InventoryType.PLAYER){ //kliknuto do hracova inventare
                return;
            }
            //Je kliknuto v menu shopu
            event.setCancelled(true);
            if(slot < 9){
                //Kliknuto na prvnich 9 slotu - vyber kategorie shopu
                if(item == null || item.getType() == Material.AIR){
                    return; //kliknuto na prazdno, nic se nedeje
                }
                ArrayList<ItemStack> list = this.plugin.getMenuInstance().getShopCategoryListItems();
                for(int i=0; i<list.size(); i++){
                    ItemStack is = list.get(i);
                    if(i == slot){
                        is.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                    }
                    open.setItem(i, is);
                }
                ArrayList<ItemStack> goodsList = this.plugin.getMenuInstance().getShopCategoryItems(slot, player);
                for(int i=9; i<45; i++){
                    int index = i-18;
                    if(index >= 0 && index < goodsList.size()) {
                        ItemStack is = goodsList.get(i - 18);
                        open.setItem(i, is);
                    } else {
                        open.setItem(i, null);
                    }
                }
            }else if(slot >= 18){
                //Kliknuto na spravne itemy v shopu
                if(item == null || item.getType() == Material.AIR){
                    return; //kliknuto na prazdno, nic se nedeje
                }
                int i = 0;
                ItemStack[] contents = open.getContents();
                int size = contents.length;
                int selectedCategory = -1;
                while(i<9 && i<size){
                    ItemStack itemStackInMenu = contents[i];
                    if(itemStackInMenu.getEnchantmentLevel(Enchantment.DURABILITY) != 0){
                        selectedCategory = i;
                        break;
                    }
                    i++;
                }
                if(selectedCategory == -1){
                    return; //neni selectnuta kategorie - divny, ale neresit
                }
                ArrayList<ItemStack> list = this.plugin.getMenuInstance().getShopCategoryItems(selectedCategory, player);
                int clickedItemSlot = slot-18;
                try {
                    if(selectedCategory == 0){
                        //Armor
                        if(this.plugin.getPlayerArmor(player) > clickedItemSlot){
                            //Jiz ma stejny nebo lepsi armor, nekupovat znovu
                            player.sendMessage(this.plugin.getPrefix()+this.plugin.getConfig().getString("game.shopAlreadyHaveArmor"));
                            return;
                        }
                    }
                    ItemStack clickedItem = list.get(clickedItemSlot);
                    ItemMeta im = clickedItem.getItemMeta();
                    List<String> lore = im.getLore();
                    String priceLine = lore.get(lore.size()-1);
                    String[] priceSplit = priceLine.split(" ");
                    int priceNumber = Integer.parseInt(priceSplit[0].substring(2));
                    String priceItem = priceSplit[1];
                    //Zkontrolovat, jestli hrac ma dostatek potrebnych itemu
                    ItemStack[] invContents = player.getInventory().getStorageContents();
                    int itemsLeft = priceNumber;
                    Material neededMaterial = getMaterialFromResourceName(priceItem);
                    for(ItemStack is : invContents){
                        if(is == null) continue;
                        if(is.getType() == neededMaterial){
                            int amount = is.getAmount();
                            itemsLeft -= amount;
                            if(itemsLeft <= 0){
                                itemsLeft = 0;
                                break;
                            }
                        }
                    }
                    player.sendMessage("§fCena: "+priceLine);
                    if(itemsLeft == 0){
                        player.sendMessage("§aMas dostatek itemu na koupi");
                        //Sebrat itemy
                        int itemsToRemoveLeft = priceNumber;
                        for(ItemStack is : invContents){
                            if(is == null) continue;
                            if(is.getType() == neededMaterial){
                                int amount = is.getAmount();
                                while(amount > 0 && itemsToRemoveLeft > 0) {
                                    is.setAmount(amount-1);
                                    itemsToRemoveLeft--;
                                    amount = is.getAmount();
                                    if (itemsToRemoveLeft <= 0) {
                                        itemsToRemoveLeft = 0;
                                        break;
                                    }
                                }
                            }
                        }
                        //Itemy odebrány, udělat správný nákup (přidat hráči to, co si koupil)
                        this.processShopBoughtItem(player, selectedCategory, clickedItem);
                    }else{
                        player.sendMessage("§cNa koupi ti chybi "+itemsLeft+"× "+neededMaterial.toString());
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        else if(event.getSlotType() == InventoryType.SlotType.ARMOR){
            //Zakazat sundavani armoru
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        Player player = event.getPlayer();
        int team = plugin.getTeamOfPlayer(player);
        if(this.plugin.getGameState() == GameState.INGAME){
            Location respawnLoc = plugin.teamSpawns.get(team);
            event.setRespawnLocation(respawnLoc);
            this.setPlayersArmor(player);
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

    @EventHandler
    public void onClickOnSign(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK){
            return;
        }
        //Hrac klikl na blok
        Block block = event.getClickedBlock();
        if(!block.getType().toString().contains("SIGN")){
            return;
        }
        //Hrac klikl na cedulku
        Sign sign = (Sign) block.getState();
        String line1 = sign.getLine(0);
        if(line1.toLowerCase().contains("shop")){
            //Je to cedulka shopu
            this.plugin.getMenuInstance().openShopMenu(player);
        }
    }

}
