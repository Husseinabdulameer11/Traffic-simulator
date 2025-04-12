
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// VehicleType enum definition (can be here or in its own file)
enum VehicleType {
    CAR(4.5, 2.0), TRUCK(8.0, 2.5);
    private final double length; private final double width;
    VehicleType(double l, double w){length=l;width=w;}
    public double getLength(){return length;} public double getWidth(){return width;}
}

/**
 * Represents a single vehicle within the simulation.
 * Manages its state, physics-based movement (IDM), lane adherence,
 * and interactions with traffic lights and other vehicles.
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
    private double stopPositionFraction = 1.0;

    // --- Constants ---
    private static final double STOP_DISTANCE_BEFORE_JUNCTION = 5.0; // pixels
    private static final double TRAFFIC_LIGHT_CHECK_DISTANCE = 50.0; // pixels

    /**
     * Constructs a new Vehicle.
     * @param type VehicleType (CAR or TRUCK).
     * @param color Rendering color.
     * @param lane Initial lane. Cannot be null.
     * @param vm VehicleManager reference. Cannot be null.
     * @param rn RoadNetwork reference. Cannot be null.
     */
    public Vehicle(VehicleType type, Color color, Lane lane, VehicleManager vm, RoadNetwork rn) {
        if (lane == null || vm == null || rn == null) {
            throw new IllegalArgumentException("Null references not allowed in Vehicle constructor.");
        }
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
        if (currentLane == null || deltaTime <= 0) return; // Vehicle might have been removed or invalid step

        // Order of operations:
        // 1. Check lights (determines if braking is mandatory)
        // 2. Calculate desired acceleration (IDM)
        // 3. Apply braking or IDM acceleration
        // 4. Update speed
        // 5. Update position (handles lane end/exit/change)
        // 6. Update visual representation coords/angle

        checkTrafficLightAndDetermineStop();
        Vehicle leader = findVehicleAhead();
        double desiredAcceleration = calculateIDMAcceleration(leader);

        if (mustStopForLight) {
            applyBrakingForLight();
        } else {
            this.acceleration = desiredAcceleration;
        }
        // Clamp acceleration to limits
        this.acceleration = Math.max(-getComfortableDeceleration(), Math.min(maxAcceleration, this.acceleration));

        updateSpeed(deltaTime);
        updatePositionAndLaneChange(deltaTime); // This method now calls requestRemoval if needed

        // Update visuals only if the vehicle is still on a lane (hasn't been removed conceptually)
        if (currentLane != null) {
            updateVisuals();
        }
    }
    /**
     * Calculates straight-line distance to the end of the current lane (junction entrance)
     */
    public double getDistanceToJunction() {
        if (currentLane == null) return Double.MAX_VALUE;

        // Get vehicle's current position
        double vehicleX = this.x;  // Your x-coordinate field name
        double vehicleY = this.y;  // Your y-coordinate field name

        // Get lane's endpoint (junction position)
        double junctionX = currentLane.getEndX();
        double junctionY = currentLane.getEndY();

        // Calculate Euclidean distance manually
        double dx = junctionX - vehicleX;
        double dy = junctionY - vehicleY;
        return Math.sqrt(dx * dx + dy * dy);
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
        // IDM formula components: s*, free road, interaction
        double desiredSpacing = minDistance + Math.max(0, this.speed * desiredTimeGap + (this.speed * deltaSpeed) / (2 * Math.sqrt(maxAcceleration * Math.abs(getComfortableDeceleration()))));
        double freeRoadComponent = maxAcceleration * (1 - Math.pow(this.speed / maxSpeed, 4));
        double interactionComponent = (leader != null) ? -maxAcceleration * Math.pow(desiredSpacing / Math.max(spacing, minDistance / 2.0), 2) : 0;
        return freeRoadComponent + interactionComponent;
    }

    /** Applies braking logic if required to stop for a traffic light. */
    private void applyBrakingForLight() {
        double distanceToStopPoint = (stopPositionFraction - position) * currentLane.getLength();
        if (distanceToStopPoint > 0.1) { // Still before the stop point
            // Calculate deceleration needed to stop exactly at the stop point
            double requiredDecel = (speed * speed) / (2 * Math.max(0.1, distanceToStopPoint));
            // Apply necessary braking, capped reasonably
            this.acceleration = -Math.min(Math.max(requiredDecel, getComfortableDeceleration() * 0.8), getComfortableDeceleration() * 2.5); // Allow slightly harder braking
        } else {
            // At or past the stop point, ensure full stop
            this.acceleration = 0;
            this.speed = 0;
        }
    }

    /** Updates speed based on current acceleration and delta time. */
    private void updateSpeed(double deltaTime) {
        this.speed += this.acceleration * deltaTime;
        this.speed = Math.max(0, this.speed); // Speed cannot be negative
    }

    /**
     * Updates the vehicle's fractional position along the current lane.
     * If the end of the lane is reached, it attempts to find the next lane.
     * If the lane exits the network, it requests removal from the VehicleManager.
     * If it reaches a dead end at a junction, it requests removal.
     * Includes debugging output for lane change attempts.
     * @param deltaTime Time elapsed in seconds.
     */
    private void updatePositionAndLaneChange(double deltaTime) {
        // Use average speed over timestep for position update (optional, using current speed is simpler)
        double distanceToTravel = this.speed * deltaTime;

        // Only process movement if the vehicle has speed
        if (distanceToTravel > 0.001 && currentLane != null) {
            double laneLength = Math.max(0.01, currentLane.getLength());
            double positionChange = distanceToTravel / laneLength;
            double nextTheoreticalPosition = position + positionChange;

            // Check if the vehicle reaches or passes the end of the current lane
            if (nextTheoreticalPosition >= 1.0 - 0.001) { // Check against end (with tolerance)

                Junction endingJunction = currentLane.getEndingJunction();

                if (endingJunction != null) {
                    // Arrived at a junction, find the next lane
                    Lane nextLane = roadNetwork.getNextLane(currentLane, endingJunction);
                    if (nextLane != null) {
                        // Transition to the next lane
                        double overshootDistance = distanceToTravel - (laneLength * (1.0 - position));
                        currentLane = nextLane;
                        position = Math.min(1.0, overshootDistance / Math.max(0.01, currentLane.getLength()));
                        mustStopForLight = false; // Reset state for new lane
                    } else {
                        // No next lane from this junction (dead end) -> Request removal
                        // System.out.println("Vehicle " + System.identityHashCode(this) + " reached dead end at J" + endingJunction.getId() + ". Requesting removal.");
                        vehicleManager.requestRemoval(this);
                        currentLane = null; // Mark as invalid for further processing this frame
                    }
                } else {
                    // Reached the end of a lane that is NOT connected to a junction -> Vehicle exits network
                    // System.out.println("Vehicle " + System.identityHashCode(this) + " exited network via lane " + currentLane + ". Requesting removal.");
                    vehicleManager.requestRemoval(this);
                    currentLane = null; // Mark as invalid
                }
            } else {
                // Continue moving along the current lane
                position = nextTheoreticalPosition;
            }
        }
        // Ensure position stays within valid bounds if still on a lane
        if (currentLane != null) {
            position = Math.max(0.0, Math.min(position, 1.0));
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

            if (position < stopPositionFraction) { // Only check if before stop point
                double distanceToStopPixels = (stopPositionFraction - position) * laneLength;
                if (distanceToStopPixels < TRAFFIC_LIGHT_CHECK_DISTANCE) { // Only check if close enough
                    TrafficLight light = endJunction.getTrafficLightForLane(currentLane);
                    if (light != null) {
                        LightState state = light.getCurrentState();
                        if (state == LightState.RED || state == LightState.YELLOW) {
                            this.mustStopForLight = true;
                        }
                    } else { /* Error already logged if light is null */ }
                }
            }
        }
    }

    /** Updates visual coordinates (x, y, angle) based on current position and lane. */
    private void updateVisuals() {
        if (currentLane == null) return; // Don't update if lane is invalid (e.g., after removal request)
        double[] coords = currentLane.getPointAt(position);
        this.x = coords[0];
        this.y = coords[1];
        this.angleDeg = Math.toDegrees(currentLane.getAngleRad());
    }

    /** Finds the vehicle directly ahead on the same lane. */
    private Vehicle findVehicleAhead() {
        return (vehicleManager != null) ? vehicleManager.findVehicleAheadOnLane(this, currentLane) : null;
    }

    /** Calculates bumper-to-bumper distance to the vehicle ahead. */
    private double calculateNetDistance(Vehicle ahead) {
        // Ensure leader is not null before proceeding
        if(ahead == null) return Double.POSITIVE_INFINITY;
        double positionDifference = ahead.position - this.position;
        // Handle wrap-around case for circular lanes (not applicable here but good practice)
        // if (positionDifference < 0) positionDifference += 1.0; // Assumes fractional position [0,1)
        double centerToCenterDistance = positionDifference * currentLane.getLength();
        double netDistance = centerToCenterDistance - (ahead.type.getLength() / 2.0) - (this.type.getLength() / 2.0);
        return Math.max(0, netDistance); // Prevent negative distance
    }

    /**
     * Renders the vehicle on the canvas.
     * @param gc The GraphicsContext for drawing.
     */
    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angleDeg);

        double visualScaleFactor = 2.0;
        double visualLength = type.getLength() * visualScaleFactor;
        double visualWidth = type.getWidth() * visualScaleFactor;

        // Draw main body
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

    /** @return The center x-coordinate for rendering. */
    public double getX() {
        return x;
    }

    /** @return The center y-coordinate for rendering. */
    public double getY() {
        return y;
    }

    /** @return The {@link VehicleType} (e.g., CAR, TRUCK). */
    public VehicleType getType() {
        return type;
    }

    /** @return The current speed of the vehicle in meters per second. */
    public double getSpeed() {
        return speed;
    }

    /** @return The current {@link Lane} the vehicle is occupying. */
    public Lane getCurrentLane() {
        return currentLane;
    }

    /** @return The vehicle's current fractional position [0.0, 1.0] on its lane. */
    public double getPosition() {
        return position;
    }

    /** @return The logical length of the vehicle based on its type. */
    public double getLength() {
        return type.getLength();
    }

} // End of Vehicle class