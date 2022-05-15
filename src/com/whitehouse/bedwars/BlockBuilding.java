package com.whitehouse.bedwars;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockBuilding {
    private final HashMap<XYZCoords, ArrayList<XYZCoords>> modifiedBlocks = new HashMap<>();

    public BlockBuilding(){
    }

    public void addBlock(Block block){
        int x = block.getX()/16;
        int y = block.getY()/16;
        int z = block.getZ()/16;
        XYZCoords chunkCoords = new XYZCoords(x, y, z);
        XYZCoords blockCoords = new XYZCoords(block.getX()%16, block.getY()%16, block.getZ()%16);
        ArrayList<XYZCoords> list = this.modifiedBlocks.getOrDefault(chunkCoords, new ArrayList<>());
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
        ArrayList<XYZCoords> list = this.modifiedBlocks.getOrDefault(chunkCoords, new ArrayList<>());
        return list.contains(blockCoords);
    }

    public void clearMap(){
        this.modifiedBlocks.clear();
    }

}
