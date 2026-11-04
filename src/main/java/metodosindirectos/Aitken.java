package metodosindirectos;

public class Aitken extends PuntoFijo {

    public Aitken(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xOriginal = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;
        registrar(0, xOriginal, Double.NaN);

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            // Evaluamos x1 y x2 usando el método 'evaluar' heredado (mxparser)
            double x1 = evaluar(xOriginal);
            double x2 = evaluar(x1);

            double denominador = x2 - 2 * x1 + xOriginal;

            if (Math.abs(denominador) < 1e-15) {
                System.out.println("Error: División por cero en la aceleración de Aitken.");
                break;
            }

            // Fórmula de aceleración de Aitken
            double xAcelerado = xOriginal - Math.pow(x1 - xOriginal, 2) / denominador;

            error = Math.abs(xAcelerado - xOriginal);
            xOriginal = xAcelerado;
            iteracion++;
            registrar(iteracion, xOriginal, error);
        }

        System.out.println("Aitken - Raíz aproximada: " + xOriginal);
        return xOriginal;
    }
}





/*package metodosindirectos;
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
 */
