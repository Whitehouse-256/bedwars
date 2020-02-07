package com.whitehouse.bedwars;

import java.util.Objects;

public class XYZCoords {
    public int x;
    public int y;
    public int z;
    public XYZCoords(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XYZCoords xyzCoords = (XYZCoords) o;
        return x == xyzCoords.x &&
                y == xyzCoords.y &&
                z == xyzCoords.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
