package metodosdirectos;
import funcionmatematica.Funcion;

public abstract class CondicionMetodoDirectos {
    protected double a;
    protected double b;
    protected double epsilon;
    protected Funcion f;

    public CondicionMetodoDirectos(double a,double b,double epsilon,Funcion f){
        this.a=a;
        this.b=b;
        this.epsilon=epsilon;
        this.f=f;
    }
    public boolean evaluarCondicion() {
        if (f.evaluar(a) * f.evaluar(b) > 0) {
            System.out.println("Error: f(a) y f(b) tienen el mismo signo. No se garantiza raíz.");
            return false;
        }
        return true;
    }
    public abstract double resolver();
}
