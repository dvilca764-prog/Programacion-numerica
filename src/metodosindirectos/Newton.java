package metodosindirectos;
import funcionmatematica.Funcion;

public class Newton extends CondicionMetodoIndirectos{
    private Funcion df;
    public Newton(double x0, double epsilon, Funcion f, Funcion df){
        super(x0, epsilon, f);
        this.df = df;
    }

    public double resolver(){
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while(iteracion < 100 && error > getEpsilon()){
            double xant = xs;
            xs = xs - (getF().evaluar(xs) / df.evaluar(xs));
            error = Math.abs(xs - xant);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }
    
}
