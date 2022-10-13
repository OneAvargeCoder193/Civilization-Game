package game;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.MouseInfo;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import static org.jocl.struct.CLTypes.*;

import java.awt.event.MouseMotionListener;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.ArrayList;

import net.coobird.thumbnailator.*;
import net.coobird.thumbnailator.resizers.*;

import map.*;
import utils.*;

public class Game implements MouseInputListener
{
    Map map;
    int tilesize;

    ArrayList<Long> pops = new ArrayList<>();

    long year = 0;
    int month = 0;

    CountryData opened = null;
    
    int mx, my;

    public Game(int width, int height, int numcountries, int seed, float scale, int tilesize)
    {
        this.map = new Map(numcountries, width, height, seed, scale);
        this.tilesize = tilesize;
    }

    public Game(int width, int height, int seed, int tilesize)
    {
        this.map = Map.GetRealWorldMap(width, height, seed);
        this.tilesize = tilesize;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                map.shutdown();
            }
            
        }));
    }

    public void draw(Graphics2D g, int width, int height)
    {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        BufferedImage img = this.map.RenderMap();
        AffineTransform at = new AffineTransform();
        at.scale(tilesize, tilesize);
        g.drawRenderedImage(img, at);

        for (Country cont : map.countries)
        {
            for (Boat boat : cont.boats)
            {
                int ax = boat.getX();
                int ay = boat.getY();

                g.setColor(Color.WHITE);
                g.fillRect(ax * tilesize, ay * tilesize, tilesize, tilesize);
            }

            if (cont.capital != null)
            {
                Point capital = cont.capital;
                int radius = 2;
                
                g.setColor(Color.WHITE);
                g.fillOval((int)(((double)capital.x + 0.5) * tilesize - radius), (int)(((double)capital.y + 0.5) * tilesize - radius), radius * 2, radius * 2);
            }
        }

        Country cont = map.getCountry(mx / tilesize, my / tilesize);

        if (cont != null)
        {
            Rectangle2D.Float rect = (Rectangle2D.Float)g.getFontMetrics().getStringBounds(cont.name, g);
            g.drawString(cont.name, Math.min(mx, width - (int)rect.width), Math.max(my, (int)rect.height));
        }

        String text = "Population: " + Long.toString(map.GetWorldPopulation());

        Font old = g.getFont();
        g.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
        g.drawString(text, width - g.getFontMetrics().stringWidth(text) - 5, (int)g.getFontMetrics().getStringBounds(text, g).getHeight());
        
        text = "Year: " + Long.toString(year);
        g.drawString(text, 0, (int)g.getFontMetrics().getStringBounds(text, g).getHeight());

        g.setFont(old);

        Long max = 0l;
        for (Long p : pops)
        {
            if (p > max) max = p;
        }

        int ly = 0;
        int lx = 0;

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 500, 500);

        g.setColor(Color.BLACK);

        for (int i = 0; i < pops.size(); i++)
        {
            Long pop = pops.get(i);
            float p = (float)pop / max;
            int y = (int)(500 - p * 500);
            int x = (int)(i / (float)pops.size() * 500);

            g.drawLine(lx, ly, x, y);

            ly = y;
            lx = x;
        }

        if (opened != null) opened.draw(g, width, height);
    }

    public void update()
    {
        map.UpdateMap();

        month++;
        if (month == 12)
        {
            year++;
            month = 0;
        }

        if (clicked)
        {
            Country cont = map.getCountry(mx / tilesize, my / tilesize);
            if (cont != null) opened = new CountryData(cont);
        }

        if (opened != null)
        {
            opened.update(this);
            if (opened.close) opened = null;
        }

        pops.add(map.GetWorldPopulation());
        // if (pops.size() > 500) pops.remove(0);

        clicked = false;
        released = false;
    }

    public boolean held = false;
    public boolean clicked = false;
    public boolean released = false;

    @Override
    public void mouseDragged(MouseEvent e)
    {
        this.mx = e.getX();
        this.my = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        this.mx = e.getX();
        this.my = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        clicked = true;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        held = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        held = false;
        released = true;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
}