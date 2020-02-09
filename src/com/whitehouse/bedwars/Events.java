package com.whitehouse.bedwars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Objects.requireNonNull(plugin.getConfig().getString("main.cannotJoinMessage")));
        }
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int maxPlayers = teamCount*playersPerTeam;
        if(Bukkit.getOnlinePlayers().size() >= maxPlayers){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, Objects.requireNonNull(plugin.getConfig().getString("main.serverFullMessage")));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        this.plugin.getPlayerUtilsInstance().handlePlayerJoin(player);
        int playersOnline = Bukkit.getOnlinePlayers().size();
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int maxPlayers = teamCount*playersPerTeam;
        String joinMessage = Objects.requireNonNull(plugin.getConfig().getString("main.joinBroadcastMessage"))
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(playersOnline))
                .replace("%max%", String.valueOf(maxPlayers));
        event.setJoinMessage(joinMessage);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersOnline = Bukkit.getOnlinePlayers().size()-1;
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        int maxPlayers = teamCount*playersPerTeam;
        int team = plugin.getTeamOfPlayer(player);
        String color = this.plugin.getPlayerUtilsInstance().getColorOfNthTeam(team).toString();
        String quitMessage = Objects.requireNonNull(plugin.getConfig().getString("main.quitBroadcastMessage"))
                .replace("%playerColor%", color)
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(playersOnline))
                .replace("%max%", String.valueOf(maxPlayers));
        event.setQuitMessage(quitMessage);
        for (int i = 0; i < teamCount; i++) {
            this.plugin.removePlayerFromTeam(i, player);
        }
        plugin.getMyScoreboardInstance().removePlayerFromAllTeams(player);
        plugin.checkEndGame();
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event){
        //jidlo nema ubyvat nikde a nikdy
        event.setFoodLevel(20);
        //if(this.plugin.getGameState().isInvincible()) event.setFoodLevel(20);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event){
        if(event.getEntityType() == EntityType.PLAYER){
            if(this.plugin.getGameState().isInvincible()){
                event.setCancelled(true);
                if(event.getCause() == EntityDamageEvent.DamageCause.VOID){
                    Location lobby = plugin.getPlayerUtilsInstance().getLobby();
                    if(lobby != null){
                        event.getEntity().teleport(lobby);
                    }
                }
            }
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
        if(this.plugin.getGameState().isTeamSelectable()){ //vyber tymu
            if(item.getType() == Material.RED_BED && player.getGameMode() != GameMode.CREATIVE){
                event.setCancelled(true);
                this.plugin.getPlayerUtilsInstance().openTeamSelectMenu(player);
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
                        String name = Objects.requireNonNull(invCont[8].getItemMeta()).getDisplayName();
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
                            player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successBedSet")+this.plugin.getPlayerUtilsInstance().getNameOfNthTeam(slot));
                        }
                        else if(name.contains("Nastaveni spawnu")){
                            //Nastavit spawn tymu (slot)
                            Location location = player.getLocation();
                            String loc = location.getX()+";"+location.getY()+";"+location.getZ()+";"+location.getYaw()+";"+location.getPitch();
                            this.plugin.getConfig().set("arena.spawn."+slot, loc);
                            this.plugin.saveConfig();
                            player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successSpawnSet")+this.plugin.getPlayerUtilsInstance().getNameOfNthTeam(slot));
                        }
                        else if(name.contains("Ostatni nastaveni")){
                            if(slot <= 2){
                                //Nastavit spawner na itemy
                                Location location = player.getLocation();
                                String loc = location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
                                List<String> list = this.plugin.getConfig().getStringList("arena.resources." + this.plugin.getPlayerUtilsInstance().getResourceById(slot));
                                list.add(loc);
                                this.plugin.getConfig().set("arena.resources." + this.plugin.getPlayerUtilsInstance().getResourceById(slot), list);
                                this.plugin.saveConfig();
                                player.sendMessage(plugin.getPrefix()+plugin.getConfig().getString("main.successResourceSpawnerAdded")+this.plugin.getPlayerUtilsInstance().getResourceById(slot));
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
        }else if(plugin.getGameState() == GameState.INGAME){
            //specialni itemy ve hre
            if(item.getType() == Material.FIRE_CHARGE){
                event.setCancelled(true);
                item.setAmount(item.getAmount()-1); //sebrat 1 item z ruky
                this.plugin.getPlayerUtilsInstance().shootFireball(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event){
        Player pl = event.getPlayer();
        Item it = event.getItemDrop();
        ItemStack is = it.getItemStack();
        ItemMeta im = is.getItemMeta();
        if(im == null) return;
        try {
            if(!im.hasLore()) return;
            ArrayList<String> lore = new ArrayList<>(Objects.requireNonNull(im.getLore()));
            for (String line : lore) {
                if (line.contains(Objects.requireNonNull(this.plugin.getConfig().getString("main.soulBoundLore")))) {
                    it.remove();
                    pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    break;
                }
                if(line.contains(Objects.requireNonNull(this.plugin.getConfig().getString("main.unDroppableLore")))){
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player player = (Player) event.getWhoClicked();
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
            if(slot >= teamCount) return; //kliknuto mimo vlny - ignorovat
            event.setCancelled(true);
            player.closeInventory();
            //Neni tym plny? Tym balancer
            int maxInTeam = this.plugin.getPlayerUtilsInstance().getMaxInTeam();
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
                ArrayList<ItemStack> list = this.plugin.getShopUtilsInstance().getShopCategoryListItems();
                for(int i=0; i<list.size(); i++){
                    ItemStack is = list.get(i);
                    if(i == slot){
                        is.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                    }
                    open.setItem(i, is);
                }
                ArrayList<ItemStack> goodsList = this.plugin.getShopUtilsInstance().getShopCategoryItems(slot, player);
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
                ArrayList<ItemStack> list = this.plugin.getShopUtilsInstance().getShopCategoryItems(selectedCategory, player);
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
                    if(im == null || !im.hasLore()){
                        return; //kliknuto na item bez ceny, nic nedelat
                    }
                    List<String> lore = im.getLore();
                    String priceLine = Objects.requireNonNull(lore).get(lore.size()-1);
                    String[] priceSplit = priceLine.split(" ");
                    int priceNumber = Integer.parseInt(priceSplit[0].substring(2));
                    String priceItem = priceSplit[1];
                    //Zkontrolovat, jestli hrac ma dostatek potrebnych itemu
                    ItemStack[] invContents = player.getInventory().getStorageContents();
                    int itemsLeft = priceNumber;
                    Material neededMaterial = this.plugin.getPlayerUtilsInstance().getMaterialFromResourceName(priceItem);
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
                    if(itemsLeft == 0){
                        //Hrac ma dostatek itemu
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
                        this.plugin.getShopUtilsInstance().processShopBoughtItem(player, selectedCategory, clickedItem);
                    }else{
                        player.sendMessage(this.plugin.getPrefix()+this.plugin.getConfig().getString("game.shopNotEnoughResources"));
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
        if (this.plugin.getGameState() == GameState.INGAME || this.plugin.getGameState() == GameState.RESTARTING) {
            if(this.plugin.teamHasBed(team)) {
                Location respawnLoc = plugin.teamSpawns.get(team);
                event.setRespawnLocation(respawnLoc);
                this.plugin.getPlayerUtilsInstance().setPlayersArmor(player);
            }else{
                player.setGameMode(GameMode.SPECTATOR);
                Location respawnLoc = plugin.teamSpawns.get(0);
                event.setRespawnLocation(respawnLoc);
                new BukkitRunnable(){
                    @Override
                    public void run(){
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                }.runTaskLater(plugin, 5);
            }
        }else{
            //Teleportovat hrace do herniho lobby
            Location lobby = this.plugin.getPlayerUtilsInstance().getLobby();
            if(lobby != null){
                player.teleport(lobby);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){ //dropovat pri smrti pouze dia a emeraldy
        Player player = event.getEntity();
        int team = this.plugin.getTeamOfPlayer(player);
        String color = this.plugin.getPlayerUtilsInstance().getColorOfNthTeam(team).toString();
        String oldMsg = event.getDeathMessage();
        if(oldMsg == null) oldMsg = "";
        String newMsg;
        if(oldMsg.contains("was slain by")){
            String killer = oldMsg.substring(oldMsg.indexOf("was slain by ")+"was slain by ".length());
            killer = killer.substring(0, killer.indexOf(" "));
            String killerColor = "§8";
            try{
                Player killerPlayer = Bukkit.getPlayer(killer);
                killerColor = this.plugin.getPlayerUtilsInstance().getColorOfNthTeam(this.plugin.getTeamOfPlayer(killerPlayer)).toString();
            }catch (Exception e){/*neni to hrac*/}
            newMsg = Objects.requireNonNull(plugin.getConfig().getString("game.playerKilledBy"))
                    .replace("%victimColor%", color)
                    .replace("%victim%", player.getName())
                    .replace("%killerColor%", killerColor)
                    .replace("%killer%", killer);
        }else{
            newMsg = Objects.requireNonNull(plugin.getConfig().getString("game.playerDied"))
                    .replace("%victimColor%", color)
                    .replace("%victim%", player.getName());
        }
        event.setDeathMessage(newMsg);
        List<ItemStack> drops = event.getDrops();
        for(int i=0; i < drops.size(); i++){
            if(drops.get(i) == null) continue;
            if(drops.get(i).getType() != Material.DIAMOND && drops.get(i).getType() != Material.EMERALD){
                drops.set(i, null);
            }
        }
        if(!this.plugin.teamHasBed(team)){
            String message = Objects.requireNonNull(this.plugin.getConfig().getString("game.playerEliminated"))
                    .replace("%player%", player.getName())
                    .replace("%playerColor%", plugin.getPlayerUtilsInstance().getColorOfNthTeam(team).toString());
            Bukkit.broadcastMessage(this.plugin.getPrefix()+message);
            this.plugin.setPlayerToSpectator(player);
            plugin.checkEndGame();
        }
        new BukkitRunnable(){
            @Override
            public void run() {
                if(player.isDead()){
                    player.spigot().respawn();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
        //player.spigot().respawn();
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            Entity attacker = event.getDamager();
            if(attacker.getType() == EntityType.PLAYER){ //utoci hrac na hrace
                Player attackerPlayer = (Player) attacker;
                if(player.getHealth()-event.getFinalDamage()<=0){ //zabil ho
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
        if(plugin.getGameState() != GameState.INGAME){
            return; //arena neni ve hre
        }
        //Hrac klikl na blok
        Block block = event.getClickedBlock();
        if(block == null) return;
        if(!block.getType().toString().contains("SIGN")){
            return;
        }
        //Hrac klikl na cedulku
        Sign sign = (Sign) block.getState();
        String line1 = sign.getLine(0);
        if(line1.toLowerCase().contains("shop")){
            //Je to cedulka shopu
            this.plugin.getShopUtilsInstance().openShopMenu(player);
        }
    }

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event){
        if(!(event.getEntity() instanceof Fireball)){
            return;
        }
        //je to fireball
        if(this.plugin.getGameState() != GameState.INGAME){
            return;
        }
        //hra je spustena
        Block hitBlock = event.getHitBlock();
        if(hitBlock == null){
            hitBlock = Objects.requireNonNull(event.getHitEntity()).getLocation().getBlock();
        }
        //vzit bloky v okoli a znicit je
        hitBlock.getWorld().playEffect(hitBlock.getLocation(), Effect.SMOKE, 0);
        int blockX = hitBlock.getX();
        int blockY = hitBlock.getY();
        int blockZ = hitBlock.getZ();
        for (int x = blockX - 1; x <= blockX + 1; x++) {
            for (int y = blockY - 1; y <= blockY + 1; y++) {
                for (int z = blockZ - 1; z <= blockZ + 1; z++) {
                    Block toDelete = hitBlock.getWorld().getBlockAt(x, y, z);
                    if(plugin.getBlockBuildingInstance().containsBlock(toDelete)){
                        if(toDelete.getType() != Material.OBSIDIAN) {
                            toDelete.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(this.plugin.getGameState() == GameState.INGAME) {
            this.plugin.getBlockBuildingInstance().addBlock(event.getBlockPlaced());
        } else {
            if(player.getGameMode() != GameMode.CREATIVE) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if(this.plugin.getGameState() == GameState.INGAME) {
            if (block.getType().toString().contains("BED")) {
                int team = this.plugin.getPlayerUtilsInstance().getTeamByBedBlock(block);
                int playersTeam = this.plugin.getTeamOfPlayer(player);
                if (team == -1) {
                    //je to random postel
                    event.setCancelled(true);
                } else if (team == playersTeam) {
                    //je to postel tymu hrace - nemuze znicit svou
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + plugin.getConfig().getString("game.cannotDestroyOwnBed"));
                } else {
                    //je to postel jineho tymu - znicit, pokud je vubec team ve hre
                    if (this.plugin.teamHasBed(team)) {
                        event.setDropItems(false);
                        plugin.destroyTeamBed(team);
                        String message = Objects.requireNonNull(plugin.getConfig().getString("game.bedDestroyed"))
                                .replace("%player%", player.getName())
                                .replace("%destroyedTeam%", plugin.getPlayerUtilsInstance().getNameOfNthTeam(team))
                                .replace("%playerColor%", plugin.getPlayerUtilsInstance().getColorOfNthTeam(playersTeam).toString());
                        Bukkit.broadcastMessage(plugin.getPrefix() + message);
                        ArrayList<Player> victimPlayers = plugin.getPlayersInTeam(team);
                        for(Player p : victimPlayers){
                            p.sendTitle(plugin.getConfig().getString("game.destroyedYourBedTitle"), plugin.getConfig().getString("game.destroyedYourBedSubTitle"), 0, 45, 15);
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
                        }
                    }else{
                        event.setCancelled(true);
                    }
                }
            } else {
                //neni to postel
                if (!plugin.getBlockBuildingInstance().containsBlock(block)) {
                    //tento blok nebyl postaven hracem
                    event.setCancelled(true);
                }
            }
        } else {
            if(player.getGameMode() != GameMode.CREATIVE) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event){
        Item item = event.getEntity();
        ItemStack is = item.getItemStack();
        ItemMeta im = is.getItemMeta();
        if(im != null) {
            im.setDisplayName(this.plugin.getShopUtilsInstance().getNameOfMaterial(is.getType()));
            is.setItemMeta(im);
        }
        item.setItemStack(is);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event){
        if(plugin.getGameState() != GameState.INGAME){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event){
        event.setCancelled(true);
        Player player = event.getPlayer();
        String msg = event.getMessage();
        if(plugin.getGameState() != GameState.INGAME){
            String format = Objects.requireNonNull(plugin.getConfig().getString("main.chatFormatLobby"))
                    .replace("%player%", player.getName())
                    .replace("%message%", msg);
            Bukkit.broadcastMessage(format);
        }else{
            int team = plugin.getTeamOfPlayer(player);
            if(team < 0){
                //hrac neni ve hre - je spec - chat mu nepujde
                return;
            }
            String color = this.plugin.getPlayerUtilsInstance().getColorOfNthTeam(team).toString();
            //ingame
            if(msg.charAt(0) == '!'){
                //all chat
                String format = Objects.requireNonNull(plugin.getConfig().getString("main.chatFormatIngameAll"))
                        .replace("%playerColor%", color)
                        .replace("%player%", player.getName())
                        .replace("%message%", msg.substring(1));
                Bukkit.broadcastMessage(format);
            }else{
                //team chat
                String format = Objects.requireNonNull(plugin.getConfig().getString("main.chatFormatIngameTeam"))
                        .replace("%playerColor%", color)
                        .replace("%player%", player.getName())
                        .replace("%message%", msg);
                List<Player> playersInTeam = plugin.getPlayersInTeam(team);
                for(Player p : playersInTeam){
                    p.sendMessage(format);
                }
            }
        }
    }

}
