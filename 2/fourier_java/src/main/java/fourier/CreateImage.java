package fourier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static fourier.Fourier.dft;
import static fourier.MyGame.getScalarAndOffset;
import static java.lang.Math.*;

public class CreateImage {
    public static void main(String[] args) {
        createImage("/drawings/clef.txt", 10000, 10000, "clef", 10);
    }

    public static void createImage(String fourierFileName, int width, int height, String fileName, int strokeWidth) {
        // Constructs a BufferedImage of one of the predefined image types.
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Create a graphics which can be used to draw into the buffered image
        Graphics2D g = bufferedImage.createGraphics();


        // fourier
        g.setColor(new Color(0x000000));
        g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        double[][][] drawingSection = Drawing.getDrawingSection(fourierFileName);
        double[][] drawing = Drawing.getDrawing(fourierFileName);
        int skip = 1;
        ArrayList<Complex> originalFunction = new ArrayList<>();
        double[] d = getScalarAndOffset(width, height, drawing);
        double scalar = d[0];
        double offsetX = d[1];
        double offsetY = d[2];
        System.out.println("Sections: " + drawingSection[0].length);
        for (int s = 0; s < drawingSection[0].length; s++) {
            System.out.println("Section: " + s);
            originalFunction = new ArrayList<>();
            for (int i = 0; i < drawing.length; i += 1) {
                if (i % skip == 0) {
                    if (drawingSection[i][s][0] != 0 && drawingSection[i][s][1] != 0) {
                        originalFunction.add(new Complex(drawingSection[i][s][0] * scalar + offsetX, drawingSection[i][s][1] * scalar + offsetY));
                    }
                }
                if (drawingSection[i][s][0] == 0 && drawingSection[i][s][1] == 0) {
                    originalFunction.add(new Complex(true));
                }
            }
            FourierComponent[] fourier = dft(originalFunction.toArray(Complex[]::new));
            fourier = Arrays.stream(fourier).sorted((o1, o2) -> {
                if (o2.amp - o1.amp < 0) {
                    return -1;
                } else if (o2.amp - o1.amp > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }).toList().toArray(new FourierComponent[0]);

            double time = 0;
            ArrayList<MyGame.Pair<Double, Double>> path = new ArrayList<>();
            double dt = (PI * 2) / fourier.length;
            while (time < PI * 2) {
                g.setColor(new Color(0xffffff));
                MyGame.Pair<Double, Double> v = epiCycles(bufferedImage.getWidth() / 2.0, bufferedImage.getHeight() / 2.0, 0, fourier, g, time);
                if (!path.contains(v)) path.add(0, v);
                g.setColor(new Color(0xffffff));
                for (int i = 0; i < path.size(); i++) {
                    MyGame.Pair<Double, Double> doubleDoublePair = path.get(i);
                    if (i != 0) {
                        g.setStroke(new BasicStroke(strokeWidth));
                        g.drawLine((int) doubleDoublePair.a.doubleValue(), (int) doubleDoublePair.b.doubleValue(),
                                (int) path.get(i - 1).a.doubleValue(), (int) path.get(i - 1).b.doubleValue());
                    }
                }
                time += dt;
            }
            g.drawLine((int) path.get(0).a.doubleValue(), (int) path.get(0).b.doubleValue(),
                    (int) path.get(path.size() - 1).a.doubleValue(), (int) path.get(path.size() - 1).b.doubleValue());
        }

        // Disposes of this graphics context and releases any system resources that it is using.
        g.dispose();

        try {
            // Save as PNG
            File file = new File("renders/" + fileName + ".png");
            ImageIO.write(bufferedImage, "png", file);

            // Save as JPEG
            file = new File("renders/" + fileName + ".jpg");
            ImageIO.write(bufferedImage, "jpg", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MyGame.Pair<Double, Double> epiCycles(double x, double y, double rotation, FourierComponent[] fourier, Graphics g, double time) {
        for (FourierComponent FourierComponent : fourier) {
            double freq = FourierComponent.freq;
            double radius = FourierComponent.amp;
            double phase = FourierComponent.phase;
            x += radius * cos(freq * time + phase + rotation);
            y += radius * sin(freq * time + phase + rotation);
        }

        return new MyGame.Pair<>(x, y);
    }
}
