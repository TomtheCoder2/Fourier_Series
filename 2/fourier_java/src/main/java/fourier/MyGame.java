package fourier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static fourier.CreateImage.createImage;
import static fourier.Fourier.dft;
import static java.lang.Math.*;

public class MyGame {
    private static final long REFRESH_INTERVAL_MS = 17;
    public static ArrayList<Complex> originalFunction;
    public static FourierComponent[] fourier;
    private static double time = 0;
    private static ArrayList<Pair<Double, Double>> path = new ArrayList<>();
    private static String[] static_args;
    private static int skip = 1;
    private static boolean clearScreen = false;
    private static Component component;
    private static Image imageBuffer;
    private static boolean resized = false;
    private static double[][] drawing;
    private static double zoom = 1;
    private static int zoomPointX;
    private static int zoomPointY;
    private static String fileName = "/drawings/java.txt";
    private final Object redrawLock = new Object();
    private volatile boolean keepGoing = true;

    public static void main(String[] args) {
        static_args = args;
        System.out.println(Arrays.toString(static_args));
        if (args.length > 1) {
            try {
                skip = Integer.parseInt(args[1]);
            } catch (Exception ignored) {
            }
        }
        if (args.length > 2) {
            try {
                clearScreen = Boolean.parseBoolean(args[2]);
            } catch (Exception ignored) {
            }
        }
        java.awt.EventQueue.invokeLater(() -> {
            MyGame game = new MyGame();
            MyComponent component = new MyComponent(game);

            JFrame frame = new JFrame();
            // put the component in a frame

            frame.setTitle("Fourier Series");
            Dimension size
                    = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(size.width, size.height);

            frame.setLayout(new BorderLayout());
            frame.getContentPane().add(component, BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            frame.addComponentListener(new ResizeListener());
            frame.addMouseWheelListener(e -> {
                zoomPointX = e.getX();
                zoomPointY = e.getY();
                if (e.getPreciseWheelRotation() < 0) {
                    zoom -= 0.1;
                } else {
                    zoom += 0.1;
                }
                if (zoom < 0.01) {
                    zoom = 0.01;
                }
            });

            game.start(component);
        });
    }

    public static double[] getScalarAndOffset(int width, int height, double[][] drawing) {
        // calculate by how much to translate the picture so that it fits on to the screen
        double topLeftX = width, topLeftY = height, bottomRightX = 0, bottomRightY = 0;
        for (double[] doubles : drawing) {
            if (doubles[1] == 0 && doubles[0] == 0) continue;
            topLeftX = min(doubles[0], topLeftX);
            topLeftY = min(doubles[1], topLeftY);

            bottomRightX = max(doubles[0], bottomRightX);
            bottomRightY = max(doubles[1], bottomRightY);
        }
        double targetHeight = height * 3.0 / 4.0;
        double targetWidth = width * 3.0 / 4.0;
        double currentHeight = bottomRightY - topLeftY;
        double currentWidth = bottomRightX - topLeftX;
        double scalar = targetHeight / currentHeight;
        if (scalar * currentWidth > targetWidth) {
            scalar = targetWidth / currentWidth;
        }
        double offsetY = -currentHeight * scalar / 2 - topLeftY * scalar;
        double offsetX = -currentWidth * scalar / 2 - topLeftX * scalar;
        return new double[]{scalar, offsetX, offsetY};
    }

    public static void setup() {
        System.out.printf("Setup everything...\nskip: %d, clearScreen: %b\nDimensions:\n\twidth: %dpx\n\theight: %dpx\n\n", skip, clearScreen, component.getWidth(), component.getHeight());
        path = new ArrayList<>();
        imageBuffer = component.createImage(component.getWidth(),
                component.getHeight());
        // init
        originalFunction = new ArrayList<>();
        double[] d = getScalarAndOffset(component.getWidth(), component.getHeight(), drawing);
        double scalar = d[0];
        double offsetX = d[1];
        double offsetY = d[2];
        for (int i = 0; i < drawing.length; i += 1) {
            if (i % skip == 0) {
                if (drawing[i][0] != 0 && drawing[i][1] != 0) {
                    originalFunction.add(
                            new Complex(drawing[i][0] * scalar + offsetX, drawing[i][1] * scalar + offsetY));
                }
            }
            if (drawing[i][0] == 0 && drawing[i][1] == 0) {
                originalFunction.add(new Complex(true));
            }
        }
        fourier = dft(originalFunction.toArray(Complex[]::new));
        fourier = Arrays.stream(fourier).sorted((o1, o2) -> {
            if (o2.amp - o1.amp < 0) {
                return -1;
            } else if (o2.amp - o1.amp > 0) {
                return 1;
            } else {
                return 0;
            }
        }).toList().toArray(new FourierComponent[0]);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (FourierComponent fc : fourier) {
            sb.append(String.format("{%f, %f}, ", fc.re, fc.im));
        }
        sb.append("}");
        try {
            String str = sb.toString();
            BufferedWriter writer = new BufferedWriter(new FileWriter("output.fou"));
            writer.write(str);

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(fileName);
        System.out.println(Arrays.toString(fileName.split("(\\\\|\\/)")));
        System.out.println(fileName.split("(\\\\|\\/)")[fileName.split("(\\\\|\\/)").length - 1].replaceAll("\\..*", ""));
//        createImage(fourier, component.getWidth(), component.getHeight(), fileName.split("(\\\\|\\/)")[fileName.split("(\\\\|\\/)").length - 1].replaceAll("\\..*", ""));
    }

    public void start(Component componentIn) {
        component = componentIn;
        if (static_args.length == 0) {
            skip = 10;
        } else {
            fileName = static_args[0];
        }
        try {
            if (Objects.requireNonNull(MyGame.class.getResourceAsStream(fileName)).readAllBytes().length == 0) {
                System.out.println("File does not exist! " + fileName);
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("File does not exist! " + fileName);
            e.printStackTrace();
            System.exit(1);
        }
        drawing = Drawing.getDrawing(fileName);
        Thread thread = new Thread(this::runGameLoop);
        setup();
        thread.start();
    }

    public void stop() {
        keepGoing = false;
    }

    private void runGameLoop() {
        // update the game repeatedly
        while (keepGoing) {
            long durationMs = redraw();
            try {
                Thread.sleep(Math.max(0, REFRESH_INTERVAL_MS - durationMs));
            } catch (InterruptedException ignored) {
            }
        }
    }

    private long redraw() {
        long t = System.currentTimeMillis();

        // At this point perform changes to the model that the component will
        // redraw

        updateModel();

        // draw the model state to a buffered image which will get
        // painted by component.paint().
        drawModelToImageBuffer();

        // asynchronously signals the paint to happen in the awt event
        // dispatcher thread
        component.repaint();

        // use a lock here that is only released once the paintComponent
        // has happened so that we know exactly when the paint was completed and
        // thus know how long to pause till the next redraw.
        waitForPaint();

        // return time taken to do redraw in ms
        return System.currentTimeMillis() - t;
    }

    private void updateModel() {
        // do stuff here to the model based on time
    }

    private void drawModelToImageBuffer() {
        drawModel((Graphics2D) imageBuffer.getGraphics());
    }

    private Pair<Double, Double> epiCycles(double x, double y, double rotation, FourierComponent[] fourier, Graphics g) {
        for (FourierComponent FourierComponent : fourier) {
            double prevX = x;
            double prevY = y;

            double freq = FourierComponent.freq;
            double radius = FourierComponent.amp;
            double phase = FourierComponent.phase;
            x += radius * cos(freq * time + phase + rotation);
            y += radius * sin(freq * time + phase + rotation);

            g.setColor(new Color(0x3DFFFFFF, true));
            g.drawOval((int) (prevX - radius), (int) (prevY - radius), (int) (radius * 2), (int) (radius * 2));

            g.setColor(new Color(0x6CFFFFFF, true));
            g.drawLine((int) prevX, (int) prevY, (int) x, (int) y);
        }

        return new Pair<>(x, y);
    }

    private void drawModel(Graphics2D g) {
        if (resized) {
            setup();
            resized = false;
        }

        g.setColor(new Color(0x000000));
        g.fillRect(0, 0, component.getWidth(), component.getHeight());
        g.setColor(new Color(0xffffff));
        Pair<Double, Double> v = epiCycles(component.getWidth() / 2.0, component.getHeight() / 2.0, 0, fourier, g);
        if (!path.contains(v)) path.add(0, v);
        g.setColor(new Color(0xffffff));
//        System.out.println();
        double avgDist = 0;
        int avgCount = 0;
        for (int i = 0; i < path.size(); i++) {
            Pair<Double, Double> doubleDoublePair = path.get(i);
//            g.drawOval((int) doubleDoublePair.a.doubleValue(), (int) doubleDoublePair.b.doubleValue(), 1, 1);
            if (i != 0) {
                double dist = sqrt(pow(doubleDoublePair.a - path.get(i - 1).a, 2) + pow(doubleDoublePair.b - path.get(i - 1).b, 2));
                avgDist += dist;
//                if (!originalFunction.get(i - 1).sectionEnd) {
                if (dist < avgDist / avgCount + 10) {
                    g.drawLine((int) doubleDoublePair.a.doubleValue(), (int) doubleDoublePair.b.doubleValue(),
                            (int) path.get(i - 1).a.doubleValue(), (int) path.get(i - 1).b.doubleValue());
                    avgCount += 1;
                } else {
                    avgDist -= dist;
                }
//                }
            }
//            System.out.printf("or: %f,%f, path: %f,%f\n", originalFunction.get(i).re, originalFunction.get(i).im, path.get(i).a, path.get(i).b);
        }
        if (time > PI * 2) {
            time = 0;
            if (clearScreen) {
                path = new ArrayList<>();
            }
        }

        double dt = (PI * 2) / fourier.length;
        time += dt;
    }

    private void waitForPaint() {
        try {
            synchronized (redrawLock) {
                redrawLock.wait();
            }
        } catch (InterruptedException e) {
        }
    }

    private void resume() {
        synchronized (redrawLock) {
            redrawLock.notify();
        }
    }

    public void paint(Graphics g) {
        // paint the buffered image to the graphics
        g.drawImage(imageBuffer, 0, 0, component);

        // resume the game loop
        resume();
    }

    static class ResizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            resized = true;
        }
    }

    public static class MyComponent extends JPanel {
        private final MyGame game;

        public MyComponent(MyGame game) {
            this.game = game;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2D = (Graphics2D) g;
            AffineTransform at = g2D.getTransform();
            at.translate(zoomPointX, zoomPointY);
            at.scale(zoom, zoom);
            at.translate(-zoomPointX, -zoomPointY);
            g2D.setTransform(at);
            game.paint(g);
        }
    }


    static class Pair<T, K> {
        public T a;
        public K b;

        public Pair(T a, K b) {
            this.a = a;
            this.b = b;
        }
    }
}