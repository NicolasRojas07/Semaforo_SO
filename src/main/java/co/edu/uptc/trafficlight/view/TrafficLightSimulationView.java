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

import java.util.Map;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TrafficLightSimulationView implements Observer {
    private TrafficController controller;
    private Stage primaryStage;
    private BorderPane root;
    private Pane intersectionPane;
    private VBox controlPanel;
    private TextArea logArea;

    private Map<String, Circle[]> trafficLightElements;
    private Label[] vehicleCountLabels;

    // Estadísticas
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

        createAnimatedIntersectionView();
        root.setCenter(intersectionPane);

        createEnhancedControlPanel();
        root.setRight(controlPanel);

        createSafetyLogPanel();
        root.setBottom(logArea);

        Scene scene = new Scene(root, 1200, 800);

        primaryStage.setTitle("🚦 Simulación de Semáforo - UPTC (Completo)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createAnimatedIntersectionView() {
        intersectionPane = new Pane();
        intersectionPane.setPrefSize(800, 600);
        intersectionPane.setStyle("-fx-background-color: #2c3e50;");

        // calles
        Rectangle verticalStreet = new Rectangle(350, 0, 100, 600);
        verticalStreet.setFill(Color.DARKGRAY);
        Rectangle horizontalStreet = new Rectangle(0, 300, 800, 100);
        horizontalStreet.setFill(Color.DARKGRAY);

        Line verticalLine = new Line(400, 0, 400, 600);
        verticalLine.setStroke(Color.YELLOW);
        verticalLine.getStrokeDashArray().addAll(10.0, 5.0);
        Line horizontalLine = new Line(0, 350, 800, 350);
        horizontalLine.setStroke(Color.YELLOW);
        horizontalLine.getStrokeDashArray().addAll(10.0, 5.0);

        intersectionPane.getChildren().addAll(verticalStreet, horizontalStreet, verticalLine, horizontalLine);

        // intersección central
        Rectangle intersection = new Rectangle(350, 300, 100, 100);
        intersection.setFill(Color.LIGHTGRAY);
        intersection.setStroke(Color.YELLOW);
        intersection.setStrokeWidth(3);
        intersectionPane.getChildren().add(intersection);

        // semáforos visuales
        createTrafficLightDisplay("NORTH", 375, 250);
        createTrafficLightDisplay("SOUTH", 375, 450);
        createTrafficLightDisplay("EAST", 500, 325);
        createTrafficLightDisplay("WEST", 250, 325);

        Text safeZoneLabel = new Text(360, 290, "ZONA SEGURA");
        safeZoneLabel.setFill(Color.WHITE);
        safeZoneLabel.setFont(Font.font("Arial", 10));
        intersectionPane.getChildren().add(safeZoneLabel);

        Text maxVehicles = new Text(360, 440, "Máx: 2 vehículos");
        maxVehicles.setFill(Color.YELLOW);
        maxVehicles.setFont(Font.font("Arial", 8));
        intersectionPane.getChildren().add(maxVehicles);
    }

    private void createTrafficLightDisplay(String direction, double x, double y) {
        VBox lightBox = new VBox(3);
        lightBox.setLayoutX(x);
        lightBox.setLayoutY(y);

        Circle red = new Circle(8); Circle yellow = new Circle(8); Circle green = new Circle(8);
        red.setFill(Color.DARKRED); yellow.setFill(Color.DARKGOLDENROD); green.setFill(Color.DARKGREEN);
        red.setStroke(Color.WHITE); yellow.setStroke(Color.WHITE); green.setStroke(Color.WHITE);

        VBox lights = new VBox(2);
        lights.setAlignment(Pos.CENTER);
        lights.setStyle("-fx-background-color: black; -fx-padding: 5px; -fx-background-radius: 5px;");
        lights.getChildren().addAll(red, yellow, green);
        lightBox.getChildren().add(lights);
        intersectionPane.getChildren().add(lightBox);

        if (trafficLightElements == null) trafficLightElements = new java.util.HashMap<>();
        trafficLightElements.put(direction, new Circle[]{red, yellow, green});
    }

    private void createEnhancedControlPanel() {
        controlPanel = new VBox(15);
        controlPanel.setAlignment(Pos.TOP_CENTER);
        controlPanel.setStyle("-fx-background-color: #34495e; -fx-padding: 20px;");
        controlPanel.setPrefWidth(350);

        Label titleLabel = new Label("🚦 Control de Tráfico Seguro");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button startButton = new Button("▶️ Iniciar Simulación");
        Button stopButton = new Button("⏹️ Detener Simulación");
        Button resetButton = new Button("🔄 Reiniciar");

        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> stopSimulation());
        resetButton.setOnAction(e -> resetSimulation());

        styleButton(startButton, "#27ae60");
        styleButton(stopButton, "#e74c3c");
        styleButton(resetButton, "#f39c12");

        VBox safetyStatsBox = createSafetyStatsPanel();
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

        Label[] statLabels = {totalVehiclesLabel, safeCrossingsLabel, accidentsPreventedLabel,
                currentInIntersectionLabel, maxConcurrentLabel};

        for (Label label : statLabels) {
            label.setTextFill(Color.LIGHTGRAY);
            label.setStyle("-fx-font-family: 'Courier New';");
        }

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
        String[] directions = {"NORTE", "SUR", "ESTE", "OESTE"};

        for (int i = 0; i < directions.length; i++) {
            vehicleCountLabels[i] = new Label(directions[i] + ": 0 esperando");
            vehicleCountLabels[i].setTextFill(Color.LIGHTGRAY);
            vehicleCountLabels[i].setStyle("-fx-font-family: 'Courier New';");
        }

        trafficBox.getChildren().add(trafficTitle);
        for (Label label : vehicleCountLabels) trafficBox.getChildren().add(label);

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

        // Limpiar sólo nodos dinámicos (los vehiculos usamos userData = Integer id)
        intersectionPane.getChildren().removeIf(node -> node.getUserData() instanceof Integer);

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
        if (trafficLightElements == null) return;
        for (Map.Entry<String, TrafficLight> entry : controller.getTrafficLights().entrySet()) {
            String direction = entry.getKey();
            TrafficLight light = entry.getValue();
            Circle[] circles = trafficLightElements.get(direction);

            // Reset
            circles[0].setFill(Color.DARKRED);
            circles[1].setFill(Color.DARKGOLDENROD);
            circles[2].setFill(Color.DARKGREEN);

            switch (light.getCurrentState()) {
                case RED: circles[0].setFill(Color.RED); break;
                case YELLOW: circles[1].setFill(Color.YELLOW); break;
                case GREEN: circles[2].setFill(Color.LIME); break;
            }
        }
    }

    private void updateVehicleDisplay() {
        // eliminar nodos dinámicos previos (vehículos y etiquetas) -> userData = Integer (vehicleId)
        intersectionPane.getChildren().removeIf(node -> node.getUserData() instanceof Integer);

        List<Vehicle> vehicles = controller.getActiveVehicles();
        for (Vehicle v : vehicles) {
            if (v.getState() == Vehicle.VehicleState.WAITING) continue;

            Text vehicleIcon = new Text(v.getX(), v.getY(), v.getVehicleType());
            vehicleIcon.setFont(Font.font(20));
            vehicleIcon.setUserData(v.getId()); // marcar como dinámico

            switch (v.getState()) {
                case APPROACHING: vehicleIcon.setFill(Color.YELLOW); break;
                case CROSSING: vehicleIcon.setFill(Color.RED); break;
                case CROSSED: vehicleIcon.setFill(Color.LIGHTGREEN); break;
            }

            intersectionPane.getChildren().add(vehicleIcon);

            // letra de movimiento (S/L/R) sobre el vehículo
            String m = v.getMovementType().toString().substring(0,1);
            Text movementLabel = new Text(v.getX(), v.getY() - 12, m);
            movementLabel.setFont(Font.font(10));
            movementLabel.setFill(Color.WHITE);
            movementLabel.setUserData(v.getId()); // también dinámico
            intersectionPane.getChildren().add(movementLabel);
        }
    }

    private void updateStatistics() {
        totalVehiclesLabel.setText("Total generados: " + controller.getTotalVehiclesGenerated());
        safeCrossingsLabel.setText("Cruces seguros: " + controller.getVehiclesCrossedSafely());
        accidentsPreventedLabel.setText("Accidentes prevenidos: " + controller.getAccidentsPrevented());
        currentInIntersectionLabel.setText("En intersección: " + controller.getCurrentVehiclesInIntersection() + "/2");
        maxConcurrentLabel.setText("Máximo concurrente: " + controller.getMaxConcurrentInIntersection());

        String[] directions = {"NORTH", "SOUTH", "EAST", "WEST"};
        for (int i = 0; i < directions.length; i++) {
            String dir = directions[i];
            long waitingCount = controller.getActiveVehicles().stream()
                    .filter(v -> v.getDirection().equals(dir) && v.getState() == Vehicle.VehicleState.WAITING)
                    .count();
            vehicleCountLabels[i].setText(dir + ": " + waitingCount + " esperando");
        }
    }
}
