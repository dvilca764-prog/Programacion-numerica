package metodosdirectos;
import funcionmatematica.Funcion;
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

        Expression fa = new Expression(f + " | x = " + aActual);
        Expression fb = new Expression(f + " | x = " + bActual);
        double FA = fa.calculate();
        double FB = fb.calculate();
        double W = fa.calculate();

        double fValue = FA;
        double gValue = FB;
        //double w = f.evaluar(aActual);

        double c = (aActual * FB - bActual * FA) / (FB - FA);

        Expression fc = new Expression(f + " | x = " + c);
        double FC = fc.calculate();

        while (Math.abs(FC) > epsilon) {
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

            fa = new Expression(f + " | x = " + aActual);
            fb = new Expression(f + " | x = " + bActual);
            FA = fa.calculate();
            FB = fb.calculate();

            c = (aActual * gValue - bActual * fValue) / (gValue - fValue);
        }

        System.out.println("Regula Falsi Modificada - Raíz encontrada c: " + c);
        return c;
    }
}
