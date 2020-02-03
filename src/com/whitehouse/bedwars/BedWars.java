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
    private HashMap<Integer, ArrayList<Player>> playerTeams = new HashMap<Integer, ArrayList<Player>>();
    private BukkitRunnable gameLoop;
    private Random random;

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
        this.random = new Random();
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

    private void startGameAndLoop(){
        this.gameLoop = new BukkitRunnable() {
            private long ticks = 0;
            @Override
            public void run() {
                ticks++; //kazdych 10 ticku projde tato funkce
                //Game Loop:
                //Spawnout vsechny itemy, co se maji spawnout
                //base iron spawner - kazdych 10 ticku
                int teamCount = getConfig().getInt("arena.teams");
                for(int i=0; i<teamCount; i++){
                    String loc = getConfig().getString("arena.spawn."+i);
                    String[] split = loc.split(";");
                    double x = Double.parseDouble(split[0]);
                    double y = Double.parseDouble(split[1]);
                    double z = Double.parseDouble(split[2]);
                    float yaw = Float.parseFloat(split[3]);
                    float pitch = Float.parseFloat(split[4]);
                    Location location = new Location(Bukkit.getWorld("world"), x, y, z, yaw, pitch);
                    location.add(2*random.nextDouble()-1.0, 0, 2*random.nextDouble()-1.0); //radius 2
                    ItemStack is = new ItemStack(Material.IRON_INGOT);
                    location.getWorld().dropItem(location, is);
                }
                //base gold spawner - kazdych 100 ticku
                if(ticks%10 == 0);
                //iron spawner - kazdych 20 ticku
                if(ticks%2 == 0);
                //List<String> spawnerList1 = getConfig().getStringList();
                //gold spawner - kazdych 40 ticku
                if(ticks%4 == 0);
                //diamond spawner - kazdych 300 ticku
                if(ticks%30 == 0);
            }
        };
        this.gameLoop.runTaskTimer(this, 10, 10);
    }

    public void setGameStarting(boolean starting){
        if(starting){
            this.gameState = GameState.STARTING;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.startingMessage"));
            this.startTime = getConfig().getInt("game.startTime");
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
                }
            }.runTaskTimer(this, 20, 20);
        }else{
            this.gameState = GameState.LOBBY;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.notStartingMessage"));
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

    public void enableSetup(){
        this.gameState = GameState.SETUP;
    }

    public void disableSetup(){
        this.gameState = GameState.LOBBY;
    }

}
