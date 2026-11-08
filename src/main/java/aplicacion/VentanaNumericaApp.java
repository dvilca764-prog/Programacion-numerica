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
import java.math.BigDecimal;
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
    private Label lblPuntoGrafica;
    private Tab tabTabla;
    private VBox panelDesarrollo;
    private ComboBox<Metodo> cmbCompararA, cmbCompararB;
    private VBox panelComparacion;
    private TextField cmpA, cmpB, cmpX0, cmpX1, cmpG;
    private Label valRaizExacta;

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
            new Ejemplo("Polinomio Cúbico (Raíces Múltiples): (x-1)³", "(x-1)^3", "(x-1)^3 + x", "0", "2", "0.2", "0.5"),
            new Ejemplo("Función Altamente Oscilatoria: eˣ - cos(x) - 1", "exp(x) - cos(x) - 1", "log(cos(x) + 1)", "0", "1.5", "0.5", "1.0"),
            new Ejemplo("Pendiente extrema: arctan(x) - 0.1", "atan(x) - 0.1", "tan(0.1)", "0", "1", "0.1", "0.5"),
            new Ejemplo("x³ − x − 2", "x^3 - x - 2", "(x + 2)^(1/3)", "1", "2", "1.5", "2"),
            new Ejemplo("eˣ − 3x", "exp(x) - 3*x", "exp(x)/3", "0", "1", "0", "1")
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
        // Ancho mínimo suficiente para que la tabla entre completa, sin recortar columnas.
        stage.setMinWidth(1180);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();

        // cargarEjemplo(EJEMPLOS.get(0)); sin valor preescrito
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
                campoConEtiqueta("f(x)", txtFuncion), crearTecladoFuncion(txtFuncion), boxG, crearTecladoFuncion(txtG),
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

    /** Teclado matemático: inserta en la posición del cursor sin bloquear el teclado físico. */
    private Node crearTecladoFuncion(TextField destino) {
        String[][] teclas = {{"sin(","cos(","tan(","asin(","acos(","atan("},
                {"ln(","log(","exp(","sqrt(","abs(","pi"},
                {"x","(",")","^","/","*","-","+"},
                {"7","8","9","4","5","6","1","2","3","0",".",","}};
        VBox teclado = new VBox(5);
        for (String[] fila : teclas) {
            HBox botones = new HBox(5);
            for (String simbolo : fila) {
                Button b = new Button(simbolo);
                b.getStyleClass().add("tecla-matematica");
                b.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(b, Priority.ALWAYS);
                b.setOnAction(ev -> {
                    int inicio = destino.getSelection().getStart(), fin = destino.getSelection().getEnd();
                    String texto = destino.getText();
                    destino.replaceText(inicio, fin, simbolo);
                    destino.requestFocus(); destino.positionCaret(inicio + simbolo.length());
                });
                botones.getChildren().add(b);
            }
            teclado.getChildren().add(botones);
        }
        Button borrar = new Button("⌫ Borrar carácter");
        borrar.setOnAction(ev -> { int p = destino.getCaretPosition(); if (p > 0) destino.deleteText(p-1,p); destino.requestFocus(); });
        teclado.getChildren().add(borrar);
        TitledPane plegable = new TitledPane("Teclado matemático", teclado);
        plegable.setExpanded(false);
        return plegable;
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
                stat("RAÍZ EXACTA (si se identifica)", valRaizExacta = new Label("—"), false),
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
        Tab tabDesarrollo = new Tab("🧮  Desarrollo y criterios", crearPanelDesarrollo());
        tabDesarrollo.setClosable(false);
        Tab tabComparar = new Tab("⚖  Comparar métodos", crearPanelComparacion());
        tabComparar.setClosable(false);
        tabs.getTabs().addAll(tabGrafica, tabTabla, tabDesarrollo, tabComparar);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox v = new VBox(12, stats, lblEstado, tabs);
        v.setPadding(new Insets(16, 16, 16, 4));
        // Reserva espacio real para las columnas de la tabla en el área de resultados.
        v.setMinWidth(760);
        return v;
    }

    private VBox crearPanelComparacion() {
        cmbCompararA = new ComboBox<>(FXCollections.observableArrayList(Metodo.values()));
        cmbCompararB = new ComboBox<>(FXCollections.observableArrayList(Metodo.values()));
        cmbCompararA.getSelectionModel().select(Metodo.BISECCION);
        cmbCompararB.getSelectionModel().select(Metodo.NEWTON);
        cmbCompararA.setMaxWidth(Double.MAX_VALUE);
        cmbCompararB.setMaxWidth(Double.MAX_VALUE);
        cmpA = campo("Extremo a"); cmpB = campo("Extremo b");
        cmpX0 = campo("Punto inicial x₀"); cmpX1 = campo("Segundo punto x₁ (Secante)");
        cmpG = campo("ej: cos(x) o (x + 2/x)/2");
        cmpA.setText("0"); cmpB.setText("1.3"); cmpX0.setText("0.5"); cmpX1.setText("1");
        VBox parametros = card("PARÁMETROS DE COMPARACIÓN (independientes de la pantalla principal)",
                new HBox(10, campoConEtiqueta("Extremo a", cmpA), campoConEtiqueta("Extremo b", cmpB)),
                new HBox(10, campoConEtiqueta("Punto inicial x₀", cmpX0), campoConEtiqueta("Punto x₁", cmpX1)),
                campoConEtiqueta("g(x) · función de iteración (métodos que la requieren)", cmpG),
                crearTecladoFuncion(cmpG));
        Button btn = new Button("Comparar métodos seleccionados");
        btn.getStyleClass().add("boton-primario");
        btn.setOnAction(e -> compararMetodos());
        cmbCompararA.setOnAction(e -> actualizarParametrosComparacion());
        cmbCompararB.setOnAction(e -> actualizarParametrosComparacion());
        panelComparacion = new VBox(12,
                bloqueDesarrollo("Comparación de dos métodos", "Cada método utiliza sus propios parámetros. La comparación no depende del algoritmo seleccionado en la pantalla principal."),
                campoConEtiqueta("Primer método", cmbCompararA),
                campoConEtiqueta("Segundo método", cmbCompararB), parametros, btn);
        panelComparacion.setPadding(new Insets(14));
        actualizarParametrosComparacion();
        ScrollPane scroll = new ScrollPane(panelComparacion);
        scroll.setFitToWidth(true);
        return new VBox(scroll);
    }

    private void actualizarParametrosComparacion() {
        if (cmpA == null) return;
        boolean cerrado = cmbCompararA.getValue().esCerrado() || cmbCompararB.getValue().esCerrado();
        boolean abierto = !cmbCompararA.getValue().esCerrado() || !cmbCompararB.getValue().esCerrado();
        boolean secante = cmbCompararA.getValue().usaX1() || cmbCompararB.getValue().usaX1();
        cmpA.setDisable(!cerrado); cmpB.setDisable(!cerrado);
        cmpX0.setDisable(!abierto); cmpX1.setDisable(!secante);
        boolean usaG = cmbCompararA.getValue().usaG() || cmbCompararB.getValue().usaG();
        cmpG.setDisable(!usaG);
    }

    private void compararMetodos() {
        try {
            Metodo a = cmbCompararA.getValue(), b = cmbCompararB.getValue();
            if (a == null || b == null) throw new IllegalArgumentException("Seleccioná los dos métodos.");
            if (a == b) throw new IllegalArgumentException("Elegí dos métodos distintos para compararlos.");
            String fTexto = txtFuncion.getText().trim();
            DoubleUnaryOperator f = funcion(fTexto, "f(x)");
            double tol = numero(txtEpsilon, "la tolerancia");
            if (tol <= 0) throw new IllegalArgumentException("La tolerancia debe ser mayor que 0.");
            int max = (int) numero(txtMaxIter, "las iteraciones máximas");
            if (max < 1 || max > 100000) throw new IllegalArgumentException("Las iteraciones máximas deben estar entre 1 y 100000.");
            double aa = Double.NaN, bb = Double.NaN, x0 = Double.NaN, x1 = Double.NaN;
            if (a.esCerrado() || b.esCerrado()) { aa = numero(cmpA, "el extremo a de la comparación"); bb = numero(cmpB, "el extremo b de la comparación"); }
            if (!a.esCerrado() || !b.esCerrado()) x0 = numero(cmpX0, "el punto inicial x₀ de la comparación");
            if (a.usaX1() || b.usaX1()) x1 = numero(cmpX1, "el punto x₁ de la comparación");
            String gTexto = cmpG.getText().trim();
            DoubleUnaryOperator g = (a.usaG() || b.usaG()) ? funcion(gTexto, "g(x) de la comparación") : null;
            Resultado ra = ejecutarComparacion(a, fTexto, f, gTexto, g, aa, bb, x0, x1, tol, max);
            Resultado rb = ejecutarComparacion(b, fTexto, f, gTexto, g, aa, bb, x0, x1, tol, max);
            panelComparacion.getChildren().removeIf(n -> "resultado-comparacion".equals(n.getUserData()));
            VBox resultado = new VBox(10);
            resultado.setUserData("resultado-comparacion");
            resultado.getChildren().add(bloqueDesarrollo("Resultado de la comparación",
                    "Función utilizada: f(x) = " + fTexto,
                    "Tolerancia: " + num(tol) + " · Máximo de iteraciones: " + max));
            resultado.getChildren().add(bloqueDesarrollo("Método 1: " + a,
                    "Raíz racional identificada: " + ResolverRaizExacta.buscar(fTexto, ra.raiz),
                    "Raíz aproximada: " + num(ra.raiz), "f(raíz): " + (Double.isFinite(ra.raiz) ? num(f.applyAsDouble(ra.raiz)) : "no definido"),
                    "Iteraciones: " + ra.iteraciones(), "Error final: " + cient(ra.errorFinal()),
                    ra.convergio ? "Estado: convergió." : "Estado: no alcanzó la tolerancia."));
            resultado.getChildren().add(bloqueDesarrollo("Método 2: " + b,
                    "Raíz racional identificada: " + ResolverRaizExacta.buscar(fTexto, rb.raiz),
                    "Raíz aproximada: " + num(rb.raiz), "f(raíz): " + (Double.isFinite(rb.raiz) ? num(f.applyAsDouble(rb.raiz)) : "no definido"),
                    "Iteraciones: " + rb.iteraciones(), "Error final: " + cient(rb.errorFinal()),
                    rb.convergio ? "Estado: convergió." : "Estado: no alcanzó la tolerancia."));
            String ganador = ra.convergio && rb.convergio ? (ra.iteraciones() < rb.iteraciones() ? a + " necesitó menos iteraciones." : rb.iteraciones() < ra.iteraciones() ? b + " necesitó menos iteraciones." : "Ambos necesitaron la misma cantidad de iteraciones.") :
                    ra.convergio ? a + " convergió y " + b + " no alcanzó la tolerancia." : rb.convergio ? b + " convergió y " + a + " no alcanzó la tolerancia." : "Ninguno alcanzó la tolerancia indicada.";
            resultado.getChildren().add(bloqueDesarrollo("Conclusión", ganador));
            panelComparacion.getChildren().add(resultado);
        } catch (IllegalArgumentException ex) {
            panelComparacion.getChildren().removeIf(n -> "resultado-comparacion".equals(n.getUserData()));
            panelComparacion.getChildren().add(bloqueDesarrollo("No se pudo comparar", ex.getMessage()));
        }
    }

    private Resultado ejecutarComparacion(Metodo m, String fTexto, DoubleUnaryOperator f,
                                          String gTexto, DoubleUnaryOperator g, double a, double b,
                                          double x0, double x1, double tol, int max) {
        if (m.esCerrado()) return MotorNumerico.cerrado(m, fTexto, f, a, b, tol, max);
        return MotorNumerico.abierto(m, fTexto, gTexto, f, m.usaG() ? g : null, x0, m.usaX1() ? x1 : Double.NaN, tol, max);
    }

    private VBox crearPanelDesarrollo() {
        panelDesarrollo = new VBox(12);
        panelDesarrollo.setPadding(new Insets(12));
        panelDesarrollo.getStyleClass().add("desarrollo-panel");
        Label vacio = new Label("Al resolver, acá se mostrarán los criterios, las derivadas y el desarrollo del método paso a paso.");
        vacio.setWrapText(true);
        vacio.getStyleClass().add("subtitulo");
        panelDesarrollo.getChildren().add(vacio);
        ScrollPane scroll = new ScrollPane(panelDesarrollo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return new VBox(scroll);
    }

    private VBox bloqueDesarrollo(String titulo, String... lineas) {
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("desarrollo-titulo");
        VBox bloque = new VBox(7);
        bloque.getStyleClass().add("desarrollo-bloque");
        bloque.getChildren().add(encabezado);
        for (String linea : lineas) {
            Label l = new Label(linea);
            l.setWrapText(true);
            l.setMaxWidth(Double.MAX_VALUE);
            l.getStyleClass().add("desarrollo-linea");
            bloque.getChildren().add(l);
        }
        return bloque;
    }

    private double evaluarExpresion(String expresion, double x) {
        Argument arg = new Argument("x", x);
        return new Expression(expresion, arg).calculate();
    }

    private void mostrarDesarrollo(Metodo m, Resultado r, String textoF, String textoG,
                                   double x0, double x1, double tol, int maxIter) {
        panelDesarrollo.getChildren().clear();
        panelDesarrollo.getChildren().add(bloqueDesarrollo("1. Datos del problema",
                "Función: f(x) = " + textoF,
                m.usaG() ? "Función de iteración: g(x) = " + textoG : "Método: " + m,
                "Tolerancia: ε = " + num(tol),
                "Punto inicial: x₀ = " + num(x0),
                m.usaX1() ? "Segundo punto: x₁ = " + num(x1) : "Iteraciones registradas: " + r.filas.size()));
        panelDesarrollo.getChildren().add(bloqueDesarrollo("2. Criterio de corte utilizado",
                "Método ejecutado: " + m,
                "Tolerancia configurada: ε = " + num(tol),
                "Límite de seguridad: " + maxIter + " iteraciones.",
                descripcionCorte(m),
                "El algoritmo se detiene cuando se cumple el criterio indicado o cuando llega al máximo de iteraciones."));
        if (m.esCerrado()) {
            panelDesarrollo.getChildren().add(bloqueDesarrollo("3. Criterio del método cerrado",
                    "Se trabaja con el intervalo [a, b] y se comprueba el cambio de signo de f en sus extremos.",
                    "En cada iteración se actualiza el intervalo hasta alcanzar la tolerancia o el máximo permitido.",
                    "Criterio de parada: el error indicado por el método debe ser menor que ε."));
            panelDesarrollo.getChildren().add(bloqueDesarrollo("4. Resultado y conclusión",
                    "Aproximación obtenida: x ≈ " + num(r.raiz),
                    "f(x) = " + (Double.isFinite(r.raiz) ? num(ultimaF.applyAsDouble(r.raiz)) : "no definido"),
                    "Iteraciones realizadas: " + r.iteraciones(),
                    "Error final: " + num(r.errorFinal()),
                    r.convergio ? "Conclusión: el método convergió según la tolerancia indicada." :
                            "Conclusión: no se alcanzó la tolerancia indicada."));
            return;
        } else if (m == Metodo.NEWTON) {
            double fx = evaluarExpresion(textoF, x0);
            double d1 = evaluarExpresion("der(" + textoF + ",x)", x0);
            double d2 = evaluarExpresion("der(der(" + textoF + ",x),x)", x0);
            boolean fourier = Double.isFinite(fx*d2) && fx*d2 > 0 && Math.abs(d1) > 1e-14;
            panelDesarrollo.getChildren().add(bloqueDesarrollo("3. Derivadas de la función",
                    "Función original: f(x) = " + textoF,
                    "Primera derivada: f'(x) = " + r.derivadaPrimera,
                    "Segunda derivada: f''(x) = " + r.derivadaSegunda,
                    "Evaluación inicial: f(x₀) = " + num(fx),
                    "Evaluación de la primera derivada: f'(x₀) = " + num(d1),
                    "Evaluación de la segunda derivada: f''(x₀) = " + num(d2)));
            panelDesarrollo.getChildren().add(bloqueDesarrollo("4. Aplicación del criterio de Fourier para Newton",
                    "Se comprueba en el punto inicial que f(x₀) · f''(x₀) = " + num(fx*d2),
                    "Además, f'(x₀) = " + num(d1) + (Math.abs(d1) > 1e-14 ? " (distinta de cero)." : " (es cero o muy cercana a cero)."),
                    fourier ? "Resultado: se cumple la comprobación local de signo para elegir x₀ en Newton." :
                            "Resultado: NO se cumple la comprobación local de Fourier en x₀; conviene elegir otro punto inicial.",
                    "Importante: la verificación completa del criterio de Fourier requiere estudiar el signo de f'' en todo el intervalo considerado."));
            panelDesarrollo.getChildren().add(bloqueDesarrollo("5. Fórmula de Newton-Raphson",
                    "xₙ₊₁ = xₙ − f(xₙ) / f'(xₙ)",
                    "En cada paso se evalúa f(xₙ), se divide por f'(xₙ) y se obtiene el siguiente x.",
                    "Error utilizado: |xₙ₊₁ − xₙ|."));
        } else if (m == Metodo.PUNTO_FIJO) {
            String dg = DerivadorSimbolico.derivarPrimera(textoG);
            double d = evaluarExpresion("der(" + textoG + ",x)", x0);
            panelDesarrollo.getChildren().add(bloqueDesarrollo("3. Derivada de la función de iteración",
                    "g(x) = " + textoG,
                    "Derivada calculada: g'(x) = " + dg,
                    "Evaluación en el punto inicial: g'(x₀) = " + num(d)));
            panelDesarrollo.getChildren().add(bloqueDesarrollo("4. Criterio de convergencia de Punto Fijo",
                    "g(x) = " + textoG,
                    "g'(x) = " + dg,
                    "g'(x₀) = " + num(d),
                    "|g'(x₀)| = " + num(Math.abs(d)),
                    Math.abs(d) < 1 ? "Conclusión: |g'(x₀)| < 1. Hay indicio local de convergencia." :
                            "Conclusión: |g'(x₀)| ≥ 1. No se garantiza la convergencia; puede divergir.",
                    "La condición rigurosa se verifica en un entorno/intervalo, no solamente en x₀."));
            panelDesarrollo.getChildren().add(bloqueDesarrollo("5. Fórmula de iteración",
                    "xₙ₊₁ = g(xₙ)",
                    "Error utilizado: |xₙ₊₁ − xₙ|.",
                    "Se repite la sustitución hasta que el error sea menor que ε o se alcance el máximo de iteraciones."));
        } else {
            panelDesarrollo.getChildren().add(bloqueDesarrollo("3. Expresiones de las derivadas",
                    "f'(x) = " + r.derivadaPrimera,
                    "f''(x) = " + r.derivadaSegunda));
            String formula = m == Metodo.SECANTE ? "xₙ₊₁ = xₙ − f(xₙ)(xₙ − xₙ₋₁) / (f(xₙ) − f(xₙ₋₁))" :
                    m == Metodo.HALLEY ? "xₙ₊₁ = xₙ − 2f(xₙ)f'(xₙ) / (2[f'(xₙ)]² − f(xₙ)f''(xₙ))" :
                            m == Metodo.NEWTON_EXT ? "Método de Newton extendido: actualización basada en derivadas para raíces múltiples." :
                                    m == Metodo.AITKEN ? "Aceleración Δ² aplicada a la sucesión xₙ₊₁ = g(xₙ)." :
                                            m == Metodo.STEFFENSEN ? "Aceleración de Steffensen aplicada a la iteración de punto fijo." :
                                                    "Se actualiza la aproximación mediante la regla propia del método seleccionado.";
            panelDesarrollo.getChildren().add(bloqueDesarrollo("4. Fórmula / procedimiento", formula));
        }
        panelDesarrollo.getChildren().add(bloqueDesarrollo("5. Resultado y conclusión",
                "Aproximación obtenida: x ≈ " + num(r.raiz),
                "f(x) = " + (Double.isFinite(r.raiz) ? num(ultimaF.applyAsDouble(r.raiz)) : "no definido"),
                "Iteraciones realizadas: " + r.iteraciones(),
                "Error final: " + num(r.errorFinal()),
                r.convergio ? "Conclusión: el método convergió según la tolerancia indicada." :
                        "Conclusión: no se alcanzó la tolerancia indicada. Revisá los datos iniciales o el criterio."));
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
        lblPuntoGrafica = new Label("Seleccioná un punto de la gráfica para ver sus coordenadas.");
        lblPuntoGrafica.getStyleClass().add("subtitulo");
        lblPuntoGrafica.setWrapText(true);
        lblPuntoGrafica.setMaxWidth(Double.MAX_VALUE);
        HBox barra = new HBox(12, chkVerF, chkVerG, chkVerIter, espacio, leyenda);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(4, 4, 0, 4));

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("x");
        yAxis.setLabel("y");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        javafx.util.StringConverter<Number> formatoEje = new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number valor) { return valor == null ? "" : num(valor.doubleValue()); }
            @Override public Number fromString(String texto) { return Double.parseDouble(texto.replace(',', '.')); }
        };
        xAxis.setTickLabelFormatter(formatoEje);
        yAxis.setTickLabelFormatter(formatoEje);
        xAxis.setMinorTickVisible(false);
        yAxis.setMinorTickVisible(false);

        grafico = new LineChart<>(xAxis, yAxis);
        grafico.setAnimated(false);
        grafico.setLegendVisible(false);
        grafico.setCreateSymbols(true);
        grafico.setTitle("Comportamiento de la función");
        VBox.setVgrow(grafico, Priority.ALWAYS);
        instalarNavegacionGrafico();

        VBox v = new VBox(6, barra, lblPuntoGrafica, grafico);
        v.getStyleClass().add("panel-grafica");
        v.setPadding(new Insets(10));
        return v;
    }

    // ------------------------------------------------------------------ tabla
    private Node crearPanelTabla() {
        tabla = new TableView<>();
        // Las columnas se reparten proporcionalmente según el ancho real disponible.
        // No se usa una tabla con ancho fijo ni se recortan los valores de las celdas.
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(36);
        tabla.setSortPolicy(tv -> false);
        tabla.widthProperty().addListener((obs, anterior, nuevo) -> ajustarAnchosColumnas());
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
            tabla.getColumns().add(columna("f(c)", f -> num(f.fx), null));
        } else {
            tabla.getColumns().add(columna("xₙ", f -> num(f.x), null));
            tabla.getColumns().add(columna("f(xₙ)", f -> num(f.fx), null));
            if (m.usaG()) tabla.getColumns().add(columna("g(xₙ)", f -> num(f.gx), null));
        }
        tabla.getColumns().add(columna("Error", f -> num(f.error), null));
        ajustarAnchosColumnas();
    }

    /**
     * Distribuye el ancho de la tabla entre todas sus columnas. Los anchos mínimos
     * alcanzan para mostrar completos los números con el formato usado (8 decimales
     * o notación científica), mientras que el espacio sobrante se reparte según
     * la importancia de cada dato. Así se evita que una columna quede comprimida.
     */
    private void ajustarAnchosColumnas() {
        if (tabla == null || tabla.getColumns().isEmpty()) return;
        int cantidad = tabla.getColumns().size();
        double ancho = Math.max(0, tabla.getWidth() - 30); // bordes y barra vertical
        if (ancho <= 0) return;

        double[] pesos;
        if (cantidad == 6) {          // Bisección y Regula Falsi
            pesos = new double[]{0.08, 0.17, 0.17, 0.17, 0.20, 0.21};
        } else if (cantidad == 5) {   // métodos abiertos que también muestran g(xₙ)
            pesos = new double[]{0.08, 0.21, 0.21, 0.21, 0.29};
        } else {                      // métodos abiertos sin g(xₙ)
            pesos = new double[]{0.09, 0.27, 0.27, 0.37};
        }

        double minimoTotal = 0;
        for (int i = 0; i < cantidad; i++) {
            double minimo = i == 0 ? 64 : 132;
            tabla.getColumns().get(i).setMinWidth(minimo);
            minimoTotal += minimo;
        }
        // El ancho mínimo de la ventana garantiza que normalmente se disponga
        // de espacio suficiente. Si el sistema resta algunos píxeles, se reparte
        // igualmente el ancho disponible sin ocultar ni eliminar columnas.
        double base = Math.max(ancho, minimoTotal);
        double sobrante = Math.max(0, base - minimoTotal);
        double sumaPesos = 0;
        for (double peso : pesos) sumaPesos += peso;
        for (int i = 0; i < cantidad; i++) {
            double minimo = i == 0 ? 64 : 132;
            double pref = minimo + sobrante * (pesos[i] / sumaPesos);
            tabla.getColumns().get(i).setPrefWidth(pref);
            tabla.getColumns().get(i).setMaxWidth(Double.MAX_VALUE);
        }
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
            if (lblPuntoGrafica != null) lblPuntoGrafica.setText("Seleccioná un punto de la gráfica para ver sus coordenadas.");
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
        // Mostrar también el criterio y las derivadas cuando el método falla al iterar.
        mostrarDesarrollo(m, r, ultimoTextoF, txtG.getText().trim(), ultimoX0, ultimoX1,
                numero(txtEpsilon, "la tolerancia"), (int) numero(txtMaxIter, "las iteraciones máximas"));

        if (r.fallo) {
            valRaiz.setText(Double.isNaN(r.raiz) ? "—" : num(r.raiz));
            valRaizExacta.setText("No identificada");
            valFRaiz.setText("—");
            valIter.setText(r.filas.isEmpty() ? "—" : String.valueOf(r.iteraciones()));
            valError.setText("—");
            setEstado("err", "✖  " + r.mensaje);
            setBadge("Sin solución", "err");
            panelDesarrollo.getChildren().clear();
            panelDesarrollo.getChildren().add(bloqueDesarrollo("Resultado del método",
                    r.mensaje,
                    "Iteraciones registradas: " + r.filas.size(),
                    "Revisá las expresiones y los valores iniciales."));
            return;
        }
        valRaiz.setText(num(r.raiz));
        valRaizExacta.setText(ResolverRaizExacta.buscar(ultimoTextoF, r.raiz));
        valFRaiz.setText(cient(ultimaF.applyAsDouble(r.raiz)));
        valIter.setText(String.valueOf(r.iteraciones()));
        valError.setText(num(r.errorFinal()));
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

    private void instalarInfoPunto(XYChart.Data<Number, Number> dato, String titulo, String... lineas) {
        final String detalle = titulo + "\n" + String.join("\n", lineas);
        dato.nodeProperty().addListener((obs, anterior, nodo) -> {
            if (nodo == null) return;
            Tooltip.install(nodo, new Tooltip(detalle));
            nodo.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && lblPuntoGrafica != null) {
                    lblPuntoGrafica.setText(detalle.replace("\n", "     |     "));
                    evento.consume();
                }
            });
        });
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
                final String nombreX = (ultimoMetodo.esCerrado() ? "c" : "x") + "₍" + f.n + "₎";
                final String nombreY = fp ? "g(xₙ)" : "f(xₙ)";
                if (fp) {
                    instalarInfoPunto(d, "Iteración " + f.n,
                            nombreX + " = " + num(f.x),
                            "f(xₙ) = " + num(f.fx),
                            nombreY + " = " + num(y));
                } else {
                    instalarInfoPunto(d, "Iteración " + f.n,
                            nombreX + " = " + num(f.x),
                            nombreY + " = " + num(y));
                }
                sIt.getData().add(d);
            }
        }
        if (r != null && !r.fallo && ok(r.raiz)) {
            XYChart.Data<Number, Number> d = new XYChart.Data<>(r.raiz, 0);
            instalarInfoPunto(d, "Raíz aproximada", "x = " + num(r.raiz), "f(x) = " + num(ultimaF.applyAsDouble(r.raiz)));
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

    private boolean esSimboloGrafico(Object objetivo) {
        Node nodo = objetivo instanceof Node ? (Node) objetivo : null;
        while (nodo != null && nodo != grafico) {
            if (nodo.getStyleClass().contains("chart-line-symbol")) return true;
            nodo = nodo.getParent();
        }
        return false;
    }

    private void instalarNavegacionGrafico() {
        grafico.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY || !vistaInicializada
                    || !dentroZona(e.getSceneX(), e.getSceneY()) || esSimboloGrafico(e.getTarget())) return;
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

    /** Representa cualquier double en decimal completo, sin notación científica (E-...). */
    private static String num(double v) {
        if (Double.isNaN(v)) return "No definido";
        if (Double.isInfinite(v)) return v > 0 ? "Infinito" : "Menos infinito";
        if (v == 0.0) return "0";
        return new BigDecimal(Double.toString(v)).stripTrailingZeros().toPlainString();
    }

    /** Se conserva como alias para llamadas antiguas: nunca muestra E ni exponentes. */
    private static String cient(double v) { return num(v); }

    private String descripcionCorte(Metodo m) {
        switch (m) {
            case BISECCION:
                return "Corte efectivo: |f(cₙ)| ≤ ε. La columna Error muestra además la cota (bₙ − aₙ) / 2; el bucle de esta implementación se detiene por el valor absoluto de f(cₙ).";
            case REGULA_FALSI:
                return "Corte efectivo: |f(cₙ)| ≤ ε. En la columna Error se registra |cₙ − cₙ₋₁| desde la segunda fila; la primera no tiene error anterior.";
            case REGULA_FALSI_MOD:
                return "Corte efectivo: |f(cₙ)| ≤ ε. En la columna Error se registra |cₙ − cₙ₋₁| desde la segunda fila; la primera no tiene error anterior.";
            case NEWTON:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε. También se detiene si la derivada es prácticamente nula o si alcanza el máximo de iteraciones.";
            case NEWTON_EXT:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε. También se detiene ante un denominador prácticamente nulo o al alcanzar el máximo de iteraciones.";
            case HALLEY:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε. También se detiene ante un denominador prácticamente nulo o al alcanzar el máximo de iteraciones.";
            case SECANTE:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε. Usa los dos puntos iniciales y se detiene también si la diferencia entre sus evaluaciones es prácticamente nula.";
            case PUNTO_FIJO:
                return "Corte efectivo: |g(xₙ) − xₙ| ≤ ε, que se registra como |xₙ₊₁ − xₙ|. También se detiene al alcanzar el máximo de iteraciones.";
            case AITKEN:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε, donde xₙ₊₁ es la aproximación acelerada de Aitken. También se detiene si el denominador es prácticamente nulo.";
            case STEFFENSEN:
                return "Corte efectivo: |xₙ₊₁ − xₙ| ≤ ε, donde xₙ₊₁ es la aproximación acelerada de Steffensen. También se detiene si el denominador es prácticamente nulo.";
            default:
                return "Corte efectivo: error menor o igual que la tolerancia ε, o máximo de iteraciones alcanzado.";
        }
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
        .tecla-matematica { -fx-background-color: #354D60; -fx-text-fill: #E5F1F8; -fx-background-radius: 7; -fx-padding: 6 8; }
        .tecla-matematica:hover { -fx-background-color: #47718A; }

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
        .table-row-cell { -fx-background-color: -fx-control-inner-background; -fx-cell-size: 36; -fx-border-color: transparent; }
        .table-row-cell:odd { -fx-background-color: -fx-control-inner-background-alt; }
        .table-row-cell:selected { -fx-background-color: #42627C; }
        .table-cell { -fx-text-fill: #E2ECF3; -fx-alignment: CENTER-RIGHT; -fx-font-family: "Consolas", "JetBrains Mono", monospace; -fx-font-size: 12px; -fx-padding: 0 10 0 8; }
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
        .desarrollo-panel { -fx-background-color: transparent; }
        .desarrollo-bloque {
            -fx-background-color: #273746; -fx-background-radius: 12;
            -fx-border-color: #3A5263; -fx-border-radius: 12; -fx-padding: 14;
        }
        .desarrollo-titulo { -fx-text-fill: #80C1DF; -fx-font-size: 14px; -fx-font-weight: bold; }
        .desarrollo-linea { -fx-text-fill: #E0E9EF; -fx-font-size: 13px; -fx-padding: 2 0 2 4; }
        .table-view { -fx-background-color: #1B2632; -fx-border-color: #3A5263; }
        .table-view .column-header-background { -fx-background-color: #354D61; }
        .table-view .column-header, .table-view .filler { -fx-background-color: #354D61; -fx-size: 38px; }
        .table-view .column-header .label { -fx-text-fill: #F0F5F8; -fx-font-weight: bold; }
        .table-row-cell { -fx-background-color: #22313F; -fx-border-color: #344858; -fx-table-cell-border-color: #344858; }
        .table-row-cell:odd { -fx-background-color: #273746; }
        .table-cell { -fx-text-fill: #E0E9EF; -fx-alignment: CENTER; -fx-padding: 7 10 7 10; }
        """;

    // ================================================================== MAIN
    public static void main(String[] args) {
        launch(args);
    }
}
