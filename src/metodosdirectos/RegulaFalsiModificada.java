package metodosdirectos;
import funcionmatematica.Funcion;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

public class RegulaFalsiModificada extends CondicionMetodoDirectos{
    public RegulaFalsiModificada(double a, double b, double epsilon, String f) {
        super(a, b, epsilon, f);
    }

    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }
        double aActual = this.a;
        double bActual = this.b;

        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);

        x.setArgumentValue(aActual);
        double FA = funcion.calculate();

        x.setArgumentValue(bActual);
        double FB = funcion.calculate();

        double W = FA;

        double fValue = FA;
        double gValue = FB;

        double c = (aActual * FB - bActual * FA) / (FB - FA);

        x.setArgumentValue(c);
        double FC = funcion.calculate();
        int iters = 0;

        while (Math.abs(FC) > epsilon) {
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

            c = (aActual * gValue - bActual * fValue) / (gValue - fValue);

            x.setArgumentValue(c);
            FC = funcion.calculate();
        }

        System.out.println("Regula Falsi Modificada - Raíz aproximada: " + c +"\niteraciones: "+iters);
        return c;
    }
}
