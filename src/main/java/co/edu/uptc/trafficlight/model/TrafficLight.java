package co.edu.uptc.trafficlight.model;

public class TrafficLight {
    private TrafficLightState currentState;
    private String direction;
    private long stateStartTime;

    public TrafficLight(String direction) {
        this.direction = direction;
        this.currentState = TrafficLightState.RED;
        this.stateStartTime = System.currentTimeMillis();
    }

    // Getters y setters
    public TrafficLightState getCurrentState() { return currentState; }
    public void setCurrentState(TrafficLightState state) {
        this.currentState = state;
        this.stateStartTime = System.currentTimeMillis();
    }
    public String getDirection() { return direction; }
    public long getStateStartTime() { return stateStartTime; }
}
