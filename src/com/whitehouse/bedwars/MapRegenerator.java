package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class MapRegenerator {
    private final BedWars plugin;
    private ArrayList<BlockState> mapData = new ArrayList<BlockState>();
    private Location bound1;
    private Location bound2;

    public MapRegenerator(BedWars plugin){
        this.plugin = plugin;
    }

    public void setBound1(Location location){
        this.bound1 = location;
    }

    public void setBound2(Location location){
        this.bound2 = location;
    }

    public void loadAllBlocks(@Nullable Player player){
        int min_x, max_x, min_y, max_y, min_z, max_z;
        if(this.bound1 == null || this.bound2 == null){
            if(player != null) player.sendMessage(plugin.getPrefix()+"§cNezadal jsi dva rohove body!");
            return;
        }

        if(this.bound1.getBlockX() < this.bound2.getBlockX()){
            min_x = this.bound1.getBlockX();
            max_x = this.bound2.getBlockX();
        }else{
            min_x = this.bound2.getBlockX();
            max_x = this.bound1.getBlockX();
        }

        if(this.bound1.getBlockY() < this.bound2.getBlockY()){
            min_y = this.bound1.getBlockY();
            max_y = this.bound2.getBlockY();
        }else{
            min_y = this.bound2.getBlockY();
            max_y = this.bound1.getBlockY();
        }

        if(this.bound1.getBlockZ() < this.bound2.getBlockZ()){
            min_z = this.bound1.getBlockZ();
            max_z = this.bound2.getBlockZ();
        }else{
            min_z = this.bound2.getBlockZ();
            max_z = this.bound1.getBlockZ();
        }

        if(player != null) player.sendMessage(plugin.getPrefix()+"§aZacinam ukladat bloky do seznamu. Toto muze chvili trvat...");

        for(int x=min_x; x<=max_x; x++){
            for(int y=min_y; y<=max_y; y++){
                for(int z=min_z; z<=max_z; z++){
                    //Pro kazdy blok postupne
                    BlockState blockState = bound1.getWorld().getBlockAt(x, y, z).getState();
                    this.mapData.add(blockState);
                }
            }
        }

        if(player != null) player.sendMessage(plugin.getPrefix()+"§aUlozeno "+this.mapData.size()+" bloku do seznamu.");
    }

    public void saveMap(@Nullable Player player){
        ArrayList<BlockState> localCopy = (ArrayList<BlockState>) this.mapData.clone();
        BedWars localPlugin = this.plugin;
        File dir = this.plugin.getDataFolder();
        if(player != null) player.sendMessage(plugin.getPrefix()+"§aVytvarim nove vlakno pro ulozeni bloku do souboru. Toto muze chvili trvat...");
        Thread thread = new Thread(){
            public void run(){
                String save = "";
                for(BlockState bs : localCopy){
                    String serial = bs.getX()+";"+bs.getY()+";"+bs.getZ()+";"+bs.getType().toString()+";"+bs.getBlockData().getAsString();
                    save += serial+'\n';
                }
                try {
                    FileWriter fw = new FileWriter(new File(dir, "arenaBlocks.csv"));
                    fw.write(save);
                    fw.close();
                    Bukkit.getScheduler().runTask(localPlugin, new Runnable() {
                        @Override
                        public void run() {
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§aBloky uspesne ulozeny do souboru!");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(localPlugin, new Runnable() {
                        @Override
                        public void run() {
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§cNastala chyba pri zapisu do souboru! Cela chyba se nachazi v konzoli.");
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void regenMap(@Nullable Player player){
        BedWars localPlugin = this.plugin;
        File dir = this.plugin.getDataFolder();
        if(player != null) player.sendMessage(plugin.getPrefix()+"§aVytvarim vlakno pro pokus o obnoveni mapy ze souboru. Toto muze chvili trvat...");
        Thread thread = new Thread(){
            public void run(){
                ArrayList<ConcreteBlockState> localList = new ArrayList<ConcreteBlockState>();
//                for(BlockState bs : localCopy){
//                    String serial = bs.getX()+";"+bs.getY()+";"+bs.getZ()+";"+bs.getType().toString()+";"+bs.getBlockData().getAsString();
//                    save += serial;
//                }
                try {
                    File dataFile = new File(dir, "arenaBlocks.csv");
                    Scanner sc = new Scanner(dataFile);
                    while(sc.hasNextLine()){
                        String data = sc.nextLine();
                        //zpracovat data z linky
                        String[] parts = data.split(";", 5);
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Material material = Material.valueOf(parts[3]);
                        BlockData blockData = Bukkit.createBlockData(parts[4]);
                        ConcreteBlockState concreteBlockState = new ConcreteBlockState(x, y, z, material, blockData);
                        localList.add(concreteBlockState);
                    }
                    sc.close();
                    Bukkit.getScheduler().runTask(localPlugin, new Runnable() {
                        @Override
                        public void run() {
                            //zkopirovat data do main threadu a tam pokracovat
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§aNacteno "+localList.size()+" bloku ze souboru!");
                            //v druhem threadu se k datum v localList uz nepristupuje, je to tedy bezpecne tady v main threadu
                            World world = Bukkit.getWorld("world");
                            for(ConcreteBlockState cbs : localList){
                                Block b = world.getBlockAt(cbs.x, cbs.y, cbs.z);
                                b.setType(cbs.material);
                                b.setBlockData(cbs.blockData);
                            }
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§aBloky regenerovany!");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(localPlugin, new Runnable() {
                        @Override
                        public void run() {
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§cNastala chyba pri cteni ze souboru! Cela chyba se nachazi v konzoli.");
                        }
                    });
                }
            }
        };
        thread.start();
    }

}



