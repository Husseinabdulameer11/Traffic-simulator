
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrafficLightCycleManager {
    // Existing fields
    private final int intersectionId;
    private final long baseGreenDuration;
    private final long yellowDurationMillis;
    private final long baseRedDurationMillis;

    // New congestion tracking fields
    private final Map<Lane, Integer> laneCongestion = new HashMap<>();
    private long currentGreenDuration;
    private static final int CONGESTION_THRESHOLD = 3;
    private static final long MAX_GREEN_TIME = 15000;
    private static final long MIN_GREEN_TIME = 4000;

    // Rest of existing fields remain the same
    private final List<TrafficLight> nsLights = new ArrayList<>();
    private final List<TrafficLight> ewLights = new ArrayList<>();
    private volatile LightState nsState;
    private volatile LightState ewState;
    private double timeInCurrentStateMillis = 0;
    private boolean nsCurrentlyActive;

    public TrafficLightCycleManager(int intersectionId, LightState startNsState, LightState startEwState,
                                    long greenDuration, long yellowDuration, long redDuration) {
        this.intersectionId = intersectionId;
        this.nsState = startNsState;
        this.ewState = startEwState;
        this.baseGreenDuration = greenDuration;
        this.currentGreenDuration = greenDuration;
        this.yellowDurationMillis = yellowDuration;
        this.baseRedDurationMillis = Math.max(redDuration, greenDuration + yellowDuration);
        this.nsCurrentlyActive = (startNsState == LightState.GREEN);
    }

    // Add these new methods
    public void updateCongestion(List<Vehicle> vehicles) {
        resetCongestionCounts();

        for (Vehicle vehicle : vehicles) {
            Lane vehicleLane = vehicle.getCurrentLane();
            if (vehicleLane != null && isApproachingJunction(vehicle)) {
                laneCongestion.put(vehicleLane,
                        laneCongestion.getOrDefault(vehicleLane, 0) + 1);
            }
        }
    }

    private boolean isApproachingJunction(Vehicle vehicle) {
        return vehicle.getDistanceToJunction() < 150; // 150px from junction
    }

    private void resetCongestionCounts() {
        for (Lane key : laneCongestion.keySet()) {
            laneCongestion.put(key, 0);
        }
    }

    private void adjustGreenTime() {
        int nsCount = getTotalCongestion(nsLights);
        int ewCount = getTotalCongestion(ewLights);

        if (nsCount > ewCount + CONGESTION_THRESHOLD) {
            currentGreenDuration = Math.min(MAX_GREEN_TIME,
                    (long)(baseGreenDuration * 1.5));
        }
        else if (ewCount > nsCount + CONGESTION_THRESHOLD) {
            currentGreenDuration = Math.max(MIN_GREEN_TIME,
                    (long)(baseGreenDuration * 0.75));
        }
        else {
            currentGreenDuration = baseGreenDuration;
        }
    }

    private int getTotalCongestion(List<TrafficLight> lights) {
        return lights.stream()
                .mapToInt(light -> laneCongestion.getOrDefault(light.getControlledLane(), 0))
                .sum();
    }

    // Modified update method
    public void update(double deltaTimeMillis) {
        if (deltaTimeMillis <= 0) return;

        timeInCurrentStateMillis += deltaTimeMillis;

        // Adjust timing when entering green phase
        if ((nsCurrentlyActive && nsState == LightState.GREEN && timeInCurrentStateMillis < 100) ||
                (!nsCurrentlyActive && ewState == LightState.GREEN && timeInCurrentStateMillis < 100)) {
            adjustGreenTime();
        }

        long currentPhaseDuration = getCurrentPhaseDuration();

        if (timeInCurrentStateMillis >= currentPhaseDuration) {
            timeInCurrentStateMillis = 0;

            // Original state transition logic from your code
            if (nsCurrentlyActive) {
                if (nsState == LightState.GREEN) {
                    setStates(LightState.YELLOW, LightState.RED);
                } else if (nsState == LightState.YELLOW) {
                    setStates(LightState.RED, LightState.GREEN);
                    nsCurrentlyActive = false;
                }
            } else {
                if (ewState == LightState.GREEN) {
                    setStates(LightState.RED, LightState.YELLOW);
                } else if (ewState == LightState.YELLOW) {
                    setStates(LightState.GREEN, LightState.RED);
                    nsCurrentlyActive = true;
                }
            }
        }
    }
    private long getCurrentPhaseDuration() {
        if (nsCurrentlyActive) {
            return nsState == LightState.GREEN ? currentGreenDuration :
                    nsState == LightState.YELLOW ? yellowDurationMillis :
                            baseRedDurationMillis;
        }
        return ewState == LightState.GREEN ? currentGreenDuration :
                ewState == LightState.YELLOW ? yellowDurationMillis :
                        baseRedDurationMillis;
    }


    /**
     * Adds a visual {@link TrafficLight} instance to be controlled by the North/South cycle group.
     * Sets the light's initial state to match the manager's current N/S state.
     * @param light The {@link TrafficLight} object to add. Must not be null.
     */
    public void addNsLight(TrafficLight light) {
        if (light != null) {
            nsLights.add(light);
            // Ensure the added light immediately reflects the current state
            light.setState(this.nsState);
        }
    }

    /**
     * Adds a visual {@link TrafficLight} instance to be controlled by the East/West cycle group.
     * Sets the light's initial state to match the manager's current E/W state.
     * @param light The {@link TrafficLight} object to add. Must not be null.
     */
    public void addEwLight(TrafficLight light) {
        if (light != null) {
            ewLights.add(light);
            // Ensure the added light immediately reflects the current state
            light.setState(this.ewState);
        }
    }
    /**
     * Sets the internal state variables and updates all associated visual {@link TrafficLight} objects.
     * This method is synchronized to prevent potential race conditions if ever called
     * concurrently, although the current design uses single-threaded updates via the manager.
     *
     * @param newNsState The new {@link LightState} for the North/South flow.
     * @param newEwState The new {@link LightState} for the East/West flow.
     */
    private synchronized void setStates(LightState newNsState, LightState newEwState) {
        boolean nsChanged = false;
        boolean ewChanged = false;

        // Update North/South state and associated lights if changed
        if (this.nsState != newNsState) {
            this.nsState = newNsState;
            for (TrafficLight light : nsLights) {
                light.setState(newNsState); // Update the visual light instance
            }
            nsChanged = true;
        }

        // Update East/West state and associated lights if changed
        if (this.ewState != newEwState) {
            this.ewState = newEwState;
            for (TrafficLight light : ewLights) {
                light.setState(newEwState); // Update the visual light instance
            }
            ewChanged = true;
        }

        // Optional: Log state changes
        // if (nsChanged || ewChanged) {
        //     System.out.println("J" + intersectionId + " State Change -> N/S: " + this.nsState + ", E/W: " + this.ewState);
        // }
    }

    // --- Standard Getters ---

    /** @return The ID of the intersection this manager controls. */
    public int getIntersectionId() { return intersectionId; }

    /** @return The current {@link LightState} for the North/South traffic flow. */
    public LightState getCurrentNsState() { return nsState; }

    /** @return The current {@link LightState} for the East/West traffic flow. */
    public LightState getCurrentEwState() { return ewState; }
}