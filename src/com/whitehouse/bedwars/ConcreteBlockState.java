package com.whitehouse.bedwars;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class ConcreteBlockState {
    public final int x;
    public final int y;
    public final int z;
    public final Material material;
    public final BlockData blockData;
    public final String signLines;
    public ConcreteBlockState(int x, int y, int z, Material material, BlockData blockData, String signLines){
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.blockData = blockData;
        this.signLines = signLines;
    }
}
