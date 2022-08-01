package fourier;

public class FourierComponent {
    public double re;
    public double im;
    public double freq;
    public double amp;
    public double phase;
    public boolean sectionEnd = false;

    public FourierComponent(boolean sectionEnd) {
        this.sectionEnd = sectionEnd;
    }

    public FourierComponent(double re, double im, double freq, double amp, double phase) {
        this.re = re;
        this.im = im;
        this.freq = freq;
        this.amp = amp;
        this.phase = phase;
    }

    @Override
    public String toString() {
        return "FourierComponent{" +
                "re=" + re +
                ", im=" + im +
                '}';
    }
}
