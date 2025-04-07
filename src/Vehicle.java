// Filnavn: Vehicle.java
// (Ingen package-setning)

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// VehicleType enum definition remains the same (usually in its own file or kept simple)
enum VehicleType {
    CAR(4.5, 2.0), TRUCK(8.0, 2.5);
    private final double length; private final double width;
    VehicleType(double length, double width) { this.length = length; this.width = width; }
    public double getLength() { return length; } public double getWidth() { return width; }
}

/**
 * Represents a vehicle in the traffic simulation.
 * Manages state including position, speed, acceleration, and handles
 * interactions like car-following (IDM) and traffic light response.
 */
public class Vehicle {

    // --- Attributes & State ---
    private final VehicleType type;
    private final Color color;
    private Lane currentLane;
    private double position; // Fractional position on lane [0.0, 1.0]
    private double speed;    // m/s
    private double maxSpeed = 15; // m/s
    private double acceleration = 0; // m/s^2

    // --- Car-Following Parameters ---
    private final double maxAcceleration = 2.0;
    private final double desiredTimeGap = 1.5; // seconds
    private final double minDistance = 2.0; // meters

    // --- Rendering ---
    private double x, y;       // Pixel coordinates
    private double angleDeg;   // Degrees

    // --- Context ---
    private final VehicleManager vehicleManager;
    private final RoadNetwork roadNetwork;

    // --- Traffic Light State ---
    private boolean mustStopForLight = false;
    private double stopPositionFraction = 1.0; // Calculated stop point fraction

    // --- Constants ---
    private static final double STOP_DISTANCE_BEFORE_JUNCTION = 5.0; // pixels
    private static final double TRAFFIC_LIGHT_CHECK_DISTANCE = 50.0; // pixels

    /**
     * Constructs a new Vehicle.
     * @param type VehicleType (CAR or TRUCK).
     * @param color Rendering color.
     * @param lane Initial lane.
     * @param vm VehicleManager reference.
     * @param rn RoadNetwork reference.
     */
    public Vehicle(VehicleType type, Color color, Lane lane, VehicleManager vm, RoadNetwork rn) {
        // Null checks could be added for robustness
        this.type = type;
        this.color = color;
        this.currentLane = lane;
        this.position = 0.0;
        this.speed = maxSpeed * (0.3 + Math.random() * 0.5); // Start with some speed
        this.vehicleManager = vm;
        this.roadNetwork = rn;
        updateVisuals();
    }

    /**
     * Updates the vehicle's state for one time step (deltaTime).
     * @param deltaTime Time elapsed in seconds.
     */
    public void update(double deltaTime) {
        if (currentLane == null || deltaTime <= 0) return;

        checkTrafficLightAndDetermineStop();
        Vehicle leader = findVehicleAhead();
        double desiredAcceleration = calculateIDMAcceleration(leader);

        // Adjust acceleration and speed based on traffic light requirement
        if (mustStopForLight) {
            applyBrakingForLight();
        } else {
            this.acceleration = desiredAcceleration;
        }
        this.acceleration = Math.max(-getComfortableDeceleration(), Math.min(maxAcceleration, this.acceleration));

        updateSpeed(deltaTime);
        updatePosition(deltaTime);
        updateVisuals();
    }

    /** Calculates comfortable deceleration rate. */
    private double getComfortableDeceleration() {
        return maxAcceleration * 1.2;
    }

    /** Calculates desired acceleration based on IDM. */
    private double calculateIDMAcceleration(Vehicle leader) {
        double deltaSpeed = 0;
        double spacing = Double.POSITIVE_INFINITY;
        if (leader != null) {
            spacing = calculateNetDistance(leader);
            deltaSpeed = this.speed - leader.getSpeed();
        }
        double desiredSpacing = minDistance + Math.max(0, this.speed * desiredTimeGap + (this.speed * deltaSpeed) / (2 * Math.sqrt(maxAcceleration * Math.abs(getComfortableDeceleration()))));
        double freeRoadComponent = maxAcceleration * (1 - Math.pow(this.speed / maxSpeed, 4));
        double interactionComponent = (leader != null) ? -maxAcceleration * Math.pow(desiredSpacing / Math.max(spacing, minDistance / 2.0), 2) : 0;
        return freeRoadComponent + interactionComponent;
    }

    /** Applies braking logic if required to stop for a traffic light. */
    private void applyBrakingForLight() {
        double distanceToStopPoint = (stopPositionFraction - position) * currentLane.getLength();
        if (distanceToStopPoint > 0.1) {
            // Calculate required deceleration to stop at the exact point
            double requiredDecel = (speed * speed) / (2 * Math.max(0.1, distanceToStopPoint));
            // Apply braking, ensuring it's strong enough but within comfortable limits if possible
            this.acceleration = -Math.max(requiredDecel, getComfortableDeceleration() * 0.8);
        } else {
            // At or past the stop point, force stop
            this.acceleration = 0;
            this.speed = 0;
        }
    }

    /** Updates speed based on current acceleration and delta time. */
    private void updateSpeed(double deltaTime) {
        this.speed += this.acceleration * deltaTime;
        this.speed = Math.max(0, this.speed); // Prevent negative speed
    }

    /** Updates fractional position based on speed and delta time, handling lane ends. */
    private void updatePosition(double deltaTime) {
        // Use average speed for position update to improve accuracy
        // Note: previousSpeed would need to be stored before updateSpeed is called.
        // Simplified approach using current speed:
        double distanceToTravel = this.speed * deltaTime;

        if (distanceToTravel > 0.001) { // Only update if moving
            double laneLength = Math.max(0.01, currentLane.getLength());
            double positionChange = distanceToTravel / laneLength;
            double nextPosition = position + positionChange;

            if (nextPosition >= 1.0 - 0.001) { // Reached or passed the end (with tolerance)
                double overshootFraction = nextPosition - 1.0;
                double overshootDistance = overshootFraction * laneLength;

                Junction endingJunction = currentLane.getEndingJunction();
                Lane nextLane = roadNetwork.getNextLane(currentLane, endingJunction);

                if (nextLane != null) {
                    // Move to next lane
                    currentLane = nextLane;
                    position = Math.min(1.0, overshootDistance / Math.max(0.01, currentLane.getLength()));
                    mustStopForLight = false; // Reset flag
                } else {
                    // End of network path
                    position = 1.0;
                    speed = 0;
                    acceleration = 0;
                    // System.out.println("Vehicle " + System.identityHashCode(this) + " reached end of road network.");
                }
            } else {
                // Continue on current lane
                position = nextPosition;
            }
        }
    }


    /** Checks traffic light state if approaching a relevant junction. */
    private void checkTrafficLightAndDetermineStop() {
        this.mustStopForLight = false;
        this.stopPositionFraction = 1.0;
        if (currentLane == null) return;

        Junction endJunction = currentLane.getEndingJunction();
        if (endJunction != null && endJunction.getType() == JunctionType.TRAFFIC_LIGHT) {
            double laneLength = currentLane.getLength();
            if (laneLength <= 0) return;

            stopPositionFraction = 1.0 - (STOP_DISTANCE_BEFORE_JUNCTION / laneLength);
            stopPositionFraction = Math.max(0, stopPositionFraction);

            if (position < stopPositionFraction) {
                double distanceToStopPixels = (stopPositionFraction - position) * laneLength;
                // Check light only if within reasonable stopping distance
                if (distanceToStopPixels < TRAFFIC_LIGHT_CHECK_DISTANCE) {
                    TrafficLight light = endJunction.getTrafficLightForLane(currentLane);
                    if (light != null) {
                        LightState state = light.getCurrentState();
                        if (state == LightState.RED || state == LightState.YELLOW) {
                            this.mustStopForLight = true;
                        }
                    } // else: Warning about missing light (logged elsewhere or handled)
                }
            }
        }
    }

    /** Updates visual coordinates (x, y, angle) based on current position. */
    private void updateVisuals() {
        if (currentLane == null) return;
        double[] coords = currentLane.getPointAt(position);
        this.x = coords[0];
        this.y = coords[1];
        this.angleDeg = Math.toDegrees(currentLane.getAngleRad());
    }

    /** Finds the vehicle ahead on the same lane. */
    private Vehicle findVehicleAhead() {
        // Delegate to VehicleManager to search the global list
        return (vehicleManager != null) ? vehicleManager.findVehicleAheadOnLane(this, currentLane) : null;
    }

    /** Calculates bumper-to-bumper distance to the vehicle ahead. */
    private double calculateNetDistance(Vehicle ahead) {
        // Assumes 'ahead' is on the same lane and 'position' values are correct
        double positionDifference = ahead.position - this.position;
        double centerToCenterDistance = positionDifference * currentLane.getLength();
        double netDistance = centerToCenterDistance - (ahead.type.getLength() / 2.0) - (this.type.getLength() / 2.0);
        return Math.max(0, netDistance); // Distance cannot be negative
    }

    /**
     * Renders the vehicle on the canvas.
     * @param gc The GraphicsContext for drawing.
     */
    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angleDeg);

        double visualScaleFactor = 2.0; // Adjust for visual size
        double visualLength = type.getLength() * visualScaleFactor;
        double visualWidth = type.getWidth() * visualScaleFactor;

        // Draw vehicle body
        gc.setFill(color);
        gc.fillRect(-visualLength / 2, -visualWidth / 2, visualLength, visualWidth);

        // Draw windshield indicator
        gc.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        double windshieldWidth = visualWidth * 0.8;
        double windshieldHeight = visualLength * 0.25;
        gc.fillRect(visualLength * 0.15, -windshieldWidth / 2, windshieldHeight, windshieldWidth);

        // Draw outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeRect(-visualLength / 2, -visualWidth / 2, visualLength, visualWidth);

        gc.restore();
    }

    // --- Getters ---
    public double getX() { return x; }
    public double getY() { return y; }
    public VehicleType getType() { return type; }
    /** @return Current speed in m/s. */
    public double getSpeed() { return speed; }
    public Lane getCurrentLane() { return currentLane; }
    /** @return Fractional position [0.0, 1.0] on the current lane. */
    public double getPosition() { return position; }
    /** @return Logical length of the vehicle. */
    public double getLength() { return type.getLength(); }
}