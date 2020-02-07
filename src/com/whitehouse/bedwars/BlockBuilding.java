package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockBuilding {
    private final BedWars plugin;
    private final HashMap<XYZCoords, ArrayList<XYZCoords>> modifiedBlocks = new HashMap<XYZCoords, ArrayList<XYZCoords>>();

    public BlockBuilding(BedWars plugin){
        this.plugin = plugin;
    }

    public void addBlock(Block block){
        int x = block.getX()/16;
        int y = block.getY()/16;
        int z = block.getZ()/16;
        XYZCoords chunkCoords = new XYZCoords(x, y, z);
        XYZCoords blockCoords = new XYZCoords(block.getX()%16, block.getY()%16, block.getZ()%16);
        ArrayList<XYZCoords> list = this.modifiedBlocks.getOrDefault(chunkCoords, new ArrayList<XYZCoords>());
        if(!list.contains(blockCoords)){
            list.add(blockCoords);
        }
        this.modifiedBlocks.put(chunkCoords, list);
    }

    public boolean containsBlock(Block block){
        int x = block.getX()/16;
        int y = block.getY()/16;
        int z = block.getZ()/16;
        XYZCoords chunkCoords = new XYZCoords(x, y, z);
        XYZCoords blockCoords = new XYZCoords(block.getX()%16, block.getY()%16, block.getZ()%16);
        ArrayList<XYZCoords> list = this.modifiedBlocks.getOrDefault(chunkCoords, new ArrayList<XYZCoords>());
        return list.contains(blockCoords);
    }

}
