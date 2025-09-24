package co.edu.uptc.trafficlight.business;

import co.edu.uptc.trafficlight.model.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class TrafficController extends Observable {
    private final Map<String, TrafficLight> trafficLights;

    /**
     * laneSemaphores:
     *  "NORTH" -> { STRAIGHT -> Semaphore, LEFT -> Semaphore, RIGHT -> Semaphore }
     */
    private final Map<String, Map<Vehicle.MovementType, Semaphore>> laneSemaphores;

    private final Semaphore intersectionSemaphore; // control de sección crítica
    private final List<Vehicle> activeVehicles;
    private final List<Vehicle> crossingVehicles; // Vehículos en intersección
    private boolean running;

    // Hilos principales
    private Thread lightCycleThread;
    private Thread vehicleGeneratorThread;
    private Thread safetyMonitorThread;

    // Hilos de vehículos para control/interrupción
    private final Map<Integer, Thread> vehicleThreads = new ConcurrentHashMap<>();

    // Estadísticas
    private final AtomicInteger totalVehiclesGenerated = new AtomicInteger(0);
    private final AtomicInteger vehiclesCrossedSafely = new AtomicInteger(0);
    private final AtomicInteger accidentsPrevented = new AtomicInteger(0);
    private final AtomicInteger maxConcurrentInIntersection = new AtomicInteger(0);

    private static final String[] DIRECTIONS = {"NORTH", "SOUTH", "EAST", "WEST"};
    private static final int MAX_VEHICLES_IN_INTERSECTION = 2;

    // Permisos por tipo de movimiento en verde
    private static final int GREEN_STRAIGHT_PERMITS = 3;
    private static final int GREEN_LEFT_PERMITS = 1;
    private static final int GREEN_RIGHT_PERMITS = 1;

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
            Map<Vehicle.MovementType, Semaphore> movementMap = new ConcurrentHashMap<>();
            for (Vehicle.MovementType mt : Vehicle.MovementType.values()) {
                movementMap.put(mt, new Semaphore(0));
            }
            laneSemaphores.put(direction, movementMap);
        }
    }

    public void startSimulation() {
        running = true;

        lightCycleThread = new Thread(this::runLightCycle, "LightCycleThread");
        lightCycleThread.setDaemon(true);
        lightCycleThread.start();

        vehicleGeneratorThread = new Thread(this::generateVehicles, "VehicleGeneratorThread");
        vehicleGeneratorThread.setDaemon(true);
        vehicleGeneratorThread.start();

        safetyMonitorThread = new Thread(this::monitorSafety, "SafetyMonitorThread");
        safetyMonitorThread.setDaemon(true);
        safetyMonitorThread.start();
    }

    private void runLightCycle() {
        String[] phases = {"NORTH_SOUTH", "EAST_WEST"};
        int currentPhase = 0;

        while (running) {
            try {
                String phase = phases[currentPhase];

                // Verde
                setPhaseGreen(phase);
                Thread.sleep(5000);

                // Amarillo
                setPhaseYellow(phase);
                Thread.sleep(2000);

                // Rojo
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
            setLightAndPermits("NORTH", TrafficLightState.GREEN);
            setLightAndPermits("SOUTH", TrafficLightState.GREEN);
            setLightAndPermits("EAST", TrafficLightState.RED);
            setLightAndPermits("WEST", TrafficLightState.RED);
        } else {
            setLightAndPermits("EAST", TrafficLightState.GREEN);
            setLightAndPermits("WEST", TrafficLightState.GREEN);
            setLightAndPermits("NORTH", TrafficLightState.RED);
            setLightAndPermits("SOUTH", TrafficLightState.RED);
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
        // No asignamos permisos en amarillo
        notifyUpdate();
    }

    private void setPhaseRed(String phase) {
        if (phase.equals("NORTH_SOUTH")) {
            setLightAndPermits("NORTH", TrafficLightState.RED);
            setLightAndPermits("SOUTH", TrafficLightState.RED);
        } else {
            setLightAndPermits("EAST", TrafficLightState.RED);
            setLightAndPermits("WEST", TrafficLightState.RED);
        }
        notifyUpdate();
    }

    private void setLightAndPermits(String direction, TrafficLightState state) {
        setLightState(direction, state);

        Map<Vehicle.MovementType, Semaphore> movementMap = laneSemaphores.get(direction);
        if (movementMap == null) return;

        // Drenar antes
        for (Semaphore s : movementMap.values()) {
            s.drainPermits();
        }

        if (state == TrafficLightState.GREEN) {
            movementMap.get(Vehicle.MovementType.STRAIGHT).release(GREEN_STRAIGHT_PERMITS);
            movementMap.get(Vehicle.MovementType.LEFT).release(GREEN_LEFT_PERMITS);
            movementMap.get(Vehicle.MovementType.RIGHT).release(GREEN_RIGHT_PERMITS);
        }
        // si es YELLOW/RED no damos permisos
    }

    private void setLightState(String direction, TrafficLightState state) {
        trafficLights.get(direction).setCurrentState(state);
    }

    private void generateVehicles() {
        while (running) {
            try {
                Thread.sleep(1500 + (int)(Math.random() * 2000)); // 1.5-3.5s

                String direction = DIRECTIONS[(int)(Math.random() * DIRECTIONS.length)];
                Vehicle vehicle = new Vehicle(direction, this);

                synchronized (activeVehicles) {
                    activeVehicles.add(vehicle);
                }

                totalVehiclesGenerated.incrementAndGet();

                Thread vehicleThread = new Thread(vehicle, "Vehicle-" + vehicle.getId());
                vehicleThreads.put(vehicle.getId(), vehicleThread);
                vehicleThread.start();

                notifyUpdate();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Adquiere semáforo por movimiento (blocking). Usa intersectionSemaphore para
     * garantizar límite de vehículos en intersección.
     */
    public void requestCrossing(Vehicle vehicle) throws InterruptedException {
        System.out.println("🚦 Vehículo " + vehicle.getId() + " desde " + vehicle.getDirection()
                + " solicita cruzar (" + vehicle.getMovementType() + ")");

        Map<Vehicle.MovementType, Semaphore> movementMap = laneSemaphores.get(vehicle.getDirection());
        if (movementMap == null) throw new IllegalStateException("Dirección no existe");

        Semaphore movementSemaphore = movementMap.get(vehicle.getMovementType());
        // Esperar permiso del movimiento (p.ej. verde)
        movementSemaphore.acquire();

        // Esperar disponibilidad en la intersección
        int waiting = 0;
        while (!intersectionSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            waiting += 100;
            if (waiting > 1000) {
                accidentsPrevented.incrementAndGet();
                System.out.println("⚠️ Accidente prevenido: vehículo " + vehicle.getId() +
                        " esperó " + waiting + "ms por intersección ocupada");
            }
            if (!running) {
                // simulación detenida mientras esperaba
                return;
            }
        }

        synchronized (crossingVehicles) {
            crossingVehicles.add(vehicle);
            int cur = crossingVehicles.size();
            maxConcurrentInIntersection.updateAndGet(max -> Math.max(max, cur));
        }

        System.out.println("✅ Vehículo " + vehicle.getId() + " ENTRA a intersección.");
    }

    public void finishCrossing(Vehicle vehicle) {
        intersectionSemaphore.release();

        synchronized (crossingVehicles) {
            crossingVehicles.remove(vehicle);
        }

        vehiclesCrossedSafely.incrementAndGet();

        // Limpiar hilo del mapa si ya terminó
        vehicleThreads.remove(vehicle.getId());

        System.out.println("🏁 Vehículo " + vehicle.getId() + " SALIÓ de intersección.");
        notifyUpdate();
    }

    public void removeVehicle(Vehicle vehicle) {
        synchronized (activeVehicles) {
            activeVehicles.remove(vehicle);
        }
        vehicleThreads.remove(vehicle.getId());
        notifyUpdate();
    }

    private void monitorSafety() {
        while (running) {
            try {
                Thread.sleep(1000);

                synchronized (crossingVehicles) {
                    if (crossingVehicles.size() > MAX_VEHICLES_IN_INTERSECTION) {
                        System.err.println("🚨 ALERTA: " + crossingVehicles.size() + " en intersección");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void notifyVehicleUpdate() { notifyUpdate(); }

    private void notifyUpdate() {
        setChanged();
        notifyObservers();
    }

    public void stopSimulation() {
        running = false;

        if (lightCycleThread != null) lightCycleThread.interrupt();
        if (vehicleGeneratorThread != null) vehicleGeneratorThread.interrupt();
        if (safetyMonitorThread != null) safetyMonitorThread.interrupt();

        // Interrumpir hilos de vehículos
        for (Thread t : vehicleThreads.values()) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        vehicleThreads.clear();

        synchronized (activeVehicles) { activeVehicles.clear(); }
        synchronized (crossingVehicles) { crossingVehicles.clear(); }

        // Drenar semáforos
        for (Map<Vehicle.MovementType, Semaphore> map : laneSemaphores.values()) {
            for (Semaphore s : map.values()) s.drainPermits();
        }

        notifyUpdate();
    }

    // Getters estadísticos
    public int getTotalVehiclesGenerated() { return totalVehiclesGenerated.get(); }
    public int getVehiclesCrossedSafely() { return vehiclesCrossedSafely.get(); }
    public int getAccidentsPrevented() { return accidentsPrevented.get(); }
    public int getMaxConcurrentInIntersection() { return maxConcurrentInIntersection.get(); }
    public int getCurrentVehiclesInIntersection() {
        synchronized (crossingVehicles) { return crossingVehicles.size(); }
    }

    public Map<String, TrafficLight> getTrafficLights() { return trafficLights; }

    public List<Vehicle> getActiveVehicles() {
        synchronized (activeVehicles) { return new ArrayList<>(activeVehicles); }
    }

    public boolean isRunning() { return running; }
}
