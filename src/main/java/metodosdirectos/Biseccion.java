package metodosdirectos;
import org.mariuszgromada.math.mxparser.Argument;
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
        double c = (aActual + bActual) / 2.0;
        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);
        x.setArgumentValue(c);
        double FC = funcion.calculate();
        double FA;
        int iters = 0;

        while (Math.abs(FC) > epsilon) {
            //System.out.println("c = "+c);
            iters++;
            x.setArgumentValue(aActual);
            FA = funcion.calculate();
            if (FA * FC < 0) {
                bActual = c;
            } else {
                aActual = c;
            }
            c = (aActual + bActual) / 2.0;
            x.setArgumentValue(c);
            FC = funcion.calculate();
        }

        System.out.println("Bisección - Raíz aproximada: " + c +"\niteraciones: "+iters);
        return c;
    }
}
