package aplicacion;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.mariuszgromada.math.mxparser.*;

// Importa tus paquetes de métodos
import metodosdirectos.*;
import metodosindirectos.*;

public class VentanaNumericaApp extends Application {

    // Componentes de la interfaz
    private TextField txtFuncion, txtA, txtB, txtEpsilon, txtMaxIter;
    private ComboBox<String> cmbTipoMetodo, cmbAlgoritmo;
    private TableView<FilaIteracion> tablaIteraciones;
    private LineChart<Number, Number> graficoFuncion;
    private Label lblEstadoResultado;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Resolución de Métodos Numéricos Avanzados");

        // Panel Principal
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 1. PANEL DE CONFIGURACIÓN (IZQUIERDA)
        VBox panelConfig = crearPanelConfiguracion();
        root.setLeft(panelConfig);

        // 2. PANEL CENTRAL (GRÁFICA Y TABLA)
        TabPane tabPane = new TabPane();

        Tab tabGrafica = new Tab("Gráfica de la Función", crearPanelGrafica());
        tabGrafica.setClosable(false);

        Tab tabTabla = new Tab("Tabla de Iteraciones", crearPanelTabla());
        tabTabla.setClosable(false);

        tabPane.getTabs().addAll(tabGrafica, tabTabla);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1100, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox crearPanelConfiguracion() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setPrefWidth(320);

        vbox.getChildren().add(new Label("--- PARÁMETROS DE ENTRADA ---"));

        vbox.getChildren().add(new Label("Función f(x):"));
        txtFuncion = new TextField("x^2 - 1.5");
        vbox.getChildren().add(txtFuncion);

        vbox.getChildren().add(new Label("Tipo de Método:"));
        cmbTipoMetodo = new ComboBox<>(FXCollections.observableArrayList("Métodos Cerrados", "Métodos Abiertos"));
        cmbTipoMetodo.setValue("Métodos Cerrados");
        vbox.getChildren().add(cmbTipoMetodo);

        vbox.getChildren().add(new Label("Algoritmo Específico:"));
        cmbAlgoritmo = new ComboBox<>();
        actualizarListaAlgoritmos("Métodos Cerrados");
        vbox.getChildren().add(cmbAlgoritmo);

        cmbTipoMetodo.setOnAction(e -> actualizarListaAlgoritmos(cmbTipoMetodo.getValue()));

        // Campos de intervalos y puntos
        vbox.getChildren().add(new Label("Extremo 'a' o Punto Inicial 'x0':"));
        txtA = new TextField("1.0");
        vbox.getChildren().add(txtA);

        vbox.getChildren().add(new Label("Extremo 'b' (Solo cerrados):"));
        txtB = new TextField("2.0");
        vbox.getChildren().add(txtB);

        vbox.getChildren().add(new Label("Tolerancia (Epsilon):"));
        txtEpsilon = new TextField("0.000001");
        vbox.getChildren().add(txtEpsilon);

        vbox.getChildren().add(new Label("Máx. Iteraciones:"));
        txtMaxIter = new TextField("100");
        vbox.getChildren().add(txtMaxIter);

        Button btnEjecutar = new Button("Resolver y Graficar");
        btnEjecutar.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEjecutar.setMaxWidth(Double.MAX_VALUE);
        btnEjecutar.setOnAction(e -> ejecutarYVisualizar());
        vbox.getChildren().add(btnEjecutar);

        lblEstadoResultado = new Label("Estado: Listo");
        lblEstadoResultado.setWrapText(true);
        vbox.getChildren().add(lblEstadoResultado);

        return vbox;
    }

    private void actualizarListaAlgoritmos(String tipo) {
        if (tipo.equals("Métodos Cerrados")) {
            cmbAlgoritmo.setItems(FXCollections.observableArrayList("Bisección", "Regula Falsi", "Regula Falsi Modificada"));
        } else {
            cmbAlgoritmo.setItems(FXCollections.observableArrayList("Newton", "Newton Extendido", "Halley", "Punto Fijo", "Secante"));
        }
        cmbAlgoritmo.getSelectionModel().selectFirst();
    }

    private VBox crearPanelGrafica() {
        VBox vbox = new VBox();
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("f(x)");

        graficoFuncion = new LineChart<>(xAxis, yAxis);
        graficoFuncion.setTitle("Comportamiento de la Función");
        graficoFuncion.setCreateSymbols(false);
        vbox.getChildren().add(graficoFuncion);
        VBox.setVgrow(graficoFuncion, Priority.ALWAYS);
        return vbox;
    }

    @SuppressWarnings("unchecked")
    private VBox crearPanelTabla() {
        VBox vbox = new VBox();
        tablaIteraciones = new TableView<>();

        TableColumn<FilaIteracion, Integer> colIter = new TableColumn<>("Iteración");
        colIter.setCellValueFactory(new PropertyValueFactory<>("iteracion"));

        TableColumn<FilaIteracion, Double> colX = new TableColumn<>("Valor x / c");
        colX.setCellValueFactory(new PropertyValueFactory<>("valorX"));

        TableColumn<FilaIteracion, Double> colFx = new TableColumn<>("f(x)");
        colFx.setCellValueFactory(new PropertyValueFactory<>("valorFx"));

        TableColumn<FilaIteracion, Double> colError = new TableColumn<>("Error Absoluto");
        colError.setCellValueFactory(new PropertyValueFactory<>("error"));

        tablaIteraciones.getColumns().addAll(colIter, colX, colFx, colError);
        vbox.getChildren().add(tablaIteraciones);
        VBox.setVgrow(tablaIteraciones, Priority.ALWAYS);
        return vbox;
    }

    private void ejecutarYVisualizar() {
        try {
            String funcionStr = txtFuncion.getText();
            double paramA = Double.parseDouble(txtA.getText());
            double paramB = txtB.getText().isEmpty() ? 0 : Double.parseDouble(txtB.getText());
            double epsilon = Double.parseDouble(txtEpsilon.getText());
            int maxIter = Integer.parseInt(txtMaxIter.getText());
            String algoritmo = cmbAlgoritmo.getValue();

            double raizResultado = Double.NaN;
            ObservableList<FilaIteracion> historial = FXCollections.observableArrayList();

            // Configuración de mXparser con Argument y Expression
            Argument xArg = new Argument("x");
            Expression expr = new Expression(funcionStr, xArg);

            if (cmbTipoMetodo.getValue().equals("Métodos Cerrados")) {
                if (algoritmo.equals("Bisección")) {
                    Biseccion met = new Biseccion(paramA, paramB, epsilon, funcionStr);
                    if (!met.evaluarCondicion()) {
                        lblEstadoResultado.setText("Error: f(a) y f(b) tienen el mismo signo. No se cumple Bolzano.");
                        return;
                    }

                    double a = paramA, b = paramB, c = a;
                    int it = 0;
                    while ((b - a) / 2.0 > epsilon && it < maxIter) {
                        c = (a + b) / 2.0;
                        xArg.setArgumentValue(c);
                        double fx = expr.calculate();
                        double errorActual = (b - a) / 2.0;

                        historial.add(new FilaIteracion(it + 1, c, fx, errorActual));

                        xArg.setArgumentValue(a);
                        double fa = expr.calculate();
                        xArg.setArgumentValue(c);
                        double fc = expr.calculate();

                        if (fa * fc < 0) {
                            b = c;
                        } else {
                            a = c;
                        }
                        it++;
                    }
                    raizResultado = c;
                } else if (algoritmo.equals("Regula Falsi")) {
                    RegulaFalsi met = new RegulaFalsi(paramA, paramB, epsilon, funcionStr);
                    raizResultado = met.resolver();
                } else if (algoritmo.equals("Regula Falsi Modificada")) {
                    RegulaFalsiModificada met = new RegulaFalsiModificada(paramA, paramB, epsilon, funcionStr);
                    raizResultado = met.resolver();
                }
            } else {
                if (algoritmo.equals("Newton")) {
                    Newton met = new Newton(paramA, epsilon, funcionStr);
                    raizResultado = met.resolver(maxIter);
                } else if (algoritmo.equals("Halley")) {
                    Halley met = new Halley(paramA, epsilon, funcionStr);
                    raizResultado = met.resolver(maxIter);
                } else if (algoritmo.equals("Punto Fijo")) {
                    PuntoFijo met = new PuntoFijo(paramA, epsilon, funcionStr);
                    raizResultado = met.resolver(maxIter);
                }
            }

            lblEstadoResultado.setText("Raíz aproximada encontrada: " + raizResultado);
            tablaIteraciones.setItems(historial);

            // Trazar la gráfica dinámicamente
            trazarGrafica(funcionStr, paramA - 2, paramB + 2);

        } catch (Exception ex) {
            lblEstadoResultado.setText("Error en los datos de entrada: " + ex.getMessage());
        }
    }

    private void trazarGrafica(String funcionStr, double minX, double maxX) {
        graficoFuncion.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("f(x) = " + funcionStr);

        Argument xArg = new Argument("x");
        Expression expr = new Expression(funcionStr, xArg);

        double paso = (maxX - minX) / 100.0;
        for (double valX = minX; valX <= maxX; valX += paso) {
            xArg.setArgumentValue(valX);
            double valY = expr.calculate();
            series.getData().add(new XYChart.Data<>(valX, valY));
        }

        graficoFuncion.getData().add(series);
    }

    // Clase auxiliar para la tabla de iteraciones
    public static class FilaIteracion {
        private int iteracion;
        private double valorX;
        private double valorFx;
        private double error;

        public FilaIteracion(int iteracion, double valorX, double valorFx, double error) {
            this.iteracion = iteracion;
            this.valorX = valorX;
            this.valorFx = valorFx;
            this.error = error;
        }

        public int getIteracion() { return iteracion; }
        public double getValorX() { return valorX; }
        public double getValorFx() { return valorFx; }
        public double getError() { return error; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}