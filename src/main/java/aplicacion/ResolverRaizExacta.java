package aplicacion;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

/** Identifica raíces racionales sencillas y las verifica en la función original. */
public final class ResolverRaizExacta {
    private ResolverRaizExacta() {}

    public static String buscar(String funcion, double aproximacion) {
        if (!Double.isFinite(aproximacion) || funcion == null || funcion.isBlank()) {
            return "No identificada";
        }
        Argument x = new Argument("x", 0);
        Expression expresion = new Expression(funcion, x);
        if (!expresion.checkSyntax()) return "No identificada";

        // Búsqueda acotada de raíces racionales p/q de denominador pequeño.
        for (int q = 1; q <= 100; q++) {
            long centro = Math.round(aproximacion * q);
            for (long p = centro - 1; p <= centro + 1; p++) {
                double candidato = (double) p / q;
                if (Math.abs(candidato - aproximacion) > 1e-5) continue;
                x.setArgumentValue(candidato);
                double valor = expresion.calculate();
                if (Double.isFinite(valor) && Math.abs(valor) <= 1e-12) {
                    long mcd = mcd(Math.abs(p), q);
                    long numerador = p / mcd;
                    long denominador = q / mcd;
                    return denominador == 1 ? Long.toString(numerador)
                            : numerador + "/" + denominador;
                }
            }
        }
        return "No identificada (mostrar aproximación)";
    }

    private static long mcd(long a, long b) {
        while (b != 0) { long resto = a % b; a = b; b = resto; }
        return Math.max(1, a);
    }
}
