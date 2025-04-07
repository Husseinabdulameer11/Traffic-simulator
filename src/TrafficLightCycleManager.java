

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the state transitions (RED -> GREEN -> YELLOW -> RED) for a set of traffic lights
 * at a single intersection, ensuring proper synchronization between opposing directions (e.g., North/South vs. East/West).
 * This class maintains the timing for each light phase and updates the state of associated
 * {@link TrafficLight} instances when a phase change occurs. It is typically updated
 * periodically by the main simulation loop (e.g., {@code AnimationTimer}).
 */
public class TrafficLightCycleManager {

    // Identifier for the intersection this manager controls.
    private final int intersectionId;
    // Durations for each light phase in milliseconds.
    private final long greenDurationMillis;
    private final long yellowDurationMillis;
    private final long redDurationMillis; // Calculated to ensure safety clearance

    // Lists of visual TrafficLight objects controlled by this manager.
    // One list for the North/South traffic flow, one for East/West.
    private final List<TrafficLight> nsLights = new ArrayList<>();
    private final List<TrafficLight> ewLights = new ArrayList<>();

    // Current state of the North/South and East/West flows.
    // Marked volatile as they might be read by vehicle threads checking light status,
    // although updates happen synchronously within the update method.
    private volatile LightState nsState;
    private volatile LightState ewState;

    // Timer to track elapsed time within the current light phase.
    private double timeInCurrentStateMillis = 0;
    // Flag indicating which direction flow (N/S or E/W) is currently in the active (Green/Yellow) part of the cycle.
    private boolean nsCurrentlyActive;

    /**
     * Constructs a new TrafficLightCycleManager for a specific intersection.
     *
     * @param intersectionId   The unique ID of the {@link Junction} this manager controls.
     * @param startNsState     The initial {@link LightState} for the North/South lights.
     * @param startEwState     The initial {@link LightState} for the East/West lights (should typically be the opposite of startNsState).
     * @param greenDuration    Duration of the GREEN phase in milliseconds.
     * @param yellowDuration   Duration of the YELLOW phase in milliseconds.
     * @param redDuration      Minimum duration of the RED phase in milliseconds. This will be automatically adjusted
     *                         if it's shorter than the opposing green + yellow duration to ensure safety.
     */
    public TrafficLightCycleManager(int intersectionId, LightState startNsState, LightState startEwState,
                                    long greenDuration, long yellowDuration, long redDuration) {
        this.intersectionId = intersectionId;
        this.nsState = startNsState;
        this.ewState = startEwState;
        this.greenDurationMillis = greenDuration;
        this.yellowDurationMillis = yellowDuration;
        // Ensure the red duration is long enough for the other direction's green+yellow cycle.
        this.redDurationMillis = Math.max(redDuration, greenDuration + yellowDuration);

        // Determine which direction starts the active cycle based on initial GREEN state.
        this.nsCurrentlyActive = (startNsState == LightState.GREEN);
        this.timeInCurrentStateMillis = 0; // Initialize the phase timer
        // System.out.println("CycleManager created for intersection J" + intersectionId);
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
     * Updates the traffic light cycle based on the elapsed time.
     * This method should be called repeatedly from the main simulation loop.
     * It increments the internal timer and triggers state changes when phase durations are met.
     *
     * @param deltaTimeMillis The time that has passed since the last call to update, in milliseconds.
     */
    public void update(double deltaTimeMillis) {
        if (deltaTimeMillis <= 0) return; // No time has passed

        timeInCurrentStateMillis += deltaTimeMillis;

        // Determine the duration of the current phase for the *active* direction
        long currentPhaseDuration;
        LightState activeState = nsCurrentlyActive ? nsState : ewState;

        switch (activeState) {
            case GREEN:
                currentPhaseDuration = greenDurationMillis;
                break;
            case YELLOW:
                currentPhaseDuration = yellowDurationMillis;
                break;
            case RED:
            default:
                // When the active flow is RED, the cycle is driven by the *other* flow's
                // green/yellow timing. The red duration itself defines the minimum wait time.
                // The transition happens when the *inactive* flow finishes its green/yellow.
                // For simplicity in checking time elapsed, we use redDuration here,
                // but the logic below correctly transitions based on the active flow's state change.
                currentPhaseDuration = redDurationMillis;
                break;
        }

        // Check if the timer for the current phase has exceeded its duration
        if (timeInCurrentStateMillis >= currentPhaseDuration) {
            // Reset the timer for the start of the next phase
            // Note: A small negative offset could account for overshoot, but 0 is usually sufficient
            timeInCurrentStateMillis = 0; // timeInCurrentStateMillis - currentPhaseDuration;

            // Transition to the next state based on which direction is currently active
            if (nsCurrentlyActive) { // North/South flow was active (Green or Yellow)
                if (nsState == LightState.GREEN) {
                    // Transition from Green to Yellow for N/S
                    setStates(LightState.YELLOW, LightState.RED); // E/W remains Red
                } else if (nsState == LightState.YELLOW) {
                    // Transition from Yellow to Red for N/S, and start E/W cycle
                    setStates(LightState.RED, LightState.GREEN); // N/S becomes Red, E/W becomes Green
                    nsCurrentlyActive = false; // Switch active direction to E/W
                }
                // No action needed if N/S was RED (shouldn't happen if nsCurrentlyActive was true)
            } else { // East/West flow was active (Green or Yellow)
                if (ewState == LightState.GREEN) {
                    // Transition from Green to Yellow for E/W
                    setStates(LightState.RED, LightState.YELLOW); // N/S remains Red
                } else if (ewState == LightState.YELLOW) {
                    // Transition from Yellow to Red for E/W, and start N/S cycle
                    setStates(LightState.GREEN, LightState.RED); // E/W becomes Red, N/S becomes Green
                    nsCurrentlyActive = true; // Switch active direction to N/S
                }
                // No action needed if E/W was RED
            }
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