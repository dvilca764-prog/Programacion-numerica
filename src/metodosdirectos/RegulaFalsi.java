package metodosdirectos;

import funcionmatematica.Funcion;

public class RegulaFalsi extends CondicionMetodoDirectos{
    public RegulaFalsi(double a, double b, double epsilon,Funcion f){
        super(a,b,epsilon,f);
    }
    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }

        double aActual = this.a;
        double bActual = this.b;

        double c = (aActual * f.evaluar(bActual) - bActual * f.evaluar(aActual)) / (f.evaluar(bActual) - f.evaluar(aActual));

        while (Math.abs(f.evaluar(c)) > epsilon) {
            if (f.evaluar(aActual) * f.evaluar(c) < 0) {
                bActual = c;
            } else {
                aActual = c;
            }

            c = (aActual * f.evaluar(bActual) - bActual * f.evaluar(aActual)) / (f.evaluar(bActual) - f.evaluar(aActual));
        }

        System.out.println("Regula Falsi - Raíz encontrada c: " + c);
        return c;
    }
}
