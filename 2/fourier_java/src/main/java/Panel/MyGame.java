package Panel;

import fourier.Complex;
import fourier.Drawing;
import fourier.FourierComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;

import static fourier.Fourier.dft;
import static java.lang.Math.*;

/**
 * Self contained demo swing game for stackoverflow frame rate question.
 *
 * @author dave
 */
public class MyGame {
    private static final long REFRESH_INTERVAL_MS = 17;
    public static ArrayList<Complex> originalFunction;
    public static FourierComponent[] fourier;
    private static double time = 0;
    private static ArrayList<Pair<Double, Double>> path = new ArrayList<>();
    private static String[] static_args;
    private static int skip = 10;
    private static boolean clearScreen = false;
    private static Component component;
    private static Image imageBuffer;
    private static boolean resized = false;
    private static double[][] drawing;
    private static double zoom = 1;
    private static int zoomPointX;
    private static int zoomPointY;
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
//                    repaint();
//                    System.out.println("zoom = " + zoom);
            });

            game.start(component);
        });
    }

    public static void setup() {
        System.out.printf("Setup everything...\nskip: %d, clearScreen: %b\nDimensions:\n\twidth: %dpx\n\theight: %dpx\n\n", skip, clearScreen, component.getWidth(), component.getHeight());
        path = new ArrayList<>();
        imageBuffer = component.createImage(component.getWidth(),
                component.getHeight());
        // init
//        originalFunction = new Complex[drawing.length / skip + ((double) (drawing.length % skip) / (double) skip == 0 ? 0 : 1)];
        originalFunction = new ArrayList<>();
        // calculate by how much to translate the picture so that it fits on to the screen
        double topLeftX = component.getWidth(), topLeftY = component.getHeight(), bottomRightX = 0, bottomRightY = 0;
        for (double[] doubles : drawing) {
            if (doubles[1] == 0 && doubles[0] == 0) continue;
            topLeftX = min(doubles[0], topLeftX);
            topLeftY = min(doubles[1], topLeftY);

            bottomRightX = max(doubles[0], bottomRightX);
            bottomRightY = max(doubles[1], bottomRightY);
        }
//        System.out.printf("topLeftX: %f, topLeftY: %f, bottomRightX: %f, bottomRightY: %f\n", topLeftX, topLeftY, bottomRightX, bottomRightY);
        double targetHeight = component.getHeight() * 3.0 / 4.0;
        double targetWidth = component.getWidth() * 3.0 / 4.0;
//        System.out.printf("targetHeight: %f, targetWidth: %f\n", targetHeight, targetWidth);
        double currentHeight = bottomRightY - topLeftY;
        double currentWidth = bottomRightX - topLeftX;
        double scalar = targetHeight / currentHeight;
        if (scalar * currentWidth > targetWidth) {
            scalar = targetWidth / currentWidth;
        }
        double offsetY = -currentHeight * scalar / 2 - topLeftY * scalar;
        double offsetX = -currentWidth * scalar / 2 - topLeftX * scalar;

//        System.out.printf("topLeftX: %f, topLeftY: %f, bottomRightX: %f, bottomRightY: %f\n", scalar * topLeftX, scalar * topLeftY, scalar * bottomRightX, scalar * bottomRightY);
//        System.out.printf("offsetX: %f, offsetY: %f, scalar: %f\n", offsetX, offsetY, scalar);
//        System.out.printf("topLeftX: %f, topLeftY: %f, bottomRightX: %f, bottomRightY: %f\n", scalar * topLeftX + offsetX, scalar * topLeftY + offsetY, scalar * bottomRightX + offsetX, scalar * bottomRightY + offsetY);
//        System.out.println(Arrays.deepToString(drawing));
        for (int i = 0; i < drawing.length; i += 1) {
//            System.out.printf("i: %d, len: %d, len or: %d\n", i, drawing.length, originalFunction.length);
            if (i % skip == 0) {
                if (drawing[i][0] != 0 && drawing[i][1] != 0) {
                    originalFunction.add(
                            new Complex(drawing[i][0]
                                    * scalar
                                    + offsetX
                                    , drawing[i][1]
                                    * scalar
                                    + offsetY
                            ));
                }
            }
            if (drawing[i][0] == 0 && drawing[i][1] == 0) {
                originalFunction.add(new Complex(true));
            }
        }
        for (Complex c : originalFunction) {
            if (!c.sectionEnd && c.im == 0 && c.re == 0) {
                System.out.println(c);
            }
        }
//        System.out.println(Arrays.toString(originalFunction));
        fourier = dft(originalFunction.toArray(Complex[]::new));
        fourier = Arrays.stream(fourier).sorted((o1, o2) -> {
            if (o2.amp - o1.amp < 0) {
                return -1;
            } else if (o2.amp - o1.amp > 0) {
                return 1;
            } else {
                return 0;
            }
//           return (int) (o2.amp - o1.amp);
        }).toList().toArray(new FourierComponent[0]);
    }

    public void start(Component componentIn) {
        component = componentIn;
        drawing = Drawing.getDrawing(static_args.length > 0 ? static_args[0] : "/drawings/java.txt");
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

        AffineTransform oldTransform = g.getTransform();
//        g.scale(1.0, 2.0);

//        g.setColor(component.getBackground());
//        g.fillRect(0, 0, component.getWidth(), component.getHeight());
//        g.setColor(component.getForeground());
//        g.drawString("Hi", x++, y++);
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
        g.setTransform(oldTransform);
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