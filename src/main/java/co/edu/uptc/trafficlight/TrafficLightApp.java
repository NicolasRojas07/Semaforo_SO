// TrafficLightApp.java - Aplicación principal
package co.edu.uptc.trafficlight;

import co.edu.uptc.trafficlight.view.TrafficLightSimulationView;
import javafx.application.Application;
import javafx.stage.Stage;

public class TrafficLightApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        new TrafficLightSimulationView(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
