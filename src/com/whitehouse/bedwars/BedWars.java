package com.whitehouse.bedwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BedWars extends JavaPlugin {

    private GameState gameState = GameState.LOBBY;
    private int startTime;
    private Events eventsInstance;
    private Menu menuInstance;
    private MyScoreboard myScoreboardInstance;
    private MapRegenerator mapRegeneratorInstance;
    private HashMap<Integer, ArrayList<Player>> playerTeams = new HashMap<Integer, ArrayList<Player>>();
    private HashMap<Integer, Boolean> teamBeds = new HashMap<Integer, Boolean>();
    private BukkitRunnable gameLoop;
    private Random random;
    public List<Location> teamSpawns = new ArrayList<Location>();
    private HashMap<Player, Integer> playerArmor = new HashMap<Player, Integer>();

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        //eventy
        this.eventsInstance = new Events(this);
        pm.registerEvents(this.eventsInstance, this);
        //prikazy - TBD
        getCommand("bw-setup").setExecutor(new SetupCommand(this));
        this.saveDefaultConfig();
        this.reloadConfig();
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
        this.menuInstance = new Menu(this);
        this.myScoreboardInstance = new MyScoreboard(this);
        this.mapRegeneratorInstance = new MapRegenerator(this);
        this.random = new Random();
        this.myScoreboardInstance.setLine(0, "Lobby", "");
        this.myScoreboardInstance.setLine(1, "Pripojujte se", "");
        this.myScoreboardInstance.setLineCount(2);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling!");
    }

    public String getPrefix(){
        return getConfig().getString("main.prefix");
    }

    public GameState getGameState(){
        return this.gameState;
    }

    public Menu getMenuInstance(){
        return this.menuInstance;
    }

    public MyScoreboard getMyScoreboardInstance(){
        return this.myScoreboardInstance;
    }

    public MapRegenerator getMapRegeneratorInstance(){
        return this.mapRegeneratorInstance;
    }

    public Events getEventsInstance(){
        return this.eventsInstance;
    }

    private void startGameAndLoop(){
        this.reloadConfig();
        int teamCount = getConfig().getInt("arena.teams");

        //Nacist team spawny (viz nize reseni spawneru)
        //Spawner: team spawn
        //List<Location> resourceSpawners_teamSpawns = new ArrayList<Location>();
        teamSpawns.clear();
        for(int i=0; i<teamCount; i++){
            String loc = getConfig().getString("arena.spawn."+i);
            String[] split = loc.split(";");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            float yaw = Float.parseFloat(split[3]);
            float pitch = Float.parseFloat(split[4]);
            Location location = new Location(Bukkit.getWorld("world"), x, y, z, yaw, pitch);
            teamSpawns.add(location);
        }

        //Dat vsechny hrace do nejakeho tymu, pak jim smazat inv a teleportovat je na team spawn
        ArrayList<Player> onlinePlayers = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        for(Player p : onlinePlayers){
            int team = getTeamOfPlayer(p);
            if(team == -1){ //hrac neni v tymu
                team = getTeamWithLowestCount();
                addPlayerToTeamAndRemoveFromOthers(team, p);
            }
            //Vsichni hraci jsou v nejakem tymu
            p.getInventory().clear();
            p.teleport(teamSpawns.get(team));
            this.eventsInstance.setPlayersArmor(p);
            this.getMyScoreboardInstance().addPlayerToTeam(team, p);
            //Vycisten inventar a teleportovan
        }

        //Udelat scoreboard:
        this.myScoreboardInstance.setLine(0, "Arena je ve ", "§fhre");
        for(int i=1; i<=teamCount; i++) {
            boolean hasBed = (getPlayersInTeam(i-1).size() > 0);
            this.teamBeds.put(i-1, hasBed);
            String suffix = (hasBed ? getConfig().getString("game.charHasBed") : getConfig().getString("game.charEliminated"));
            this.myScoreboardInstance.setLine(i, this.menuInstance.getNameOfNthTeam(i-1), suffix);
        }
        this.myScoreboardInstance.setLineCount(teamCount+1);

        /* Vyresit spawnery:
        Nejdriv si nactu vsechny mozny resource spawnery do listu,
        pak budu jen prochazet jednotlive listy a z nich spawnovat.
         */

        //Spawner: iron
        List<Location> resourceSpawners_2 = new ArrayList<Location>();
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
        List<Location> resourceSpawners_3 = new ArrayList<Location>();
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
        List<Location> resourceSpawners_4 = new ArrayList<Location>();
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
        this.gameLoop = new BukkitRunnable() {
            //private long startTime = 0;
            @Override
            public void run() {
                if(getGameState() != GameState.INGAME){
                    //Vypnout game loop kdyz se prerusi hra
                    this.cancel();
                    return;
                }
                startTime++; //kazdych 10 ticku projde tato funkce
                //Game Loop:
                //Updatovani scoreboardu
                int seconds = startTime/2;
                int minutes = seconds/60;
                seconds -= minutes*60;
                myScoreboardInstance.setLine(0, "Hra bezi uz: ", String.format("§a%02d:%02d", minutes, seconds));
                for(int i=1; i<=teamCount; i++) {
                    boolean hasBed = teamBeds.get(i-1);
                    String suffix = (hasBed ? getConfig().getString("game.charHasBed") : (getPlayersInTeam(i-1).size()>0? " §e"+getPlayersInTeam(i-1).size() : getConfig().getString("game.charEliminated")));
                    myScoreboardInstance.setLine(i, menuInstance.getNameOfNthTeam(i-1), suffix);
                }
                //Spawnout vsechny itemy, co se maji spawnout
                //base iron spawner - kazdych 10 ticku
                for(Location l : teamSpawns){
                    Location loc = l.clone();
                    loc.add(2*random.nextDouble()-1.0, 0, 2*random.nextDouble()-1.0); //radius 2
                    ItemStack is = new ItemStack(Material.IRON_INGOT);
                    loc.getWorld().dropItem(loc, is);
                }
                //base gold spawner - kazdych 100 ticku
                if(startTime %10 == 0){
                    for(Location l : teamSpawns){
                        Location loc = l.clone();
                        loc.add(2*random.nextDouble()-1.0, 0, 2*random.nextDouble()-1.0); //radius 2
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        loc.getWorld().dropItem(loc, is);
                    }
                }
                //iron spawner - kazdych 20 ticku
                if(startTime %2 == 0){
                    for(Location l : resourceSpawners_2){
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.IRON_INGOT);
                        loc.getWorld().dropItemNaturally(loc, is);
                    }
                }
                //List<String> spawnerList1 = getConfig().getStringList();
                //gold spawner - kazdych 40 ticku
                if(startTime %4 == 0){
                    for(Location l : resourceSpawners_3){
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        loc.getWorld().dropItemNaturally(loc, is);
                    }
                }
                //diamond spawner - kazdych 300 ticku
                if(startTime %30 == 0){
                    for(Location l : resourceSpawners_4){
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.DIAMOND);
                        loc.getWorld().dropItemNaturally(loc, is);
                    }
                }
            }
        };
        this.gameLoop.runTaskTimer(this, 10, 10);
    }

    public void setGameStarting(boolean starting){
        if(starting){
            this.gameState = GameState.STARTING;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.startingMessage"));
            this.startTime = getConfig().getInt("game.startTime");
            this.myScoreboardInstance.setLine(0, "Zacatek za: ", "§a"+this.startTime+" §fsekund");
            this.myScoreboardInstance.setLineCount(1);
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
                    }
                    startTime--;
                    if(startTime == 0){
                        Bukkit.broadcastMessage(getPrefix()+"Hra zacala");
                        this.cancel();
                        gameState = GameState.INGAME;
                        startGameAndLoop();
                        return;
                    }
                    Bukkit.broadcastMessage(getPrefix()+"Hra zacne za "+startTime+" sekund!");
                    myScoreboardInstance.setLine(0, "Zacatek za: ", "§a"+startTime+" §fsekund");
                }
            }.runTaskTimer(this, 20, 20);
        }else{
            this.gameState = GameState.LOBBY;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.notStartingMessage"));
            this.myScoreboardInstance.setLine(0, "Lobby", "");
        }
    }

    public ArrayList<Player> getPlayersInTeam(int team){
        ArrayList<Player> list = this.playerTeams.get(team);
        if(list == null) return new ArrayList<Player>();
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
        player.sendMessage(getPrefix()+getConfig().getString("main.joinedTeam")+getMenuInstance().getNameOfNthTeam(team));
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

    public int getPlayerArmor(Player player){
        int armor = this.playerArmor.getOrDefault(player, 0);
        return armor;
    }

    public void setPlayerArmor(Player player, int armor){
        this.playerArmor.put(player, armor);
    }

}
