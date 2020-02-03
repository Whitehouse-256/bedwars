package com.whitehouse.bedwars;

import com.mysql.fabric.xmlrpc.base.Array;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class BedWars extends JavaPlugin {

    private GameState gameState = GameState.LOBBY;
    private int startTime;
    private Menu menuInstance;
    private HashMap<Integer, ArrayList<Player>> playerTeams = new HashMap<Integer, ArrayList<Player>>();

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
