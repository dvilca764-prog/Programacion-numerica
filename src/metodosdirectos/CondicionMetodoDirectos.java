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
        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);

        x.setArgumentValue(a);
        double A = funcion.calculate();

        x.setArgumentValue(b);
        double B = funcion.calculate();

        System.out.println("f(a) = " + A);
        System.out.println("f(b) = " + B);

        if (A * B > 0) {
            System.out.println("Error: f(a) y f(b) tienen el mismo signo. No se garantiza raíz.");
            return false;
        }
        return true;
    }
    public abstract double resolver();
}
