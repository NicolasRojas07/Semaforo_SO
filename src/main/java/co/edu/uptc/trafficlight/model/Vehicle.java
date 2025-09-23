// Vehicle.java - VERSIÓN MEJORADA
package co.edu.uptc.trafficlight.model;

import co.edu.uptc.trafficlight.business.TrafficController;
import javafx.application.Platform;
import java.util.concurrent.atomic.AtomicInteger;

public class Vehicle implements Runnable {
    private static final AtomicInteger vehicleCounter = new AtomicInteger(0);
    private final int id;
    private final String direction;
    private VehicleState state;
    private final TrafficController controller;

    // Posición para animación
    private double x, y;
    private double targetX, targetY;
    private boolean isAnimating = false;
    private String vehicleType;

    public enum VehicleState {
        WAITING, APPROACHING, CROSSING, CROSSED
    }

    public Vehicle(String direction, TrafficController controller) {
        this.id = vehicleCounter.incrementAndGet();
        this.direction = direction;
        this.state = VehicleState.WAITING;
        this.controller = controller;
        this.vehicleType = getRandomVehicleType();
        initializePosition();
    }

    private void initializePosition() {
        // Posiciones iniciales basadas en dirección
        switch (direction) {
            case "NORTH":
                this.x = 400; // Centro horizontal
                this.y = 50;  // Arriba
                this.targetX = 400;
                this.targetY = 600; // Abajo
                break;
            case "SOUTH":
                this.x = 400;
                this.y = 600;
                this.targetX = 400;
                this.targetY = 50;
                break;
            case "EAST":
                this.x = 50;
                this.y = 350;
                this.targetX = 600;
                this.targetY = 350;
                break;
            case "WEST":
                this.x = 600;
                this.y = 350;
                this.targetX = 50;
                this.targetY = 350;
                break;
        }
    }

    private String getRandomVehicleType() {
        String[] types = {"🚗", "🚙", "🚕", "🚐", "🚌"};
        return types[(int)(Math.random() * types.length)];
    }

    @Override
    public void run() {
        try {
            // 1. Vehículo esperando
            setState(VehicleState.WAITING);
            Thread.sleep(500 + (int)(Math.random() * 1000)); // Tiempo aleatorio de espera

            // 2. Solicitar permiso para cruzar (SEMÁFORO CRÍTICO)
            controller.requestCrossing(this);

            // 3. Aproximarse a la intersección
            setState(VehicleState.APPROACHING);
            animateToIntersection();

            // 4. Cruzar la intersección (SECCIÓN CRÍTICA)
            setState(VehicleState.CROSSING);
            animateCrossing();

            // 5. Completar cruce
            controller.finishCrossing(this);
            setState(VehicleState.CROSSED);

            // 6. Salir del área visible
            animateExit();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void animateToIntersection() throws InterruptedException {
        isAnimating = true;
        double startX = x, startY = y;
        double endX, endY;

        // Calcular punto de entrada a la intersección
        switch (direction) {
            case "NORTH":
                endX = 400; endY = 300;
                break;
            case "SOUTH":
                endX = 400; endY = 400;
                break;
            case "EAST":
                endX = 300; endY = 350;
                break;
            case "WEST":
            default:
                endX = 500; endY = 350;
                break;
        }

        // Animación suave en 1 segundo
        for (int i = 0; i <= 20; i++) {
            double progress = i / 20.0;
            x = startX + (endX - startX) * progress;
            y = startY + (endY - startY) * progress;

            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(50);
        }
        isAnimating = false;
    }

    private void animateCrossing() throws InterruptedException {
        isAnimating = true;
        double startX = x, startY = y;
        double endX, endY;

        // Calcular punto de salida de la intersección
        switch (direction) {
            case "NORTH":
                endX = 400; endY = 400;
                break;
            case "SOUTH":
                endX = 400; endY = 300;
                break;
            case "EAST":
                endX = 500; endY = 350;
                break;
            case "WEST":
            default:
                endX = 300; endY = 350;
                break;
        }

        // Cruce más lento para mostrar la sincronización
        for (int i = 0; i <= 30; i++) {
            double progress = i / 30.0;
            x = startX + (endX - startX) * progress;
            y = startY + (endY - startY) * progress;

            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(100); // Más lento para mostrar el cruce
        }
        isAnimating = false;
    }

    private void animateExit() throws InterruptedException {
        isAnimating = true;
        for (int i = 0; i <= 20; i++) {
            double progress = i / 20.0;
            double startX = x, startY = y;

            x = startX + (targetX - startX) * progress;
            y = startY + (targetY - startY) * progress;

            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(30);
        }

        // Remover vehículo después de la animación
        Platform.runLater(() -> controller.removeVehicle(this));
    }

    // Getters adicionales para animación
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAnimating() { return isAnimating; }
    public String getVehicleType() { return vehicleType; }

    // Getters existentes
    public int getId() { return id; }
    public String getDirection() { return direction; }
    public VehicleState getState() { return state; }
    public void setState(VehicleState state) { this.state = state; }
}
