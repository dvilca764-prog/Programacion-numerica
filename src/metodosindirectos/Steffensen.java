package metodosindirectos;
import funcionmatematica.Funcion;

public class Steffensen extends PuntoFijo{
    public Steffensen(double x0, double epsilon, Funcion f){
        super(x0, epsilon, f);
    }

    public double resolver() {
        double x0 = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < 100 && error > getEpsilon()) {
            double x1 = getF().evaluar(x0);
            double x2 = getF().evaluar(x1);

            double denominador = x2 - 2 * x1 + x0;
            if (Math.abs(denominador) < 1e-15) break; // Evita división por cero

            double xNuevo = x0 - Math.pow(x1 - x0, 2) / denominador;

            error = Math.abs(xNuevo - x0);
            x0 = xNuevo;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return x0;
    }
}
