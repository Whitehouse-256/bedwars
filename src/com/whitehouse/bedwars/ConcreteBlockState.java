package com.whitehouse.bedwars;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class ConcreteBlockState {
    public int x;
    public int y;
    public int z;
    public Material material;
    public BlockData blockData;
    public ConcreteBlockState(int x, int y, int z, Material material, BlockData blockData){
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.blockData = blockData;
    }
}
