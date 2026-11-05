
package metodosdirectos;

public class Biseccion extends CondicionMetodoDirectos {

    public Biseccion(double a, double b, double epsilon, String f) {
        super(a, b, epsilon, f);
    }

    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }

        double aActual = this.a;
        double bActual = this.b;
        double c = (aActual + bActual) / 2.0;

        double FC = evaluar(c);
        int iters = 0;

        registrar(1, aActual, bActual, c, FC, (bActual - aActual) / 2.0);

        while (Math.abs(FC) > epsilon && iters < maxIter) {
            iters++;

            double FA = evaluar(aActual);

            if (FA * FC < 0) {
                bActual = c;
            } else {
                aActual = c;
            }

            c = (aActual + bActual) / 2.0;
            FC = evaluar(c);

            registrar(iters + 1, aActual, bActual, c, FC, (bActual - aActual) / 2.0);
        }

        System.out.println("Bisección - Raíz aproximada: " + c
                + "\niteraciones: " + iters);

        return c;
    }
}
