package com.whitehouse.bedwars;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class BedWars extends JavaPlugin {

    private GameState gameState = GameState.LOBBY;
    private int startTime;

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        //eventy
        pm.registerEvents(new Events(this), this);
        //prikazy - TBD
        this.saveDefaultConfig();
        this.reloadConfig();
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
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

    public void setGameStarting(boolean starting){
        if(starting){
            this.gameState = GameState.STARTING;
            Bukkit.broadcastMessage(getPrefix()+getConfig().getString("game.startingMessage"));
            this.startTime = getConfig().getInt("game.startTime");
            //Spustit casovac
            new BukkitRunnable(){
                @Override
                public void run(){
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

}
