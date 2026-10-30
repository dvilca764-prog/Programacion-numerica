package metodosindirectos;
import funcionmatematica.Funcion;

public class Aitken extends PuntoFijo {

    public Aitken(double x0, double epsilon, Funcion f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver() {
        double x0 = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < 100 && error > getEpsilon()) {
            double x1 = getF().evaluar(x0);
            double x2 = getF().evaluar(x1);

            double denominador = x2 - 2 * x1 + x0;
            if (Math.abs(denominador) < 1e-15) break;

            // Aceleración de Aitken
            double xAcelerado = x0 - Math.pow(x1 - x0, 2) / denominador;

            error = Math.abs(xAcelerado - x0);
            x0 = xAcelerado; // Se utiliza la aceleración para el siguiente paso
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return x0;
    }
}
