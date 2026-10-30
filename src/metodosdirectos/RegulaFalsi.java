package metodosdirectos;

import funcionmatematica.Funcion;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

public class RegulaFalsi extends CondicionMetodoDirectos{
    public RegulaFalsi(double a, double b, double epsilon,String f){
        super(a,b,epsilon,f);
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

        double c = (aActual * FB - bActual * FA) / (FB - FA);

        x.setArgumentValue(c);
        double FC = funcion.calculate();
        int iters = 0;

        while (Math.abs(FC) > epsilon) {
            iters++;
            if (FA * FC < 0) {
                bActual = c;
                FB = FC;
            } else {
                aActual = c;
                FA = FC;
            }

            c = (aActual * FB - bActual * FA) / (FB - FA);

            x.setArgumentValue(c);
            FC = funcion.calculate();
        }

        System.out.println("Regula Falsi - Raíz aproximada: " + c +"\niteraciones: "+iters);
        return c;
    }
}
