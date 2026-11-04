package metodosdirectos;
import org.mariuszgromada.math.mxparser.*;
import java.util.ArrayList;
import java.util.List;

public abstract class CondicionMetodoDirectos {
    protected double a;
    protected double b;
    protected double epsilon;
    protected String f;

    // --- Registro de iteraciones (lo usa la interfaz para la tabla y la gráfica) ---
    // Cada fila: {n, a, b, c, f(c), error}
    protected final List<double[]> historial = new ArrayList<>();
    // Tope de seguridad para que el bucle no sea infinito
    protected int maxIter = 1000;

    public List<double[]> getHistorial() { return historial; }
    public void setMaxIter(int maxIter) { this.maxIter = maxIter; }

    protected void registrar(int n, double a, double b, double c, double fc, double error) {
        historial.add(new double[]{n, a, b, c, fc, error});
    }

    public CondicionMetodoDirectos(double a,double b,double epsilon,String f){
        this.a=a;
        this.b=b;
        this.epsilon=epsilon;
        this.f=f;
    }
    public boolean evaluarCondicion() {
        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);

        x.setArgumentValue(a);
        double A = funcion.calculate();

        x.setArgumentValue(b);
        double B = funcion.calculate();

        /*System.out.println("f(a) = " + A);
        System.out.println("f(b) = " + B);*/

        if (A * B > 0) {
            System.out.println("Error: f(a) y f(b) tienen el mismo signo. No se garantiza raíz.");
            return false;
        }
        return true;
    }
    public abstract double resolver();
}
