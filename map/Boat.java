package map;

public class Boat
{
    private double x;
    private double y;

    public int sx;
    public int sy;

    private double angle;

    private boolean hitLand = false;

    public Boat(int x, int y, double angle)
    {
        this.x = x;
        this.y = y;

        this.sx = x;
        this.sy = y;

        this.angle = angle;
    }

    private void move(int width, int height)
    {
        this.x += Math.cos(angle);
        this.y += Math.sin(angle);

        this.angle += (Math.random() * 2 - 1) * 5 * (Math.PI / 180);

        while (this.x < 0) this.x += width;
        while (this.y < 0) this.y += height;

        this.x %= width;
        this.y %= height;
    }

    public int getX()
    {
        return (int)this.x;
    }

    public int getY()
    {
        return (int)this.y;
    }

    public void update(Map map)
    {
        move(map.width, map.height);

        if (map.getTile((int)x, (int)y) == null) return;

        hitLand = !map.getTile((int)x, (int)y).IsType(TileType.WATER);
    }

    public boolean HasHit()
    {
        return hitLand;
    }
}