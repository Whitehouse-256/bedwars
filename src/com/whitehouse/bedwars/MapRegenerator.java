package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class MapRegenerator {
    private final BedWars plugin;
    private final ArrayList<BlockState> mapData = new ArrayList<>();
    private Location bound1;
    private Location bound2;

    public MapRegenerator(BedWars plugin){
        this.plugin = plugin;
    }

    public void setBound1(Location location){
        this.bound1 = location;
        String loc = location.getBlockX()+";"+location.getBlockY()+";"+location.getBlockZ();
        this.plugin.getConfig().set("arena.bound1", loc);
        this.plugin.saveConfig();
    }

    public void setBound2(Location location){
        this.bound2 = location;
        String loc = location.getBlockX()+";"+location.getBlockY()+";"+location.getBlockZ();
        this.plugin.getConfig().set("arena.bound2", loc);
        this.plugin.saveConfig();
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

        World world = Objects.requireNonNull(bound1.getWorld());
        for(int y=min_y; y<=max_y; y++){
            for(int x=min_x; x<=max_x; x++){
                for(int z=min_z; z<=max_z; z++){
                    //Pro kazdy blok postupne
                    BlockState blockState = world.getBlockAt(x, y, z).getState();
                    if(blockState.getType() != Material.AIR) { //ukladat pouze neprazdne bloky
                        this.mapData.add(blockState);
                    }
                }
            }
        }

        if(player != null) player.sendMessage(plugin.getPrefix()+"§aUlozeno "+this.mapData.size()+" bloku do seznamu.");
    }

    public void saveMap(@Nullable Player player){
        ArrayList<BlockState> localCopy = new ArrayList<>(this.mapData);
        BedWars localPlugin = this.plugin;
        File dir = this.plugin.getDataFolder();
        if(player != null) player.sendMessage(plugin.getPrefix()+"§aVytvarim nove vlakno pro ulozeni bloku do souboru. Toto muze chvili trvat...");
        Thread thread = new Thread(){
            public void run(){
                StringBuilder save = new StringBuilder();
                for(BlockState bs : localCopy){
                    StringBuilder add = new StringBuilder();
                    if(bs.getType().toString().contains("SIGN")){
                        try{
                            Sign sign = (Sign)bs;
                            String[] lines = sign.getLines();
                            add.append(";");
                            for(String line : lines){
                                add.append(line.replace("\\", "/BACK/")).append("\\n");
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    String serial = bs.getX()+";"+bs.getY()+";"+bs.getZ()+";"+bs.getType().toString()+";"+bs.getBlockData().getAsString()+add;
                    save.append(serial).append('\n');
                }
                try {
                    FileWriter fw = new FileWriter(new File(dir, "arenaBlocks.csv"));
                    fw.write(save.toString());
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
        //Ziskat hranice mapy
        String bound1Loc = this.plugin.getConfig().getString("arena.bound1", null);
        String bound2Loc = this.plugin.getConfig().getString("arena.bound2", null);
        if(bound1Loc == null || bound2Loc == null){
            if(player != null) player.sendMessage(plugin.getPrefix()+"§cArena nema nastavene hranice!");
            plugin.getLogger().info("§cCannot regenerate, arena has not set bounds.");
            return;
        }
        World world = Bukkit.getWorld("world");
        if(world == null){
            plugin.getLogger().info("§cCannot regenerate, world does not exist.");
            return;
        }
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
        //Precist soubor s daty o blocich
        if(player != null) player.sendMessage(plugin.getPrefix()+"§aVytvarim vlakno pro pokus o obnoveni mapy ze souboru. Toto muze chvili trvat...");
        Thread thread = new Thread(){
            public void run(){
                HashMap<XYZCoords, ConcreteBlockState> localMap = new HashMap<>();
                try {
                    File dataFile = new File(dir, "arenaBlocks.csv");
                    Scanner sc = new Scanner(dataFile);
                    while(sc.hasNextLine()){
                        String data = sc.nextLine();
                        //zpracovat data z linky
                        String[] parts = data.split(";", 6);
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Material material = Material.valueOf(parts[3]);
                        BlockData blockData = Bukkit.createBlockData(parts[4]);
                        String signLines = null;
                        if(material.toString().contains("SIGN")){
                            try{
                                signLines = parts[5];
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        XYZCoords xyzCoords = new XYZCoords(x, y, z);
                        ConcreteBlockState concreteBlockState = new ConcreteBlockState(x, y, z, material, blockData, signLines);
                        localMap.put(xyzCoords, concreteBlockState);
                    }
                    sc.close();
                    Bukkit.getScheduler().runTask(localPlugin, new Runnable() {
                        @Override
                        public void run() {
                            //zkopirovat data do main threadu a tam pokracovat
                            ArrayList<ConcreteBlockState> bedHeads = new ArrayList<>();
                            ArrayList<ConcreteBlockState> bedFoots = new ArrayList<>();
                            ArrayList<ConcreteBlockState> doubleBlocks = new ArrayList<>();
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§aNacteno "+localMap.size()+" bloku ze souboru!");
                            //v druhem threadu se k datum v localMap uz nepristupuje, je to tedy bezpecne tady v main threadu
                            World world = Bukkit.getWorld("world");
                            assert world != null;
                            //Ted se zacnou obnovovat bloky

                            int min_x, max_x, min_y, max_y, min_z, max_z;

                            if(bound1.getX() < bound2.getX()){
                                min_x = bound1.getX();
                                max_x = bound2.getX();
                            }else{
                                min_x = bound2.getX();
                                max_x = bound1.getX();
                            }
                            if(bound1.getY() < bound2.getY()){
                                min_y = bound1.getY();
                                max_y = bound2.getY();
                            }else{
                                min_y = bound2.getY();
                                max_y = bound1.getY();
                            }
                            if(bound1.getZ() < bound2.getZ()){
                                min_z = bound1.getZ();
                                max_z = bound2.getZ();
                            }else{
                                min_z = bound2.getZ();
                                max_z = bound1.getZ();
                            }

                            //Zvetsit oblast
                            min_x -= 10;
                            min_y -= 10;
                            min_z -= 10;
                            max_x += 10;
                            max_y += 10;
                            max_z += 10;
                            if(min_y<0) min_y=0;
                            if(max_y>255) max_y=255;

                            for(int y=min_y; y<=max_y; y++) {
                                for (int x = min_x; x <= max_x; x++) {
                                    for (int z = min_z; z <= max_z; z++) {
                                        Block block = world.getBlockAt(x, y, z);
                                        XYZCoords xyzCoords = new XYZCoords(x, y, z);
                                        if(localMap.containsKey(xyzCoords)){
                                            //je to nejaky neprazdny blok
                                            ConcreteBlockState cbs = localMap.get(xyzCoords);
                                            String materialStr = cbs.material.toString();
                                            if(materialStr.contains("BED")){
                                                //postele se musi resit tak, ze se nejdrive postavi head, potom az foot
                                                if(cbs.blockData.getAsString().contains("part=head")){
                                                    bedHeads.add(cbs);
                                                }else{
                                                    bedFoots.add(cbs);
                                                }
                                                continue;
                                            }else if(materialStr.contains("DOOR") || cbs.material == Material.TALL_GRASS || cbs.material == Material.LARGE_FERN
                                                    || cbs.material == Material.PEONY || cbs.material == Material.ROSE_BUSH || cbs.material == Material.LILAC
                                                    || cbs.material == Material.SUNFLOWER){
                                                //double bloky se musi stavet v urcitem poradi
                                                doubleBlocks.add(cbs);
                                                continue;
                                            }
                                            Block b = world.getBlockAt(cbs.x, cbs.y, cbs.z);
                                            b.setType(Material.STONE); //pro smazani inventare kontejneru
                                            BlockData blockData = b.getBlockData();
                                            b.setType(cbs.material);
                                            b.setBlockData(cbs.blockData);
                                            if(cbs.signLines != null){
                                                try {
                                                    Sign sign = (Sign) b.getState();
                                                    String[] lines = cbs.signLines.split("\\\\n");
                                                    for (int i = 0; i < lines.length && i < 4; i++) {
                                                        sign.setLine(i, lines[i].replace("/BACK/", "\\"));
                                                    }
                                                    sign.update();
                                                }catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                            }
                                        }else{
                                            //je to vzduch
                                            block.setType(Material.AIR);
                                        }
                                    }
                                }
                            }



//                            for(ConcreteBlockState cbs : localMap){
//                                String materialStr = cbs.material.toString();
//                                if(materialStr.contains("BED")){
//                                    //postele se musi resit tak, ze se nejdrive postavi head, potom az foot
//                                    if(cbs.blockData.getAsString().contains("part=head")){
//                                        bedHeads.add(cbs);
//                                    }else{
//                                        bedFoots.add(cbs);
//                                    }
//                                    continue;
//                                }else if(materialStr.contains("DOOR") || cbs.material == Material.TALL_GRASS || cbs.material == Material.LARGE_FERN
//                                        || cbs.material == Material.PEONY || cbs.material == Material.ROSE_BUSH || cbs.material == Material.LILAC
//                                        || cbs.material == Material.SUNFLOWER){
//                                    //double bloky se musi stavet v urcitem poradi
//                                    doubleBlocks.add(cbs);
//                                    continue;
//                                }
//                                Block b = world.getBlockAt(cbs.x, cbs.y, cbs.z);
//                                b.setType(cbs.material);
//                                b.setBlockData(cbs.blockData);
//                                if(cbs.signLines != null){
//                                    try {
//                                        Sign sign = (Sign) b.getState();
//                                        String[] lines = cbs.signLines.split("\\\\n");
//                                        for (int i = 0; i < lines.length && i < 4; i++) {
//                                            sign.setLine(i, lines[i].replace("/BACK/", "\\"));
//                                        }
//                                        sign.update();
//                                    }catch (Exception e){
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }

                            //Ted postavit postele
                            for(ConcreteBlockState cbsHead : bedHeads){
                                //Najit odpovidajici foot cast
                                for(ConcreteBlockState cbsFoot : bedFoots){
                                    if((cbsHead.x-cbsFoot.x)*(cbsHead.x-cbsFoot.x) + (cbsHead.y-cbsFoot.y)*(cbsHead.y-cbsFoot.y) + (cbsHead.z-cbsFoot.z)*(cbsHead.z-cbsFoot.z) == 1){
                                        //Je to vedlejsi blok -> odpovidajici postel
                                        //Nejdriv postavit head
                                        Block b = world.getBlockAt(cbsHead.x, cbsHead.y, cbsHead.z);
                                        b.setType(cbsHead.material);
                                        b.setBlockData(cbsHead.blockData);
                                        //Pak postavit foot
                                        b = world.getBlockAt(cbsFoot.x, cbsFoot.y, cbsFoot.z);
                                        b.setType(cbsFoot.material);
                                        b.setBlockData(cbsFoot.blockData);
                                        break;
                                    }
                                }
                            }
                            //Ted postavit dvojite bloky
                            for(ConcreteBlockState cbsUpper : doubleBlocks){
                                if(!cbsUpper.blockData.getAsString().contains("half=upper")){
                                    continue; //neni to horni cast
                                }
                                for(ConcreteBlockState cbsLower : doubleBlocks){
                                    if(!cbsLower.blockData.getAsString().contains("half=lower")){
                                        continue; //neni to spodni cast
                                    }
                                    if(cbsLower.x == cbsUpper.x && cbsLower.z == cbsUpper.z && cbsLower.y+1 == cbsUpper.y){
                                        //jsou to bloky nad sebou
                                        Block b;
                                        //postavit horni cast
                                        b = world.getBlockAt(cbsUpper.x, cbsUpper.y, cbsUpper.z);
                                        b.setType(cbsUpper.material);
                                        b.setBlockData(cbsUpper.blockData);
                                        //postavit spodni cast
                                        b = world.getBlockAt(cbsLower.x, cbsLower.y, cbsLower.z);
                                        b.setType(cbsLower.material);
                                        b.setBlockData(cbsLower.blockData);
                                        break;
                                    }
                                }
                            }
                            if(player != null) player.sendMessage(localPlugin.getPrefix()+"§aBloky regenerovany!");
                            plugin.getLogger().info("§aBloky regenerovany!");
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



