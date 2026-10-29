package MetodosDirectos;
import FuncionMatematica.Funcion;

public class RegulaFalsiModificada extends CondicionMetodoDirectos{
    public RegulaFalsiModificada(double a, double b, double epsilon, Funcion f) {
        super(a, b, epsilon, f);
    }

    public double resolver() {
        if (!evaluarCondicion()) {
            return Double.NaN;
        }
        double aActual = this.a;
        double bActual = this.b;

        double F = f.evaluar(aActual);
        double G = f.evaluar(bActual);
        double w = f.evaluar(aActual);

        double c = (aActual * f.evaluar(bActual) - bActual * f.evaluar(aActual)) / (f.evaluar(bActual) - f.evaluar(aActual));

        while (Math.abs(f.evaluar(c)) > epsilon) {
            if (f.evaluar(aActual) * f.evaluar(c) < 0) {
                bActual = c;
                G = f.evaluar(c);
                if (w * G > 0) {
                    F = F / 2.0;
                }
            } else {
                aActual = c;
                F = f.evaluar(c);
                if (w * F > 0) {
                    G = G / 2.0;
                }
            }

            w = f.evaluar(c);
            c = (aActual * G - bActual * F) / (G - F);
        }

        System.out.println("Regula Falsi Modificada - Raíz encontrada c: " + c);
        return c;
    }
}
