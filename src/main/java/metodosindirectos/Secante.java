package metodosindirectos;

public class Secante extends CondicionMetodoIndirectos {
    private double x1;

    // Constructor que recibe x0, x1, epsilon y el String de la función
    public Secante(double x0, double x1, double epsilon, String f) {
        super(x0, epsilon, f);
        this.x1 = x1;
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xkMenos1 = getX0(); // x_{k-1}
        double xk = this.x1;        // x_k
        int iteracion = 0;
        double error = Double.MAX_VALUE;
        registrar(0, xkMenos1, Double.NaN);
        registrar(1, xk, Math.abs(xk - xkMenos1));

        while (iteracion < maxIteraciones && error > getEpsilon()) {

            // Evaluamos f(x_k) y f(x_{k-1}) usando el método 'evaluar' de la clase padre
            double fxk = evaluar(xk);
            double fxkMenos1 = evaluar(xkMenos1);

            // Protección contra división por cero
            if (Math.abs(fxk - fxkMenos1) < 1e-15) {
                System.out.println("Error: División por cero en el método de la Secante.");
                return Double.NaN;
            }

            // Fórmula del método de la Secante
            double xkSiguiente = xk - (fxk * (xk - xkMenos1)) / (fxk - fxkMenos1);

            error = Math.abs(xkSiguiente - xk);

            // Actualizamos los valores para la siguiente iteración
            xkMenos1 = xk;
            xk = xkSiguiente;
            iteracion++;
            registrar(iteracion + 1, xk, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Secante - Raíz aproximada: " + xk);
        return xk;
    }
}



/*package metodosindirectos;
import funcionmatematica.Funcion;

public class Secante extends CondicionMetodoIndirectos{
    public Secante(double x0, double epsilon, Funcion f){
        super(x0, epsilon, f);
    }

    public double resolver() {
        double x0 = getX0();
        double x1 = x0 + 0.1; // Segundo valor inicial auxiliar
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < 100 && error > getEpsilon()) {
            double fx0 = getF().evaluar(x0);
            double fx1 = getF().evaluar(x1);

            if (Math.abs(fx1 - fx0) < 1e-15) break;

            double x2 = x1 - (fx1 * (x1 - x0)) / (fx1 - fx0);

            error = Math.abs(x2 - x1);
            x0 = x1;
            x1 = x2;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return x1;
    }
}
*/