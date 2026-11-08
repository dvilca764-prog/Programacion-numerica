package metodosindirectos;

public class PuntoFijo extends CondicionMetodoIndirectos {

    public PuntoFijo(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;
        registrar(0, xs, Double.NaN);

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            // Evaluamos la expresión de texto directamente con mxparser mediante la clase padre
            double xn = evaluar(xs);

            if (!Double.isFinite(xn)) return Double.NaN;
            error = Math.abs(xn - xs);
            xs = xn;
            iteracion++;
            registrar(iteracion, xs, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Punto Fijo - Raíz aproximada: " + xs);
        return xs;
    }
}
