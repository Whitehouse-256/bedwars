package com.whitehouse.bedwars;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

public class BedWars extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        this.saveDefaultConfig();
        this.reloadConfig();
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling!");
    }

}
