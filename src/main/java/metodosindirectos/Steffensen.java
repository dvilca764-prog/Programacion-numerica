package metodosindirectos;

/** Aceleración de Steffensen para la iteración x = g(x). */
public class Steffensen extends PuntoFijo {

    public Steffensen(double x0, double epsilon, String g) {
        super(x0, epsilon, g);
    }

    @Override
    public double resolver(int maxIteraciones) {
        historial.clear();
        double x = getX0();
        double tolerancia = getEpsilon();
        registrar(0, x, Double.NaN);

        for (int iteracion = 1; iteracion <= maxIteraciones; iteracion++) {
            double x1 = evaluar(x);       // g(x)
            if (!Double.isFinite(x1)) return Double.NaN;

            // Si g(x) ya es indistinguible de x dentro de la tolerancia,
            // el punto fijo está alcanzado, incluso si la fórmula acelerada
            // tiene un denominador muy pequeño.
            double residuoPuntoFijo = Math.abs(x1 - x);
            if (residuoPuntoFijo <= tolerancia) {
                registrar(iteracion, x1, residuoPuntoFijo);
                return x1;
            }

            double x2 = evaluar(x1);      // g(g(x))
            if (!Double.isFinite(x2)) return Double.NaN;

            double denominador = x2 - 2.0 * x1 + x;
            if (!Double.isFinite(denominador) || Math.abs(denominador) < 1e-15) {
                // No afirmar convergencia si el residuo no cumple la tolerancia.
                // Devolvemos la aproximación actual; MotorNumerico la marcará
                // como no convergente porque el último error no se redujo.
                return x;
            }

            double siguiente = x - (x1 - x) * (x1 - x) / denominador;
            if (!Double.isFinite(siguiente)) return Double.NaN;

            double error = Math.abs(siguiente - x);
            x = siguiente;
            registrar(iteracion, x, error);
            if (error <= tolerancia) return x;
        }
        return x;
    }
}
