// TrafficController.java - VERSIÓN MEJORADA
package co.edu.uptc.trafficlight.business;

import co.edu.uptc.trafficlight.model.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficController extends Observable {
    private final Map<String, TrafficLight> trafficLights;
    private final Map<String, Semaphore> laneSemaphores;
    private final Semaphore intersectionSemaphore; // CRÍTICO: Previene accidentes
    private final List<Vehicle> activeVehicles;
    private final List<Vehicle> crossingVehicles; // Vehículos en intersección
    private boolean running;

    // Estadísticas mejoradas
    private final AtomicInteger totalVehiclesGenerated = new AtomicInteger(0);
    private final AtomicInteger vehiclesCrossedSafely = new AtomicInteger(0);
    private final AtomicInteger accidentsPrevented = new AtomicInteger(0);
    private final AtomicInteger maxConcurrentInIntersection = new AtomicInteger(0);

    private static final String[] DIRECTIONS = {"NORTH", "SOUTH", "EAST", "WEST"};
    private static final int MAX_VEHICLES_IN_INTERSECTION = 2; // SEGURIDAD CRÍTICA

    public TrafficController() {
        this.trafficLights = new ConcurrentHashMap<>();
        this.laneSemaphores = new ConcurrentHashMap<>();
        this.intersectionSemaphore = new Semaphore(MAX_VEHICLES_IN_INTERSECTION);
        this.activeVehicles = new ArrayList<>();
        this.crossingVehicles = new ArrayList<>();

        initializeTrafficLights();
        initializeSemaphores();
    }

    private void initializeTrafficLights() {
        for (String direction : DIRECTIONS) {
            trafficLights.put(direction, new TrafficLight(direction));
        }
    }

    private void initializeSemaphores() {
        for (String direction : DIRECTIONS) {
            laneSemaphores.put(direction, new Semaphore(0));
        }
    }

    public void startSimulation() {
        running = true;

        Thread lightCycleThread = new Thread(this::runLightCycle);
        lightCycleThread.setDaemon(true);
        lightCycleThread.start();

        Thread vehicleGeneratorThread = new Thread(this::generateVehicles);
        vehicleGeneratorThread.setDaemon(true);
        vehicleGeneratorThread.start();

        Thread safetyMonitorThread = new Thread(this::monitorSafety);
        safetyMonitorThread.setDaemon(true);
        safetyMonitorThread.start();
    }

    private void runLightCycle() {
        String[] phases = {"NORTH_SOUTH", "EAST_WEST"};
        int currentPhase = 0;

        while (running) {
            try {
                String phase = phases[currentPhase];

                // Fase verde (5 segundos)
                setPhaseGreen(phase);
                Thread.sleep(5000);

                // Fase amarilla (2 segundos)
                setPhaseYellow(phase);
                Thread.sleep(2000);

                // Fase roja (1 segundo de transición)
                setPhaseRed(phase);
                Thread.sleep(1000);

                currentPhase = (currentPhase + 1) % phases.length;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void setPhaseGreen(String phase) {
        if (phase.equals("NORTH_SOUTH")) {
            setLightAndPermits("NORTH", TrafficLightState.GREEN, 5);
            setLightAndPermits("SOUTH", TrafficLightState.GREEN, 5);
            setLightAndPermits("EAST", TrafficLightState.RED, 0);
            setLightAndPermits("WEST", TrafficLightState.RED, 0);
        } else {
            setLightAndPermits("EAST", TrafficLightState.GREEN, 5);
            setLightAndPermits("WEST", TrafficLightState.GREEN, 5);
            setLightAndPermits("NORTH", TrafficLightState.RED, 0);
            setLightAndPermits("SOUTH", TrafficLightState.RED, 0);
        }
        notifyUpdate();
    }

    private void setPhaseYellow(String phase) {
        if (phase.equals("NORTH_SOUTH")) {
            setLightState("NORTH", TrafficLightState.YELLOW);
            setLightState("SOUTH", TrafficLightState.YELLOW);
        } else {
            setLightState("EAST", TrafficLightState.YELLOW);
            setLightState("WEST", TrafficLightState.YELLOW);
        }
        notifyUpdate();
    }

    private void setPhaseRed(String phase) {
        if (phase.equals("NORTH_SOUTH")) {
            setLightAndPermits("NORTH", TrafficLightState.RED, 0);
            setLightAndPermits("SOUTH", TrafficLightState.RED, 0);
        } else {
            setLightAndPermits("EAST", TrafficLightState.RED, 0);
            setLightAndPermits("WEST", TrafficLightState.RED, 0);
        }
        notifyUpdate();
    }

    private void setLightAndPermits(String direction, TrafficLightState state, int permits) {
        setLightState(direction, state);

        // Drenar permisos existentes y asignar nuevos
        laneSemaphores.get(direction).drainPermits();
        if (permits > 0) {
            laneSemaphores.get(direction).release(permits);
        }
    }

    private void setLightState(String direction, TrafficLightState state) {
        trafficLights.get(direction).setCurrentState(state);
    }

    private void generateVehicles() {
        while (running) {
            try {
                Thread.sleep(1500 + (int)(Math.random() * 2000)); // 1.5-3.5 segundos

                String direction = DIRECTIONS[(int)(Math.random() * DIRECTIONS.length)];
                Vehicle vehicle = new Vehicle(direction, this);

                synchronized (activeVehicles) {
                    activeVehicles.add(vehicle);
                }

                totalVehiclesGenerated.incrementAndGet();

                Thread vehicleThread = new Thread(vehicle, "Vehicle-" + vehicle.getId());
                vehicleThread.start();

                notifyUpdate();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // MÉTODO CRÍTICO: Controla acceso seguro a la intersección
    public void requestCrossing(Vehicle vehicle) throws InterruptedException {
        System.out.println("🚦 Vehículo " + vehicle.getId() + " (" + vehicle.getVehicleType() +
                ") desde " + vehicle.getDirection() + " solicitando cruzar...");

        // 1. Esperar luz verde (semáforo del carril)
        laneSemaphores.get(vehicle.getDirection()).acquire();

        // 2. Verificar disponibilidad en intersección (PREVENCIÓN DE ACCIDENTES)
        int waitingTime = 0;
        while (!intersectionSemaphore.tryAcquire()) {
            Thread.sleep(100);
            waitingTime += 100;

            if (waitingTime > 50) { // Detectar espera por congestión
                accidentsPrevented.incrementAndGet();
                System.out.println("⚠️  ACCIDENTE PREVENIDO: Vehículo " + vehicle.getId() +
                        " esperó " + waitingTime + "ms por intersección ocupada");
            }
        }

        // 3. Registrar entrada segura a intersección
        synchronized (crossingVehicles) {
            crossingVehicles.add(vehicle);
            int current = crossingVehicles.size();
            maxConcurrentInIntersection.updateAndGet(max -> Math.max(max, current));
        }

        System.out.println("✅ Vehículo " + vehicle.getId() + " ENTRA SEGURO a intersección. " +
                "Vehículos actuales en intersección: " + crossingVehicles.size());
    }

    public void finishCrossing(Vehicle vehicle) {
        // Liberar intersección
        intersectionSemaphore.release();

        synchronized (crossingVehicles) {
            crossingVehicles.remove(vehicle);
        }

        vehiclesCrossedSafely.incrementAndGet();

        System.out.println("🏁 Vehículo " + vehicle.getId() + " SALIÓ SEGURO de intersección. " +
                "Vehículos restantes: " + crossingVehicles.size());

        notifyUpdate();
    }

    public void removeVehicle(Vehicle vehicle) {
        synchronized (activeVehicles) {
            activeVehicles.remove(vehicle);
        }
        notifyUpdate();
    }

    // Monitor de seguridad en tiempo real
    private void monitorSafety() {
        while (running) {
            try {
                Thread.sleep(1000);

                synchronized (crossingVehicles) {
                    if (crossingVehicles.size() > MAX_VEHICLES_IN_INTERSECTION) {
                        System.err.println("🚨 ALERTA DE SEGURIDAD: " + crossingVehicles.size() +
                                " vehículos en intersección (máximo: " + MAX_VEHICLES_IN_INTERSECTION + ")");
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void notifyVehicleUpdate() {
        notifyUpdate();
    }

    private void notifyUpdate() {
        setChanged();
        notifyObservers();
    }

    public void stopSimulation() {
        running = false;
    }

    // Getters para estadísticas mejoradas
    public int getTotalVehiclesGenerated() { return totalVehiclesGenerated.get(); }
    public int getVehiclesCrossedSafely() { return vehiclesCrossedSafely.get(); }
    public int getAccidentsPrevented() { return accidentsPrevented.get(); }
    public int getMaxConcurrentInIntersection() { return maxConcurrentInIntersection.get(); }
    public int getCurrentVehiclesInIntersection() {
        synchronized (crossingVehicles) {
            return crossingVehicles.size();
        }
    }

    // Getters existentes
    public Map<String, TrafficLight> getTrafficLights() { return trafficLights; }
    public List<Vehicle> getActiveVehicles() {
        synchronized (activeVehicles) {
            return new ArrayList<>(activeVehicles);
        }
    }
    public boolean isRunning() { return running; }
}
