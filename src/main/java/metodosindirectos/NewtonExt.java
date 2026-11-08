package metodosindirectos;

import org.mariuszgromada.math.mxparser.*;

public class NewtonExt extends CondicionMetodoIndirectos {

    public NewtonExt(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;
        registrar(0, xs, Double.NaN);

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            double xAnterior = xs;

            // 1. Evaluamos f(x) directamente con el método heredado de la clase padre
            double fx = evaluar(xs);
            if (!Double.isFinite(fx)) return Double.NaN;
            // Antes de dividir, comprobar si la aproximación ya es una raíz.
            if (Math.abs(fx) <= getEpsilon()) {
                registrar(iteracion + 1, xs, 0.0);
                return xs;
            }

            // 2. Creamos el argumento x para evaluar las derivadas con mxparser
            Argument x = new Argument("x");
            x.setArgumentValue(xs);

            // 3. Evaluamos la primera derivada f'(x) usando der() de mxparser
            Expression exprDF = new Expression("der(" + f + ", x)", x);
            double dfx = exprDF.calculate();

            // 4. Evaluamos la segunda derivada f''(x) anidando der() en mxparser
            Expression exprDDF = new Expression("der(der(" + f + ", x), x)", x);
            double ddfx = exprDDF.calculate();

            // Fórmula de Newton Extendido para raíces múltiples:
            // x_{k+1} = x_k - [ (f(x_k) * f'(x_k)) / ((f'(x_k))^2 - f(x_k) * f''(x_k)) ]
            double numerador = fx * dfx;
            double denominador = Math.pow(dfx, 2) - (fx * ddfx);

            // Protección contra división por cero
            if (!Double.isFinite(denominador) || Math.abs(denominador) < 1e-15) {
                System.out.println("Error: Denominador nulo en el método de Newton Extendido.");
                return Double.NaN;
            }

            xs = xs - (numerador / denominador);
            if (!Double.isFinite(xs)) return Double.NaN;
            error = Math.abs(xs - xAnterior);
            iteracion++;
            registrar(iteracion, xs, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Newton Extendido - Raíz aproximada: " + xs);
        return xs;
    }
}
