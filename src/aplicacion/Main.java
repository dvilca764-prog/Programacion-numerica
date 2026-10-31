package aplicacion;
import funcionmatematica.Funcion;
import metodosindirectos.*;
import metodosdirectos.*;
import org.mariuszgromada.math.mxparser.License;

public class Main {
    public static void main(String[] args) {
        License.iConfirmNonCommercialUse("Tilin");

        //Metodos directos
        double epsilon = 0.00000001;
        double a = -10;
        double b = 0;

        String funcion = "x^2-1.5";
        System.out.println("Funcion matematica: "+funcion);

        Biseccion biseccion = new Biseccion(a,b,epsilon,funcion);
        double resultado = biseccion.resolver();

        RegulaFalsi regula = new RegulaFalsi(a,b,epsilon,funcion);
        resultado = regula.resolver();

        RegulaFalsiModificada regulaMod = new RegulaFalsiModificada(a,b,epsilon,funcion);
        resultado = regulaMod.resolver();

        //Metodos indirectos
        double x0 = 0.5; // Valor inicial
        epsilon = 0.00000001; // Tolerancia

        funcion = "x^2-1.5";
        System.out.println("Funcion matematica: "+funcion);

    }
}
