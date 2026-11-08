package metodosindirectos;

import org.mariuszgromada.math.mxparser.*;

public class Newton extends CondicionMetodoIndirectos {

    // Comprobación local del criterio de Fourier en el punto inicial.
    private double valorFInicial = Double.NaN;
    private double valorPrimeraDerivada = Double.NaN;
    private double valorSegundaDerivada = Double.NaN;
    private boolean fourierCumplido;

    public double getValorFInicial() { return valorFInicial; }
    public double getValorPrimeraDerivada() { return valorPrimeraDerivada; }
    public double getValorSegundaDerivada() { return valorSegundaDerivada; }
    public boolean isFourierCumplido() { return fourierCumplido; }

    public Newton(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xs = getX0();
        // Se evalúa Fourier antes de iniciar las iteraciones de Newton.
        valorFInicial = evaluar(xs);
        Argument xInicial = new Argument("x");
        xInicial.setArgumentValue(xs);
        valorPrimeraDerivada = new Expression("der(" + f + ",x)", xInicial).calculate();
        valorSegundaDerivada = new Expression("der(der(" + f + ",x),x)", xInicial).calculate();
        fourierCumplido = Double.isFinite(valorFInicial * valorSegundaDerivada)
                && valorFInicial * valorSegundaDerivada > 0
                && Double.isFinite(valorPrimeraDerivada) && Math.abs(valorPrimeraDerivada) > 1e-14;

        int iteracion = 0;
        double error = Double.MAX_VALUE;
        registrar(0, xs, Double.NaN);

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            double xAnterior = xs;

            // 1. Evaluamos f(x) directamente usando el método 'evaluar' heredado
            double fx = evaluar(xs);

            // 2. Creamos el argumento x para calcular la derivada con mxparser
            Argument x = new Argument("x");
            x.setArgumentValue(xs);

            // 3. Evaluamos la primera derivada f'(x) de forma automática usando der()
            Expression exprDF = new Expression("der(" + f + ", x)", x);
            double dfx = exprDF.calculate();

            // Protección contra división por cero (derivada nula)
            if (Math.abs(dfx) < 1e-15) {
                System.out.println("Error: Derivada nula en el método de Newton.");
                return Double.NaN;
            }

            // Fórmula de Newton-Raphson: x_{k+1} = x_k - [f(x_k) / f'(x_k)]
            xs = xs - (fx / dfx);

            error = Math.abs(xs - xAnterior);
            iteracion++;
            registrar(iteracion, xs, error);
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Newton - Raíz aproximada: " + xs);
        return xs;
    }
}

/*package metodosindirectos;
import funcionmatematica.Funcion;

public class Newton extends CondicionMetodoIndirectos{
    private Funcion df;
    public Newton(double x0, double epsilon, Funcion f, Funcion df){
        super(x0, epsilon, f);
        this.df = df;
    }

    public double resolver(){
        double xs = getX0();
        // Se evalúa Fourier antes de iniciar las iteraciones de Newton.
        valorFInicial = evaluar(xs);
        Argument xInicial = new Argument("x");
        xInicial.setArgumentValue(xs);
        valorPrimeraDerivada = new Expression("der(" + f + ",x)", xInicial).calculate();
        valorSegundaDerivada = new Expression("der(der(" + f + ",x),x)", xInicial).calculate();
        fourierCumplido = Double.isFinite(valorFInicial * valorSegundaDerivada)
                && valorFInicial * valorSegundaDerivada > 0
                && Double.isFinite(valorPrimeraDerivada) && Math.abs(valorPrimeraDerivada) > 1e-14;

        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while(iteracion < 100 && error > getEpsilon()){
            double xant = xs;
            xs = xs - (getF().evaluar(xs) / df.evaluar(xs));
            error = Math.abs(xs - xant);
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }
    
}
*/