package metodosindirectos;

import org.mariuszgromada.math.mxparser.*;
import java.util.ArrayList;
import java.util.List;

public abstract class CondicionMetodoIndirectos {
    protected double x0;
    protected double epsilon;
    protected String f; // Recibe la función como texto plano

    // --- Registro de iteraciones (lo usa la interfaz para la tabla y la gráfica) ---
    // Cada fila: {n, x, error}
    protected final List<double[]> historial = new ArrayList<>();

    public List<double[]> getHistorial() { return historial; }

    protected void registrar(int n, double x, double error) {
        historial.add(new double[]{n, x, error});
    }

    public CondicionMetodoIndirectos(double x0, double epsilon, String f) {
        this.x0 = x0;
        this.epsilon = epsilon;
        this.f = f;
    }

    // Método auxiliar para evaluar usando mxparser (Argument, Expression y calculate)
    protected double evaluar(double valorX) {
        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);
        x.setArgumentValue(valorX);
        return funcion.calculate();
    }

    public double getX0() {
        return x0;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public abstract double resolver(int maxIteraciones);
}