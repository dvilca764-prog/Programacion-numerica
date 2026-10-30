package metodosindirectos;
import funcionmatematica.Funcion;

public class PuntoFijo extends CondicionMetodoIndirectos{
    public PuntoFijo(double x0, double epsilon, Funcion f){
        super(x0, epsilon, f);
    }

    public double resolver(){
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while(iteracion < 100 && error > getEpsilon()){
            double xn = getF().evaluar(xs);
            error = Math.abs(xn - xs);
            xs = xn;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }

}
