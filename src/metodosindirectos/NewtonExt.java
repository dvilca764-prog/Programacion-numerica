package metodosindirectos;
import funcionmatematica.Funcion;

public class NewtonExt extends CondicionMetodoIndirectos{
    private Funcion fDerivada;
    public NewtonExt(double x0, double epsilon, Funcion f, Funcion fDerivada){
        super(x0, epsilon, f);
        this.fDerivada = fDerivada;
    }

    public double resolver() {
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < 100 && error > getEpsilon()) {
            double xAnterior = xs;
            
            double fx = getF().evaluar(xs);
            double dfx = getDerivada().evaluar(xs);
            double ddfx = getDerivada().derivada().evaluar(xs);

            double aTerm = dfx * fx;
            double bTerm = fx * ddfx;

            xs = xs - (aTerm / (Math.pow(dfx, 2) - bTerm));
            error = Math.abs(xs - xAnterior);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }

    public Funcion getDerivada(){
        return this.fDerivada;
    }
}
