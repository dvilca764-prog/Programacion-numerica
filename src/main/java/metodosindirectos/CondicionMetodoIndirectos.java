package metodosindirectos;

import org.mariuszgromada.math.mxparser.*;

public abstract class CondicionMetodoIndirectos {
    protected double x0;
    protected double epsilon;
    protected String f; // Recibe la función como texto plano

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



/*package metodosindirectos;
import funcionmatematica.Funcion;

public abstract class CondicionMetodoIndirectos{
    private double x0;
    private double epsilon;
    private String f;

    public CondicionMetodoIndirectos(double x0,double epsilon,String f){
        this.x0=x0;
        this.epsilon=epsilon;
        this.f=f;
    }

    public double getX0() {
        return x0;
    }

    public void setX0(double x0) {
        this.x0 = x0;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        if(epsilon <= 0){
            throw new IllegalArgumentException("Epsilon debe ser mayor que cero.");
        }
        this.epsilon = epsilon;
    }

    public String getF() {
        return f;
    }

    public void setF(String f) {
        this.f = f;
    }
}*/