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
            if (!Double.isFinite(fxk)) return Double.NaN;
            if (Math.abs(fxk) <= getEpsilon()) {
                registrar(iteracion + 2, xk, 0.0);
                return xk;
            }
            double fxkMenos1 = evaluar(xkMenos1);

            // Protección contra división por cero
            if (!Double.isFinite(fxkMenos1) || !Double.isFinite(fxk - fxkMenos1) || Math.abs(fxk - fxkMenos1) < 1e-15) {
                System.out.println("Error: División por cero en el método de la Secante.");
                return Double.NaN;
            }

            // Fórmula del método de la Secante
            double xkSiguiente = xk - (fxk * (xk - xkMenos1)) / (fxk - fxkMenos1);

            if (!Double.isFinite(xkSiguiente)) return Double.NaN;
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
