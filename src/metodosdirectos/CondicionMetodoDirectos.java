package metodosdirectos;
import org.mariuszgromada.math.mxparser.*;

public abstract class CondicionMetodoDirectos {
    protected double a;
    protected double b;
    protected double epsilon;
    protected String f;

    public CondicionMetodoDirectos(double a,double b,double epsilon,String f){
        this.a=a;
        this.b=b;
        this.epsilon=epsilon;
        this.f=f;
    }
    public boolean evaluarCondicion() {
        Expression fa = new Expression(f + " | x = " + a);
        Expression fb = new Expression(f + " | x = " + b);
        double A = fa.calculate();
        double B = fb.calculate();
        if (A * B > 0) {
            System.out.println("Error: f(a) y f(b) tienen el mismo signo. No se garantiza raíz.");
            return false;
        }
        return true;
    }
    public abstract double resolver();
}
