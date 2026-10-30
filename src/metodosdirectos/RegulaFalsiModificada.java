package metodosdirectos;
import funcionmatematica.Funcion;

public class RegulaFalsiModificada extends CondicionMetodoDirectos{
    public RegulaFalsiModificada(double a, double b, double epsilon, Funcion f) {
        super(a, b, epsilon, f);
    }

    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }
        double aActual = this.a;
        double bActual = this.b;

        double fValue = f.evaluar(aActual);
        double gValue = f.evaluar(bActual);
        double w = f.evaluar(aActual);

        double c = (aActual * f.evaluar(bActual) - bActual * f.evaluar(aActual)) / (f.evaluar(bActual) - f.evaluar(aActual));

        while (Math.abs(f.evaluar(c)) > epsilon) {
            if (f.evaluar(aActual) * f.evaluar(c) < 0) {
                bActual = c;
                gValue = f.evaluar(c);
                if (w * gValue > 0) {
                    fValue = fValue / 2.0;
                }
            } else {
                aActual = c;
                fValue = f.evaluar(c);
                if (w * fValue > 0) {
                    gValue = gValue / 2.0;
                }
            }

            w = f.evaluar(c);
            c = (aActual * gValue - bActual * fValue) / (gValue - fValue);
        }

        System.out.println("Regula Falsi Modificada - Raíz encontrada c: " + c);
        return c;
    }
}
