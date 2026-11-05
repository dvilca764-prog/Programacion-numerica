
package metodosdirectos;

public class RegulaFalsi extends CondicionMetodoDirectos {

    public RegulaFalsi(double a, double b, double epsilon, String f) {
        super(a, b, epsilon, f);
    }

    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }

        double aActual = this.a;
        double bActual = this.b;

        double FA = evaluar(aActual);
        double FB = evaluar(bActual);

        double c = (aActual * FB - bActual * FA) / (FB - FA);
        double FC = evaluar(c);

        int iters = 0;
        double cPrev = c;

        registrar(1, aActual, bActual, c, FC, Double.NaN);

        while (Math.abs(FC) > epsilon && iters < maxIter) {
            iters++;

            if (FA * FC < 0) {
                bActual = c;
                FB = FC;
            } else {
                aActual = c;
                FA = FC;
            }

            c = (aActual * FB - bActual * FA) / (FB - FA);
            FC = evaluar(c);

            registrar(iters + 1, aActual, bActual, c, FC, Math.abs(c - cPrev));
            cPrev = c;
        }

        System.out.println("Regula Falsi - Raíz aproximada: " + c
                + "\niteraciones: " + iters);

        return c;
    }
}
