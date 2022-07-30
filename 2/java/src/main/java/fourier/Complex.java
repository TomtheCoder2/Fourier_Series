package fourier;

public class Complex {
    public double re = 0;
    public double im = 0;
    public Complex(double a, double b) {
        this.re = a;
        this.im = b;
    }

    public void add(Complex c) {
        this.re += c.re;
        this.im += c.im;
    }

    public Complex mult(Complex c) {
        return new Complex(this.re * c.re - this.im * c.im, this.re * c.im + this.im * c.re);
    }

    @Override
    public String toString() {
        return "Complex{" +
                "re=" + re +
                ", im=" + im +
                '}';
    }
}
