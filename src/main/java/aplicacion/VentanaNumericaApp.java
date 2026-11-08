package aplicacion;

import aplicacion.MotorNumerico.Fila;
import aplicacion.MotorNumerico.Metodo;
import aplicacion.MotorNumerico.Resultado;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.KeyCode;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    // Graficador cartesiano propio: escala geométrica idéntica en ambos ejes.
    private Canvas lienzoGrafica, lienzoCursor;
    private StackPane contenedorGrafica;
    private Label lblFormulaGrafica, lblCoordenadasGrafica;
    private boolean cursorEnGrafica = false;
    private double cursorPx, cursorPy;
    private HBox leyenda;
    private Label lblPuntoGrafica;
    private Tab tabTabla;
    private VBox panelDesarrollo;
    private ComboBox<Metodo> cmbCompararA, cmbCompararB;
    private VBox panelComparacion;
    private TextField cmpA, cmpB, cmpX0, cmpX1, cmpG;
    // Campo que recibirá las teclas del teclado matemático compartido.
    private TextField campoActivoTeclado;
    private Label valRaizExacta;

    // Vista interactiva: una unidad tiene el mismo tamaño en X e Y.
    private boolean vistaInicializada = false;
    private double centroX, centroY, unidadesPorPixel;
    private double arrastreX, arrastreY;
    private boolean arrastrando = false;


    // ---------------------------------------------- estado de la última corrida
    private DoubleUnaryOperator ultimaF, ultimaG;
    private Metodo ultimoMetodo;
    private Resultado ultimoResultado;
    private double ultimoA, ultimoB, ultimoX0, ultimoX1;
    private String ultimoTextoF = "";

    private static final String[] COLOR = {
            "#53BDEB", "#00A884", "#8696A0", "#7AE3C3", "#F0B86E", "#D8A6FF", "#25D366"};

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
        Node panelIzquierdo = crearPanelIzquierdo();
        root.setLeft(panelIzquierdo);
        root.setCenter(crearPanelDerecho());
        // Espacio externo: evita que el panel quede pegado al borde de la ventana.
        root.setPadding(new Insets(0, 10, 10, 10));
        BorderPane.setMargin(panelIzquierdo, new Insets(10, 6, 0, 4));

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

        registrarCampoTeclado(txtFuncion);
        registrarCampoTeclado(txtG);
        campoActivoTeclado = txtFuncion;
        VBox cardFuncion = card("FUNCIÓN",
                campoConEtiqueta("f(x)", txtFuncion), boxG,
                crearTecladoCompartido(), campoConEtiqueta("Ejemplos", cmbEjemplo));

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
            if (ultimaF != null) { vistaInicializada = false; dibujar(); }
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

        VBox contenido = new VBox(8, cardFuncion, cardMetodo, cardParams, cardGrafica, btnResolver);
        contenido.setPadding(new Insets(4, 6, 12, 6));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(326);
        scroll.setMinWidth(310);
        scroll.setMaxWidth(340);
        scroll.getStyleClass().add("panel-izq");

        actualizarVisibilidad();
        return scroll;
    }

    /** Mantiene el destino seleccionado aunque el usuario pulse un botón del teclado. */
    private void registrarCampoTeclado(TextField campo) {
        campo.focusedProperty().addListener((obs, antes, ahora) -> {
            if (ahora) campoActivoTeclado = campo;
        });
        campo.setOnMouseClicked(e -> campoActivoTeclado = campo);
    }

    /** Un solo teclado para f(x) y g(x), contraído por defecto para ahorrar altura. */
    private Node crearTecladoCompartido() {
        String[][] teclas = {
                {"sin(", "cos(", "tan(", "asin(", "acos(", "atan("},
                {"ln(", "log(", "exp(", "sqrt(", "abs(", "pi"},
                {"x", "(", ")", "^", "/", "*", "-", "+"},
                {"7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", ","}
        };
        VBox teclado = new VBox(4);
        for (String[] fila : teclas) {
            HBox botones = new HBox(3);
            for (String simbolo : fila) {
                Button b = new Button(simbolo);
                b.getStyleClass().add("tecla-matematica");
                b.setMinWidth(0);
                b.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(b, Priority.ALWAYS);
                b.setOnAction(ev -> {
                    TextField destino = campoActivoTeclado == null ? txtFuncion : campoActivoTeclado;
                    int inicio = destino.getSelection().getStart();
                    int fin = destino.getSelection().getEnd();
                    destino.replaceText(inicio, fin, simbolo);
                    destino.requestFocus();
                    destino.positionCaret(inicio + simbolo.length());
                });
                botones.getChildren().add(b);
            }
            teclado.getChildren().add(botones);
        }
        Button borrar = new Button("⌫ Borrar carácter");
        borrar.getStyleClass().add("boton-secundario");
        borrar.setMaxWidth(Double.MAX_VALUE);
        borrar.setOnAction(ev -> {
            TextField destino = campoActivoTeclado == null ? txtFuncion : campoActivoTeclado;
            int inicio = destino.getSelection().getStart();
            int fin = destino.getSelection().getEnd();
            if (inicio != fin) destino.deleteText(inicio, fin);
            else if (inicio > 0) destino.deleteText(inicio - 1, inicio);
            destino.requestFocus();
        });
        teclado.getChildren().add(borrar);
        TitledPane plegable = new TitledPane("Teclado matemático · f(x) / g(x)", teclado);
        plegable.setExpanded(false);
        plegable.getStyleClass().add("teclado-plegable");
        return plegable;
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
        // Resumen compacto en dos filas: más altura útil para el plano.
        // La raíz exacta conserva doble ancho para expresiones extensas.
        GridPane stats = new GridPane();
        stats.getStyleClass().add("stats-grid");
        stats.setHgap(6);
        stats.setVgap(5);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / 3);
            columna.setMinWidth(0);
            columna.setHgrow(Priority.ALWAYS);
            stats.getColumnConstraints().add(columna);
        }
        VBox raizExacta = stat("RAÍZ EXACTA (si se identifica)", valRaizExacta = new Label("—"), false);
        stats.add(raizExacta, 0, 0, 2, 1);
        stats.add(stat("RAÍZ APROXIMADA", valRaiz, true), 2, 0);
        stats.add(stat("f(raíz)", valFRaiz, false), 0, 1);
        stats.add(stat("ITERACIONES", valIter, false), 1, 1);
        stats.add(stat("ERROR FINAL", valError, false), 2, 1);

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

        VBox v = new VBox(6, stats, lblEstado, tabs);
        v.setPadding(new Insets(7, 10, 7, 4));
        VBox.setVgrow(tabs, Priority.ALWAYS);
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
                r.convergio ? (m == Metodo.PUNTO_FIJO
                        ? "Conclusión: " + r.mensaje
                        : "Conclusión: el método convergió según la tolerancia indicada.") :
                        "Conclusión: no se alcanzó la tolerancia indicada. Revisá los datos iniciales o el criterio."));
    }

    private VBox stat(String titulo, Label valor, boolean destacado) {
        Label t = new Label(titulo);
        t.getStyleClass().add("stat-titulo");
        t.setWrapText(true);
        t.setMinWidth(0);
        t.setMaxWidth(Double.MAX_VALUE);

        valor.getStyleClass().addAll("stat-valor", "mono");
        if (destacado) valor.getStyleClass().add("stat-destacado");
        valor.setWrapText(true);                // Permite varias líneas para cifras extensas.
        valor.setMinWidth(0);                  // No obliga a ensanchar las columnas.
        valor.setMaxWidth(Double.MAX_VALUE);
        valor.setTextOverrun(OverrunStyle.CLIP);
        valor.setMaxHeight(35);             // Hasta dos líneas; el tooltip conserva el valor completo.

        // El texto completo también queda disponible al pasar el mouse.
        Tooltip detalle = new Tooltip();
        detalle.textProperty().bind(valor.textProperty());
        detalle.setWrapText(true);
        detalle.setMaxWidth(600);
        valor.setTooltip(detalle);

        VBox b = new VBox(1, t, valor);
        b.setMinWidth(0);
        b.setMaxWidth(Double.MAX_VALUE);
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
        for (CheckBox c : List.of(chkVerF, chkVerG, chkVerIter)) {
            c.setOnAction(e -> { actualizarLeyenda(); dibujar(); });
        }

        lblFormulaGrafica = new Label("Plano cartesiano · escala 1:1");
        lblFormulaGrafica.getStyleClass().add("grafica-formula");
        lblFormulaGrafica.setMaxWidth(Double.MAX_VALUE);
        lblFormulaGrafica.setEllipsisString("…");

        Button acercar = botonGrafica("+", "Acercar", () -> zoomEnCentro(0.75));
        Button alejar = botonGrafica("−", "Alejar", () -> zoomEnCentro(1.0 / 0.75));
        Button ajustar = botonGrafica("Ajustar", "Ajustar vista a los datos", () -> {
            vistaInicializada = false;
            dibujar();
        });
        Button origen = botonGrafica("Origen", "Centrar en (0, 0)", () -> {
            centroX = 0;
            centroY = 0;
            dibujar();
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox herramientas = new HBox(6, acercar, alejar, ajustar, origen);
        herramientas.setAlignment(Pos.CENTER_RIGHT);
        HBox barra = new HBox(10, chkVerF, chkVerG, chkVerIter, espacio, herramientas);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("grafica-barra");

        lienzoGrafica = new Canvas();
        lienzoGrafica.setManaged(false);
        // El lienzo superior dibuja solo la mira del mouse: no recalcula curvas.
        lienzoCursor = new Canvas();
        lienzoCursor.setManaged(false);
        lienzoCursor.setFocusTraversable(true);
        contenedorGrafica = new StackPane(lienzoGrafica, lienzoCursor);
        contenedorGrafica.setMinHeight(220);
        contenedorGrafica.setPrefHeight(560);
        contenedorGrafica.getStyleClass().add("plano-cartesiano");
        VBox.setVgrow(contenedorGrafica, Priority.ALWAYS);

        // El lienzo utiliza TODO el espacio disponible, sin la reserva variable
        // que hacen NumberAxis y LineChart para las etiquetas de los ejes.
        lienzoGrafica.widthProperty().bind(contenedorGrafica.widthProperty());
        lienzoGrafica.heightProperty().bind(contenedorGrafica.heightProperty());
        lienzoCursor.widthProperty().bind(contenedorGrafica.widthProperty());
        lienzoCursor.heightProperty().bind(contenedorGrafica.heightProperty());
        lienzoCursor.widthProperty().addListener((obs, viejo, nuevo) -> repintarCursor());
        lienzoCursor.heightProperty().addListener((obs, viejo, nuevo) -> repintarCursor());
        lienzoGrafica.widthProperty().addListener((obs, viejo, nuevo) -> dibujar());
        lienzoGrafica.heightProperty().addListener((obs, viejo, nuevo) -> dibujar());
        instalarNavegacionGrafico();

        leyenda = new HBox(14);
        leyenda.setAlignment(Pos.CENTER_LEFT);
        lblCoordenadasGrafica = new Label("Mover: arrastrar  ·  Zoom: rueda o +/−  ·  Doble clic: ajustar");
        lblCoordenadasGrafica.getStyleClass().add("grafica-coordenadas");
        lblPuntoGrafica = new Label("Hacé clic cerca de una iteración o de la raíz para ver sus valores.");
        lblPuntoGrafica.getStyleClass().add("subtitulo");
        lblPuntoGrafica.setWrapText(false);
        lblPuntoGrafica.setTextOverrun(OverrunStyle.ELLIPSIS);
        lblPuntoGrafica.setMaxWidth(Double.MAX_VALUE);
        Tooltip ayudaPunto = new Tooltip();
        ayudaPunto.textProperty().bind(lblPuntoGrafica.textProperty());
        lblPuntoGrafica.setTooltip(ayudaPunto);
        actualizarLeyenda();

        // La fórmula comparte línea con la barra de herramientas.
        // Los mensajes inferiores se apilan con espacios mínimos para dejar
        // la mayor superficie posible al plano cartesiano.
        VBox v = new VBox(4, barra, lblFormulaGrafica, contenedorGrafica,
                leyenda, lblCoordenadasGrafica, lblPuntoGrafica);
        v.getStyleClass().add("panel-grafica");
        v.setPadding(new Insets(5));
        return v;
    }

    private Button botonGrafica(String texto, String ayuda, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("grafica-boton");
        boton.setTooltip(new Tooltip(ayuda));
        boton.setOnAction(e -> accion.run());
        return boton;
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
            if (lblPuntoGrafica != null) lblPuntoGrafica.setText("Hacé clic cerca de una iteración o de la raíz para ver sus valores.");
            vistaInicializada = false; // Reajustar al nuevo problema, manteniendo escala 1:1.
            actualizarLeyenda();
            if (lblFormulaGrafica != null) lblFormulaGrafica.setTooltip(new Tooltip("f(x) = " + ultimoTextoF));
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
            boolean puntoFijoSinRaizVerificada = m == Metodo.PUNTO_FIJO
                    && (!Double.isFinite(ultimaF.applyAsDouble(r.raiz))
                    || Math.abs(ultimaF.applyAsDouble(r.raiz)) > numero(txtEpsilon, "la tolerancia"));
            if (puntoFijoSinRaizVerificada) {
                setEstado("warn", "⚠  " + r.mensaje);
                setBadge("Punto fijo alcanzado", "warn");
            } else {
                setEstado("ok", "✔  " + r.mensaje + "  ·  " + m);
                setBadge("Convergió", "ok");
            }
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

    // ======================================================= PLANO CARTESIANO
    // Se dibuja sobre un Canvas con una sola escala (unidadesPorPixel).
    // Por construcción, 1 unidad horizontal ocupa exactamente los mismos
    // píxeles que 1 unidad vertical, incluso al cambiar el tamaño o hacer zoom.
    private void dibujar() {
        if (lienzoGrafica == null) return;
        double w = lienzoGrafica.getWidth(), h = lienzoGrafica.getHeight();
        if (w < 40 || h < 40) return;

        if (!vistaInicializada) inicializarVista(w, h);
        GraphicsContext gc = lienzoGrafica.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#0B141A"));
        gc.fillRect(0, 0, w, h);
        gc.save();
        gc.beginPath();
        gc.rect(0, 0, w, h);
        gc.clip();

        dibujarCuadricula(gc, w, h);
        if (ultimaF != null) {
            if (chkVerF.isSelected()) {
                trazarFuncion(gc, ultimaF, COLOR[0], 2.5, w, h);
            }
            if (ultimoMetodo != null && ultimoMetodo.usaG() && chkVerG.isSelected() && ultimaG != null) {
                trazarFuncion(gc, x -> x, COLOR[2], 1.6, w, h);
                trazarFuncion(gc, ultimaG, COLOR[1], 2.5, w, h);
            }
            if (chkVerIter.isSelected() && ultimoResultado != null) dibujarIteraciones(gc, w, h);
            if (ultimoResultado != null && !ultimoResultado.fallo && ok(ultimoResultado.raiz)) {
                dibujarRaiz(gc, ultimoResultado.raiz, w, h);
            }
        }

        gc.restore();
        repintarCursor();
        if (lblFormulaGrafica != null) {
            lblFormulaGrafica.setText(ultimaF == null
                    ? "Plano cartesiano · escala 1:1"
                    : "f(x) = " + ultimoTextoF + "     ·     escala idéntica en X e Y");
        }
    }

    private void inicializarVista(double w, double h) {
        if (ultimaF == null || ultimoMetodo == null) {
            centroX = 0;
            centroY = 0;
            unidadesPorPixel = Math.max(10.0 / w, 6.0 / h);
        } else {
            double[] r = rangoX();
            // Con rango manual, el centro respeta el intervalo indicado.
            boolean manual = chkRangoAuto != null && !chkRangoAuto.isSelected();
            centroX = !manual && ultimoResultado != null && !ultimoResultado.fallo
                    && ok(ultimoResultado.raiz) ? ultimoResultado.raiz : (r[0] + r[1]) / 2.0;
            centroY = 0;
            double spanX = Math.max(1e-9, r[1] - r[0]);
            double radioNecesario = Math.max(centroX - r[0], r[1] - centroX);
            unidadesPorPixel = Math.max(Math.max(2.0 * radioNecesario / (w * 0.88),
                    spanX / (w * 0.88)), 4.5 / h);
        }
        vistaInicializada = true;
        if (lblCoordenadasGrafica != null) {
            lblCoordenadasGrafica.setText("Mover: arrastrar  ·  Zoom: rueda o +/−  ·  Doble clic: ajustar");
        }
    }

    private double graficaX(double x, double w) {
        return w / 2.0 + (x - centroX) / unidadesPorPixel;
    }

    private double graficaY(double y, double h) {
        return h / 2.0 - (y - centroY) / unidadesPorPixel;
    }

    private double coordenadaX(double px, double w) {
        return centroX + (px - w / 2.0) * unidadesPorPixel;
    }

    private double coordenadaY(double py, double h) {
        return centroY - (py - h / 2.0) * unidadesPorPixel;
    }

    /** Etiquetas que mantienen precisión al acercarse y se abrevian a escala extrema. */
    private String etiquetaEje(double valor, double paso) {
        if (!ok(valor) || Math.abs(valor) < paso * 1e-7) return "0";
        int decimales = (int) Math.max(0, Math.min(12, -Math.floor(Math.log10(paso)) + 1));
        if (Math.abs(valor) >= 1e8 || (Math.abs(valor) < 1e-7 && decimales > 10)) {
            return String.format(Locale.ROOT, "%.4g", valor);
        }
        return BigDecimal.valueOf(valor).setScale(decimales, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private void dibujarCuadricula(GraphicsContext gc, double w, double h) {
        double xmin = coordenadaX(0, w), xmax = coordenadaX(w, w);
        double ymin = coordenadaY(h, h), ymax = coordenadaY(0, h);

        // Cuadrícula dinámica: divisiones 1, 2 o 5 × 10^n, con marcas menores.
        // La referencia es una distancia en PÍXELES, no un rango X o Y aislado.
        double mayor = pasoLindo(unidadesPorPixel * 95.0);
        double menor = mayor / 5.0;
        if (menor / unidadesPorPixel >= 12) {
            gc.setStroke(Color.web("#213139"));
            gc.setLineWidth(0.65);
            lineasVerticales(gc, xmin, xmax, menor, w, h);
            lineasHorizontales(gc, ymin, ymax, menor, w, h);
        }

        gc.setStroke(Color.web("#354951"));
        gc.setLineWidth(1.0);
        lineasVerticales(gc, xmin, xmax, mayor, w, h);
        lineasHorizontales(gc, ymin, ymax, mayor, w, h);

        // Ejes verdaderos en cero; no se simula el eje X dibujando una serie.
        double ejeX = graficaX(0, w), ejeY = graficaY(0, h);
        gc.setStroke(Color.web("#91A5AC"));
        gc.setLineWidth(1.6);
        if (ejeX >= 0 && ejeX <= w) gc.strokeLine(ejeX + .5, 0, ejeX + .5, h);
        if (ejeY >= 0 && ejeY <= h) gc.strokeLine(0, ejeY + .5, w, ejeY + .5);

        // Cuando el origen queda fuera, las etiquetas permanecen visibles en
        // un margen fijo del plano, como en una calculadora gráfica.
        double textoEjeX = ejeY >= 22 && ejeY <= h - 24 ? ejeY + 16 : h - 10;
        double textoEjeY = ejeX >= 40 && ejeX <= w - 100 ? ejeX + 8 : 8;
        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.web("#AEBAC1"));
        gc.setTextAlign(TextAlignment.CENTER);
        long xInicio = (long) Math.ceil(xmin / mayor);
        long xFin = (long) Math.floor(xmax / mayor);
        for (long i = xInicio; i <= xFin && i - xInicio < 120; i++) {
            double v = i * mayor;
            double px = graficaX(v, w);
            if (px < 28 || px > w - 28) continue;
            if (Math.abs(v) < mayor * 1e-7 && ejeX >= 0 && ejeX <= w && ejeY >= 0 && ejeY <= h) continue;
            gc.fillText(etiquetaEje(v, mayor), px, textoEjeX);
        }

        gc.setTextAlign(TextAlignment.LEFT);
        long yInicio = (long) Math.ceil(ymin / mayor);
        long yFin = (long) Math.floor(ymax / mayor);
        for (long i = yInicio; i <= yFin && i - yInicio < 120; i++) {
            double v = i * mayor;
            double py = graficaY(v, h);
            if (py < 14 || py > h - 14) continue;
            if (Math.abs(v) < mayor * 1e-7 && ejeX >= 0 && ejeX <= w && ejeY >= 0 && ejeY <= h) continue;
            gc.fillText(etiquetaEje(v, mayor), textoEjeY, py - 5);
        }
        if (ejeX >= 0 && ejeX <= w && ejeY >= 0 && ejeY <= h) {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText("0", Math.min(w - 16, ejeX + 7), Math.min(h - 7, ejeY + 15));
        }
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Segoe UI", 12));
        gc.setFill(Color.web("#D1D7DB"));
        gc.fillText("x", w - 16, Math.min(Math.max(17, ejeY - 7), h - 15));
        gc.fillText("y", Math.min(Math.max(11, ejeX + 9), w - 20), 18);
    }

    private void lineasVerticales(GraphicsContext gc, double min, double max,
                                  double paso, double w, double h) {
        if (!(paso > 0) || !ok(min) || !ok(max)) return;
        long inicio = (long) Math.ceil(min / paso), fin = (long) Math.floor(max / paso);
        for (long i = inicio; i <= fin && i - inicio < 400; i++) {
            double px = graficaX(i * paso, w);
            if (px >= 0 && px <= w) gc.strokeLine(Math.floor(px) + .5, 0, Math.floor(px) + .5, h);
        }
    }

    private void lineasHorizontales(GraphicsContext gc, double min, double max,
                                    double paso, double w, double h) {
        if (!(paso > 0) || !ok(min) || !ok(max)) return;
        long inicio = (long) Math.ceil(min / paso), fin = (long) Math.floor(max / paso);
        for (long i = inicio; i <= fin && i - inicio < 400; i++) {
            double py = graficaY(i * paso, h);
            if (py >= 0 && py <= h) gc.strokeLine(0, Math.floor(py) + .5, w, Math.floor(py) + .5);
        }
    }

    private void trazarFuncion(GraphicsContext gc, DoubleUnaryOperator f, String color,
                               double grosor, double w, double h) {
        int muestras = Math.max(400, Math.min(1800, (int) (w * 1.3)));
        gc.setStroke(Color.web(color));
        gc.setLineWidth(grosor);
        gc.setLineDashes();
        gc.beginPath();
        boolean segmento = false;
        double pyAnterior = Double.NaN;
        for (int i = 0; i <= muestras; i++) {
            double px = w * i / muestras;
            double x = coordenadaX(px, w);
            double y;
            try { y = f.applyAsDouble(x); }
            catch (RuntimeException ex) { y = Double.NaN; }
            double py = graficaY(y, h);
            if (!ok(py) || Math.abs(py) > Math.max(50000, h * 60)) {
                segmento = false;
                continue;
            }
            // No conectar dos ramas a través de una asíntota vertical.
            if (!segmento || Math.abs(py - pyAnterior) > Math.max(500, 2.5 * h)) {
                gc.moveTo(px, py);
            } else {
                gc.lineTo(px, py);
            }
            pyAnterior = py;
            segmento = true;
        }
        gc.stroke();
    }

    private void dibujarIteraciones(GraphicsContext gc, double w, double h) {
        if (ultimoResultado == null || ultimoMetodo == null) return;
        List<Fila> filas = ultimoResultado.filas;
        if (filas.isEmpty()) return;

        if (ultimoMetodo == Metodo.PUNTO_FIJO) {
            gc.setStroke(Color.web(COLOR[4]));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4, 4);
            gc.beginPath();
            double prev = filas.get(0).x;
            if (ok(prev)) {
                gc.moveTo(graficaX(prev, w), graficaY(0, h));
                for (int i = 0; i < Math.min(65, filas.size()); i++) {
                    Fila fila = filas.get(i);
                    if (!ok(fila.x) || !ok(fila.gx)) break;
                    // Cobweb: vertical hasta g(x_n), luego horizontal a y = x.
                    gc.lineTo(graficaX(fila.x, w), graficaY(fila.gx, h));
                    gc.lineTo(graficaX(fila.gx, w), graficaY(fila.gx, h));
                }
                gc.stroke();
            }
            gc.setLineDashes();
        }

        gc.setFill(Color.web(COLOR[5]));
        int salto = Math.max(1, filas.size() / 1200);
        for (int i = 0; i < filas.size(); i += salto) {
            Fila fila = filas.get(i);
            double valor = ultimoMetodo.usaG() ? fila.gx : fila.fx;
            if (!ok(fila.x) || !ok(valor)) continue;
            double px = graficaX(fila.x, w), py = graficaY(valor, h);
            if (px >= -5 && px <= w + 5 && py >= -5 && py <= h + 5) {
                gc.fillOval(px - 3.5, py - 3.5, 7, 7);
            }
        }
    }

    private void dibujarRaiz(GraphicsContext gc, double raiz, double w, double h) {
        double px = graficaX(raiz, w), py = graficaY(0, h);
        if (px < -12 || px > w + 12 || py < -12 || py > h + 12) return;
        gc.setFill(Color.web("#0B141A"));
        gc.fillOval(px - 8, py - 8, 16, 16);
        gc.setStroke(Color.web(COLOR[6]));
        gc.setLineWidth(2.6);
        gc.strokeOval(px - 6.5, py - 6.5, 13, 13);
        gc.setFill(Color.web(COLOR[6]));
        gc.fillOval(px - 3.5, py - 3.5, 7, 7);
    }

    private void repintarCursor() {
        if (lienzoCursor == null) return;
        GraphicsContext gc = lienzoCursor.getGraphicsContext2D();
        double w = lienzoCursor.getWidth(), h = lienzoCursor.getHeight();
        gc.clearRect(0, 0, w, h);
        if (!cursorEnGrafica || arrastrando) return;
        dibujarCursor(gc, w, h);
    }

    private void dibujarCursor(GraphicsContext gc, double w, double h) {
        if (cursorPx < 0 || cursorPx > w || cursorPy < 0 || cursorPy > h) return;
        gc.setStroke(Color.web("#637D83", 0.70));
        gc.setLineDashes(4, 5);
        gc.setLineWidth(1);
        gc.strokeLine(cursorPx, 0, cursorPx, h);
        gc.strokeLine(0, cursorPy, w, cursorPy);
        gc.setLineDashes();
    }

    private void actualizarLeyenda() {
        if (leyenda == null || chkVerF == null) return;
        leyenda.getChildren().clear();
        if (chkVerF.isSelected()) leyenda.getChildren().add(itemLeyenda(COLOR[0], "f(x)", false));
        if (ultimoMetodo != null && ultimoMetodo.usaG() && chkVerG.isSelected()) {
            leyenda.getChildren().add(itemLeyenda(COLOR[1], "g(x)", false));
            leyenda.getChildren().add(itemLeyenda(COLOR[2], "y = x", true));
        }
        if (ultimoMetodo != null && chkVerIter.isSelected()) {
            if (ultimoMetodo == Metodo.PUNTO_FIJO) leyenda.getChildren().add(itemLeyenda(COLOR[4], "telaraña", true));
            leyenda.getChildren().add(itemLeyenda(COLOR[5], "iteraciones", false));
        }
        if (ultimoResultado != null && !ultimoResultado.fallo && ok(ultimoResultado.raiz)) {
            leyenda.getChildren().add(itemLeyenda(COLOR[6], "raíz", false));
        }
    }

    // ================================================= NAVEGACIÓN DEL GRÁFICO
    private void instalarNavegacionGrafico() {
        lienzoCursor.setOnMouseMoved(e -> {
            cursorEnGrafica = true;
            cursorPx = e.getX();
            cursorPy = e.getY();
            actualizarCoordenadas(e.getX(), e.getY());
            repintarCursor();
        });
        lienzoCursor.setOnMouseExited(e -> {
            cursorEnGrafica = false;
            if (lblCoordenadasGrafica != null) {
                lblCoordenadasGrafica.setText("Mover: arrastrar  ·  Zoom: rueda o +/−  ·  Doble clic: ajustar");
            }
            repintarCursor();
        });
        lienzoCursor.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            lienzoCursor.requestFocus();
            arrastrando = true;
            arrastreX = e.getX();
            arrastreY = e.getY();
        });
        lienzoCursor.setOnMouseDragged(e -> {
            if (!arrastrando) return;
            centroX -= (e.getX() - arrastreX) * unidadesPorPixel;
            centroY += (e.getY() - arrastreY) * unidadesPorPixel;
            arrastreX = e.getX();
            arrastreY = e.getY();
            cursorPx = e.getX();
            cursorPy = e.getY();
            actualizarCoordenadas(e.getX(), e.getY());
            dibujar();
        });
        lienzoCursor.setOnMouseReleased(e -> { arrastrando = false; repintarCursor(); });
        lienzoCursor.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (e.getClickCount() >= 2) {
                vistaInicializada = false;
                dibujar();
            } else if (lblPuntoGrafica != null) {
                mostrarPuntoCercano(e.getX(), e.getY());
            }
        });
        lienzoCursor.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            double factor = Math.exp(-Math.max(-500, Math.min(500, e.getDeltaY())) * 0.0022);
            zoomAlPunto(factor, e.getX(), e.getY());
            e.consume();
        });
        lienzoCursor.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ADD: case PLUS: case EQUALS:
                    zoomEnCentro(0.75); break;
                case SUBTRACT: case MINUS:
                    zoomEnCentro(1.0 / 0.75); break;
                case DIGIT0: case NUMPAD0:
                    vistaInicializada = false; dibujar(); break;
                case LEFT: centroX -= 55 * unidadesPorPixel; dibujar(); break;
                case RIGHT: centroX += 55 * unidadesPorPixel; dibujar(); break;
                case UP: centroY += 55 * unidadesPorPixel; dibujar(); break;
                case DOWN: centroY -= 55 * unidadesPorPixel; dibujar(); break;
                default: return;
            }
            e.consume();
        });
    }

    private void actualizarCoordenadas(double px, double py) {
        if (lblCoordenadasGrafica == null) return;
        double x = coordenadaX(px, lienzoGrafica.getWidth());
        double y = coordenadaY(py, lienzoGrafica.getHeight());
        lblCoordenadasGrafica.setText("Cursor: x = " + etiquetaEje(x, unidadesPorPixel * 0.01)
                + "     ·     y = " + etiquetaEje(y, unidadesPorPixel * 0.01));
    }

    private void mostrarPuntoCercano(double px, double py) {
        if (ultimoResultado == null || ultimoMetodo == null) return;
        double w = lienzoGrafica.getWidth(), h = lienzoGrafica.getHeight();
        double menorDist = 14 * 14;
        String descripcion = null;
        if (!ultimoResultado.fallo && ok(ultimoResultado.raiz)) {
            double x = graficaX(ultimoResultado.raiz, w), y = graficaY(0, h);
            double d = (px - x) * (px - x) + (py - y) * (py - y);
            if (d <= menorDist) {
                menorDist = d;
                descripcion = "Raíz aproximada  |  x = " + num(ultimoResultado.raiz)
                        + "  |  f(x) = " + num(ultimaF.applyAsDouble(ultimoResultado.raiz));
            }
        }
        if (chkVerIter.isSelected()) {
            for (Fila fila : ultimoResultado.filas) {
                double valor = ultimoMetodo.usaG() ? fila.gx : fila.fx;
                if (!ok(fila.x) || !ok(valor)) continue;
                double x = graficaX(fila.x, w), y = graficaY(valor, h);
                double d = (px - x) * (px - x) + (py - y) * (py - y);
                if (d < menorDist) {
                    menorDist = d;
                    descripcion = "Iteración " + fila.n + "  |  x = " + num(fila.x)
                            + "  |  f(x) = " + num(fila.fx)
                            + (ultimoMetodo.usaG() ? "  |  g(x) = " + num(fila.gx) : "");
                }
            }
        }
        lblPuntoGrafica.setText(descripcion != null ? descripcion
                : "Hacé clic cerca de una iteración o de la raíz para ver sus valores.");
    }

    private void zoomEnCentro(double factor) {
        if (lienzoGrafica == null) return;
        zoomAlPunto(factor, lienzoGrafica.getWidth() / 2.0, lienzoGrafica.getHeight() / 2.0);
    }

    private void zoomAlPunto(double factor, double px, double py) {
        if (lienzoGrafica == null || !vistaInicializada) return;
        double w = lienzoGrafica.getWidth(), h = lienzoGrafica.getHeight();
        if (w < 40 || h < 40) return;
        double nuevaEscala = unidadesPorPixel * factor;
        // Evitar la pérdida de precisión de double a zoom extremo.
        double precisionMinima = Math.max(1e-12, Math.max(Math.ulp(centroX), Math.ulp(centroY)) * 16);
        if (!ok(nuevaEscala) || nuevaEscala < precisionMinima || nuevaEscala > 1e10) return;
        double mundoX = coordenadaX(px, w), mundoY = coordenadaY(py, h);
        unidadesPorPixel = nuevaEscala;
        centroX = mundoX - (px - w / 2.0) * unidadesPorPixel;
        centroY = mundoY + (py - h / 2.0) * unidadesPorPixel;
        dibujar();
        if (cursorEnGrafica) actualizarCoordenadas(cursorPx, cursorPy);
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
        VBox v = new VBox(7);
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
            -fx-background-color: #111B21;
        }
        .label { -fx-text-fill: #E9EDEF; }
        .mono { -fx-font-family: "Consolas", "JetBrains Mono", "Menlo", monospace; }

        /* Paleta gris pizarra + azul acero: superficies sobrias, acentos accesibles. */
        /* ---------- cabecera ---------- */
        .header {
            -fx-background-color: linear-gradient(to right, #202C33, #111B21);
            -fx-border-color: #2A3942; -fx-border-width: 0 0 1 0;
            -fx-padding: 14 22 14 22;
        }
        .titulo { -fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: white; }
        .subtitulo { -fx-text-fill: #8696A0; -fx-font-size: 12px; }
        .badge {
            -fx-background-color: #2A3942; -fx-background-radius: 20;
            -fx-padding: 5 14 5 14; -fx-font-weight: bold; -fx-text-fill: #D1D7DB;
        }
        .badge-ok   { -fx-background-color: #123C33; -fx-text-fill: #25D366; }
        .badge-warn { -fx-background-color: #4A3922; -fx-text-fill: #F0B86E; }
        .badge-err  { -fx-background-color: #4A2630; -fx-text-fill: #FF8FA3; }

        /* ---------- panel izquierdo ---------- */
        .panel-izq, .panel-izq > .viewport { -fx-background-color: transparent; -fx-background: transparent; }
        .scroll-pane { -fx-background-color: transparent; -fx-padding: 0; }
        .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: #324650; -fx-background-radius: 6; }
        .scroll-bar .track, .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; }
        .corner { -fx-background-color: transparent; }

        .card {
            -fx-background-color: #202C33; -fx-background-radius: 14;
            -fx-border-color: #2A3942; -fx-border-radius: 14; -fx-padding: 14;
        }
        .card-titulo { -fx-text-fill: #53BDEB; -fx-font-size: 11px; -fx-font-weight: bold; }
        .campo-label { -fx-text-fill: #AEBAC1; -fx-font-size: 12px; }

        .text-field {
            -fx-background-color: #111B21; -fx-text-fill: #E9EDEF; -fx-prompt-text-fill: #8696A0;
            -fx-background-radius: 9; -fx-border-color: #324650; -fx-border-radius: 9;
            -fx-padding: 8 11 8 11; -fx-highlight-fill: #00A884;
        }
        .text-field:focused { -fx-border-color: #53BDEB; }
        .tecla-matematica { -fx-background-color: #2A3942; -fx-text-fill: #E5F1F8; -fx-background-radius: 7; -fx-padding: 6 8; }
        .tecla-matematica:hover { -fx-background-color: #005C4B; }

        .combo-box {
            -fx-background-color: #111B21; -fx-background-radius: 9;
            -fx-border-color: #324650; -fx-border-radius: 9;
        }
        .combo-box:focused { -fx-border-color: #53BDEB; }
        .combo-box .list-cell { -fx-text-fill: #E9EDEF; -fx-background-color: transparent; -fx-padding: 7 11 7 11; }
        .combo-box .arrow-button { -fx-background-color: transparent; }
        .combo-box .arrow { -fx-background-color: #D1D7DB; }
        .combo-box-popup .list-view { -fx-background-color: #202C33; -fx-border-color: #324650; -fx-background-radius: 9; -fx-border-radius: 9; }
        .combo-box-popup .list-cell { -fx-background-color: #202C33; -fx-text-fill: #E9EDEF; -fx-padding: 8 12 8 12; }
        .combo-box-popup .list-cell:hover { -fx-background-color: #30444C; }
        .combo-box-popup .list-cell:filled:selected { -fx-background-color: #005C4B; }

        .seg {
            -fx-background-color: #111B21; -fx-text-fill: #AEBAC1; -fx-font-weight: bold;
            -fx-border-color: #324650; -fx-padding: 8 10 8 10; -fx-cursor: hand;
        }
        .seg-izq { -fx-background-radius: 9 0 0 9; -fx-border-radius: 9 0 0 9; }
        .seg-der { -fx-background-radius: 0 9 9 0; -fx-border-radius: 0 9 9 0; -fx-border-width: 1 1 1 0; }
        .seg:selected { -fx-background-color: linear-gradient(to bottom, #53BDEB, #00A884); -fx-text-fill: white; -fx-border-color: transparent; }

        .check-box { -fx-text-fill: #E9EDEF; }
        .check-box .box { -fx-background-color: #111B21; -fx-border-color: #50616B; -fx-border-radius: 5; -fx-background-radius: 5; }
        .check-box:selected .box { -fx-background-color: #53BDEB; -fx-border-color: #53BDEB; }
        .check-box:selected .mark { -fx-background-color: #111B21; }
        .check-box:disabled { -fx-opacity: 0.4; }

        .boton-primario {
            -fx-background-color: linear-gradient(to bottom right, #25D366, #00A884);
            -fx-text-fill: #122B37; -fx-font-weight: bold; -fx-font-size: 14px;
            -fx-background-radius: 11; -fx-padding: 12 16 12 16; -fx-cursor: hand;
        }
        .boton-primario:hover { -fx-effect: dropshadow(gaussian, rgba(128,193,223,0.34), 16, 0.2, 0, 0); }
        .boton-primario:pressed { -fx-opacity: 0.85; }
        .boton-secundario {
            -fx-background-color: #2A3942; -fx-text-fill: #E9EDEF; -fx-background-radius: 8;
            -fx-padding: 6 14 6 14; -fx-cursor: hand;
        }
        .boton-secundario:hover { -fx-background-color: #005C4B; }

        /* ---------- resultados ---------- */
        .stat { -fx-background-color: #202C33; -fx-background-radius: 10; -fx-border-color: #2A3942; -fx-border-radius: 10; -fx-padding: 7 11 7 11; }
        .stat-titulo { -fx-text-fill: #8696A0; -fx-font-size: 9.5px; -fx-font-weight: bold; }
        .stat-valor { -fx-text-fill: #E9EDEF; -fx-font-size: 13px; -fx-font-weight: bold; }
        .stat-destacado { -fx-text-fill: #25D366; }

        .estado { -fx-background-radius: 10; -fx-padding: 10 14 10 14; -fx-border-radius: 10; -fx-font-size: 13px; }
        .estado-info { -fx-background-color: #202C33; -fx-text-fill: #D1D7DB; -fx-border-color: #2A3942; }
        .estado-ok   { -fx-background-color: #123C33; -fx-text-fill: #25D366; -fx-border-color: #1F6F5F; }
        .estado-warn { -fx-background-color: #4A3922; -fx-text-fill: #F0B86E; -fx-border-color: #8D6B3C; }
        .estado-err  { -fx-background-color: #4A2630; -fx-text-fill: #FF8FA3; -fx-border-color: #B84A62; }

        /* ---------- pestañas ---------- */
        .tab-pane > .tab-header-area > .tab-header-background { -fx-background-color: transparent; }
        .tab-pane > .tab-header-area { -fx-padding: 0 0 0 4; }
        .tab { -fx-background-color: #1F2C34; -fx-background-radius: 10 10 0 0; -fx-padding: 6 14 6 14; }
        .tab:selected { -fx-background-color: #202C33; }
        .tab .tab-label { -fx-text-fill: #8696A0; -fx-font-weight: bold; }
        .tab:selected .tab-label { -fx-text-fill: white; }
        .tab-pane > .tab-content-area { -fx-background-color: #202C33; -fx-background-radius: 0 12 12 12; -fx-border-color: #2A3942; -fx-border-radius: 0 12 12 12; }
        .tab-pane:top > .tab-header-area > .headers-region > .tab { -fx-focus-color: transparent; -fx-faint-focus-color: transparent; }
        .leyenda-texto { -fx-text-fill: #D1D7DB; -fx-font-size: 12px; }

        /* ---------- tabla ---------- */
        .table-view {
            -fx-background-color: #111B21; -fx-control-inner-background: #111B21;
            -fx-control-inner-background-alt: #182229; -fx-table-cell-border-color: transparent;
            -fx-background-radius: 10; -fx-border-color: #2A3942; -fx-border-radius: 10;
            -fx-padding: 0;
        }
        .table-view .column-header-background { -fx-background-color: #2A3942; -fx-background-radius: 10 10 0 0; }
        .table-view .column-header, .table-view .filler {
            -fx-background-color: #2A3942; -fx-size: 38; -fx-border-color: transparent;
        }
        .table-view .column-header .label { -fx-text-fill: #E9EDEF; -fx-font-weight: bold; -fx-alignment: CENTER; }
        .table-row-cell { -fx-background-color: -fx-control-inner-background; -fx-cell-size: 36; -fx-border-color: transparent; }
        .table-row-cell:odd { -fx-background-color: -fx-control-inner-background-alt; }
        .table-row-cell:selected { -fx-background-color: #005C4B; }
        .table-cell { -fx-text-fill: #E9EDEF; -fx-alignment: CENTER-RIGHT; -fx-font-family: "Consolas", "JetBrains Mono", monospace; -fx-font-size: 12px; -fx-padding: 0 10 0 8; }
        .table-cell.centro { -fx-alignment: CENTER; -fx-text-fill: #53BDEB; -fx-font-weight: bold; }
        .table-view .placeholder .label { -fx-text-fill: #8696A0; }

        /* ---------- gráfica ---------- */
        .chart { -fx-background-color: transparent; }
        .chart-title { -fx-text-fill: #E9EDEF; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: "Consolas", monospace; }
        .chart-content { -fx-padding: 6; }
        .chart-plot-background { -fx-background-color: #111B21; }
        .chart-vertical-grid-lines, .chart-horizontal-grid-lines { -fx-stroke: #2A3942; -fx-stroke-width: 1; }
        .chart-alternative-row-fill, .chart-alternative-column-fill { -fx-fill: transparent; -fx-stroke: transparent; }
        .axis { -fx-tick-label-fill: #AEBAC1; -fx-font-size: 11px; }
        .axis-label { -fx-text-fill: #AEBAC1; }
        .axis-tick-mark, .axis-minor-tick-mark { -fx-stroke: #50616B; }
        .axis:top, .axis:bottom, .axis:left, .axis:right { -fx-border-color: #50616B; }

        .chart-series-line { -fx-stroke-width: 2.4px; }
        .chart-line-symbol { -fx-background-color: transparent; -fx-padding: 0; }

        .default-color0.chart-series-line { -fx-stroke: #53BDEB; -fx-stroke-width: 2.8px; }
        .default-color1.chart-series-line { -fx-stroke: #F0B86E; -fx-stroke-width: 2.8px; }
        .default-color2.chart-series-line { -fx-stroke: #D1D7DB; -fx-stroke-width: 1.6px; -fx-stroke-dash-array: 7 6; }
        .default-color3.chart-series-line { -fx-stroke: #8696A0; -fx-stroke-width: 1.4px; }
        .default-color4.chart-series-line { -fx-stroke: #FF8FA3; -fx-stroke-width: 1.3px; }
        .default-color5.chart-series-line { -fx-stroke: transparent; }
        .default-color6.chart-series-line { -fx-stroke: transparent; }

        .default-color5.chart-line-symbol {
            -fx-background-color: #D8A6FF, #111B21; -fx-background-insets: 0, 2;
            -fx-background-radius: 8px; -fx-padding: 5px;
        }
        .default-color6.chart-line-symbol {
            -fx-background-color: #25D366, #111B21, #25D366; -fx-background-insets: 0, 2, 4;
            -fx-background-radius: 10px; -fx-padding: 7px;
            -fx-effect: dropshadow(gaussian, rgba(116,200,180,0.50), 12, 0.3, 0, 0);
        }
        .tooltip { -fx-background-color: #2A3942; -fx-text-fill: white; -fx-font-family: "Consolas", monospace; -fx-background-radius: 8; }
        .desarrollo-panel { -fx-background-color: transparent; }
        .desarrollo-bloque {
            -fx-background-color: #202C33; -fx-background-radius: 12;
            -fx-border-color: #2A3942; -fx-border-radius: 12; -fx-padding: 14;
        }
        .desarrollo-titulo { -fx-text-fill: #53BDEB; -fx-font-size: 14px; -fx-font-weight: bold; }
        .desarrollo-linea { -fx-text-fill: #E9EDEF; -fx-font-size: 13px; -fx-padding: 2 0 2 4; }
        .table-view { -fx-background-color: #111B21; -fx-border-color: #2A3942; }
        .table-view .column-header-background { -fx-background-color: #2A3942; }
        .table-view .column-header, .table-view .filler { -fx-background-color: #2A3942; -fx-size: 38px; }
        .table-view .column-header .label { -fx-text-fill: #E9EDEF; -fx-font-weight: bold; }
        .table-row-cell { -fx-background-color: #182229; -fx-border-color: #2A3942; -fx-table-cell-border-color: #2A3942; }
        .table-row-cell:odd { -fx-background-color: #202C33; }
        .table-cell { -fx-text-fill: #E9EDEF; -fx-alignment: CENTER; -fx-padding: 7 10 7 10; }

        /* ===== Ajustes finales: compacto + WhatsApp Web oscuro ===== */
        .root { -fx-background-color: #111B21; }
        .header { -fx-background-color: linear-gradient(to right, #202C33, #111B21); -fx-border-color: #2A3942; -fx-padding: 11 20 11 20; }
        .card { -fx-background-color: #202C33; -fx-border-color: #2A3942; -fx-padding: 9 10 9 10; -fx-background-radius: 12; -fx-border-radius: 12; }
        .card-titulo { -fx-text-fill: #00A884; }
        .campo-label { -fx-text-fill: #AEBAC1; -fx-font-size: 11.5px; }
        .text-field { -fx-background-color: #111B21; -fx-text-fill: #E9EDEF; -fx-prompt-text-fill: #8696A0; -fx-border-color: #324650; -fx-padding: 6 9 6 9; }
        .text-field:focused { -fx-border-color: #00A884; }
        .combo-box { -fx-background-color: #111B21; -fx-border-color: #324650; }
        .combo-box:focused { -fx-border-color: #00A884; }
        .combo-box-popup .list-view, .combo-box-popup .list-cell { -fx-background-color: #202C33; }
        .combo-box-popup .list-cell:hover { -fx-background-color: #30444C; }
        .combo-box-popup .list-cell:filled:selected { -fx-background-color: #005C4B; }
        .tecla-matematica { -fx-background-color: #2A3942; -fx-text-fill: #E9EDEF; -fx-padding: 4 3 4 3; }
        .tecla-matematica:hover { -fx-background-color: #005C4B; }
        .teclado-plegable { -fx-background-color: #182229; -fx-border-color: #324650; -fx-border-radius: 8; }
        .teclado-plegable > .title { -fx-background-color: #182229; -fx-padding: 6 9 6 9; }
        .teclado-plegable > .title > .text { -fx-fill: #D1D7DB; }
        .teclado-plegable > .content { -fx-background-color: #202C33; -fx-padding: 7; }
        .boton-primario { -fx-background-color: linear-gradient(to bottom right, #00A884, #25D366); -fx-text-fill: #0B141A; -fx-padding: 9 14 9 14; }
        .boton-primario:hover { -fx-effect: dropshadow(gaussian, rgba(37,211,102,0.3), 14, 0.2, 0, 0); }
        .stat-destacado { -fx-text-fill: #25D366; }
        .check-box:selected .box { -fx-background-color: #00A884; -fx-border-color: #00A884; }
        .check-box:selected .mark { -fx-background-color: #111B21; }
        .estado-ok, .badge-ok { -fx-text-fill: #25D366; }
        .chart-plot-background { -fx-background-color: #111B21; }
        .chart-vertical-grid-lines, .chart-horizontal-grid-lines { -fx-stroke: #2A3942; }
        .default-color0.chart-series-line { -fx-stroke: #53BDEB; }
        .default-color1.chart-series-line { -fx-stroke: #00A884; }
        .default-color2.chart-series-line { -fx-stroke: #8696A0; }
        .default-color3.chart-series-line { -fx-stroke: #7AE3C3; }
        .default-color4.chart-series-line { -fx-stroke: #F0B86E; }
        .default-color5.chart-line-symbol { -fx-background-color: #D8A6FF, #111B21; }
        .default-color6.chart-line-symbol { -fx-background-color: #25D366, #111B21, #25D366; }
        .desarrollo-titulo { -fx-text-fill: #53BDEB; }
        .table-row-cell:selected { -fx-background-color: #005C4B; }
        /* ===== Graficador cartesiano con cuadrícula adaptable ===== */
        .plano-cartesiano {
            -fx-background-color: #0B141A;
            -fx-background-radius: 10;
            -fx-border-color: #354951;
            -fx-border-width: 1;
            -fx-border-radius: 10;
        }
        .grafica-formula {
            -fx-font-family: "Consolas", "JetBrains Mono", monospace;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-text-fill: #D1D7DB;
        }
        .grafica-barra { -fx-padding: 0; }
        .grafica-boton {
            -fx-background-color: #263A42;
            -fx-text-fill: #E9EDEF;
            -fx-background-radius: 7;
            -fx-border-color: #3B555C;
            -fx-border-radius: 7;
            -fx-font-size: 12px;
            -fx-font-weight: bold;
            -fx-padding: 5 10 5 10;
            -fx-cursor: hand;
        }
        .grafica-boton:hover { -fx-background-color: #005C4B; -fx-border-color: #00A884; }
        /* Área derecha compacta: más altura para la gráfica sin cortar números. */
        .stat { -fx-padding: 4 8 4 8; -fx-background-radius: 8; -fx-border-radius: 8; }
        .stat-titulo { -fx-font-size: 9px; }
        .stat-valor { -fx-font-size: 12px; }
        .estado { -fx-padding: 5 9 5 9; -fx-font-size: 11px; -fx-background-radius: 7; }
        .tab { -fx-padding: 3 10 3 10; }
        .grafica-boton { -fx-padding: 3 8 3 8; -fx-font-size: 11px; }
        .grafica-formula { -fx-font-size: 11px; }
        .grafica-coordenadas { -fx-font-size: 10px; }
        .leyenda-texto { -fx-font-size: 10px; }
        .grafica-coordenadas {
            -fx-text-fill: #7AE3C3;
            -fx-font-family: "Consolas", "JetBrains Mono", monospace;
            -fx-font-size: 11px;
        }
        """;

    // ================================================================== MAIN
    public static void main(String[] args) {
        launch(args);
    }
}
