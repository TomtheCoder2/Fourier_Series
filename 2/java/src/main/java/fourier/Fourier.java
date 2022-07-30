package fourier;

import static java.lang.Math.atan2;

public class Fourier {
    /**
     * Look at the formula <a href="https://wikimedia.org/api/rest_v1/media/math/render/svg/8e904306df1ed48e64053b1f579c8bfab5508157">here</a>
     * <svg xmlns="https://wikimedia.org/api/rest_v1/media/math/render/svg/8e904306df1ed48e64053b1f579c8bfab5508157" viewBox="0 0 800 600">
     */
    public static fourierComponent[] dft(Complex[] x) {
        int N = x.length;
        fourierComponent[] X = new fourierComponent[N];
        for (int k = 0; k < N; k++) {
            Complex sum = new Complex(0, 0);
            for (int n = 0; n < N; n++) {
                double phi = (Math.PI * 2 * k * n) / N;
                Complex c = new Complex(Math.cos(phi), -Math.sin(phi));
                sum.add(x[n].mult(c));
            }
            sum.re /= N;
            sum.im /= N;

            double amp = Math.sqrt(sum.re * sum.re + sum.im * sum.im);
            double phase = atan2(sum.im, sum.re);
            X[k] = new fourierComponent(sum.re, sum.im, k, amp, phase);
        }
        return X;
    }

    static class fourierComponent {
        public double re;
        public double im;
        public double freq;
        public double amp;
        public double phase;

        public fourierComponent(double re, double im, double freq, double amp, double phase) {
            this.re = re;
            this.im = im;
            this.freq = freq;
            this.amp = amp;
            this.phase = phase;
        }
    }
}
