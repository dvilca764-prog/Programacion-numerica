package aplicacion;
import org.mariuszgromada.math.mxparser.*;

public class Pruebamx {

    public static void main(String[] args) {
        License.iConfirmNonCommercialUse("TU NOMBRE");

        // Función que queremos evaluar
        String funcion = "x^3 + 2*x^2 + 5*x + 1";

        // Definir el valor de x
        Argument x = new Argument("x = 2");

        // Crear la expresión original
        Expression f = new Expression(funcion, x);

        // Crear la expresión de la derivada
        Expression derivada = new Expression(
                "der(" + funcion + ", x)", x
        );

        // Calcular
        double resultado = f.calculate();
        double resultadoDerivada = derivada.calculate();

        // Mostrar resultados
        System.out.println("f(x) = " + funcion);
        System.out.println("f(2) = " + resultado);
        System.out.println("f'(2) = " + resultadoDerivada);
    }
}