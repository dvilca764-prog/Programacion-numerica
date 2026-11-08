package aplicacion;

import org.matheclipse.core.eval.ExprEvaluator;

public final class DerivadorSimbolico {

    private DerivadorSimbolico() {}

    public static String derivarPrimera(String funcion) {
        try {
            ExprEvaluator eval = new ExprEvaluator();
            return eval.eval("D(" + funcion + ", x)").toString();
        } catch (Exception e) {
            return "No calculable simbólicamente";
        }
    }

    public static String derivarSegunda(String funcion) {
        try {
            ExprEvaluator eval = new ExprEvaluator();
            return eval.eval("D(D(" + funcion + ", x), x)").toString();
        } catch (Exception e) {
            return "No calculable simbólicamente";
        }
    }
}
