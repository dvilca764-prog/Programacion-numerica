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
        int iteracion = 0;

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            double xAnterior = xs;

            // 1. ¡Usamos directamente el método 'evaluar' heredado del padre para f(x)!
            double fx = evaluar(xs);

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

            if (Math.abs(denominador) < 1e-15) {
                System.out.println("Error: Denominador nulo en Halley.");
                return Double.NaN;
            }

            xs = xs - (numerador / denominador);
            error = Math.abs(xs - xAnterior);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Halley - Raíz aproximada: " + xs);
        return xs;
    }
}

/*package metodosindirectos;
import funcionmatematica.Funcion;

public class Halley extends CondicionMetodoIndirectos {
    private Funcion fDerivada;

    public Halley(double x0, double epsilon, Funcion f, Funcion fDerivada) {
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

            double numerador = 2 * fx * dfx;
            double denominador = 2 * Math.pow(dfx, 2) - fx * ddfx;

            xs = xs - (numerador / denominador);
            error = Math.abs(xs - xAnterior);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }

    public Funcion getDerivada() {
        return this.fDerivada;
    }
}*/
