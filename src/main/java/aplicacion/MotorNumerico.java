package aplicacion;

import metodosdirectos.Biseccion;
import metodosdirectos.CondicionMetodoDirectos;
import metodosdirectos.RegulaFalsi;
import metodosdirectos.RegulaFalsiModificada;
import metodosindirectos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * ADAPTADOR entre la interfaz y TUS clases (metodosdirectos / metodosindirectos).
 * Aquí NO hay algoritmos: solo se crea la clase del método elegido, se llama a resolver()
 * y se leen las iteraciones que esa clase registró (getHistorial()).
 */
public final class MotorNumerico {

    private MotorNumerico() {}

    // ------------------------------------------------------------------ tipos

    public enum Metodo {
        BISECCION("Bisección", true),
        REGULA_FALSI("Regula Falsi", true),
        REGULA_FALSI_MOD("Regula Falsi Modificada", true),
        NEWTON("Newton", false),
        NEWTON_EXT("Newton Extendido", false),
        HALLEY("Halley", false),
        SECANTE("Secante", false),
        PUNTO_FIJO("Punto Fijo", false),
        AITKEN("Aitken", false),
        STEFFENSEN("Steffensen", false);

        private final String nombre;
        private final boolean cerrado;

        Metodo(String nombre, boolean cerrado) {
            this.nombre = nombre;
            this.cerrado = cerrado;
        }

        public boolean esCerrado() { return cerrado; }
        /** Punto Fijo, Aitken y Steffensen iteran sobre g(x). */
        public boolean usaG() { return this == PUNTO_FIJO || this == AITKEN || this == STEFFENSEN; }
        public boolean usaX1() { return this == SECANTE; }
        @Override public String toString() { return nombre; }
    }

    /** Una fila de la tabla de iteraciones. NaN = "no aplica". */
    public static final class Fila {
        public final int n;
        public final double a, b;   // solo métodos cerrados
        public final double x;      // c (cerrados) o x_n (abiertos)
        public final double fx;     // f(x)
        public final double gx;     // g(x) (solo punto fijo / Aitken / Steffensen)
        public final double error;

        Fila(int n, double a, double b, double x, double fx, double gx, double error) {
            this.n = n; this.a = a; this.b = b; this.x = x;
            this.fx = fx; this.gx = gx; this.error = error;
        }
    }

    public static final class Resultado {
        public final List<Fila> filas = new ArrayList<>();
        public double raiz = Double.NaN;
        public boolean convergio = false;
        public boolean fallo = false;
        public String mensaje = "";
        public int iter = 0;

        public int iteraciones() { return iter; }
        public double errorFinal() {
            return filas.isEmpty() ? Double.NaN : filas.get(filas.size() - 1).error;
        }
    }

    private static Resultado fallo(String msg) {
        Resultado r = new Resultado();
        r.fallo = true;
        r.mensaje = msg;
        return r;
    }

    private static boolean malo(double v) { return Double.isNaN(v) || Double.isInfinite(v); }

    // ---------------------------------------------------------------- cerrados

    /** Usa Biseccion, RegulaFalsi o RegulaFalsiModificada (tus clases). */
    public static Resultado cerrado(Metodo m, String textoF, DoubleUnaryOperator f,
                                    double a, double b, double tol, int maxIter) {
        if (!(a < b)) return fallo("El extremo 'a' debe ser menor que 'b'.");

        CondicionMetodoDirectos met;
        if (m == Metodo.BISECCION) met = new Biseccion(a, b, tol, textoF);
        else if (m == Metodo.REGULA_FALSI) met = new RegulaFalsi(a, b, tol, textoF);
        else met = new RegulaFalsiModificada(a, b, tol, textoF);
        met.setMaxIter(maxIter);

        if (!met.evaluarCondicion()) {
            return fallo("f(a) y f(b) tienen el mismo signo: no se cumple Bolzano en [" + a + ", " + b + "].");
        }

        double raiz = met.resolver();

        Resultado r = new Resultado();
        for (double[] h : met.getHistorial()) {
            r.filas.add(new Fila((int) h[0], h[1], h[2], h[3], h[4], Double.NaN, h[5]));
        }
        r.raiz = raiz;
        r.iter = r.filas.size();
        if (malo(raiz)) {
            r.fallo = true;
            r.mensaje = "El método no pudo calcular una raíz (resultado no numérico).";
        } else if (Math.abs(f.applyAsDouble(raiz)) <= tol) {
            r.convergio = true;
            r.mensaje = "Convergió en " + r.iter + " iteraciones.";
        } else {
            r.mensaje = "Se alcanzó el máximo de iteraciones (" + maxIter + ") sin llegar a la tolerancia.";
        }
        return r;
    }

    // ---------------------------------------------------------------- abiertos

    /**
     * Usa Newton, NewtonExt, Halley, Secante, PuntoFijo, Aitken o Steffensen (tus clases).
     * Ojo: PuntoFijo, Aitken y Steffensen reciben el texto de g(x); el resto recibe f(x).
     */
    public static Resultado abierto(Metodo m, String textoF, String textoG,
                                    DoubleUnaryOperator f, DoubleUnaryOperator g,
                                    double x0, double x1, double tol, int maxIter) {
        CondicionMetodoIndirectos met;
        switch (m) {
            case NEWTON:      met = new Newton(x0, tol, textoF); break;
            case NEWTON_EXT:  met = new NewtonExt(x0, tol, textoF); break;
            case HALLEY:      met = new Halley(x0, tol, textoF); break;
            case SECANTE:
                if (x0 == x1) return fallo("Para la secante x0 y x1 deben ser distintos.");
                met = new Secante(x0, x1, tol, textoF);
                break;
            case PUNTO_FIJO:  met = new PuntoFijo(x0, tol, textoG); break;
            case AITKEN:      met = new Aitken(x0, tol, textoG); break;
            case STEFFENSEN:  met = new Steffensen(x0, tol, textoG); break;
            default: return fallo("Método no soportado.");
        }

        double raiz = met.resolver(maxIter);

        Resultado r = new Resultado();
        for (double[] h : met.getHistorial()) {
            double x = h[1];
            double fx = malo(x) ? Double.NaN : f.applyAsDouble(x);
            double gx = (m.usaG() && !malo(x)) ? g.applyAsDouble(x) : Double.NaN;
            r.filas.add(new Fila((int) h[0], Double.NaN, Double.NaN, x, fx, gx, h[2]));
        }
        r.raiz = raiz;
        r.iter = r.filas.isEmpty() ? 0 : r.filas.get(r.filas.size() - 1).n;
        if (m == Metodo.SECANTE) r.iter = Math.max(0, r.iter - 1); // x0 y x1 son datos iniciales

        double errFinal = r.errorFinal();
        if (malo(raiz)) {
            r.fallo = true;
            r.mensaje = "El método falló o divergió (resultado no numérico). Probá con otro punto inicial.";
        } else if (!Double.isNaN(errFinal) && errFinal < tol) {
            r.convergio = true;
            r.mensaje = "Convergió en " + r.iter + " iteraciones.";
        } else if (Math.abs(raiz) > 1e10) {
            r.mensaje = "Los valores crecen sin control: el método diverge con estos datos. Probá con otro x₀ u otra g(x).";
        } else {
            r.mensaje = "Se alcanzó el máximo de iteraciones (" + maxIter + ") sin llegar a la tolerancia.";
        }
        return r;
    }
}
