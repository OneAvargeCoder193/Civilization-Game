package map;

import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Point;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;

import org.json.*;

import com.jogamp.opencl.CLVersion;

import java.util.ArrayList;
import java.util.HashMap;

import utils.*;

import static org.jocl.CL.*;
import org.jocl.*;
import org.jocl.struct.*;
import static org.jocl.struct.CLTypes.*;

public class Map
{
    Tile[] map;
    public ArrayList<Country> countries;
    Random rand;
    Random noseed;
    public int width;
    public int height;
    int seed;
    float scale;

    // private cl_context context;
    // private cl_command_queue command_queue;
    // private cl_kernel kernel;
    // private cl_program program;
    // private cl_mem mapmem;
    // public ByteBuffer mapBuffer;

    // private long[] seedoff = new long[1];

    public Map(int numcountries, int width, int height, int seed, float scale)
    {
        this.rand = new Random(seed);
        this.noseed = new Random();
        this.map = new Tile[width * height];
        this.countries = new ArrayList<Country>();
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.scale = scale;
        GenerateMap();
        // InitComputeShader();

        // seedoff[0] = 0;

        for (int i = 0; i < numcountries; i++)
        {
            Point start = GetLandPoint();
            String name = NameGenerator.GetRandomName(rand);
            Country cont = new Country(name, start, rand);
            countries.add(cont);
            getTile((int)start.getX(), (int)start.getY()).country = cont;
            getTile((int)start.getX(), (int)start.getY()).population = 10000;
        }
        // mapBuffer = ByteBuffer.allocate(map.length * SizeofStruct.sizeof(Tile.class)); //Buffers.allocateBuffer(map);
        // Buffers.writeToBuffer(mapBuffer, map);
    }

    public Map(int numcountries, int width, int height, int seed)
    {
        this.rand = new Random(seed);
        this.noseed = new Random();
        this.map = new Tile[width * height];
        this.countries = new ArrayList<Country>(numcountries);
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.scale = 1;
    }

    // public void InitComputeShader()
    // {
    //     cl_platform_id platforms[] = new cl_platform_id[1];
    //     clGetPlatformIDs(platforms.length, platforms, null);

    //     cl_context_properties context_properties = new cl_context_properties();
    //     context_properties.addProperty(CL_CONTEXT_PLATFORM, platforms[0]);

    //     context = clCreateContextFromType(context_properties, CL_DEVICE_TYPE_GPU, null, null, null);

    //     CL.setExceptionsEnabled(true);

    //     long[] numBytes = new long[1];
    //     clGetContextInfo(context, CL_CONTEXT_DEVICES, 0, null, numBytes);

    //     int numDevices = (int)numBytes[0] / Sizeof.cl_device_id;
    //     cl_device_id[] devices = new cl_device_id[numDevices];
    //     clGetContextInfo(context, CL_CONTEXT_DEVICES, numBytes[0], Pointer.to(devices), null);

    //     command_queue = clCreateCommandQueue(context, devices[0], 0, null);
    //     // command_queue = clCreateCommandQueueWithProperties(context, devices[0], null, null);

    //     try {
    //         program = clCreateProgramWithSource(context, 1, new String[] { Files.readString(Path.of("map/updateGame.cl")) }, null, null);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    //     clBuildProgram(program, 0, null, null, null, null);

    //     kernel = clCreateKernel(program, "update", null);
    // }

    public long GetWorldPopulation()
    {
        long pop = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                pop += getTile(x, y).population;
            }
        }
        return pop;
    }

    public static Map GetRealWorldMap(int width, int height, int seed)
    {
        JSONObject json = null;
        try {
            json = new JSONObject(Files.readString(Paths.get("map/countries.json")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray caps = null;
        try {
            caps = new JSONArray(Files.readString(Paths.get("map/capitals.json")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, JSONObject> capitals = new HashMap<>();
        for (int i = 0; i < caps.length(); i++)
        {
            JSONObject obj = caps.getJSONObject(i);
            capitals.put(obj.getString("CountryName"), obj);
        }

        // HashMap<String, Integer> population = new HashMap<>();
        // for (int i = 0; i < caps.length(); i++)
        // {
        //     JSONObject obj = caps.getJSONObject(i);
        //     capitals.put(obj.getString("CountryName"), obj);
        // }

        Map m = new Map(json.getJSONArray("features").length(), width, height, seed);
        ArrayList<Path2D.Double> borders = new ArrayList<>();

        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<CountryGenerator> runnables = new ArrayList<>();

        int maxThreads = Runtime.getRuntime().availableProcessors() - 1;

        threads.clear();

        for (float i = 0; i < width * height; i += (double)(width * height) / maxThreads)
        {
            long start = Math.round(i);
            long end = Math.min(Math.round(i + (double)(width * height) / maxThreads), width * height);
            Thread t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    BufferedImage img = null;
                    try {
                        img = ImageIO.read(new File("earth.jpeg"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    CreateMapRange(start, end, m, img, borders);
                }
            });
            threads.add(t);
            t.start();
        }

        String str = null;
        try {
            str = Files.readString(Paths.get("map/pop_density.asc"));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        ArrayList<String> data = new ArrayList<>();
        Scanner scan = new Scanner(str);
        while (scan.hasNext())
        {
            data.add(scan.next());
        }
        scan.close();

        int w = Integer.parseInt(data.get(1));
        int h = Integer.parseInt(data.get(3));

        long[][] population = new long[w][h];

        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                double value = Math.max(Double.parseDouble(data.get(12 + (y * w + x))), 0) * 520;
                population[x][y] = (long)(value);
            }
        }

        for (Thread t : threads)
        {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // m.InitComputeShader();

        for (float i = 0; i < json.getJSONArray("features").length(); i += (double)json.getJSONArray("features").length() / maxThreads)
        {
            long start = Math.round(i);
            long end = (long)Math.min(Math.round(i + (double)json.getJSONArray("features").length() / maxThreads), (double)json.getJSONArray("features").length());
            // System.out.println(start + " " + end);
            CountryGenerator cg = new CountryGenerator(start, end, m, json, capitals);
            runnables.add(cg);
            Thread t = new Thread(cg);
            threads.add(t);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // for (Thread t : threads)
        // {
        //     try {
        //         t.join();
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }

        for (CountryGenerator r : runnables)
        {
            borders.addAll(r.getValue());
        }

        for (int i = 0; i < borders.size(); i++)
        {
            Path2D.Double border = borders.get(i);
            Rectangle2D bound = border.getBounds2D();
            for (int y = (int)bound.getMinY(); y < bound.getMaxY(); y++)
            {
                for (int x = (int)bound.getMinX(); x < bound.getMaxX(); x++)
                {
                    boolean inside = border.contains(x, y);
                    if (inside)
                    {
                        int a = (int)(x * (w / (double)m.width));
                        int b = (int)(y * (h / (double)m.height));
                        int na = (int)Math.min(((x + 1) * (w / (double)m.width)), m.width - 1);
                        int nb = (int)Math.min(((y + 1) * (h / (double)m.height)), m.height - 1);
                        long pop = 0;
                        for (int v = b; v < nb; v++)
                        {
                            for (int u = a; u < na; u++)
                            {
                                pop += population[u][v];
                            }
                        }
                        m.setCountry(m.countries.get(i), x, y);
                        m.getTile(x, y).population = pop;
                    }
                }
            }
        }

        // m.mapBuffer = ByteBuffer.allocate(m.map.length * SizeofStruct.sizeof(TileType.class)); //Buffers.allocateBuffer(map);
        // Buffers.writeToBuffer(m.mapBuffer, m.map);

        return m;
    }

    private static void CreateMapRange(long a, long b, Map m, BufferedImage img, ArrayList<Path2D.Double> borders)
    {
        for (int i = (int)a; i < b; i++)
        {
            int x = i % m.width;
            int y = (int)Math.floor(i / m.width);

            int tx = (int)(x / (double)m.width * img.getWidth());
            int ty = (int)(y / (double)m.height * img.getHeight());
            Color c = new Color(img.getRGB(tx, ty));
            float hsb[] = new float[3];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            float hue = hsb[0] * 360;
            Random rand = new Random((y * m.width) + x + (m.seed * m.width * m.height));
            int s = rand.nextInt();
            Float temp = 1 - hsb[2];
            m.map[i] = (hue > 200 && hue < 245 && hsb[2] < 0.6)?new Tile(x, y, 0, temp, (float)hsb[2] - 1, s, 0):(hue < 27 || hue > 40)?new Tile(x, y, 1, temp, 0, s, 0):new Tile(x, y, 0, 1, 0, s, 0);
        }
    }

    public Point GetLandPoint()
    {
        int x, y;

        x = rand.nextInt(width);
        y = rand.nextInt(height);

        while (getTile(x, y).IsType(TileType.WATER)) {
            x = rand.nextInt(width);
            y = rand.nextInt(height);
        }

        return new Point(x, y);
    }

    public Tile GenerateTile(int x, int y)
    {
        float i = x / (float)width / scale;
        float j = y / (float)height / scale;
        float poles = Math.abs(y - height / 2) / (float)(height / 2);
        float temp = PerlinNoise.fractal2d(i, j, 0.05f, 5, seed) * 5 * (1 - poles);
        Random r = new Random((y * width) + x + (seed * width * height));
        return new Tile(x, y, PerlinNoise.fractal2d(i, j, 0.09f, 2, seed), temp, PerlinNoise.fractal2d(i, j, 0.1f, 16, seed) * 2 - 1, r.nextInt(), 0);
    }

    public void GenerateMap()
    {
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                this.map[y * width + x] = GenerateTile(x, y);
            }
        }
    }

    public BufferedImage RenderMap()
    {
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        ArrayList<Thread> threads = new ArrayList<Thread>();

        int maxThreads = Runtime.getRuntime().availableProcessors() - 1;

        for (float i = 0; i < width * height; i += (double)(width * height) / maxThreads)
        {
            long start = Math.round(i);
            long end = Math.min(Math.round(i + (double)(width * height) / maxThreads), width * height);
            Thread t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    DrawMapRange(start, end, res);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads)
        {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return res;
    }

    private void DrawMapRange(long start, long end, BufferedImage img)
    {
        for (int i = (int)start; i < end; i++)
        {
            int x = i % width;
            int y = (int)Math.floor(i / width);

            int neighbors = 0;
            for (int r = -1; r < 2; r++)
            {
                for (int j = -1; j < 2; j++)
                {
                    if (r != 0 || j != 0) 
                    {
                        Country cont = getCountry(x + r, y + j);
                        if (getCountry(x, y) == cont) neighbors++;
                    }
                }
            }

            Color c = this.map[y * width + x].GetColor(neighbors != 8);
            // float v = (float)Math.max(Math.min(this.map[y * width + x].density, 1), 0);
            // System.out.println(v);
            // Color c = new Color(v, v, v);
            
            img.setRGB(x, y, c.getRGB());
        }
    }

    private double lerp(double a, double b, double t)
    {
        return (b - a) * t + a;
    }

    private Color blend(Color a, Color b, double t)
    {
        return new Color((int)lerp(a.getRed(), b.getRed(), t), (int)lerp(a.getGreen(), b.getGreen(), t), (int)lerp(a.getBlue(), b.getBlue(), t));
    }

    public void UpdateMap()
    {
        // seedoff[0] += 100;
        // int sizeof = SizeofStruct.sizeof(TileType.class);
        // int size = map.length;

        // mapmem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, sizeof * size, Pointer.to(mapBuffer), null);

        // clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(mapmem));
        // clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(new int[] {width}));
        // clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[] {height}));
        // clSetKernelArg(kernel, 3, Sizeof.cl_ulong, Pointer.to(seedoff));

        // long[] global_work_size = new long[] { size };

        // clEnqueueNDRangeKernel(command_queue, kernel, 1, null, global_work_size, null, 0, null, null);

        // clFinish(command_queue);

        // clEnqueueReadBuffer(command_queue, mapmem, true, 0, sizeof * size, Pointer.to(mapBuffer), 0, null, null);

        // mapBuffer.rewind();
        // Buffers.readFromBuffer(mapBuffer, map);

        for (Country c : this.countries)
        {
            c.UpdateBoats(this);
        }

        int maxThreads = Runtime.getRuntime().availableProcessors() - 1;
        ArrayList<Thread> threads = new ArrayList<>();

        Tile old[] = this.map.clone();

        for (float i = 0; i < width * height; i += (double)(width * height) / maxThreads)
        {
            long start = Math.round(i);
            long end = Math.min(Math.round(i + (double)(width * height) / maxThreads), width * height);
            Thread t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    UpdateMapRange(start, end, old);
                }
            });
            threads.add(t);
            t.start();
        }
    }

    private void UpdateCountry(long a, long b)
    {
        for (int i = (int)a; i < b; i++)
        {
            Country cont = this.countries.get(i);
            cont.UpdateBoats(this);
        }
    }

    public Tile getTile(int x, int y)
    {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return map[y * width + x];
    }

    public void setTile(Tile t, int x, int y)
    {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        map[y * width + x] = t;
    }

    public void setCountry(Country cont, int x, int y)
    {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        if (!map[y * width + x].IsType(TileType.WATER)) map[y * width + x].country = cont;
    }

    public Country getCountry(int x, int y)
    {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return map[y * width + x].country;
    }

    public void moveCountry(Country cont, Tile tile, int x, int y, int dx, int dy)
    {
        if (getTile(x, y) == null) return;
        
        Country move = getCountry(x, y);

        if (getTile(x, y) == null) return;

        if (getTile(x, y).IsType(TileType.WATER))
        {
            int i = noseed.nextInt(10000);
            if (i == 0)
            {
                cont.AddBoat(new Boat(x, y, Math.atan2(dy, dx)));
            }
            return;
        }

        if (move == null)
        {
            setCountry(cont, x, y);
            return;
        }

        if (cont.IsEnemy(move) && noseed.nextDouble() < 0.00001) cont.DeclareWar(move);
        else if (cont.IsNeuteral(move) && noseed.nextDouble() < 0.000001) cont.DeclareWar(move);
        else if (cont.IsAlly(move) && noseed.nextDouble() < 0.000001) cont.DeclareWar(move);
        
        if (!cont.Knows(move))
        {
            int i = noseed.nextInt(3);
            if (i == 0) cont.AddAlly(move);
            if (i == 1) cont.AddNeutral(move);
            if (i == 2) cont.AddEnemy(move);
        }

        if (!cont.AtWarWith(move)) return;

        double movechance = tile.strength / (double)(getTile(x, y).strength + tile.strength);
        boolean successful = noseed.nextDouble() <= movechance;
        
        if (successful)
        {
            setCountry(cont, x, y);
            getTile(x, y).population *= 0.998;
            if (tile != null) tile.population *= 0.999;
        } else {
            getTile(x, y).population *= 0.999;
            if (tile != null)
            {
                setCountry(move, tile.x, tile.y);
                tile.population *= 0.998;
            }
        }

        if (move != null)
        {
            if (move.capital.equals(new Point(x, y)) && !cont.equals(move))
            {
                for (int ny = 0; ny < height; ny++)
                {
                    for (int nx = 0; nx < width; nx++)
                    {
                        if (getCountry(nx, ny) != null)
                        {
                            if (getCountry(nx, ny).equals(move))
                            {
                                setCountry(null, nx, ny);
                                this.countries.remove(move);
                            }
                        }
                    }
                }
            }
        }
    }

    private void UpdateMapRange(long a, long b, Tile[] old)
    {
        for (int i = (int)a; i < b; i++)
        {
            int x = i % width;
            int y = (int)Math.floor(i / width);
            Tile tile = old[i];

            if (tile.country == null) continue;

            int c = 0;
            c += (getCountry(x + 1, y) == tile.country)?1:0;
            c += (getCountry(x - 1, y) == tile.country)?1:0;
            c += (getCountry(x, y + 1) == tile.country)?1:0;
            c += (getCountry(x, y - 1) == tile.country)?1:0;

            tile.population += tile.resources / 10000;
            tile.population *= 0.999999999;

            tile.resources += 500;
            tile.resources -= tile.population;

            tile.strength = tile.population * tile.resources;

            if (tile.country != null && c != 4)
            {
                int choice = noseed.nextInt(50);
                if (choice == 0) moveCountry(tile.country, tile, x + 1, y, 1, 0);
                if (choice == 1) moveCountry(tile.country, tile, x - 1, y, -1, 0);
                if (choice == 2) moveCountry(tile.country, tile, x, y + 1, 0, 1);
                if (choice == 3) moveCountry(tile.country, tile, x, y - 1, 0, -1);
            }
        }
    }

    public void shutdown()
    {
        // clReleaseMemObject(mapmem);
        // clReleaseKernel(kernel);
        // clReleaseProgram(program);
        // clReleaseCommandQueue(command_queue);
        // clReleaseContext(context);
    }
}

class CountryGenerator implements Runnable
{
    private long start, end;
    private Map m;
    private JSONObject json;
    private HashMap<String, JSONObject> capitals;

    private volatile ArrayList<Path2D.Double> borders;

    public CountryGenerator(long start, long end, Map m, JSONObject json, HashMap<String, JSONObject> capitals)
    {
        this.start = start;
        this.end = end;
        this.m = m;
        this.json = json;
        this.capitals = capitals;
    }

    @Override
    public void run()
    {
        borders = GetCountries(start, end, m, json, capitals);
    }

    private static ArrayList<Path2D.Double> GetCountries(long start, long end, Map m, JSONObject json, HashMap<String, JSONObject> capitals)
    {
        ArrayList<Path2D.Double> borders = new ArrayList<>();
        for (int i = (int)start; i < end; i++)
        {
            JSONObject feature = json.getJSONArray("features").getJSONObject(i);
            JSONObject properties = feature.getJSONObject("properties");
            JSONObject geometry = feature.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            boolean multi = geometry.getString("type").equals("MultiPolygon");

            Path2D.Double border = new Path2D.Double();
            border.moveTo(0, 0);
            for (int f = 0; f < coordinates.length(); f++)
            {
                JSONArray shape = coordinates.getJSONArray(f);
                if (multi) shape = shape.getJSONArray(0);
                for (int j = 0; j < shape.length(); j++)
                {
                    JSONArray point = shape.getJSONArray(j);

                    int x = (int) (m.width / 2 + (point.getDouble(0) * m.width / 360));
                    int y = (int) (m.height / 2 - (point.getDouble(1) * m.height / 180));

                    if (j == 0)
                    {
                        border.moveTo(x, y);
                    } else
                    {
                        border.lineTo(x, y);
                    }
                }
            }

            border.closePath();

            borders.add(border);

            BufferedImage img = null;
            try {
                img = ImageIO.read(new File("countries/" + properties.getString("iso_a2").toLowerCase() + ".png"));
            } catch (IOException e)
            {
                try {
                    img = ImageIO.read(new File("countries/" + properties.getString("wb_a2").toLowerCase() + ".png"));
                } catch (IOException e1) {
                    try {
                        img = ImageIO.read(new File("countries/" + properties.getString("postal").toLowerCase() + ".png"));
                    } catch (IOException e2) {
                        e1.printStackTrace();
                    }
                }
            }

            String name = properties.getString("name");
            JSONObject capital = capitals.get(name);
            if (capital == null) capital = capitals.get(properties.getString("name_long"));
            if (capital == null) capital = capitals.get(properties.getString("admin"));
            
            Point res = null;

            try
            {
                double lon = Double.parseDouble(capital.getString("CapitalLongitude"));
                double lat = Double.parseDouble(capital.getString("CapitalLatitude"));

                int x = (int)((lon / 360 + 0.5) * m.width);
                int y = (int)((-lat / 180 + 0.5) * m.height);

                res = new Point(x, y);
            } catch (NumberFormatException e) {}

            m.countries.add(new Country(name, res, m.rand, img));
        }
        return borders;
    }

    public ArrayList<Path2D.Double> getValue()
    {
        return borders;
    }
};