package metodosindirectos;

/** Aceleración de Aitken aplicada iterativamente a x = g(x). */
public class Aitken extends PuntoFijo {
    public Aitken(double x0, double epsilon, String g) {
        super(x0, epsilon, g);
    }

    @Override
    public double resolver(int maxIteraciones) {
        historial.clear();
        double x = getX0();
        double tol = getEpsilon();
        registrar(0, x, Double.NaN);
        for (int n = 1; n <= maxIteraciones; n++) {
            double x1 = evaluar(x);
            if (!Double.isFinite(x1)) return Double.NaN;
            double residuo = Math.abs(x1 - x);
            // Evita un falso fallo cuando la raíz se alcanzó antes
            // de que el denominador de Aitken se haga casi cero.
            if (residuo <= tol) {
                registrar(n, x1, residuo);
                return x1;
            }
            double x2 = evaluar(x1);
            if (!Double.isFinite(x2)) return Double.NaN;
            double delta2 = x2 - 2.0 * x1 + x;
            if (!Double.isFinite(delta2) || Math.abs(delta2) < 1e-15) return x;
            double siguiente = x - (x1 - x) * (x1 - x) / delta2;
            if (!Double.isFinite(siguiente)) return Double.NaN;
            double error = Math.abs(siguiente - x);
            x = siguiente;
            registrar(n, x, error);
            if (error <= tol) return x;
        }
        return x;
    }
}
