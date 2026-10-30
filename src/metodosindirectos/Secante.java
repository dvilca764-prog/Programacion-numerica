package metodosindirectos;
import funcionmatematica.Funcion;

public class Secante extends CondicionMetodoIndirectos{
    public Secante(double x0, double epsilon, Funcion f){
        super(x0, epsilon, f);
    }

    public double resolver() {
        double x0 = getX0();
        double x1 = x0 + 0.1; // Segundo valor inicial auxiliar
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < 100 && error > getEpsilon()) {
            double fx0 = getF().evaluar(x0);
            double fx1 = getF().evaluar(x1);

            if (Math.abs(fx1 - fx0) < 1e-15) break;

            double x2 = x1 - (fx1 * (x1 - x0)) / (fx1 - fx0);

            error = Math.abs(x2 - x1);
            x0 = x1;
            x1 = x2;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return x1;
    }
}
