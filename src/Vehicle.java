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
    /**
     * Represents a single vehicle within the simulation.
     * Manages its state, physics-based movement (IDM), lane adherence,
     * and interactions with traffic lights and other vehicles.
     */
    private final VehicleType type;
    private final Color color;
    private Lane currentLane;
    private double position;
    private double speed;
    private double maxSpeed = 15;
    private double acceleration = 0;


    private final double maxAcceleration = 2.0;
    private final double desiredTimeGap = 1.5;
    private final double minDistance = 2.0;

    private double x, y;
    private double angleDeg;

    private final VehicleManager vehicleManager;
    private final RoadNetwork roadNetwork;

    private boolean mustStopForLight = false;
    private double stopPositionFraction = 1.0;

    private static final double STOP_DISTANCE_BEFORE_JUNCTION = 5.0;
    private static final double TRAFFIC_LIGHT_CHECK_DISTANCE = 50.0;

    public Vehicle(VehicleType type, Color color, Lane lane, VehicleManager vm, RoadNetwork rn) {
        if (lane == null || vm == null || rn == null) {
            throw new IllegalArgumentException("Null references not allowed in Vehicle constructor.");
        }
        this.type = type;
        this.color = color;
        this.currentLane = lane;
        this.position = 0.0;
        this.speed = maxSpeed * (0.3 + Math.random() * 0.5);
        this.vehicleManager = vm;
        this.roadNetwork = rn;
        updateVisuals();
    }

    public void update(double deltaTime) {
        if (currentLane == null || deltaTime <= 0) return;

        checkTrafficLightAndDetermineStop();
        Vehicle leader = findVehicleAhead();
        double desiredAcceleration = calculateIDMAcceleration(leader);

        if (mustStopForLight) {
            applyBrakingForLight();
        } else {
            this.acceleration = desiredAcceleration;
        }
        this.acceleration = Math.max(-getComfortableDeceleration(), Math.min(maxAcceleration, this.acceleration));

        updateSpeed(deltaTime);
        updatePositionAndLaneChange(deltaTime);

        if (currentLane != null) {
            updateVisuals();
        }
    }

    private double getComfortableDeceleration() {
        return maxAcceleration * 1.2;
    }

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

    private void applyBrakingForLight() {
        double distanceToStopPoint = (stopPositionFraction - position) * currentLane.getLength();
        if (distanceToStopPoint > 0.1) {
            double requiredDecel = (speed * speed) / (2 * Math.max(0.1, distanceToStopPoint));
            this.acceleration = -Math.min(Math.max(requiredDecel, getComfortableDeceleration() * 0.8), getComfortableDeceleration() * 2.5);
        } else {
            this.acceleration = 0;
            this.speed = 0;
        }
    }

    private void updateSpeed(double deltaTime) {
        this.speed += this.acceleration * deltaTime;
        this.speed = Math.max(0, this.speed);
    }

    /**
     * Updates the vehicle's fractional position along the current lane.
     * If the end of the lane is reached, it attempts to find the next lane.
     * only one vehicle may enter the junction at a time.
     * Prevents collisions and deadlocks when multiple vehicles approach the junction concurrently
     * @param deltaTime Time elapsed in seconds.
     */
    private void updatePositionAndLaneChange(double deltaTime) {
        double distanceToTravel = this.speed * deltaTime;

        if (distanceToTravel > 0.001 && currentLane != null) {
            double laneLength = Math.max(0.01, currentLane.getLength());
            double positionChange = distanceToTravel / laneLength;
            double nextTheoreticalPosition = position + positionChange;

            if (nextTheoreticalPosition >= 1.0 - 0.001) {
                Junction endingJunction = currentLane.getEndingJunction();

                if (endingJunction != null) {
                    // Waiting to enter
                    endingJunction.enterJunction(); // only one vehicle can be in the junction at a time

                    Lane nextLane = roadNetwork.getNextLane(currentLane, endingJunction);
                    if (nextLane != null) {
                        double overshootDistance = distanceToTravel - (laneLength * (1.0 - position));
                        currentLane = nextLane;
                        position = Math.min(1.0, overshootDistance / Math.max(0.01, currentLane.getLength()));
                        mustStopForLight = false;
                    } else {
                        vehicleManager.requestRemoval(this);
                        currentLane = null;
                    }
                    endingJunction.leaveJunction();
                } else {
                    vehicleManager.requestRemoval(this);
                    currentLane = null;
                }
            } else {
                position = nextTheoreticalPosition;
            }
        }

        if (currentLane != null) {
            position = Math.max(0.0, Math.min(position, 1.0));
        }
    }

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
                if (distanceToStopPixels < TRAFFIC_LIGHT_CHECK_DISTANCE) {
                    TrafficLight light = endJunction.getTrafficLightForLane(currentLane);
                    if (light != null) {
                        LightState state = light.getCurrentState();
                        if (state == LightState.RED || state == LightState.YELLOW) {
                            this.mustStopForLight = true;
                        }
                    }
                }
            }
        }
    }

    private void updateVisuals() {
        if (currentLane == null) return;
        double[] coords = currentLane.getPointAt(position);
        this.x = coords[0];
        this.y = coords[1];
        this.angleDeg = Math.toDegrees(currentLane.getAngleRad());
    }

    private Vehicle findVehicleAhead() {
        return (vehicleManager != null) ? vehicleManager.findVehicleAheadOnLane(this, currentLane) : null;
    }

    private double calculateNetDistance(Vehicle ahead) {
        if(ahead == null) return Double.POSITIVE_INFINITY;
        double positionDifference = ahead.position - this.position;
        double centerToCenterDistance = positionDifference * currentLane.getLength();
        double netDistance = centerToCenterDistance - (ahead.type.getLength() / 2.0) - (this.type.getLength() / 2.0);
        return Math.max(0, netDistance);
    }

    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angleDeg);

        double visualScaleFactor = 2.0;
        double visualLength = type.getLength() * visualScaleFactor;
        double visualWidth = type.getWidth() * visualScaleFactor;

        gc.setFill(color);
        gc.fillRect(-visualLength / 2, -visualWidth / 2, visualLength, visualWidth);

        gc.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        double windshieldWidth = visualWidth * 0.8;
        double windshieldHeight = visualLength * 0.25;
        gc.fillRect(visualLength * 0.15, -windshieldWidth / 2, windshieldHeight, windshieldWidth);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeRect(-visualLength / 2, -visualWidth / 2, visualLength, visualWidth);

        gc.restore();
    }
    /**
     * Calculates straight-line distance to the end of the current lane (junction entrance).
     * Useful for determining whether the vehicle is approaching a junction.
     * @return Distance in pixels to junction end, or MAX_VALUE if lane is null.
     */
    public double getDistanceToJunction() {
        if (currentLane == null) return Double.MAX_VALUE;

        double vehicleX = this.x;
        double vehicleY = this.y;

        double junctionX = currentLane.getEndX();
        double junctionY = currentLane.getEndY();

        double dx = junctionX - vehicleX;
        double dy = junctionY - vehicleY;
        return Math.sqrt(dx * dx + dy * dy);
    }


    public double getX() { return x; }
    public double getY() { return y; }
    public VehicleType getType() { return type; }
    public double getSpeed() { return speed; }
    public Lane getCurrentLane() { return currentLane; }
    public double getPosition() { return position; }
    public double getLength() { return type.getLength(); }
}
