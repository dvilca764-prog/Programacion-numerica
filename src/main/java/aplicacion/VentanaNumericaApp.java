package aplicacion;

import aplicacion.MotorNumerico.Fila;
import aplicacion.MotorNumerico.Metodo;
import aplicacion.MotorNumerico.Resultado;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public class VentanaNumericaApp extends Application {

    // ------------------------------------------------------------- controles
    private TextField txtFuncion, txtG, txtA, txtB, txtX0, txtX1, txtEpsilon, txtMaxIter, txtXmin, txtXmax;
    private ComboBox<Metodo> cmbAlgoritmo;
    private ComboBox<Ejemplo> cmbEjemplo;
    private CheckBox chkRangoAuto, chkVerF, chkVerG, chkVerIter;
    private VBox boxG, boxCerrado, boxAbierto, boxX1, boxRangoManual;
    private Label lblEstado, lblBadge;
    private Label valRaiz, valFRaiz, valIter, valError;
    private TableView<Fila> tabla;
    private LineChart<Number, Number> grafico;
    private HBox leyenda;
    private Tab tabTabla;

    // Vista interactiva: una unidad tiene el mismo tamaño en X e Y.
    private boolean vistaInicializada = false;
    private double centroX, centroY, unidadesPorPixel;
    private double arrastreX, arrastreY;
    private boolean arrastrando = false;
    private Region zonaTrazado;
    private double ultimoAnchoTrazado = -1, ultimoAltoTrazado = -1;


    // ---------------------------------------------- estado de la última corrida
    private DoubleUnaryOperator ultimaF, ultimaG;
    private Metodo ultimoMetodo;
    private Resultado ultimoResultado;
    private double ultimoA, ultimoB, ultimoX0, ultimoX1;
    private String ultimoTextoF = "";

    private static final String[] COLOR = {
            "#80C1DF", "#EDBA7A", "#AFC3D2", "#688394", "#DDA1BC", "#B6A2E2", "#74C8B4"};

    // ---------------------------------------------------------------- ejemplos
    private static final class Ejemplo {
        final String nombre, f, g, a, b, x0, x1;
        Ejemplo(String nombre, String f, String g, String a, String b, String x0, String x1) {
            this.nombre = nombre; this.f = f; this.g = g; this.a = a; this.b = b; this.x0 = x0; this.x1 = x1;
        }
        @Override public String toString() { return nombre; }
    }

    private static final List<Ejemplo> EJEMPLOS = List.of(
            new Ejemplo("x² − 1.5  (raíz de 1.5)", "x^2 - 1.5", "(x + 1.5/x)/2", "1", "2", "1", "2"),
            new Ejemplo("cos(x) − x", "cos(x) - x", "cos(x)", "0", "1", "0.5", "1"),
            new Ejemplo("x³ − x − 2", "x^3 - x - 2", "(x + 2)^(1/3)", "1", "2", "1.5", "2"),
            new Ejemplo("eˣ − 3x", "exp(x) - 3*x", "exp(x)/3", "0", "1", "0", "1"),
            new Ejemplo("x·sen(x) − 1", "x*sin(x) - 1", "1/sin(x)", "0.5", "2", "1", "1.5")
    );

    // ================================================================== START
    @Override
    public void start(Stage stage) {
        stage.setTitle("Métodos Numéricos · Búsqueda de raíces");

        BorderPane root = new BorderPane();
        root.setTop(crearCabecera());
        root.setLeft(crearPanelIzquierdo());
        root.setCenter(crearPanelDerecho());

        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(cargarCss());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();

        cargarEjemplo(EJEMPLOS.get(0));
        actualizarVisibilidad();
        resolver();
    }

    // ============================================================== CABECERA
    private Node crearCabecera() {
        Label titulo = new Label("Métodos Numéricos");
        titulo.getStyleClass().add("titulo");
        Label sub = new Label("Búsqueda de raíces · cerrados y abiertos");
        sub.getStyleClass().add("subtitulo");
        VBox textos = new VBox(2, titulo, sub);

        lblBadge = new Label("Listo");
        lblBadge.getStyleClass().add("badge");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox h = new HBox(textos, espacio, lblBadge);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("header");
        return h;
    }

    // ======================================================== PANEL IZQUIERDO
    private Node crearPanelIzquierdo() {
        // ---- Función
        txtFuncion = campo("ej: x^2 - 1.5");
        txtG = campo("ej: (x + 1.5/x)/2");
        boxG = campoConEtiqueta("g(x)  ·  función de iteración", txtG);

        cmbEjemplo = new ComboBox<>(FXCollections.observableArrayList(EJEMPLOS));
        cmbEjemplo.setPromptText("Cargar un ejemplo…");
        cmbEjemplo.setMaxWidth(Double.MAX_VALUE);
        cmbEjemplo.setOnAction(e -> { if (cmbEjemplo.getValue() != null) cargarEjemplo(cmbEjemplo.getValue()); });

        VBox cardFuncion = card("FUNCIÓN",
                campoConEtiqueta("f(x)", txtFuncion), boxG,
                campoConEtiqueta("Ejemplos", cmbEjemplo));

        // ---- Método (todos en una sola lista)
        cmbAlgoritmo = new ComboBox<>(FXCollections.observableArrayList(Metodo.values()));
        cmbAlgoritmo.setMaxWidth(Double.MAX_VALUE);
        cmbAlgoritmo.setConverter(new javafx.util.StringConverter<Metodo>() {
            @Override public String toString(Metodo m) {
                return m == null ? "" : m + (m.esCerrado() ? "   ·  cerrado" : "   ·  abierto");
            }
            @Override public Metodo fromString(String s) { return null; }
        });
        cmbAlgoritmo.setOnAction(e -> actualizarVisibilidad());
        cmbAlgoritmo.getSelectionModel().selectFirst();

        VBox cardMetodo = card("MÉTODO", campoConEtiqueta("Algoritmo", cmbAlgoritmo));

        // ---- Parámetros
        txtA = campo("a");
        txtB = campo("b");
        txtX0 = campo("x₀");
        txtX1 = campo("x₁");
        txtEpsilon = campo("1e-6");
        txtMaxIter = campo("100");

        HBox filaAB = new HBox(10, campoConEtiqueta("Extremo a", txtA), campoConEtiqueta("Extremo b", txtB));
        filaAB.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        boxCerrado = new VBox(6, etiquetaSeccion("Intervalo [a, b]"), filaAB);

        boxX1 = campoConEtiqueta("Segundo punto x₁", txtX1);
        boxAbierto = new VBox(8, campoConEtiqueta("Punto inicial x₀", txtX0), boxX1);

        HBox filaTol = new HBox(10, campoConEtiqueta("Tolerancia ε", txtEpsilon),
                campoConEtiqueta("Máx. iteraciones", txtMaxIter));
        filaTol.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox cardParams = card("PARÁMETROS", boxCerrado, boxAbierto, filaTol);

        // ---- Gráfica
        chkRangoAuto = new CheckBox("Rango automático");
        chkRangoAuto.setSelected(true);
        txtXmin = campo("x mín");
        txtXmax = campo("x máx");
        HBox filaRango = new HBox(10, campoConEtiqueta("x mínimo", txtXmin), campoConEtiqueta("x máximo", txtXmax));
        filaRango.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        boxRangoManual = new VBox(filaRango);
        mostrar(boxRangoManual, false);
        chkRangoAuto.setOnAction(e -> {
            mostrar(boxRangoManual, !chkRangoAuto.isSelected());
            if (ultimaF != null) dibujar();
        });
        VBox cardGrafica = card("GRÁFICA", chkRangoAuto, boxRangoManual);

        // ---- Botón
        Button btnResolver = new Button("Resolver y graficar");
        btnResolver.getStyleClass().add("boton-primario");
        btnResolver.setMaxWidth(Double.MAX_VALUE);
        btnResolver.setDefaultButton(true);
        btnResolver.setOnAction(e -> resolver());

        for (TextField t : List.of(txtFuncion, txtG, txtA, txtB, txtX0, txtX1, txtEpsilon, txtMaxIter, txtXmin, txtXmax)) {
            t.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) resolver(); });
        }

        VBox contenido = new VBox(12, cardFuncion, cardMetodo, cardParams, cardGrafica, btnResolver);
        contenido.setPadding(new Insets(16, 12, 16, 16));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(360);
        scroll.setMinWidth(340);
        scroll.getStyleClass().add("panel-izq");

        actualizarVisibilidad();
        return scroll;
    }

    private void actualizarVisibilidad() {
        Metodo m = cmbAlgoritmo.getValue();
        boolean cerrado = m == null || m.esCerrado();
        mostrar(boxCerrado, cerrado);
        mostrar(boxAbierto, !cerrado);
        mostrar(boxX1, m != null && m.usaX1());
        mostrar(boxG, m != null && m.usaG());
        if (chkVerG != null) {
            chkVerG.setDisable(m == null || !m.usaG());
        }
    }

    private void cargarEjemplo(Ejemplo e) {
        txtFuncion.setText(e.f);
        txtG.setText(e.g);
        txtA.setText(e.a);
        txtB.setText(e.b);
        txtX0.setText(e.x0);
        txtX1.setText(e.x1);
        txtEpsilon.setText("0.000001");
        txtMaxIter.setText("100");
    }

    // ========================================================= PANEL DERECHO
    private Node crearPanelDerecho() {
        // tarjetas de resultado
        valRaiz = new Label("—");
        valFRaiz = new Label("—");
        valIter = new Label("—");
        valError = new Label("—");
        HBox stats = new HBox(12,
                stat("RAÍZ APROXIMADA", valRaiz, true),
                stat("f(raíz)", valFRaiz, false),
                stat("ITERACIONES", valIter, false),
                stat("ERROR FINAL", valError, false));
        stats.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        lblEstado = new Label("Ingresá los datos y presioná «Resolver y graficar».");
        lblEstado.setWrapText(true);
        lblEstado.setMaxWidth(Double.MAX_VALUE);
        lblEstado.getStyleClass().addAll("estado", "estado-info");

        TabPane tabs = new TabPane();
        Tab tabGrafica = new Tab("📈  Gráfica", crearPanelGrafica());
        tabGrafica.setClosable(false);
        tabTabla = new Tab("📋  Tabla de iteraciones", crearPanelTabla());
        tabTabla.setClosable(false);
        tabs.getTabs().addAll(tabGrafica, tabTabla);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox v = new VBox(12, stats, lblEstado, tabs);
        v.setPadding(new Insets(16, 16, 16, 4));
        return v;
    }

    private VBox stat(String titulo, Label valor, boolean destacado) {
        Label t = new Label(titulo);
        t.getStyleClass().add("stat-titulo");
        valor.getStyleClass().addAll("stat-valor", "mono");
        if (destacado) valor.getStyleClass().add("stat-destacado");
        VBox b = new VBox(4, t, valor);
        b.getStyleClass().add("stat");
        return b;
    }

    // ---------------------------------------------------------------- gráfica
    private Node crearPanelGrafica() {
        chkVerF = new CheckBox("f(x)");
        chkVerG = new CheckBox("g(x) y y = x");
        chkVerIter = new CheckBox("Iteraciones");
        chkVerF.setSelected(true);
        chkVerG.setSelected(true);
        chkVerIter.setSelected(true);
        for (CheckBox c : List.of(chkVerF, chkVerG, chkVerIter)) c.setOnAction(e -> { if (ultimaF != null) dibujar(); });

        leyenda = new HBox(16);
        leyenda.setAlignment(Pos.CENTER_RIGHT);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(16, chkVerF, chkVerG, chkVerIter, espacio, leyenda);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(4, 4, 0, 4));

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("x");
        yAxis.setLabel("y");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setMinorTickVisible(false);

        grafico = new LineChart<>(xAxis, yAxis);
        grafico.setAnimated(false);
        grafico.setLegendVisible(false);
        grafico.setCreateSymbols(true);
        grafico.setTitle("Comportamiento de la función");
        VBox.setVgrow(grafico, Priority.ALWAYS);
        instalarNavegacionGrafico();

        VBox v = new VBox(6, barra, grafico);
        v.getStyleClass().add("panel-grafica");
        v.setPadding(new Insets(10));
        return v;
    }

    // ------------------------------------------------------------------ tabla
    private Node crearPanelTabla() {
        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(32);
        tabla.setPlaceholder(new Label("Todavía no hay iteraciones. Resolvé una ecuación primero."));

        Button btnCopiar = new Button("Copiar tabla");
        btnCopiar.getStyleClass().add("boton-secundario");
        btnCopiar.setOnAction(e -> copiarTabla());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label info = new Label("Cada fila es una iteración del método elegido.");
        info.getStyleClass().add("subtitulo");
        HBox barra = new HBox(10, info, espacio, btnCopiar);
        barra.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox v = new VBox(8, barra, tabla);
        v.setPadding(new Insets(10));
        return v;
    }

    private void configurarColumnas(Metodo m) {
        tabla.getColumns().clear();
        tabla.getColumns().add(columna("n", f -> String.valueOf(f.n), "centro"));
        if (m.esCerrado()) {
            tabla.getColumns().add(columna("a", f -> num(f.a), null));
            tabla.getColumns().add(columna("b", f -> num(f.b), null));
            tabla.getColumns().add(columna("c", f -> num(f.x), null));
            tabla.getColumns().add(columna("f(c)", f -> cient(f.fx), null));
        } else {
            tabla.getColumns().add(columna("xₙ", f -> num(f.x), null));
            tabla.getColumns().add(columna("f(xₙ)", f -> cient(f.fx), null));
            if (m.usaG()) tabla.getColumns().add(columna("g(xₙ)", f -> num(f.gx), null));
        }
        tabla.getColumns().add(columna("Error", f -> cient(f.error), null));
    }

    private TableColumn<Fila, String> columna(String titulo, Function<Fila, String> fn, String estilo) {
        TableColumn<Fila, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        if (estilo != null) c.getStyleClass().add(estilo);
        c.setSortable(false);
        return c;
    }

    private void copiarTabla() {
        if (tabla.getItems().isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (TableColumn<Fila, ?> c : tabla.getColumns()) sb.append(c.getText()).append('\t');
        sb.append('\n');
        for (Fila f : tabla.getItems()) {
            for (TableColumn<Fila, ?> c : tabla.getColumns()) {
                Object o = c.getCellObservableValue(f).getValue();
                sb.append(o).append('\t');
            }
            sb.append('\n');
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
        setBadge("Tabla copiada", "ok");
    }

    // =============================================================== RESOLVER
    private void resolver() {
        try {
            Metodo m = cmbAlgoritmo.getValue();
            if (m == null) throw new IllegalArgumentException("Elegí un algoritmo.");

            double tol = numero(txtEpsilon, "la tolerancia");
            if (tol <= 0) throw new IllegalArgumentException("La tolerancia debe ser mayor que 0.");
            int maxIter = (int) numero(txtMaxIter, "las iteraciones máximas");
            if (maxIter < 1 || maxIter > 100000) throw new IllegalArgumentException("Las iteraciones máximas deben estar entre 1 y 100000.");

            String textoF = txtFuncion.getText().trim();
            DoubleUnaryOperator f = funcion(textoF, "f(x)");
            DoubleUnaryOperator g = m.usaG() ? funcion(txtG.getText().trim(), "g(x)") : null;

            Resultado r;
            double a = Double.NaN, b = Double.NaN, x0 = Double.NaN, x1 = Double.NaN;
            if (m.esCerrado()) {
                a = numero(txtA, "el extremo a");
                b = numero(txtB, "el extremo b");
                r = MotorNumerico.cerrado(m, textoF, f, a, b, tol, maxIter);
            } else {
                x0 = numero(txtX0, "el punto inicial x₀");
                if (m.usaX1()) x1 = numero(txtX1, "el punto x₁");
                r = MotorNumerico.abierto(m, textoF, txtG.getText().trim(), f, g, x0, x1, tol, maxIter);
            }

            ultimaF = f;
            ultimaG = g;
            ultimoMetodo = m;
            ultimoResultado = r;
            ultimoTextoF = textoF;
            ultimoA = a; ultimoB = b; ultimoX0 = x0; ultimoX1 = x1;

            mostrarResultado(m, r);
            vistaInicializada = false; // Nuevo problema: centrar de nuevo la vista.
            dibujar();

        } catch (IllegalArgumentException ex) {
            setEstado("err", "⚠  " + ex.getMessage());
            setBadge("Error en los datos", "err");
        }
    }

    private void mostrarResultado(Metodo m, Resultado r) {
        configurarColumnas(m);
        tabla.setItems(FXCollections.observableArrayList(r.filas));

        if (r.fallo) {
            valRaiz.setText(Double.isNaN(r.raiz) ? "—" : num(r.raiz));
            valFRaiz.setText("—");
            valIter.setText(r.filas.isEmpty() ? "—" : String.valueOf(r.iteraciones()));
            valError.setText("—");
            setEstado("err", "✖  " + r.mensaje);
            setBadge("Sin solución", "err");
            return;
        }
        valRaiz.setText(num(r.raiz));
        valFRaiz.setText(cient(ultimaF.applyAsDouble(r.raiz)));
        valIter.setText(String.valueOf(r.iteraciones()));
        valError.setText(cient(r.errorFinal()));
        if (r.convergio) {
            setEstado("ok", "✔  " + r.mensaje + "  ·  " + m);
            setBadge("Convergió", "ok");
        } else {
            setEstado("warn", "⚠  " + r.mensaje + "  Probá con otro punto inicial o más iteraciones.");
            setBadge("No convergió", "warn");
        }
    }

    // ================================================================ GRÁFICA
    private double[] rangoX() {
        if (!chkRangoAuto.isSelected()) {
            try {
                double lo = numero(txtXmin, "x mínimo");
                double hi = numero(txtXmax, "x máximo");
                if (lo < hi) return new double[]{lo, hi};
            } catch (IllegalArgumentException ignorada) { /* usa automático */ }
        }
        List<Double> v = new ArrayList<>();
        if (ultimoMetodo.esCerrado()) { v.add(ultimoA); v.add(ultimoB); }
        else { v.add(ultimoX0); if (ultimoMetodo.usaX1()) v.add(ultimoX1); }
        if (ultimoResultado != null) {
            for (Fila f : ultimoResultado.filas) v.add(f.x);
            v.add(ultimoResultado.raiz);
        }
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (double d : v) {
            if (Double.isNaN(d) || Double.isInfinite(d) || Math.abs(d) > 1e4) continue;
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
        }
        if (lo > hi) { lo = -5; hi = 5; }
        double pad = Math.max(0.5, 0.25 * (hi - lo));
        return new double[]{lo - pad, hi + pad};
    }

    private void dibujar() {
        grafico.getData().clear();
        leyenda.getChildren().clear();
        if (ultimaF == null) return;

        double[] rx = rangoX();
        double ux = pasoLindo((rx[1] - rx[0]) / 10.0);
        double xmin = Math.floor(rx[0] / ux) * ux, xmax = Math.ceil(rx[1] / ux) * ux;
        boolean fp = ultimoMetodo.usaG();
        boolean verF = chkVerF.isSelected();
        boolean verG = fp && chkVerG.isSelected();
        boolean verIt = chkVerIter.isSelected();
        if (!verF && !verG) verF = true;

        final int N = 400;
        double[] xs = new double[N + 1], fy = new double[N + 1], gy = new double[N + 1];
        for (int i = 0; i <= N; i++) {
            xs[i] = xmin + (xmax - xmin) * i / N;
            fy[i] = ultimaF.applyAsDouble(xs[i]);
            gy[i] = fp ? ultimaG.applyAsDouble(xs[i]) : Double.NaN;
        }

        // rango vertical robusto (ignora asíntotas y valores absurdos)
        List<Double> ys = new ArrayList<>();
        for (int i = 0; i <= N; i++) {
            if (verF && ok(fy[i])) ys.add(fy[i]);
            if (verG && ok(gy[i])) ys.add(gy[i]);
        }
        if (verG) { ys.add(xmin); ys.add(xmax); }
        double lo, hi;
        if (ys.isEmpty()) { lo = -1; hi = 1; }
        else {
            Collections.sort(ys);
            lo = ys.get((int) (0.02 * (ys.size() - 1)));
            hi = ys.get((int) (0.98 * (ys.size() - 1)));
        }
        lo = Math.min(lo, 0);
        hi = Math.max(hi, 0);
        double span = hi - lo;
        if (span < 1e-9) span = 1;
        lo -= 0.08 * span;
        hi += 0.08 * span;
        double uy = pasoLindo((hi - lo) / 8.0);
        lo = Math.floor(lo / uy) * uy;
        hi = Math.ceil(hi / uy) * uy;
        // La vista conserva una proporción 1:1 en unidades/píxel.
        // El rango vertical automático anterior se usa solo para elegir el centro inicial.
        if (!vistaInicializada) {
            centroX = (ultimoResultado != null
                    && !ultimoResultado.fallo
                    && Double.isFinite(ultimoResultado.raiz))
                    ? ultimoResultado.raiz
                    : (xmin + xmax) / 2.0;

            centroY = 0;
            double ancho = anchoTrazado();
            unidadesPorPixel = Math.max(10.0, xmax - xmin) / ancho;
            vistaInicializada = true;
        }
        double ancho = anchoTrazado();
        double alto = altoTrazado();
        xmin = centroX - ancho * unidadesPorPixel / 2.0;
        xmax = centroX + ancho * unidadesPorPixel / 2.0;
        lo = centroY - alto * unidadesPorPixel / 2.0;
        hi = centroY + alto * unidadesPorPixel / 2.0;
        ux = pasoLindo((xmax - xmin) / 10.0);
        uy = pasoLindo((hi - lo) / 8.0);
        double tope = 5 * (hi - lo);
        // Volver a muestrear la función en el rango visible tras mover o hacer zoom.
        for (int i = 0; i <= N; i++) {
            xs[i] = xmin + (xmax - xmin) * i / N;
            fy[i] = ultimaF.applyAsDouble(xs[i]);
            gy[i] = fp ? ultimaG.applyAsDouble(xs[i]) : Double.NaN;
        }

        XYChart.Series<Number, Number> sF = new XYChart.Series<>();
        XYChart.Series<Number, Number> sG = new XYChart.Series<>();
        XYChart.Series<Number, Number> sId = new XYChart.Series<>();
        XYChart.Series<Number, Number> sCero = new XYChart.Series<>();
        XYChart.Series<Number, Number> sCob = new XYChart.Series<>();
        XYChart.Series<Number, Number> sIt = new XYChart.Series<>();
        XYChart.Series<Number, Number> sRaiz = new XYChart.Series<>();

        for (int i = 0; i <= N; i++) {
            if (verF && ok(fy[i])) sF.getData().add(new XYChart.Data<>(xs[i], clamp(fy[i], lo - tope, hi + tope)));
            if (verG && ok(gy[i])) sG.getData().add(new XYChart.Data<>(xs[i], clamp(gy[i], lo - tope, hi + tope)));
        }
        if (verG) {
            sId.getData().add(new XYChart.Data<>(xmin, xmin));
            sId.getData().add(new XYChart.Data<>(xmax, xmax));
        }
        sCero.getData().add(new XYChart.Data<>(xmin, 0));
        sCero.getData().add(new XYChart.Data<>(xmax, 0));

        Resultado r = ultimoResultado;
        if (verIt && r != null && !r.filas.isEmpty()) {
            if (ultimoMetodo == Metodo.PUNTO_FIJO) {
                // telaraña (cobweb)
                int lim = Math.min(r.filas.size(), 60);
                sCob.getData().add(new XYChart.Data<>(r.filas.get(0).x, 0));
                for (int i = 0; i < lim; i++) {
                    double xi = r.filas.get(i).x, gi = r.filas.get(i).gx;
                    if (!ok(xi) || !ok(gi) || Math.abs(gi) > 1e6) break;
                    sCob.getData().add(new XYChart.Data<>(xi, gi));
                    sCob.getData().add(new XYChart.Data<>(gi, gi));
                }
            }
            for (Fila f : r.filas) {
                double y = fp ? f.gx : f.fx;
                if (!ok(f.x) || !ok(y)) continue;
                XYChart.Data<Number, Number> d = new XYChart.Data<>(f.x, y);
                final String tip = (ultimoMetodo.esCerrado() ? "c" : "x") + "₍" + f.n + "₎ = " + num(f.x)
                        + "\n" + (fp ? "g" : "f") + " = " + num(y);
                d.nodeProperty().addListener((o, viejo, nodo) -> { if (nodo != null) Tooltip.install(nodo, new Tooltip(tip)); });
                sIt.getData().add(d);
            }
        }
        if (r != null && !r.fallo && ok(r.raiz)) {
            XYChart.Data<Number, Number> d = new XYChart.Data<>(r.raiz, 0);
            final String tip = "Raíz ≈ " + num(r.raiz);
            d.nodeProperty().addListener((o, viejo, nodo) -> { if (nodo != null) Tooltip.install(nodo, new Tooltip(tip)); });
            sRaiz.getData().add(d);
        }

        // eje X / Y
        NumberAxis ax = (NumberAxis) grafico.getXAxis();
        NumberAxis ay = (NumberAxis) grafico.getYAxis();
        ax.setLowerBound(xmin);
        ax.setUpperBound(xmax);
        ax.setTickUnit(ux);
        ay.setLowerBound(lo);
        ay.setUpperBound(hi);
        ay.setTickUnit(uy);

        grafico.setTitle("f(x) = " + ultimoTextoF);
        // IMPORTANTE: el orden fija el color (default-color0..6 en el CSS)
        grafico.getData().addAll(List.of(sF, sG, sId, sCero, sCob, sIt, sRaiz));

        // leyenda
        if (verF) leyenda.getChildren().add(itemLeyenda(COLOR[0], "f(x)", false));
        if (verG) {
            leyenda.getChildren().add(itemLeyenda(COLOR[1], "g(x)", false));
            leyenda.getChildren().add(itemLeyenda(COLOR[2], "y = x", true));
        }
        if (!sCob.getData().isEmpty()) leyenda.getChildren().add(itemLeyenda(COLOR[4], "telaraña", false));
        if (!sIt.getData().isEmpty()) leyenda.getChildren().add(itemLeyenda(COLOR[5], "iteraciones", false));
        if (!sRaiz.getData().isEmpty()) leyenda.getChildren().add(itemLeyenda(COLOR[6], "raíz", false));
    }

    // ================================================= NAVEGACIÓN DEL GRÁFICO
    private Region obtenerZonaTrazado() {
        if (zonaTrazado == null && grafico != null) {
            Node nodo = grafico.lookup(".chart-plot-background");
            if (nodo instanceof Region) zonaTrazado = (Region) nodo;
        }
        return zonaTrazado;
    }

    private double anchoTrazado() {
        Region z = obtenerZonaTrazado();
        return z != null && z.getWidth() > 1 ? z.getWidth() : 600;
    }

    private double altoTrazado() {
        Region z = obtenerZonaTrazado();
        return z != null && z.getHeight() > 1 ? z.getHeight() : 380;
    }

    private boolean dentroZona(double escenaX, double escenaY) {
        Region z = obtenerZonaTrazado();
        if (z == null) return false;
        Point2D p = z.sceneToLocal(escenaX, escenaY);
        return p.getX() >= 0 && p.getX() <= z.getWidth()
                && p.getY() >= 0 && p.getY() <= z.getHeight();
    }

    private void instalarNavegacionGrafico() {
        grafico.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY || !vistaInicializada
                    || !dentroZona(e.getSceneX(), e.getSceneY())) return;
            arrastrando = true;
            arrastreX = e.getSceneX();
            arrastreY = e.getSceneY();
            e.consume();
        });

        grafico.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (!arrastrando) return;
            centroX -= (e.getSceneX() - arrastreX) * unidadesPorPixel;
            centroY += (e.getSceneY() - arrastreY) * unidadesPorPixel;
            arrastreX = e.getSceneX();
            arrastreY = e.getSceneY();
            dibujar();
            e.consume();
        });

        grafico.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (arrastrando) {
                arrastrando = false;
                e.consume();
            }
        });

        grafico.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (!vistaInicializada || e.getDeltaY() == 0
                    || !dentroZona(e.getSceneX(), e.getSceneY())) return;
            Region z = obtenerZonaTrazado();
            Point2D p = z.sceneToLocal(e.getSceneX(), e.getSceneY());
            double puntoX = centroX + (p.getX() - z.getWidth() / 2) * unidadesPorPixel;
            double puntoY = centroY - (p.getY() - z.getHeight() / 2) * unidadesPorPixel;
            double factor = e.getDeltaY() > 0 ? 0.85 : 1.0 / 0.85;
            double nuevaEscala = unidadesPorPixel * factor;
            if (nuevaEscala < 1e-10 || nuevaEscala > 1e8) return;
            centroX = puntoX + (centroX - puntoX) * factor;
            centroY = puntoY + (centroY - puntoY) * factor;
            unidadesPorPixel = nuevaEscala;
            dibujar();
            e.consume();
        });

        // Cuando se maximiza o cambia el tamaño de la ventana, conservar la escala 1:1.
        grafico.widthProperty().addListener((obs, antes, ahora) -> reajustarVistaPorTamano());
        grafico.heightProperty().addListener((obs, antes, ahora) -> reajustarVistaPorTamano());
    }

    private boolean reajustePendiente = false;
    private void reajustarVistaPorTamano() {
        if (reajustePendiente) return;
        reajustePendiente = true;
        Platform.runLater(() -> {
            reajustePendiente = false;
            if (!vistaInicializada || ultimaF == null) return;
            double ancho = anchoTrazado(), alto = altoTrazado();
            if (Math.abs(ancho - ultimoAnchoTrazado) > 3
                    || Math.abs(alto - ultimoAltoTrazado) > 3) {
                ultimoAnchoTrazado = ancho;
                ultimoAltoTrazado = alto;
                dibujar();
            }
        });
    }

    private Node itemLeyenda(String color, String texto, boolean punteado) {
        Region marca = new Region();
        marca.setMinSize(18, 3);
        marca.setPrefSize(18, 3);
        marca.setMaxSize(18, 3);
        marca.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;"
                + (punteado ? "-fx-opacity: 0.6;" : ""));
        Label l = new Label(texto);
        l.getStyleClass().add("leyenda-texto");
        HBox h = new HBox(6, marca, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // =============================================================== UTILIDADES
    /** Paso "redondo" (1, 2, 5 × 10^k) para las marcas de los ejes. */
    private static double pasoLindo(double bruto) {
        if (!(bruto > 0) || Double.isInfinite(bruto)) return 1;
        double pot = Math.pow(10, Math.floor(Math.log10(bruto)));
        double f = bruto / pot;
        double m = f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10;
        return m * pot;
    }

    private static boolean ok(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String num(double v) {
        if (Double.isNaN(v)) return "—";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        return String.format(Locale.US, "%.8f", v);
    }

    private static String cient(double v) {
        if (Double.isNaN(v)) return "—";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (v == 0) return "0";
        return String.format(Locale.US, "%.3e", v);
    }

    private double numero(TextField t, String que) {
        String s = t.getText() == null ? "" : t.getText().trim().replace(',', '.');
        if (s.isEmpty()) throw new IllegalArgumentException("Falta completar " + que + ".");
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de " + que + " no es un número válido: «" + s + "».");
        }
    }

    private DoubleUnaryOperator funcion(String texto, String nombre) {
        if (texto.isEmpty()) throw new IllegalArgumentException("Escribí la función " + nombre + ".");
        Argument x = new Argument("x", 0);
        Expression e = new Expression(texto, x);
        if (!e.checkSyntax()) {
            throw new IllegalArgumentException("Hay un error de sintaxis en " + nombre + ". Ejemplo válido: x^2 - 1.5, sin(x), exp(x).");
        }
        return v -> {
            x.setArgumentValue(v);
            return e.calculate();
        };
    }

    private void setEstado(String tipo, String texto) {
        lblEstado.setText(texto);
        lblEstado.getStyleClass().setAll("label", "estado", "estado-" + tipo);
    }

    private void setBadge(String texto, String tipo) {
        lblBadge.setText(texto);
        lblBadge.getStyleClass().setAll("label", "badge", "badge-" + tipo);
    }

    private static void mostrar(Node n, boolean v) { n.setVisible(v); n.setManaged(v); }

    private static TextField campo(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.getStyleClass().add("mono");
        return t;
    }

    private static Label etiquetaSeccion(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("campo-label");
        return l;
    }

    private static VBox campoConEtiqueta(String etiqueta, Control control) {
        VBox v = new VBox(5, etiquetaSeccion(etiqueta), control);
        control.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private static VBox card(String titulo, Node... hijos) {
        Label t = new Label(titulo);
        t.getStyleClass().add("card-titulo");
        VBox v = new VBox(10);
        v.getChildren().add(t);
        v.getChildren().addAll(hijos);
        v.getStyleClass().add("card");
        return v;
    }

    private String cargarCss() {
        try {
            Path tmp = Files.createTempFile("metodos-numericos-", ".css");
            Files.writeString(tmp, CSS, StandardCharsets.UTF_8);
            tmp.toFile().deleteOnExit();
            return tmp.toUri().toString();
        } catch (IOException e) {
            // plan B: data URI
            return "data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ===================================================================== CSS
    private static final String CSS = """
        .root {
            -fx-font-family: "Segoe UI", "Inter", "Helvetica Neue", sans-serif;
            -fx-font-size: 13px;
            -fx-background-color: #1B2632;
        }
        .label { -fx-text-fill: #D7E2EB; }
        .mono { -fx-font-family: "Consolas", "JetBrains Mono", "Menlo", monospace; }

        /* Paleta gris pizarra + azul acero: superficies sobrias, acentos accesibles. */
        /* ---------- cabecera ---------- */
        .header {
            -fx-background-color: linear-gradient(to right, #2A4052, #1B2632);
            -fx-border-color: #354A5B; -fx-border-width: 0 0 1 0;
            -fx-padding: 14 22 14 22;
        }
        .titulo { -fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: white; }
        .subtitulo { -fx-text-fill: #A2B7C7; -fx-font-size: 12px; }
        .badge {
            -fx-background-color: #354A5B; -fx-background-radius: 20;
            -fx-padding: 5 14 5 14; -fx-font-weight: bold; -fx-text-fill: #AFC3D2;
        }
        .badge-ok   { -fx-background-color: #26483F; -fx-text-fill: #74C8B4; }
        .badge-warn { -fx-background-color: #514130; -fx-text-fill: #EDBA7A; }
        .badge-err  { -fx-background-color: #51343D; -fx-text-fill: #F1A0A0; }

        /* ---------- panel izquierdo ---------- */
        .panel-izq, .panel-izq > .viewport { -fx-background-color: transparent; -fx-background: transparent; }
        .scroll-pane { -fx-background-color: transparent; -fx-padding: 0; }
        .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: #415A6B; -fx-background-radius: 6; }
        .scroll-bar .track, .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; }
        .corner { -fx-background-color: transparent; }

        .card {
            -fx-background-color: #273746; -fx-background-radius: 14;
            -fx-border-color: #3A5263; -fx-border-radius: 14; -fx-padding: 14;
        }
        .card-titulo { -fx-text-fill: #80C1DF; -fx-font-size: 11px; -fx-font-weight: bold; }
        .campo-label { -fx-text-fill: #AEC2D0; -fx-font-size: 12px; }

        .text-field {
            -fx-background-color: #1B2632; -fx-text-fill: #F0F5F8; -fx-prompt-text-fill: #688394;
            -fx-background-radius: 9; -fx-border-color: #415A6B; -fx-border-radius: 9;
            -fx-padding: 8 11 8 11; -fx-highlight-fill: #4F87B4;
        }
        .text-field:focused { -fx-border-color: #80C1DF; }

        .combo-box {
            -fx-background-color: #1B2632; -fx-background-radius: 9;
            -fx-border-color: #415A6B; -fx-border-radius: 9;
        }
        .combo-box:focused { -fx-border-color: #80C1DF; }
        .combo-box .list-cell { -fx-text-fill: #F0F5F8; -fx-background-color: transparent; -fx-padding: 7 11 7 11; }
        .combo-box .arrow-button { -fx-background-color: transparent; }
        .combo-box .arrow { -fx-background-color: #AFC3D2; }
        .combo-box-popup .list-view { -fx-background-color: #273746; -fx-border-color: #415A6B; -fx-background-radius: 9; -fx-border-radius: 9; }
        .combo-box-popup .list-cell { -fx-background-color: #273746; -fx-text-fill: #E2ECF3; -fx-padding: 8 12 8 12; }
        .combo-box-popup .list-cell:hover { -fx-background-color: #354C60; }
        .combo-box-popup .list-cell:filled:selected { -fx-background-color: #42627C; }

        .seg {
            -fx-background-color: #1B2632; -fx-text-fill: #AEC2D0; -fx-font-weight: bold;
            -fx-border-color: #415A6B; -fx-padding: 8 10 8 10; -fx-cursor: hand;
        }
        .seg-izq { -fx-background-radius: 9 0 0 9; -fx-border-radius: 9 0 0 9; }
        .seg-der { -fx-background-radius: 0 9 9 0; -fx-border-radius: 0 9 9 0; -fx-border-width: 1 1 1 0; }
        .seg:selected { -fx-background-color: linear-gradient(to bottom, #80C1DF, #4F87B4); -fx-text-fill: white; -fx-border-color: transparent; }

        .check-box { -fx-text-fill: #D7E2EB; }
        .check-box .box { -fx-background-color: #1B2632; -fx-border-color: #516879; -fx-border-radius: 5; -fx-background-radius: 5; }
        .check-box:selected .box { -fx-background-color: #80C1DF; -fx-border-color: #80C1DF; }
        .check-box:selected .mark { -fx-background-color: #1B2632; }
        .check-box:disabled { -fx-opacity: 0.4; }

        .boton-primario {
            -fx-background-color: linear-gradient(to bottom right, #74C8B4, #5D9DBD);
            -fx-text-fill: #122B37; -fx-font-weight: bold; -fx-font-size: 14px;
            -fx-background-radius: 11; -fx-padding: 12 16 12 16; -fx-cursor: hand;
        }
        .boton-primario:hover { -fx-effect: dropshadow(gaussian, rgba(128,193,223,0.34), 16, 0.2, 0, 0); }
        .boton-primario:pressed { -fx-opacity: 0.85; }
        .boton-secundario {
            -fx-background-color: #354D61; -fx-text-fill: #D7E2EB; -fx-background-radius: 8;
            -fx-padding: 6 14 6 14; -fx-cursor: hand;
        }
        .boton-secundario:hover { -fx-background-color: #42627C; }

        /* ---------- resultados ---------- */
        .stat { -fx-background-color: #273746; -fx-background-radius: 12; -fx-border-color: #3A5263; -fx-border-radius: 12; -fx-padding: 12 16 12 16; }
        .stat-titulo { -fx-text-fill: #A2B7C7; -fx-font-size: 10.5px; -fx-font-weight: bold; }
        .stat-valor { -fx-text-fill: #F0F5F8; -fx-font-size: 18px; -fx-font-weight: bold; }
        .stat-destacado { -fx-text-fill: #74C8B4; }

        .estado { -fx-background-radius: 10; -fx-padding: 10 14 10 14; -fx-border-radius: 10; -fx-font-size: 13px; }
        .estado-info { -fx-background-color: #273746; -fx-text-fill: #AFC3D2; -fx-border-color: #3A5263; }
        .estado-ok   { -fx-background-color: #26483F; -fx-text-fill: #74C8B4; -fx-border-color: #397B69; }
        .estado-warn { -fx-background-color: #514130; -fx-text-fill: #EDBA7A; -fx-border-color: #84613B; }
        .estado-err  { -fx-background-color: #51343D; -fx-text-fill: #F1A0A0; -fx-border-color: #85434C; }

        /* ---------- pestañas ---------- */
        .tab-pane > .tab-header-area > .tab-header-background { -fx-background-color: transparent; }
        .tab-pane > .tab-header-area { -fx-padding: 0 0 0 4; }
        .tab { -fx-background-color: #233241; -fx-background-radius: 10 10 0 0; -fx-padding: 6 14 6 14; }
        .tab:selected { -fx-background-color: #273746; }
        .tab .tab-label { -fx-text-fill: #A2B7C7; -fx-font-weight: bold; }
        .tab:selected .tab-label { -fx-text-fill: white; }
        .tab-pane > .tab-content-area { -fx-background-color: #273746; -fx-background-radius: 0 12 12 12; -fx-border-color: #3A5263; -fx-border-radius: 0 12 12 12; }
        .tab-pane:top > .tab-header-area > .headers-region > .tab { -fx-focus-color: transparent; -fx-faint-focus-color: transparent; }
        .leyenda-texto { -fx-text-fill: #AFC3D2; -fx-font-size: 12px; }

        /* ---------- tabla ---------- */
        .table-view {
            -fx-background-color: #202F3C; -fx-control-inner-background: #202F3C;
            -fx-control-inner-background-alt: #2A3B49; -fx-table-cell-border-color: transparent;
            -fx-background-radius: 10; -fx-border-color: #3A5263; -fx-border-radius: 10;
            -fx-padding: 0;
        }
        .table-view .column-header-background { -fx-background-color: #354D61; -fx-background-radius: 10 10 0 0; }
        .table-view .column-header, .table-view .filler {
            -fx-background-color: #354D61; -fx-size: 38; -fx-border-color: transparent;
        }
        .table-view .column-header .label { -fx-text-fill: #E2ECF3; -fx-font-weight: bold; -fx-alignment: CENTER; }
        .table-row-cell { -fx-background-color: -fx-control-inner-background; -fx-cell-size: 32; -fx-border-color: transparent; }
        .table-row-cell:odd { -fx-background-color: -fx-control-inner-background-alt; }
        .table-row-cell:selected { -fx-background-color: #42627C; }
        .table-cell { -fx-text-fill: #E2ECF3; -fx-alignment: CENTER-RIGHT; -fx-font-family: "Consolas", "JetBrains Mono", monospace; -fx-padding: 0 14 0 8; }
        .table-cell.centro { -fx-alignment: CENTER; -fx-text-fill: #80C1DF; -fx-font-weight: bold; }
        .table-view .placeholder .label { -fx-text-fill: #91A6B6; }

        /* ---------- gráfica ---------- */
        .chart { -fx-background-color: transparent; }
        .chart-title { -fx-text-fill: #E2ECF3; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: "Consolas", monospace; }
        .chart-content { -fx-padding: 6; }
        .chart-plot-background { -fx-background-color: #1B2632; }
        .chart-vertical-grid-lines, .chart-horizontal-grid-lines { -fx-stroke: #354959; -fx-stroke-width: 1; }
        .chart-alternative-row-fill, .chart-alternative-column-fill { -fx-fill: transparent; -fx-stroke: transparent; }
        .axis { -fx-tick-label-fill: #AEC2D0; -fx-font-size: 11px; }
        .axis-label { -fx-text-fill: #AEC2D0; }
        .axis-tick-mark, .axis-minor-tick-mark { -fx-stroke: #516879; }
        .axis:top, .axis:bottom, .axis:left, .axis:right { -fx-border-color: #516879; }

        .chart-series-line { -fx-stroke-width: 2.4px; }
        .chart-line-symbol { -fx-background-color: transparent; -fx-padding: 0; }

        .default-color0.chart-series-line { -fx-stroke: #80C1DF; -fx-stroke-width: 2.8px; }
        .default-color1.chart-series-line { -fx-stroke: #EDBA7A; -fx-stroke-width: 2.8px; }
        .default-color2.chart-series-line { -fx-stroke: #AFC3D2; -fx-stroke-width: 1.6px; -fx-stroke-dash-array: 7 6; }
        .default-color3.chart-series-line { -fx-stroke: #688394; -fx-stroke-width: 1.4px; }
        .default-color4.chart-series-line { -fx-stroke: #DDA1BC; -fx-stroke-width: 1.3px; }
        .default-color5.chart-series-line { -fx-stroke: transparent; }
        .default-color6.chart-series-line { -fx-stroke: transparent; }

        .default-color5.chart-line-symbol {
            -fx-background-color: #B6A2E2, #1B2632; -fx-background-insets: 0, 2;
            -fx-background-radius: 8px; -fx-padding: 5px;
        }
        .default-color6.chart-line-symbol {
            -fx-background-color: #74C8B4, #1B2632, #74C8B4; -fx-background-insets: 0, 2, 4;
            -fx-background-radius: 10px; -fx-padding: 7px;
            -fx-effect: dropshadow(gaussian, rgba(116,200,180,0.50), 12, 0.3, 0, 0);
        }
        .tooltip { -fx-background-color: #354D61; -fx-text-fill: white; -fx-font-family: "Consolas", monospace; -fx-background-radius: 8; }
        """;

    // ================================================================== MAIN
    public static void main(String[] args) {
        launch(args);
    }
}
