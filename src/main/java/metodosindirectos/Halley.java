package metodosindirectos;

import org.mariuszgromada.math.mxparser.*;

public class Halley extends CondicionMetodoIndirectos {

    public Halley(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xs = getX0();
        double error = Double.MAX_VALUE;
        registrar(0, xs, Double.NaN);
        int iteracion = 0;

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            double xAnterior = xs;

            // 1. ¡Usamos directamente el método 'evaluar' heredado del padre para f(x)!
            double fx = evaluar(xs);
            if (!Double.isFinite(fx)) return Double.NaN;
            // Antes de dividir, comprobar si la aproximación ya es una raíz.
            if (Math.abs(fx) <= getEpsilon()) {
                registrar(iteracion + 1, xs, 0.0);
                return xs;
            }

            // 2. Para las derivadas, como mxparser usa el comando 'der()',
            // creamos el argumento únicamente para pasárselo a la derivada:
            Argument x = new Argument("x");
            x.setArgumentValue(xs);

            Expression exprDF = new Expression("der(" + f + ", x)", x);
            double dfx = exprDF.calculate();

            Expression exprDDF = new Expression("der(der(" + f + ", x), x)", x);
            double ddfx = exprDDF.calculate();

            // Fórmula de Halley
            double numerador = 2.0 * fx * dfx;
            double denominador = 2.0 * Math.pow(dfx, 2) - fx * ddfx;

            if (!Double.isFinite(denominador) || Math.abs(denominador) < 1e-15) {
                System.out.println("Error: Denominador nulo en Halley.");
                return Double.NaN;
            }

            xs = xs - (numerador / denominador);
            if (!Double.isFinite(xs)) return Double.NaN;
            error = Math.abs(xs - xAnterior);
            iteracion++;
            registrar(iteracion, xs, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Halley - Raíz aproximada: " + xs);
        return xs;
    }
}
