package map;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.util.Random;

import utils.FlagGenerator;

import java.util.ArrayList;

import static org.jocl.CL.*;
import org.jocl.*;
import org.jocl.struct.*;
import static org.jocl.struct.CLTypes.*;

public class Country
{
    public String name;
    public BufferedImage flag;
    public Color color;
    public Point capital;
    
    public ArrayList<Country> allies = new ArrayList<>();
    public ArrayList<Country> neutral = new ArrayList<>();
    public ArrayList<Country> enemies = new ArrayList<>();
    public ArrayList<Country> wars = new ArrayList<>();
    
    public ArrayList<Boat> boats = new ArrayList<>();

    public Country(String name, Point capital, Random rand)
    {
        this.name = name;
        this.color = new Color(Color.HSBtoRGB(rand.nextFloat(), 1, 1));
        this.capital = capital;
        this.flag = FlagGenerator.GetRandomFlag(rand);
    }

    public Country(String name, Point capital, Random rand, BufferedImage flag)
    {
        this.name = name;
        this.color = new Color(Color.HSBtoRGB(rand.nextFloat(), 1, 1));
        this.capital = capital;
        this.flag = flag;
    }

    public void AddAlly(Country cont)
    {
        if (IsNeuteral(cont))
        {
            allies.add(cont);
            cont.RemoveNeutral(this);
            cont.AddAlly(this);
        }
    }

    public void AddNeutral(Country cont)
    {
        if (!Knows(cont))
        {
            neutral.add(cont);
            cont.RemoveAlly(this);
            cont.RemoveEnemy(this);
            cont.AddNeutral(this);
        }
    }

    public void AddEnemy(Country cont)
    {
        if (!Knows(cont))
        {
            enemies.add(cont);
            cont.RemoveAlly(this);
            cont.RemoveNeutral(this);
            cont.AddEnemy(this);
        }
    }

    public void RemoveAlly(Country cont)
    {
        if (allies.contains(cont))
        {
            allies.remove(cont);
            cont.RemoveAlly(this);
        }
    }

    public void RemoveNeutral(Country cont)
    {
        if (neutral.contains(cont))
        {
            neutral.remove(cont);
            cont.RemoveNeutral(this);
        }
    }

    public void RemoveEnemy(Country cont)
    {
        if (enemies.contains(cont))
        {
            enemies.remove(cont);
            cont.RemoveEnemy(this);
        }
    }

    public void DeclareWar(Country cont)
    {
        if (!AtWarWith(cont))
        {
            wars.add(cont);
            cont.DeclareWar(this);
        }
    }

    public boolean AtWarWith(Country cont)
    {
        return wars.contains(cont);
    }

    public boolean IsAlly(Country other)
    {
        return allies.contains(other);
    }

    public boolean IsNeuteral(Country other)
    {
        return neutral.contains(other);
    }

    public boolean IsEnemy(Country other)
    {
        return enemies.contains(other);
    }

    public boolean Knows(Country cont)
    {
        return allies.contains(cont) || neutral.contains(cont) || enemies.contains(cont);
    }

    public void AddBoat(Boat boat)
    {
        boats.add(boat);
    }

    public void UpdateBoats(Map map)
    {
        for (int i = boats.size() - 1; i > -1; i--)
        {
            boats.get(i).update(map);
            if (boats.get(i).HasHit())
            {
                int x = (int)(boats.get(i).getX());
                int y = (int)(boats.get(i).getY());
                map.moveCountry(this, map.getTile(boats.get(i).sx, boats.get(i).sy), x, y, 0, 0);
                boats.remove(i);
            }
        }
    }
}