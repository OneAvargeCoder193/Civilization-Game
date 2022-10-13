package utils;

import java.util.Arrays;
import java.util.Random;

import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.BasicStroke;
import java.awt.Polygon;

enum flagtype
{
    BARS,
    THREESTRIPES,
    STRIPES,
    NONE
};

enum flagfeature
{
    TRIANGLE,
    LINE,
    CIRCLE
};

public class FlagGenerator
{
    public static final int SCALE = 1000;

    public static Color colors[] = {
        Color.RED,
        Color.RED,
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.BLUE,
        Color.BLUE,
        Color.MAGENTA,
        Color.WHITE,
        Color.WHITE,
        Color.WHITE,
        Color.BLACK,
    };

    private static Color GetRandomColor(Random rand)
    {
        return colors[rand.nextInt(colors.length)];
    }

    private static flagtype GetRandomFlagType(Random rand)
    {
        return flagtype.class.getEnumConstants()[rand.nextInt(flagtype.class.getEnumConstants().length)];
    }

    private static flagfeature GetRandomFlagFeature(Random rand)
    {
        return flagfeature.class.getEnumConstants()[rand.nextInt(flagfeature.class.getEnumConstants().length)];
    }

    private static Polygon CreateTriangle(int ax, int ay, int bx, int by, int cx, int cy)
    {
        return new Polygon(new int[] {ax, bx, cx}, new int[] {ay, by, cy}, 3);
    }

    public static BufferedImage GetRandomFlag(Random rand)
    {
        double aspect = rand.nextDouble(0.4, 1);
        int width = (int)(SCALE / aspect);
        int height = SCALE;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        flagtype type = GetRandomFlagType(rand);

        switch (type)
        {
        case BARS:
            for (int i = 0; i < 3; i++)
            {
                Color c = GetRandomColor(rand);
                g.setColor(c);
                g.fillRect(width / 3 * i, 0, width / 3, height);
            }
            break;
        case THREESTRIPES:
            for (int i = 0; i < 3; i++)
            {
                Color c = GetRandomColor(rand);
                g.setColor(c);
                g.fillRect(0, height / 3 * i, width, height / 3);
            }
            break;
        case STRIPES:
            Color a = GetRandomColor(rand);
            Color b = GetRandomColor(rand);
            int numstripes = rand.nextInt(2, 5);
            for (int i = 0; i < numstripes; i++)
            {
                Color c = (i % 2 == 0)?a:b;
                g.setColor(c);
                g.fillRect(0, height / numstripes * i, width, height / numstripes);
            }
            break;
        case NONE:
            g.setColor(GetRandomColor(rand));
            g.fillRect(0, 0, width, height);
            break;
        }

        if (rand.nextInt(3) != 0) return img;

        int numfeatures = rand.nextInt(3) + 1;
        if (type == flagtype.NONE && numfeatures == 0) numfeatures = 1;

        for (int i = 0; i < numfeatures; i++)
        {
            flagfeature feature = GetRandomFlagFeature(rand);

            switch (feature)
            {
            case CIRCLE:
                int radius = rand.nextInt(100, 500);
                Color c = GetRandomColor(rand);
                g.setColor(c);
                g.fillOval(width / 2 - radius / 2, height / 2 - radius / 2, radius, radius);
            case LINE:
                boolean vertical = rand.nextBoolean();
                int thickness = rand.nextInt(10, 50);
                g.setStroke(new BasicStroke(thickness));
                if (vertical)
                {
                    g.drawLine(width / 2, 0, width / 2, height);
                } else {
                    g.drawLine(0, height / 2, width, height / 2);
                }
                g.setStroke(new BasicStroke());
            case TRIANGLE:
                int side = rand.nextInt(4);
                int size = rand.nextInt(300, 500);
                g.setColor(GetRandomColor(rand));
                switch (side)
                {
                case 0:
                    g.fillPolygon(CreateTriangle(0, 0, width, 0, width / 2, size));
                    break;
                case 1:
                    g.fillPolygon(CreateTriangle(0, height, width, height, width / 2, height - size));
                    break;
                case 2:
                    g.fillPolygon(CreateTriangle(0, 0, width, 0, width / 2, size));
                    g.fillPolygon(CreateTriangle(0, height, width, height, width / 2, height - size));
                    break;
                case 3:
                    g.fillPolygon(CreateTriangle(0, 0, 0, height, size, height / 2));
                    g.fillPolygon(CreateTriangle(width, 0, width, height, width - size, height / 2));
                    break;
                }
            }
        }

        return img;
    }
}