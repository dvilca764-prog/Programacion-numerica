package metodosdirectos;
import funcionmatematica.Funcion;
import org.mariuszgromada.math.mxparser.Expression;

public class Biseccion extends CondicionMetodoDirectos{
    public Biseccion(double a,double b,double epsilon,String f){
        super(a,b,epsilon,f);
    }
    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }
        double aActual = this.a;
        double bActual = this.b;
        double c = aActual;

        while ((bActual - aActual) / 2.0 > epsilon) {
            c = (aActual + bActual) / 2.0;

            Expression fa = new Expression(f + " | x = " + aActual);
            Expression fb = new Expression(f + " | x = " + bActual);
            double A = fa.calculate();
            double B = fb.calculate();

            if (A * B < 0) {
                bActual = c;
            } else {
                aActual = c;
            }
        }

        System.out.println("Bisección - Raíz encontrada: " + c);
        return c;
    }
}
