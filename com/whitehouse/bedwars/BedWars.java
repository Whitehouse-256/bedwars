package com.whitehouse.bedwars;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.Bukkit;

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
        }else{
            this.gameState = GameState.LOBBY;
        }
    }

}
