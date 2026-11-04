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
            if (Math.abs(denominador) < 1e-15) {
                System.out.println("Error: Denominador nulo en el método de Newton Extendido.");
                return Double.NaN;
            }

            xs = xs - (numerador / denominador);
            error = Math.abs(xs - xAnterior);
            iteracion++;
            registrar(iteracion, xs, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Newton Extendido - Raíz aproximada: " + xs);
        return xs;
    }
}


/*package metodosindirectos;
import funcionmatematica.Funcion;

public class NewtonExt extends CondicionMetodoIndirectos{
    private Funcion fDerivada;
    public NewtonExt(double x0, double epsilon, Funcion f, Funcion fDerivada){
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

            double aTerm = dfx * fx;
            double bTerm = fx * ddfx;

            xs = xs - (aTerm / (Math.pow(dfx, 2) - bTerm));
            error = Math.abs(xs - xAnterior);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }

    public Funcion getDerivada(){
        return this.fDerivada;
    }
}
*/