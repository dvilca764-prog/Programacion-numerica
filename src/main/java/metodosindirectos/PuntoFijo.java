package metodosindirectos;

public class PuntoFijo extends CondicionMetodoIndirectos {

    public PuntoFijo(double x0, double epsilon, String f) {
        super(x0, epsilon, f);
    }

    @Override
    public double resolver(int maxIteraciones) {
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while (iteracion < maxIteraciones && error > getEpsilon()) {
            // Evaluamos la expresión de texto directamente con mxparser mediante la clase padre
            double xn = evaluar(xs);

            error = Math.abs(xn - xs);
            xs = xn;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        System.out.println("Punto Fijo - Raíz aproximada: " + xs);
        return xs;
    }
}



/*package metodosindirectos;
import funcionmatematica.Funcion;

public class PuntoFijo extends CondicionMetodoIndirectos{
    public PuntoFijo(double x0, double epsilon, String f){
        super(x0, epsilon, f);
    }

    /*public boolean CondicionPuntoFijo(){
        Argument x = new Argument("x");
        Expression funcion = new Expression(f, x);
        x.setArgumentValue(c);
        return true;
    }*/

   /* public double resolver(){
        double xs = getX0();
        int iteracion = 0;
        double error = Double.MAX_VALUE;

        while(iteracion < 100 && error > getEpsilon()){
            double xn = getF().evaluar(xs);
            error = Math.abs(xn - xs);
            xs = xn;
            iteracion++;
        }

        System.out.println("Iteraciones: " + iteracion);
        return xs;
    }

}
*/