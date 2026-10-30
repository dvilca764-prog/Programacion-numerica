package metodosdirectos;

import funcionmatematica.Funcion;
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
        Expression fa = new Expression(f + " | x = " + aActual);
        Expression fb = new Expression(f + " | x = " + bActual);
        double FA = fa.calculate();
        double FB = fb.calculate();

        double c = (aActual * FB - bActual * FA) / (FB - FA);

        Expression fc = new Expression(f + " | x = " + c);
        double FC= fc.calculate();

        while (Math.abs(FC) > epsilon) {
            if (FA * FC < 0) {
                bActual = c;
            } else {
                aActual = c;
            }
            fa = new Expression(f + " | x = " + aActual);
            fb = new Expression(f + " | x = " + bActual);
            FA = fa.calculate();
            FB = fb.calculate();

            System.out.println(c);
            c = (aActual * FB - bActual * FA) / (FB - FA);
        }

        System.out.println("Regula Falsi - Raíz encontrada c: " + c);
        return c;
    }
}
