package metodosindirectos;
import funcionmatematica.Funcion;

public abstract class CondicionMetodoIndirectos{
    private double x0;
    private double epsilon;
    private Funcion f;

    public CondicionMetodoIndirectos(double x0,double epsilon,Funcion f){
        this.setX0(x0);
        this.setEpsilon(epsilon);
        this.setF(f);
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

    public Funcion getF() {
        return f;
    }

    public void setF(Funcion f) {
        this.f = f;
    }
}