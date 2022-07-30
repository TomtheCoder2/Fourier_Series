package fourier;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;

public class Fourier {
    /**
     * Look at the formula <a href="https://wikimedia.org/api/rest_v1/media/math/render/svg/8e904306df1ed48e64053b1f579c8bfab5508157">here</a>
     * <svg xmlns="https://wikimedia.org/api/rest_v1/media/math/render/svg/8e904306df1ed48e64053b1f579c8bfab5508157" viewBox="0 0 800 600">
     */
    public static FourierComponent[] dft(Complex[] input) {
        ArrayList<Complex> xA = new ArrayList<>();
        for (Complex c : input) {
            if (!c.sectionEnd) {
                xA.add(c);
            }
        }
        Complex[] x = xA.toArray(Complex[]::new);

        int N = x.length;
        FourierComponent[] X = new FourierComponent[N];
        for (int k = 0; k < N; k++) {
            Complex sum = new Complex(0, 0);
            for (int n = 0; n < N; n++) {
                if (x[n].sectionEnd) {
                    continue;
                }
                double phi = (Math.PI * 2 * k * n) / N;
                Complex c = new Complex(Math.cos(phi), -Math.sin(phi));
                sum.add(x[n].mult(c));
            }
            sum.re /= N;
            sum.im /= N;

            double amp = Math.sqrt(sum.re * sum.re + sum.im * sum.im);
            double phase = atan2(sum.im, sum.re);
            X[k] = new FourierComponent(sum.re, sum.im, k, amp, phase);
        }
        return X;
    }
}
