package game;

import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;

import map.*;

public class CountryData
{
    Country cont;
    boolean close = false;

    public CountryData(Country cont)
    {
        this.cont = cont;
    }

    public void draw(Graphics2D g, int width, int height)
    {
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(10));
        
        g.drawLine(0, height / 2, width, height / 2);
        
        g.setColor(Color.RED);
        g.fillRect(10, 10, 50, 50);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(4));

        g.drawLine(15, 15, 55, 55);
        g.drawLine(15, 55, 55, 15);
        
        g.setStroke(old);

        BufferedImage flag = cont.flag;
        double scale = 300 / (double)flag.getHeight();
        AffineTransform at = new AffineTransform();
        at.translate(10, 70);
        at.scale(scale, scale);
        g.drawRenderedImage(flag, at);
    }

    public void update(Game game)
    {
        if (game.mx > 10 && game.mx < 60 && game.my > 10 && game.my < 60 && game.released)
        {
            this.close = true;
        }
    }
}