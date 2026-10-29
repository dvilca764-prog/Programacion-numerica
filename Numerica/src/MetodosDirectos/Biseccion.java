package MetodosDirectos;
import FuncionMatematica.Funcion;

public class Biseccion extends CondicionMetodoDirectos{
    public Biseccion(double a,double b,double epsilon,Funcion f){
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

            if (f.evaluar(aActual) * f.evaluar(c) < 0) {
                bActual = c;
            } else {
                aActual = c;
            }
        }

        System.out.println("Bisección - Raíz encontrada: " + c);
        return c;
    }
}
