package com.whitehouse.bedwars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BedWars extends JavaPlugin {

    private GameState gameState = GameState.LOBBY;
    private int startTime;
    private MyScoreboard myScoreboardInstance;
    private MapRegenerator mapRegeneratorInstance;
    private PlayerUtils playerUtilsInstance;
    private ShopUtils shopUtilsInstance;
    private BlockBuilding blockBuildingInstance;
    private final HashMap<Integer, ArrayList<Player>> playerTeams = new HashMap<>();
    private final HashMap<Integer, Boolean> teamBeds = new HashMap<>();
    private Random random;
    public final List<Location> teamSpawns = new ArrayList<>();
    private final HashMap<Player, Integer> playerArmor = new HashMap<>();

    private void setLobbyScoreboard(){
        List<String> lines = Objects.requireNonNull(getConfig().getStringList("main.scoreboardLobbyLines"));
        for(int i=0; i<lines.size(); i++){
            String line = lines.get(i);
            if(!line.contains("|")) line += "|";
            String[] prefSuf = line.split("[|]", 2);
            this.myScoreboardInstance.setLine(i, prefSuf[0], prefSuf[1]);
        }
        this.myScoreboardInstance.setLineCount(lines.size());
    }

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        //eventy
        Events eventsInstance = new Events(this);
        pm.registerEvents(eventsInstance, this);
        //prikazy
        Objects.requireNonNull(getCommand("bw-setup")).setExecutor(new SetupCommand(this));
        this.saveDefaultConfig();
        this.reloadConfig();
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
        this.myScoreboardInstance = new MyScoreboard(this);
        this.mapRegeneratorInstance = new MapRegenerator(this);
        this.playerUtilsInstance = new PlayerUtils(this);
        this.shopUtilsInstance = new ShopUtils(this);
        this.blockBuildingInstance = new BlockBuilding();
        this.random = new Random();
        setLobbyScoreboard();
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling!");
    }

    public String getPrefix(){
        return getConfig().getString("main.prefix");
    }

    public int getStartTime(){
        return this.startTime;
    }

    public GameState getGameState(){
        return this.gameState;
    }

    public MyScoreboard getMyScoreboardInstance(){
        return this.myScoreboardInstance;
    }

    public MapRegenerator getMapRegeneratorInstance(){
        return this.mapRegeneratorInstance;
    }

    public PlayerUtils getPlayerUtilsInstance(){
        return this.playerUtilsInstance;
    }

    public ShopUtils getShopUtilsInstance(){
        return this.shopUtilsInstance;
    }

    public BlockBuilding getBlockBuildingInstance(){
        return this.blockBuildingInstance;
    }

    private void startGameAndLoop(){
        this.reloadConfig();
        int teamCount = getConfig().getInt("arena.teams");

        //Nacist team spawny (viz nize reseni spawneru)
        //Spawner: team spawn
        //List<Location> resourceSpawners_teamSpawns = new ArrayList<Location>();
        teamSpawns.clear();
        for(int i=0; i<teamCount; i++){
            try {
                String loc = getConfig().getString("arena.spawn." + i);
                String[] split = Objects.requireNonNull(loc).split(";");
                double x = Double.parseDouble(split[0]);
                double y = Double.parseDouble(split[1]);
                double z = Double.parseDouble(split[2]);
                float yaw = Float.parseFloat(split[3]);
                float pitch = Float.parseFloat(split[4]);
                Location location = new Location(Bukkit.getWorld("world"), x, y, z, yaw, pitch);
                teamSpawns.add(location);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //Dat vsechny hrace do nejakeho tymu, pak jim smazat inv a teleportovat je na team spawn
        ArrayList<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        for(Player p : onlinePlayers){
            int team = getTeamOfPlayer(p);
            if(team == -1){ //hrac neni v tymu
                team = getTeamWithLowestCount();
                addPlayerToTeamAndRemoveFromOthers(team, p);
            }
            //Vsichni hraci jsou v nejakem tymu
            p.getInventory().clear();
            p.teleport(teamSpawns.get(team));
            p.setGameMode(GameMode.SURVIVAL);
            this.playerUtilsInstance.setPlayersArmor(p);
            myScoreboardInstance.removePlayerFromAllTeams(p);
            this.getMyScoreboardInstance().addPlayerToTeam(team, p);
            //Vycisten inventar a teleportovan
        }

        //Smazat dropy
        Collection<Item> drops = onlinePlayers.get(0).getWorld().getEntitiesByClass(Item.class);
        for(Item ent : drops){
            ent.remove();
        }

        //Udelat scoreboard:
        this.myScoreboardInstance.setLine(0, "", "");
        for(int i=1; i<=teamCount; i++) {
            boolean hasBed = (getPlayersInTeam(i-1).size() > 0);
            this.teamBeds.put(i-1, hasBed);
            String suffix = (hasBed ? getConfig().getString("game.charHasBed") : getConfig().getString("game.charEliminated"));
            this.myScoreboardInstance.setLine(i, this.playerUtilsInstance.getNameOfNthTeam(i-1), suffix);
        }
        this.myScoreboardInstance.setLineCount(teamCount+1);

        int min_x, max_x, min_y, max_y, min_z, max_z;
        //Nacteni hranic areny
        String bound1Loc = getConfig().getString("arena.bound1", null);
        String bound2Loc = getConfig().getString("arena.bound2", null);
        World world = Bukkit.getWorld("world");
        if(bound1Loc == null || bound2Loc == null || world == null){
            getLogger().info("§cCannot start game, arena has not set bounds.");
            Bukkit.broadcastMessage(getPrefix()+"§cArena nelze spustit, protoze neni nastavena!");
            return;
        }else {
            String[] split = bound1Loc.split(";");
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            Block bound1 = world.getBlockAt(x, y, z);
            split = bound2Loc.split(";");
            x = Integer.parseInt(split[0]);
            y = Integer.parseInt(split[1]);
            z = Integer.parseInt(split[2]);
            Block bound2 = world.getBlockAt(x, y, z);
            if (bound1.getX() < bound2.getX()) {
                min_x = bound1.getX();
                max_x = bound2.getX();
            } else {
                min_x = bound2.getX();
                max_x = bound1.getX();
            }
            if (bound1.getY() < bound2.getY()) {
                min_y = bound1.getY();
                max_y = bound2.getY();
            } else {
                min_y = bound2.getY();
                max_y = bound1.getY();
            }
            if (bound1.getZ() < bound2.getZ()) {
                min_z = bound1.getZ();
                max_z = bound2.getZ();
            } else {
                min_z = bound2.getZ();
                max_z = bound1.getZ();
            }
        }
        //Hranice jsou nyni v [min|max]_[x|y|z]

        /* Vyresit spawnery:
        Nejdriv si nactu vsechny mozny resource spawnery do listu,
        pak budu jen prochazet jednotlive listy a z nich spawnovat.
         */

        //Spawner: iron
        List<Location> resourceSpawners_2 = new ArrayList<>();
        List<String> listFromConfig = getConfig().getStringList("arena.resources.iron");
        for(String s : listFromConfig){
            String[] split = s.split(";");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            Location location = new Location(Bukkit.getWorld("world"), x, y, z);
            resourceSpawners_2.add(location);
        }

        //Spawner: gold
        List<Location> resourceSpawners_3 = new ArrayList<>();
        listFromConfig = getConfig().getStringList("arena.resources.gold");
        for(String s : listFromConfig){
            String[] split = s.split(";");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            Location location = new Location(Bukkit.getWorld("world"), x, y, z);
            resourceSpawners_3.add(location);
        }

        //Spawner: diamond
        List<Location> resourceSpawners_4 = new ArrayList<>();
        listFromConfig = getConfig().getStringList("arena.resources.diamond");
        for(String s : listFromConfig){
            String[] split = s.split(";");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            Location location = new Location(Bukkit.getWorld("world"), x, y, z);
            resourceSpawners_4.add(location);
        }

        //Spustit game loop
        this.startTime = 0;
        //private long startTime = 0;
        //Vypnout game loop kdyz se prerusi hra
        //kazdych 10 ticku projde tato funkce
        //Game Loop:
        //Checknout konec hry pro jistotu
        //kazde 2 sekundy
        //Updatovani scoreboardu
        //Spawnout vsechny itemy, co se maji spawnout
        //base iron spawner - kazdych 10 ticku
        //radius 2
        //base gold spawner - kazdych 100 ticku
        //radius 2
        //iron spawner - kazdych 20 ticku
        //List<String> spawnerList1 = getConfig().getStringList();
        //gold spawner - kazdych 40 ticku
        //diamond spawner - kazdych 300 ticku
        BukkitRunnable gameLoop = new BukkitRunnable() {
            //private long startTime = 0;
            @Override
            public void run() {
                if (getGameState() != GameState.INGAME) {
                    //Vypnout game loop kdyz se prerusi hra
                    this.cancel();
                    return;
                }
                startTime++; //kazdych 10 ticku projde tato funkce
                //Game Loop:
                //Checknout konec hry pro jistotu
                if (startTime % 4 == 0) { //kazde 2 sekundy
                    checkEndGame();
                }
                //Zkontrolovat hrace, jestli jsou uvnitr areny
                if (startTime % 2 == 0) { //kazdou sekundu
                    ArrayList<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    for (Player p : onlinePlayers) {
                        int team = getTeamOfPlayer(p);
                        Location location = p.getLocation();
                        if (location.getBlockX() < min_x || location.getBlockX() > max_x ||
                                location.getBlockY() > max_y ||
                                location.getBlockZ() < min_z || location.getBlockZ() > max_z){
                            //hrac je mimo mapu (nepocita se kdyz je pod ni, aby to nespamovalo ve voidu)
                            if (team == -1) { //hrac neni v tymu
                                p.teleport(teamSpawns.get(0));
                            }else{
                                //dat damage hraci
                                p.damage(6);
                                p.sendMessage(getPrefix()+getConfig().getString("game.outsideArena"));
                            }
                        }
                    }
                }
                //Updatovani scoreboardu
                int seconds = startTime / 2;
                int minutes = seconds / 60;
                seconds -= minutes * 60;
                String firstLine = Objects.requireNonNull(getConfig().getString("game.scoreboardFirstLineIngame"))
                        .replace("%time%", String.format("%02d:%02d", minutes, seconds));
                if(!firstLine.contains("|")) firstLine += "|";
                String[] prefSuf = firstLine.split("[|]", 2);
                myScoreboardInstance.setLine(0, prefSuf[0], prefSuf[1]);
                for (int i = 1; i <= teamCount; i++) {
                    boolean hasBed = teamBeds.get(i - 1);
                    String suffix = (hasBed ? getConfig().getString("game.charHasBed") : (getPlayersInTeam(i - 1).size() > 0 ? " §e" + getPlayersInTeam(i - 1).size() : getConfig().getString("game.charEliminated")));
                    myScoreboardInstance.setLine(i, playerUtilsInstance.getNameOfNthTeam(i - 1), suffix);
                }
                //Spawnout vsechny itemy, co se maji spawnout
                World world = Objects.requireNonNull(teamSpawns.get(0).getWorld());
                //base iron spawner - kazdych 10 ticku
                for (Location l : teamSpawns) {
                    Location loc = l.clone();
                    loc.add(2 * random.nextDouble() - 1.0, 0, 2 * random.nextDouble() - 1.0); //radius 2
                    ItemStack is = new ItemStack(Material.IRON_INGOT);
                    world.dropItem(loc, is);
                }
                //base gold spawner - kazdych 100 ticku
                if (startTime % 10 == 0) {
                    for (Location l : teamSpawns) {
                        Location loc = l.clone();
                        loc.add(2 * random.nextDouble() - 1.0, 0, 2 * random.nextDouble() - 1.0); //radius 2
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        world.dropItem(loc, is);
                    }
                }
                //iron spawner - kazdych 20 ticku
                if (startTime % 2 == 0) {
                    for (Location l : resourceSpawners_2) {
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.IRON_INGOT);
                        world.dropItemNaturally(loc, is);
                    }
                }
                //List<String> spawnerList1 = getConfig().getStringList();
                //gold spawner - kazdych 40 ticku
                if (startTime % 4 == 0) {
                    for (Location l : resourceSpawners_3) {
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        world.dropItemNaturally(loc, is);
                    }
                }
                //diamond spawner - kazdych 300 ticku
                if (startTime % 30 == 0) {
                    for (Location l : resourceSpawners_4) {
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.DIAMOND);
                        world.dropItemNaturally(loc, is);
                    }
                }
            }
        };
        gameLoop.runTaskTimer(this, 10, 10);
    }

    public void setGameStarting(boolean starting){
        if(starting){
            this.gameState = GameState.STARTING;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.startingMessage"));
            this.startTime = getConfig().getInt("game.startTime");
            //Scoreboard
            List<String> lines = Objects.requireNonNull(getConfig().getStringList("main.scoreboardStartingLines"));
            for(int i=0; i<lines.size(); i++){
                String line = lines.get(i)
                        .replace("%seconds%", String.valueOf(this.startTime))
                        .replace("%suffix%", PlayerUtils.sklon(this.startTime, "u", "y", ""));
                if(!line.contains("|")) line += "|";
                String[] prefSuf = line.split("[|]", 2);
                this.myScoreboardInstance.setLine(i, prefSuf[0], prefSuf[1]);
            }
            this.myScoreboardInstance.setLineCount(lines.size());
            //Spustit casovac
            new BukkitRunnable(){
                @Override
                public void run(){
                    if(gameState == GameState.SETUP){
                        //Pokud se zapne SETUP, musi se zrusit start hry
                        this.cancel();
                        return;
                    }
                    if(Bukkit.getOnlinePlayers().size() < getConfig().getInt("game.minPlayers")){
                        //Nekdo odesel, zrusit startovani
                        setGameStarting(false);
                        this.cancel();
                        return;
                    }
                    startTime--;
                    if(startTime == 0){
                        Bukkit.broadcastMessage(getPrefix()+Objects.requireNonNull(getConfig().getString("game.gameStarted")));
                        gameState = GameState.INGAME;
                        startGameAndLoop();
                        this.cancel();
                        return;
                    }
                    if(startTime % 5 == 0 || startTime < 5) {
                        String messageStartingIn = Objects.requireNonNull(getConfig().getString("main.gameStartingIn"))
                                .replace("%seconds%", String.valueOf(startTime))
                                .replace("%suffix%", PlayerUtils.sklon(startTime, "u", "y", ""));
                        Bukkit.broadcastMessage(getPrefix() + messageStartingIn);
                    }
                    //Scoreboard
                    List<String> lines = Objects.requireNonNull(getConfig().getStringList("main.scoreboardStartingLines"));
                    for(int i=0; i<lines.size(); i++){
                        String line = lines.get(i)
                                .replace("%seconds%", String.valueOf(startTime))
                                .replace("%suffix%", PlayerUtils.sklon(startTime, "u", "y", ""));
                        if(!line.contains("|")) line += "|";
                        String[] prefSuf = line.split("[|]", 2);
                        myScoreboardInstance.setLine(i, prefSuf[0], prefSuf[1]);
                    }
                    myScoreboardInstance.setLineCount(lines.size());
                }
            }.runTaskTimer(this, 20, 20);
        }else{
            this.gameState = GameState.LOBBY;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.notStartingMessage"));
            setLobbyScoreboard();
        }
    }

    public ArrayList<Player> getPlayersInTeam(int team){
        ArrayList<Player> list = this.playerTeams.get(team);
        if(list == null) return new ArrayList<>();
        return list;
    }

    public void addPlayerToTeam(int team, Player player){
        ArrayList<Player> list = this.getPlayersInTeam(team);
        if(!list.contains(player)){
            list.add(player);
            this.playerTeams.put(team, list);
        }
    }

    public void removePlayerFromTeam(int team, Player player){
        ArrayList<Player> list = this.getPlayersInTeam(team);
        if(list.contains(player)){
            list.remove(player);
            this.playerTeams.put(team, list);
            //neni team prazdny?
            if(list.isEmpty()){
                //vyradit team
                this.destroyTeamBed(team);
                this.checkEndGame();
            }
        }
    }

    public int getTeamOfPlayer(Player player){
        int numOfTeams = this.getConfig().getInt("arena.teams");
        for(int i=0; i<numOfTeams; i++){
            ArrayList<Player> list = this.getPlayersInTeam(i);
            if(list.contains(player)) return i;
        }
        return -1;
    }

    public int getTeamWithLowestCount(){
        int numOfTeams = this.getConfig().getInt("arena.teams");
        assert numOfTeams > 1;
        int minValue = this.getPlayersInTeam(0).size();
        int minIndex = 0;
        for(int i=1; i<numOfTeams; i++){
            int count = this.getPlayersInTeam(i).size();
            if(count < minValue){
                minValue = count;
                minIndex = i;
            }
        }
        return minIndex;
    }

    public void addPlayerToTeamAndRemoveFromOthers(int team, Player player){
        int teamCount = this.getConfig().getInt("arena.teams");
        for (int i = 0; i < teamCount; i++) {
            if (i == team) this.addPlayerToTeam(i, player);
            else this.removePlayerFromTeam(i, player);
        }
        myScoreboardInstance.removePlayerFromAllTeams(player);
        myScoreboardInstance.addPlayerToTeam(team, player);
        player.sendMessage(getPrefix()+getConfig().getString("main.joinedTeam")+playerUtilsInstance.getNameOfNthTeam(team));
    }

    public void setPlayerToSpectator(Player player){
        int teamCount = this.getConfig().getInt("arena.teams");
        for (int i = 0; i < teamCount; i++) {
            this.removePlayerFromTeam(i, player);
        }
        this.myScoreboardInstance.removePlayerFromAllTeams(player);
        this.myScoreboardInstance.addPlayerToSpectatorTeam(player);
    }

    public void enableSetup(){
        this.gameState = GameState.SETUP;
    }

    public void disableSetup(){
        this.gameState = GameState.LOBBY;
    }

    public boolean teamHasBed(int team){
        return this.teamBeds.getOrDefault(team, false);
    }

    public void destroyTeamBed(int team){
        this.teamBeds.put(team, false);
    }

    public int getPlayerArmor(Player player){
        return this.playerArmor.getOrDefault(player, 0);
    }

    public void setPlayerArmor(Player player, int armor){
        this.playerArmor.put(player, armor);
    }

    public void checkEndGame(){
        if(this.gameState != GameState.INGAME){
            return; //pokud neni stav INGAME, nema smysl kontrolovat konec hry
        }
        //Kdy skonci hra? Kdyz jsou ve hre pouze hraci jednoho tymu.
        int teamCount = this.getConfig().getInt("arena.teams");
        ArrayList<Integer> notEmptyTeams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            if(this.getPlayersInTeam(i).size() > 0) notEmptyTeams.add(i);
        }
        if(notEmptyTeams.size()<2){
            //Bud jsou hraci v jednom tymu, nebo neni nikdo ve hre (divny, ale treba se to stane)
            this.endGame();
        }
    }

    public void endGame(){
        //Ukoncit hru
        this.gameState = GameState.RESTARTING;
        Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.arenaRestarting"));
        ArrayList<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        for(Player p : onlinePlayers) {
            int team = getTeamOfPlayer(p);
            if(team >= 0){
                //Hrac je v tymu (zde se muze popr davat odmena)
                p.sendMessage(getPrefix()+getConfig().getString("game.youHaveWon"));
            }
            //kazdemu sundat team
            this.myScoreboardInstance.removePlayerFromAllTeams(p);
            //dat kazdemu spectatora
            p.setGameMode(GameMode.SPECTATOR);
        }
        //nastavit scoreboard
        myScoreboardInstance.setLineCount(2);
        myScoreboardInstance.setLine(1, "", "");
        //schedulnout restart areny na 10 sekund
        new BukkitRunnable(){
            private int timeToReset = 10;
            @Override
            public void run(){
                timeToReset--;
                if(timeToReset == 0){
                    Bukkit.broadcastMessage(getPrefix()+Objects.requireNonNull(getConfig().getString("game.arenaRestartingNow")));
                    gameState = GameState.LOBBY;
                    mapRegeneratorInstance.regenMap(null); //zregenerovat bloky
                    //smazat vsechny armory
                    playerArmor.clear();
                    //smazat vsechny tymy
                    playerTeams.clear();
                    //teleportovat vsechny na lobby a udelat fake join
                    ArrayList<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    for(Player p : onlinePlayers) {
                        getPlayerUtilsInstance().handlePlayerJoin(p);
                    }
                    //nastavit lobby scoreboard
                    setLobbyScoreboard();
                    this.cancel();
                    return;
                }
                String restartInMessage = Objects.requireNonNull(getConfig().getString("game.arenaRestartingIn"))
                        .replace("%seconds%", String.valueOf(timeToReset))
                        .replace("%suffix%", PlayerUtils.sklon(timeToReset, "u", "y", ""));
                Bukkit.broadcastMessage(getPrefix()+restartInMessage);
                String secondLine = Objects.requireNonNull(getConfig().getString("game.scoreboardSecondLineRestarting"))
                        .replace("%seconds%", String.valueOf(timeToReset))
                        .replace("%suffix%", PlayerUtils.sklon(timeToReset, "u", "y", ""));
                if(!secondLine.contains("|")) secondLine += "|";
                String[] prefSuf = secondLine.split("[|]", 2);
                myScoreboardInstance.setLine(1, prefSuf[0], prefSuf[1]);
            }
        }.runTaskTimer(this, 20, 20);
    }

}
