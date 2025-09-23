// TrafficLightSimulationView.java - VERSIÓN MEJORADA CON CARROS ANIMADOS
package co.edu.uptc.trafficlight.view;

import co.edu.uptc.trafficlight.business.TrafficController;
import co.edu.uptc.trafficlight.model.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.Observable;
import java.util.Observer;
import java.util.Map;
import java.util.List;

public class TrafficLightSimulationView implements Observer {
    private TrafficController controller;
    private Stage primaryStage;
    private BorderPane root;
    private Pane intersectionPane; // Cambiado a Pane para posicionamiento absoluto
    private VBox controlPanel;
    private TextArea logArea;

    // Elementos visuales
    private Map<String, Circle[]> trafficLightElements;
    private Label[] vehicleCountLabels;

    // Estadísticas mejoradas
    private Label totalVehiclesLabel;
    private Label safeCrossingsLabel;
    private Label accidentsPreventedLabel;
    private Label currentInIntersectionLabel;
    private Label maxConcurrentLabel;

    public TrafficLightSimulationView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.controller = new TrafficController();
        this.controller.addObserver(this);

        initializeUI();
    }

    private void initializeUI() {
        root = new BorderPane();

        // Panel central - Intersección con animación
        createAnimatedIntersectionView();
        root.setCenter(intersectionPane);

        // Panel de control con estadísticas mejoradas
        createEnhancedControlPanel();
        root.setRight(controlPanel);

        // Panel de logs de seguridad
        createSafetyLogPanel();
        root.setBottom(logArea);

        Scene scene = new Scene(root, 1200, 800);

        primaryStage.setTitle("🚦 Simulación de Semáforo con Prevención de Accidentes - UPTC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createAnimatedIntersectionView() {
        intersectionPane = new Pane();
        intersectionPane.setPrefSize(800, 600);
        intersectionPane.setStyle("-fx-background-color: #2c3e50;");

        // Crear calles
        createStreets();

        // Crear intersección central
        Rectangle intersection = new Rectangle(350, 300, 100, 100);
        intersection.setFill(Color.LIGHTGRAY);
        intersection.setStroke(Color.YELLOW);
        intersection.setStrokeWidth(3);
        intersectionPane.getChildren().add(intersection);

        // Crear semáforos visuales
        createTrafficLightDisplay("NORTH", 375, 250);
        createTrafficLightDisplay("SOUTH", 375, 450);
        createTrafficLightDisplay("EAST", 500, 325);
        createTrafficLightDisplay("WEST", 250, 325);

        // Agregar texto de zona segura
        Text safeZoneLabel = new Text(360, 290, "ZONA SEGURA");
        safeZoneLabel.setFill(Color.WHITE);
        safeZoneLabel.setFont(Font.font("Arial", 10));
        intersectionPane.getChildren().add(safeZoneLabel);

        Text maxVehicles = new Text(360, 440, "Máx: 2 vehículos");
        maxVehicles.setFill(Color.YELLOW);
        maxVehicles.setFont(Font.font("Arial", 8));
        intersectionPane.getChildren().add(maxVehicles);
    }

    private void createStreets() {
        // Calle vertical (Norte-Sur)
        Rectangle verticalStreet = new Rectangle(350, 0, 100, 600);
        verticalStreet.setFill(Color.DARKGRAY);

        // Calle horizontal (Este-Oeste)
        Rectangle horizontalStreet = new Rectangle(0, 300, 800, 100);
        horizontalStreet.setFill(Color.DARKGRAY);

        // Líneas divisorias
        Line verticalLine = new Line(400, 0, 400, 600);
        verticalLine.setStroke(Color.YELLOW);
        verticalLine.getStrokeDashArray().addAll(10.0, 5.0);

        Line horizontalLine = new Line(0, 350, 800, 350);
        horizontalLine.setStroke(Color.YELLOW);
        horizontalLine.getStrokeDashArray().addAll(10.0, 5.0);

        intersectionPane.getChildren().addAll(verticalStreet, horizontalStreet, verticalLine, horizontalLine);
    }

    private void createTrafficLightDisplay(String direction, double x, double y) {
        VBox lightBox = new VBox(3);
        lightBox.setLayoutX(x);
        lightBox.setLayoutY(y);

        // Crear las tres luces del semáforo
        Circle redLight = new Circle(8);
        Circle yellowLight = new Circle(8);
        Circle greenLight = new Circle(8);

        redLight.setFill(Color.DARKRED);
        yellowLight.setFill(Color.DARKGOLDENROD);
        greenLight.setFill(Color.DARKGREEN);

        redLight.setStroke(Color.WHITE);
        yellowLight.setStroke(Color.WHITE);
        greenLight.setStroke(Color.WHITE);

        VBox lights = new VBox(2);
        lights.setAlignment(Pos.CENTER);
        lights.setStyle("-fx-background-color: black; -fx-padding: 5px; -fx-background-radius: 5px;");
        lights.getChildren().addAll(redLight, yellowLight, greenLight);

        lightBox.getChildren().add(lights);
        intersectionPane.getChildren().add(lightBox);

        // Almacenar referencia
        if (trafficLightElements == null) {
            trafficLightElements = new java.util.HashMap<>();
        }
        trafficLightElements.put(direction, new Circle[]{redLight, yellowLight, greenLight});
    }

    private void createEnhancedControlPanel() {
        controlPanel = new VBox(15);
        controlPanel.setAlignment(Pos.TOP_CENTER);
        controlPanel.setStyle("-fx-background-color: #34495e; -fx-padding: 20px;");
        controlPanel.setPrefWidth(350);

        Label titleLabel = new Label("🚦 Control de Tráfico Seguro");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Botones de control
        Button startButton = new Button("▶️ Iniciar Simulación");
        Button stopButton = new Button("⏹️ Detener Simulación");
        Button resetButton = new Button("🔄 Reiniciar");

        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> stopSimulation());
        resetButton.setOnAction(e -> resetSimulation());

        styleButton(startButton, "#27ae60");
        styleButton(stopButton, "#e74c3c");
        styleButton(resetButton, "#f39c12");

        // Panel de estadísticas de seguridad
        VBox safetyStatsBox = createSafetyStatsPanel();

        // Panel de estado del semáforo
        VBox trafficStatsBox = createTrafficStatsPanel();

        controlPanel.getChildren().addAll(titleLabel, startButton, stopButton, resetButton,
                safetyStatsBox, trafficStatsBox);
    }

    private VBox createSafetyStatsPanel() {
        VBox safetyBox = new VBox(8);
        safetyBox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 15px; -fx-background-radius: 10px;");

        Label safetyTitle = new Label("🛡️ Estadísticas de Seguridad");
        safetyTitle.setTextFill(Color.WHITE);
        safetyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        totalVehiclesLabel = new Label("Total generados: 0");
        safeCrossingsLabel = new Label("Cruces seguros: 0");
        accidentsPreventedLabel = new Label("Accidentes prevenidos: 0");
        currentInIntersectionLabel = new Label("En intersección: 0/2");
        maxConcurrentLabel = new Label("Máximo concurrente: 0");

        // Estilo para estadísticas
        Label[] statLabels = {totalVehiclesLabel, safeCrossingsLabel, accidentsPreventedLabel,
                currentInIntersectionLabel, maxConcurrentLabel};

        for (Label label : statLabels) {
            label.setTextFill(Color.LIGHTGRAY);
            label.setStyle("-fx-font-family: 'Courier New';");
        }

        // Indicador de seguridad
        Label safetyIndicator = new Label("✅ Sistema Seguro");
        safetyIndicator.setTextFill(Color.LIGHTGREEN);
        safetyIndicator.setStyle("-fx-font-weight: bold;");

        safetyBox.getChildren().addAll(safetyTitle, totalVehiclesLabel, safeCrossingsLabel,
                accidentsPreventedLabel, currentInIntersectionLabel,
                maxConcurrentLabel, safetyIndicator);
        return safetyBox;
    }

    private VBox createTrafficStatsPanel() {
        VBox trafficBox = new VBox(8);
        trafficBox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 15px; -fx-background-radius: 10px;");

        Label trafficTitle = new Label("🚗 Estado del Tráfico");
        trafficTitle.setTextFill(Color.WHITE);
        trafficTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        vehicleCountLabels = new Label[4];
        String[] directions = {"Norte", "Sur", "Este", "Oeste"};

        for (int i = 0; i < directions.length; i++) {
            vehicleCountLabels[i] = new Label(directions[i] + ": 0 esperando");
            vehicleCountLabels[i].setTextFill(Color.LIGHTGRAY);
            vehicleCountLabels[i].setStyle("-fx-font-family: 'Courier New';");
        }

        trafficBox.getChildren().add(trafficTitle);
        for (Label label : vehicleCountLabels) {
            trafficBox.getChildren().add(label);
        }

        return trafficBox;
    }

    private void styleButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-min-width: 200px; -fx-min-height: 35px; " +
                "-fx-background-radius: 5px;");
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));
    }

    private void createSafetyLogPanel() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; " +
                "-fx-font-family: 'Courier New';");
        logArea.appendText("🚦 Sistema de Control de Tráfico Seguro Iniciado\n");
        logArea.appendText("🛡️ Semáforos configurados para prevenir accidentes\n");
        logArea.appendText("📋 Presione 'Iniciar Simulación' para comenzar\n");
    }

    private void startSimulation() {
        controller.startSimulation();
        logArea.appendText("▶️ Simulación iniciada - Monitoreando seguridad...\n");
    }

    private void stopSimulation() {
        controller.stopSimulation();
        logArea.appendText("⏹️ Simulación detenida\n");
    }

    private void resetSimulation() {
        controller.stopSimulation();
        controller = new TrafficController();
        controller.addObserver(this);

        // Limpiar vehículos de la vista
        intersectionPane.getChildren().removeIf(node ->
                node instanceof Text && ((Text) node).getText().matches("[🚗🚙🚕🚐🚌]"));

        logArea.clear();
        logArea.appendText("🔄 Sistema reiniciado\n");
    }

    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater(() -> {
            updateTrafficLights();
            updateVehicleDisplay();
            updateStatistics();
        });
    }

    private void updateTrafficLights() {
        for (Map.Entry<String, TrafficLight> entry : controller.getTrafficLights().entrySet()) {
            String direction = entry.getKey();
            TrafficLight light = entry.getValue();
            Circle[] circles = trafficLightElements.get(direction);

            // Resetear colores
            circles[0].setFill(Color.DARKRED);
            circles[1].setFill(Color.DARKGOLDENROD);
            circles[2].setFill(Color.DARKGREEN);

            // Activar luz correspondiente
            switch (light.getCurrentState()) {
                case RED:
                    circles[0].setFill(Color.RED);
                    break;
                case YELLOW:
                    circles[1].setFill(Color.YELLOW);
                    break;
                case GREEN:
                    circles[2].setFill(Color.LIME);
                    break;
            }
        }
    }

    private void updateVehicleDisplay() {
        // Remover vehículos anteriores
        intersectionPane.getChildren().removeIf(node ->
                node instanceof Text && ((Text) node).getText().matches("[🚗🚙🚕🚐🚌]"));

        // Agregar vehículos actuales
        List<Vehicle> vehicles = controller.getActiveVehicles();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getState() != Vehicle.VehicleState.WAITING) {
                Text vehicleIcon = new Text(vehicle.getX(), vehicle.getY(), vehicle.getVehicleType());
                vehicleIcon.setFont(Font.font(20));

                // Color según estado
                switch (vehicle.getState()) {
                    case APPROACHING:
                        vehicleIcon.setFill(Color.YELLOW);
                        break;
                    case CROSSING:
                        vehicleIcon.setFill(Color.RED); // Rojo para mostrar que está en zona crítica
                        break;
                    case CROSSED:
                        vehicleIcon.setFill(Color.LIGHTGREEN);
                        break;
                }

                intersectionPane.getChildren().add(vehicleIcon);
            }
        }
    }

    private void updateStatistics() {
        // Actualizar estadísticas de seguridad
        totalVehiclesLabel.setText("Total generados: " + controller.getTotalVehiclesGenerated());
        safeCrossingsLabel.setText("Cruces seguros: " + controller.getVehiclesCrossedSafely());
        accidentsPreventedLabel.setText("Accidentes prevenidos: " + controller.getAccidentsPrevented());
        currentInIntersectionLabel.setText("En intersección: " + controller.getCurrentVehiclesInIntersection() + "/2");
        maxConcurrentLabel.setText("Máximo concurrente: " + controller.getMaxConcurrentInIntersection());

        // Actualizar contadores por dirección
        String[] directions = {"NORTH", "SOUTH", "EAST", "WEST"};
        for (int i = 0; i < directions.length; i++) {
            String direction = directions[i];
            long waitingCount = controller.getActiveVehicles().stream()
                    .filter(v -> v.getDirection().equals(direction) &&
                            v.getState() == Vehicle.VehicleState.WAITING)
                    .count();
            vehicleCountLabels[i].setText(directions[i] + ": " + waitingCount + " esperando");
        }
    }
}
