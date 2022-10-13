package map;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Random;

import org.jocl.*;
import org.jocl.struct.*;
import static org.jocl.struct.CLTypes.*;
import static org.jocl.CL.*;

public class Tile
{
    public int x, y;
    public float rain;
    public float temp;
    public float height;
    public int seed;
    public Country country;
    public long population;

    public long strength;

    public int type;

    public int resources = 20000;

    public Tile(int x, int y, float rain, float temp, float height, int seed, long population)
    {
        this.x = x;
        this.y = y;
        this.rain = rain;
        this.temp = temp;
        this.height = height;
        this.seed = seed;
        this.country = null;
        this.type = this.GetType();
        this.population = population;
    }

    int GetType()
    {
        if (height < 0)
        {
            if (temp < 0.3) return TileType.ICE;
            return TileType.WATER;
        }

        if (height > 0.7 || temp < 0.3) return TileType.SNOW;
        if (height > 0.55) return TileType.MOUNTAIN;
        if (temp > 0.7 && rain < 0.1) return TileType.DESERT;

        return TileType.GRASS;
    }

    double lerp(double a, double b, double t)
    {
        return (b - a) * Math.min(Math.max(t, 0), 1) + a;
    }

    Color toColor(cl_float4 col) { return new Color(col.get(0), col.get(1), col.get(2), col.get(3)); }

    Color blendColors(Color a, Color b, double t) { return new Color((float)lerp(a.getRed() / 255.0, b.getRed() / 255.0, t), (float)lerp(a.getGreen() / 255.0, b.getGreen() / 255.0, t), (float)lerp(a.getBlue() / 255.0, b.getBlue() / 255.0, t)); }

    public Color GetColor(boolean edge)
    {
        Color col = Color.BLACK;

        Random rand = new Random(seed);
        double r = rand.nextDouble();

        switch (type)
        {
            case TileType.WATER:
                col = blendColors(new Color(0, 183, 255), new Color(0, 10, 97), -height);
                break;
            case TileType.GRASS:
                col = blendColors(new Color(16, 201, 0), new Color(11, 138, 0), r);
                break;
            case TileType.DESERT:
                col = blendColors(new Color(255, 179, 0), new Color(214, 150, 0), r);
                break;
            case TileType.MOUNTAIN:
                col = blendColors(new Color(135, 135, 135), new Color(71, 71, 71), (height - 0.55) / (0.7 - 0.55));
                break;
            case TileType.SNOW:
                col = blendColors(new Color(245, 245, 245), new Color(235, 235, 235), r);
                break;
            case TileType.ICE:
                col = blendColors(new Color(194, 255, 253), new Color(180, 237, 235), r);
                break;
        }

        if (country != null) col = blendColors(col, country.color, edge?0.8:0.6);

        return col;
    }

    public boolean IsType(int type)
    {
        return this.type == type;
    }
}