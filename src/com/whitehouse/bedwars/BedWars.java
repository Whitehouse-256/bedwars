package com.whitehouse.bedwars;

import com.mysql.fabric.xmlrpc.base.Array;
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
    private Menu menuInstance;
    private MyScoreboard myScoreboardInstance;
    private HashMap<Integer, ArrayList<Player>> playerTeams = new HashMap<Integer, ArrayList<Player>>();
    private BukkitRunnable gameLoop;
    private Random random;
    public List<Location> teamSpawns = new ArrayList<Location>();

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        //eventy
        pm.registerEvents(new Events(this), this);
        //prikazy - TBD
        getCommand("bw-setup").setExecutor(new SetupCommand(this));
        this.saveDefaultConfig();
        this.reloadConfig();
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
        this.menuInstance = new Menu(this);
        this.myScoreboardInstance = new MyScoreboard(this);
        this.random = new Random();
        this.myScoreboardInstance.setLine(0, "Lobby", "");
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

    private void startGameAndLoop(){
        this.reloadConfig();
        int teamCount = getConfig().getInt("arena.teams");
        this.myScoreboardInstance.setLine(0, "Arena je ve ", "§fhre");

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
            //Vycisten inventar a teleportovan
        }

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

        this.gameLoop = new BukkitRunnable() {
            private long ticks = 0;
            @Override
            public void run() {
                ticks++; //kazdych 10 ticku projde tato funkce
                //Game Loop:
                //Spawnout vsechny itemy, co se maji spawnout
                //base iron spawner - kazdych 10 ticku
                for(Location l : teamSpawns){
                    Location loc = l.clone();
                    loc.add(2*random.nextDouble()-1.0, 0, 2*random.nextDouble()-1.0); //radius 2
                    ItemStack is = new ItemStack(Material.IRON_INGOT);
                    loc.getWorld().dropItem(loc, is);
                }
                //base gold spawner - kazdych 100 ticku
                if(ticks%10 == 0){
                    for(Location l : teamSpawns){
                        Location loc = l.clone();
                        loc.add(2*random.nextDouble()-1.0, 0, 2*random.nextDouble()-1.0); //radius 2
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        loc.getWorld().dropItem(loc, is);
                    }
                }
                //iron spawner - kazdych 20 ticku
                if(ticks%2 == 0){
                    for(Location l : resourceSpawners_2){
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.IRON_INGOT);
                        loc.getWorld().dropItemNaturally(loc, is);
                    }
                }
                //List<String> spawnerList1 = getConfig().getStringList();
                //gold spawner - kazdych 40 ticku
                if(ticks%4 == 0){
                    for(Location l : resourceSpawners_3){
                        Location loc = l.clone();
                        ItemStack is = new ItemStack(Material.GOLD_INGOT);
                        loc.getWorld().dropItemNaturally(loc, is);
                    }
                }
                //diamond spawner - kazdych 300 ticku
                if(ticks%30 == 0){
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
                        gameState = GameState.INGAME;
                        startGameAndLoop();
                        this.cancel();
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

}
