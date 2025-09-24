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

    // Posición y destino
    private double x, y;
    private double targetX, targetY;
    private boolean isAnimating = false;
    private String vehicleType;
    private MovementType movementType;

    // Velocidad individual (ms entre pasos)
    private final int crossingSpeed;

    public enum VehicleState { WAITING, APPROACHING, CROSSING, CROSSED }
    public enum MovementType { STRAIGHT, LEFT, RIGHT }

    public Vehicle(String direction, TrafficController controller) {
        this.id = vehicleCounter.incrementAndGet();
        this.direction = direction;
        this.state = VehicleState.WAITING;
        this.controller = controller;
        this.vehicleType = randomVehicleType();

        // Probabilidad: STRAIGHT 60%, LEFT 20%, RIGHT 20%
        double r = Math.random();
        if (r < 0.6) movementType = MovementType.STRAIGHT;
        else if (r < 0.8) movementType = MovementType.LEFT;
        else movementType = MovementType.RIGHT;

        // Velocidad distinta por carro (50–90 ms por paso)
        this.crossingSpeed = 50 + (int)(Math.random() * 40);

        initializePosition();
    }

    private void initializePosition() {
        // Carril derecho de cada vía
        switch (direction) {
            case "NORTH": this.x = 390; this.y = 50; break;   // hacia abajo por derecha
            case "SOUTH": this.x = 410; this.y = 600; break;  // hacia arriba por derecha
            case "EAST":  this.x = 50;  this.y = 360; break;  // hacia derecha por abajo
            case "WEST":  this.x = 600; this.y = 340; break;  // hacia izquierda por arriba
        }
        this.targetX = x;
        this.targetY = y;
    }

    private String randomVehicleType() {
        String[] types = {"🚗","🚙","🚕","🚐","🚌"};
        return types[(int)(Math.random() * types.length)];
    }

    @Override
    public void run() {
        try {
            setState(VehicleState.WAITING);
            Thread.sleep(400 + (int)(Math.random() * 800));

            // solicitar permiso
            controller.requestCrossing(this);

            if (!controller.isRunning()) {
                controller.removeVehicle(this);
                return;
            }

            // retraso humano
            Thread.sleep(300 + (int)(Math.random() * 500));

            // Aproximación
            setState(VehicleState.APPROACHING);
            animateToIntersection();

            // Cruce
            setState(VehicleState.CROSSING);
            animateCrossing();

            // Terminar cruce
            controller.finishCrossing(this);
            setState(VehicleState.CROSSED);

            // Salida
            animateExit();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            controller.removeVehicle(this);
        }
    }

    private void animateToIntersection() throws InterruptedException {
        isAnimating = true;
        double startX = x, startY = y;
        double entryX = x, entryY = y;

        // punto de entrada al carril dentro de la intersección
        switch (direction) {
            case "NORTH": entryX = 390; entryY = 320; break;
            case "SOUTH": entryX = 410; entryY = 380; break;
            case "EAST":  entryX = 420; entryY = 360; break;
            case "WEST":  entryX = 380; entryY = 340; break;
        }

        for (int i = 0; i <= 20; i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            double t = i / 20.0;
            x = startX + (entryX - startX) * t;
            y = startY + (entryY - startY) * t;
            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(40);
        }
        isAnimating = false;
    }

    private void animateCrossing() throws InterruptedException {
        isAnimating = true;
        double sx = x, sy = y;
        double ex = sx, ey = sy;
        double cx = 400, cy = 350; // control de curva

        switch (direction) {
            case "NORTH":
                if (movementType == MovementType.STRAIGHT) { ex = 390; ey = 600; }
                else if (movementType == MovementType.LEFT) { ex = 600; ey = 360; cx = 480; cy = 360; }
                else { ex = 50; ey = 340; cx = 320; cy = 340; }
                break;
            case "SOUTH":
                if (movementType == MovementType.STRAIGHT) { ex = 410; ey = 50; }
                else if (movementType == MovementType.LEFT) { ex = 50; ey = 340; cx = 320; cy = 340; }
                else { ex = 600; ey = 360; cx = 480; cy = 360; }
                break;
            case "EAST":
                if (movementType == MovementType.STRAIGHT) { ex = 600; ey = 360; }
                else if (movementType == MovementType.LEFT) { ex = 410; ey = 50; cx = 410; cy = 120; }
                else { ex = 390; ey = 600; cx = 390; cy = 480; }
                break;
            case "WEST":
                if (movementType == MovementType.STRAIGHT) { ex = 50; ey = 340; }
                else if (movementType == MovementType.LEFT) { ex = 390; ey = 600; cx = 390; cy = 480; }
                else { ex = 410; ey = 50; cx = 410; cy = 120; }
                break;
        }

        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            double t = i / (double) steps;
            if (movementType == MovementType.STRAIGHT) {
                x = sx + (ex - sx) * t;
                y = sy + (ey - sy) * t;
            } else {
                double oneMinusT = 1 - t;
                x = oneMinusT * oneMinusT * sx + 2 * oneMinusT * t * cx + t * t * ex;
                y = oneMinusT * oneMinusT * sy + 2 * oneMinusT * t * cy + t * t * ey;
            }
            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(crossingSpeed);
        }

        this.targetX = ex;
        this.targetY = ey;
        isAnimating = false;
    }

    private void animateExit() throws InterruptedException {
        isAnimating = true;
        double startX = x, startY = y;
        double endX = targetX, endY = targetY;

        for (int i = 0; i <= 20; i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            double t = i / 20.0;
            x = startX + (endX - startX) * t;
            y = startY + (endY - startY) * t;
            Platform.runLater(() -> controller.notifyVehicleUpdate());
            Thread.sleep(30);
        }

        Platform.runLater(() -> controller.removeVehicle(this));
        isAnimating = false;
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAnimating() { return isAnimating; }
    public String getVehicleType() { return vehicleType; }
    public MovementType getMovementType() { return movementType; }
    public int getId() { return id; }
    public String getDirection() { return direction; }
    public VehicleState getState() { return state; }
    public void setState(VehicleState state) { this.state = state; }
}
