package aplicacion;
import funcionmatematica.Funcion;
import metodosindirectos.*;
import metodosdirectos.*;

public class Main {
    public static void main(String[] args) {
        // Aquí se pueden crear instancias de clases y probar los métodos
        Funcion f = new Funcion() {
            @Override
            public double evaluar(double x) {
                return Math.log(x) - 1 - 1/x; // Ejemplo: f(x) = ln(x) - 1 - 1/x
            }

            @Override
            public Funcion derivada() {
                return new Funcion() {
                    @Override
                    public double evaluar(double x) {
                        return 1/x + 1/Math.pow(x, 2); // Derivada: f'(x) = 1/x + 1/x^2
                    }

                    @Override
                    public Funcion derivada() {
                        return new Funcion() {
                            @Override
                            public double evaluar(double x) {
                                return 2/Math.pow(x, 3); // Segunda derivada: f''(x) = 2/x^3
                            }

                            @Override
                            public Funcion derivada() {
                                return null; // No hay tercera derivada en este caso
                            }
                        };
                    }
                };
            }
        };
    double x0 = 0.5; // Valor inicial
    double epsilon = 0.0001; // Tolerancia
    System.out.println("Método de Biseccion:");
    String f = "2*x+1";
    Biseccion biseccion = new Biseccion(1.0,2.0,0.001,f);
    double resultado = biseccion.resolver();
    System.out.println("Resultado: " + resultado);
    }
}
