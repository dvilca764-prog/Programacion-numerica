
package metodosdirectos;

public class RegulaFalsiModificada extends CondicionMetodoDirectos {

    public RegulaFalsiModificada(double a, double b, double epsilon, String f) {
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

        double W = FA;
        double fValue = FA;
        double gValue = FB;

        double c = (aActual * FB - bActual * FA) / (FB - FA);
        double FC = evaluar(c);

        int iters = 0;
        double cPrev = c;

        registrar(1, aActual, bActual, c, FC, Double.NaN);

        while (Math.abs(FC) > epsilon && iters < maxIter) {
            iters++;

            if (FA * FC < 0) {
                bActual = c;
                gValue = FC;

                if (W * gValue > 0) {
                    fValue = fValue / 2.0;
                }

            } else {
                aActual = c;
                fValue = FC;

                if (W * fValue > 0) {
                    gValue = gValue / 2.0;
                }
            }

            W = FC;

            c = (aActual * gValue - bActual * fValue)
                    / (gValue - fValue);

            FC = evaluar(c);

            registrar(iters + 1, aActual, bActual, c, FC,
                    Math.abs(c - cPrev));

            cPrev = c;
        }

        System.out.println("Regula Falsi Modificada - Raíz aproximada: " + c
                + "\niteraciones: " + iters);

        return c;
    }
}
