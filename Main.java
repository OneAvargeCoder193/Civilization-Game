import map.*;

import javax.swing.Timer;
import java.awt.image.BufferStrategy;
import java.awt.Graphics2D;
import java.awt.Canvas;
import java.awt.Dimension;

import javax.swing.JFrame;

import game.*;

public class Main extends Canvas implements Runnable
{
    JFrame frame;
	String title = "3D Renderer";
	final int WIDTH = 1280;
	final int HEIGHT = 960;
    boolean running = false;
    Thread thread;
    Game game;

	public Main()
	{
        this.frame = new JFrame(title);
        this.frame.setSize(WIDTH, HEIGHT);

        this.frame.add(this);

        this.game = new Game(WIDTH / 2, HEIGHT / 2, 100, 0, 0.01f, 2);
        // this.game = new Game(WIDTH / 2, HEIGHT / 2, 0, 2);

        this.addMouseListener(this.game);
        this.addMouseMotionListener(this.game);

        this.frame.setResizable(false);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setVisible(true);
    }

	public static void main(String[] args)
	{
		Main display = new Main();
        display.start();
	}

    public synchronized void start() {
		running = true;
		this.thread = new Thread(this, "Display");
		this.thread.start();
	}
	
	public synchronized void stop() {
		running = false;
		try {
			this.thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double fps = 0;
        double delta = 0;

		while (running)
		{
            lastTime = System.nanoTime();

            update();
            render();

            delta = (System.nanoTime() - lastTime) / 1000000000.0;

            fps = 1 / delta;

            StringBuilder t = new StringBuilder(title);
            t.append(" | ");
            t.append((int)fps);
            t.append(" fps");
            frame.setTitle(t.toString());
		}

        stop();
    }

    void render()
    {
        BufferStrategy bs = this.getBufferStrategy();
		if(bs == null) {
			this.createBufferStrategy(3);
			return;
		}

		Graphics2D g = (Graphics2D)bs.getDrawGraphics();

        game.draw(g, WIDTH, HEIGHT);

        g.dispose();
        bs.show();
    }

    void update()
    {
        this.game.update();
    }
}