// Filnavn: VehicleManager.java
// (Ingen package-setning)

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator; // Iterator can still be useful, but we manage removal separately
import java.util.List;
import java.util.Random;

/**
 * Manages the lifecycle of all vehicles in the simulation.
 * Handles spawning new vehicles based on inflow rate, updating existing vehicles,
 * rendering them, and removing vehicles that exit the network or meet removal criteria.
 */
public class VehicleManager {
    // The main list of active vehicles currently in the simulation.
    private final List<Vehicle> vehicles = new ArrayList<>();
    // A temporary list to hold vehicles marked for removal during an update cycle.
    // This avoids ConcurrentModificationException when removing while iterating.
    private final List<Vehicle> vehiclesToRemove = new ArrayList<>();
    // Reference to the road network for spawning and navigation context.
    private final RoadNetwork roadNetwork;
    // Current target inflow rate (vehicles per hour).
    private double inflow = 1000; // Default value
    // Random number generator for vehicle types, colors, etc.
    private final Random random = new Random();
    // Timer to track time since the last vehicle spawn attempt.
    private double spawnTimer = 0;
    // Calculated inflow rate in vehicles per second for easier use with deltaTime.
    private double inflowRateVehiclesPerSecond;

    // Predefined set of colors for spawned vehicles.
    private static final Color[] PREDEFINED_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
            Color.PURPLE, Color.BROWN, Color.BLACK, Color.CYAN, Color.MAGENTA,
            Color.PINK, Color.GRAY, Color.DARKCYAN, Color.LIGHTSALMON
    };

    /**
     * Constructs a VehicleManager.
     *
     * @param roadNetwork A reference to the {@link RoadNetwork} instance. Must not be null.
     */
    public VehicleManager(RoadNetwork roadNetwork) {
        if (roadNetwork == null) {
            throw new IllegalArgumentException("RoadNetwork cannot be null.");
        }
        this.roadNetwork = roadNetwork;
        // Calculate the initial vehicles per second rate based on the default inflow.
        setInflow(this.inflow);
    }

    /**
     * Sets the target traffic inflow rate and updates the calculated spawn rate.
     *
     * @param vehiclesPerHour The desired number of vehicles to spawn per hour.
     */
    public void setInflow(double vehiclesPerHour) {
        this.inflow = vehiclesPerHour; // Store the hourly rate if needed elsewhere
        if (this.inflow <= 0) {
            this.inflowRateVehiclesPerSecond = 0; // No spawning if inflow is zero or negative
        } else {
            // Convert hourly rate to vehicles per second
            this.inflowRateVehiclesPerSecond = this.inflow / 3600.0;
        }
        // System.out.println("VehicleManager inflow set to " + this.inflow + " veh/h (" + String.format("%.3f", this.inflowRateVehiclesPerSecond) + " veh/s)");
    }

    /**
     * Updates all managed vehicles for a given time step.
     * This includes attempting to spawn new vehicles based on the inflow rate
     * and updating the state of all active vehicles. Vehicles marked for removal
     * during their update are removed at the end of the cycle.
     *
     * @param deltaTime The time elapsed since the last update, in seconds.
     */
    public void update(double deltaTime) {
        // 1. Handle Vehicle Spawning
        // Only attempt to spawn if there's a positive inflow rate.
        if (inflowRateVehiclesPerSecond > 0) {
            spawnTimer += deltaTime;
            double spawnInterval = 1.0 / inflowRateVehiclesPerSecond; // Time between spawns
            // Allow multiple spawns per frame if deltaTime is large enough
            while (spawnTimer >= spawnInterval) {
                spawnVehicle();
                spawnTimer -= spawnInterval; // Deduct interval for the spawned vehicle
            }
        }

        // 2. Prepare for Vehicle Updates and Removals
        vehiclesToRemove.clear(); // Clear the list of vehicles to be removed from the previous frame

        // 3. Update each active vehicle
        // Iterate using a standard loop; removals are handled afterward.
        for (Vehicle vehicle : vehicles) {
            vehicle.update(deltaTime); // Let the vehicle update its own state
            // A vehicle might call requestRemoval(this) during its update.
        }

        // 4. Remove vehicles marked for deletion
        // This is done after the update loop to avoid modifying the list during iteration.
        if (!vehiclesToRemove.isEmpty()) {
            boolean removed = vehicles.removeAll(vehiclesToRemove);
            // if (removed) { // Optional logging
            //     System.out.println("Removed " + vehiclesToRemove.size() + " vehicles. Remaining: " + vehicles.size());
            // }
        }
    }

    /**
     * Attempts to spawn a new vehicle onto a valid entry lane in the road network.
     * Checks for sufficient space at the spawn point before creating the vehicle.
     */
    private void spawnVehicle() {
        // Find a suitable entry lane using the RoadNetwork utility method.
        Lane entryLane = roadNetwork.getRandomEntryLane();
        if (entryLane == null) {
            // No valid entry lane found (network might be closed or fully occupied near entries).
            // System.err.println("Spawn failed: No available entry lane found.");
            // Prevent constant rapid spawn attempts if no entry is available.
            // Resetting timer ensures a small delay.
            spawnTimer = 0;
            return;
        }

        // Basic check to prevent spawning directly on top of another vehicle.
        double requiredClearance = VehicleType.CAR.getLength() * 1.5; // Minimum empty space needed at start
        for (Vehicle existing : vehicles) {
            // Check only vehicles currently on the same potential entry lane.
            if (existing.getCurrentLane() == entryLane) {
                // Calculate how far the existing vehicle is from the lane start (position 0.0).
                double distanceIntoLane = existing.getPosition() * entryLane.getLength();
                // If an existing vehicle is too close to the start, abort spawn attempt.
                if (distanceIntoLane < requiredClearance) {
                    // System.out.println("Spawn prevented on lane: Collision detected near start.");
                    return;
                }
            }
        }

        // Determine vehicle type (mostly cars, some trucks) and assign a random color.
        VehicleType type = random.nextDouble() < 0.8 ? VehicleType.CAR : VehicleType.TRUCK;
        Color color = getRandomColor();

        // Create and add the new vehicle to the simulation.
        // Pass references to 'this' (VehicleManager) and 'roadNetwork'.
        Vehicle newVehicle = new Vehicle(type, color, entryLane, this, roadNetwork);
        vehicles.add(newVehicle);
        // System.out.println("Spawned vehicle " + System.identityHashCode(newVehicle) + " on lane " + entryLane);
    }

    /**
     * Adds a vehicle to the list scheduled for removal at the end of the current update cycle.
     * This method is typically called by a {@link Vehicle} instance when it determines
     * it should be removed from the simulation (e.g., upon exiting the network).
     * Ensures the same vehicle isn't added multiple times per cycle.
     *
     * @param vehicle The vehicle instance to mark for removal. Must not be null.
     */
    public void requestRemoval(Vehicle vehicle) {
        if (vehicle != null && !vehiclesToRemove.contains(vehicle)) {
            // System.out.println("Vehicle " + System.identityHashCode(vehicle) + " requested removal."); // Debug log
            vehiclesToRemove.add(vehicle);
        }
    }

    /**
     * Selects a random color from a predefined list.
     * @return A randomly chosen {@link Color}.
     */
    private Color getRandomColor() {
        return PREDEFINED_COLORS[random.nextInt(PREDEFINED_COLORS.length)];
    }

    /**
     * Checks if a vehicle's position is significantly outside the expected canvas boundaries.
     * Used as a fallback removal mechanism, although vehicles should ideally be removed
     * by reaching designated exit lanes.
     *
     * @param vehicle The vehicle to check.
     * @return True if the vehicle is considered out of bounds, false otherwise.
     */
    private boolean isOutOfBounds(Vehicle vehicle) {
        double x = vehicle.getX();
        double y = vehicle.getY();
        // Define boundaries slightly outside the visible canvas area
        double margin = 50;
        // Assuming canvas dimensions are known or accessible (using fixed values here)
        double canvasWidth = 1000;
        double canvasHeight = 750;
        return x < -margin || x > canvasWidth + margin || y < -margin || y > canvasHeight + margin;
    }

    /**
     * Renders all active vehicles onto the provided GraphicsContext.
     * Delegates the actual drawing of each vehicle to its own {@code render} method.
     *
     * @param gc The {@link GraphicsContext} to draw on.
     */
    public void render(GraphicsContext gc) {
        for (Vehicle v : vehicles) {
            v.render(gc);
        }
    }

    /**
     * Finds the vehicle directly ahead of a given vehicle on the same lane.
     *
     * @param subjectVehicle The vehicle for which to find the leader.
     * @param lane           The lane both vehicles must be on.
     * @return The leading {@link Vehicle} instance, or null if no vehicle is ahead on the specified lane.
     */
    public Vehicle findVehicleAheadOnLane(Vehicle subjectVehicle, Lane lane) {
        Vehicle leader = null;
        double minPositiveDistanceFraction = Double.POSITIVE_INFINITY; // Smallest fractional distance > 0

        for (Vehicle otherVehicle : vehicles) {
            // Skip self and vehicles on different lanes
            if (otherVehicle == subjectVehicle || otherVehicle.getCurrentLane() != lane) {
                continue;
            }

            // Calculate the difference in fractional positions along the lane
            double positionDifference = otherVehicle.getPosition() - subjectVehicle.getPosition();

            // Consider only vehicles that are strictly ahead (positive difference)
            // Use a small epsilon to handle potential floating-point inaccuracies near zero
            if (positionDifference > 0.0001) {
                // If this vehicle is closer than the current leader found, update the leader
                if (positionDifference < minPositiveDistanceFraction) {
                    minPositiveDistanceFraction = positionDifference;
                    leader = otherVehicle;
                }
            }
            // Note: This simple implementation does not handle lane wrapping (e.g., in a circular road)
        }
        return leader; // Return the closest vehicle ahead, or null
    }

    /**
     * Gets the current list of active vehicles in the simulation.
     * @return A {@link List} of {@link Vehicle} objects.
     */
    public List<Vehicle> getVehicles() {
        return vehicles;
    }

} // End of VehicleManager class