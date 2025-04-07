// Filnavn: Road.java
// (No package declaration for default package)

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Required for Objects.hash and Objects.equals

/**
 * Represents a road segment in the simulation network.
 * A road connects two points (potentially junctions) and contains one or more lanes.
 * Implements {@code equals()} and {@code hashCode()} based on a unique ID for reliable use in collections.
 */
public class Road {
    // Unique identifier for this road segment.
    private final int id;
    // Coordinates for the start and end points of the road's centerline.
    private final double startX, startY, endX, endY;
    // Visual width of the road surface.
    private final double width = 30; // Default width
    // List of lanes belonging to this road (typically one forward, one backward).
    private final List<Lane> lanes = new ArrayList<>();
    // References to the junctions connected at the start and end points.
    // These are determined after all roads and junctions are created by calling connectToJunctions.
    private Junction startJunction = null;
    private Junction endJunction = null;

    // Static counter to ensure unique IDs for each road instance.
    private static int nextId = 0;

    /**
     * Constructs a new Road segment between the specified start and end points.
     * Automatically creates forward and backward lanes associated with this road.
     *
     * @param startX The x-coordinate of the starting point.
     * @param startY The y-coordinate of the starting point.
     * @param endX   The x-coordinate of the ending point.
     * @param endY   The y-coordinate of the ending point.
     */
    public Road(double startX, double startY, double endX, double endY) {
        this.id = nextId++; // Assign the next available unique ID
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        // Initialize the lanes associated with this road upon construction.
        createLanes();
    }

    /**
     * Creates the default forward and backward {@link Lane} objects for this road segment.
     * Clears any existing lanes first to ensure a clean state.
     */
    private void createLanes() {
        lanes.clear();
        // Assumes a standard two-way road configuration
        lanes.add(new Lane(this, true));  // Lane in the direction start -> end
        lanes.add(new Lane(this, false)); // Lane in the direction end -> start
    }

    /**
     * Attempts to connect the start and end points of this road to the nearest junctions
     * from the provided list, within a defined threshold distance. This method updates the
     * {@code startJunction} and {@code endJunction} fields of this road and registers
     * this road with the connected junctions.
     * This method should typically be called after all junctions in the network have been created.
     *
     * @param allJunctions A list containing all {@link Junction} objects in the simulation network.
     */
    public void connectToJunctions(List<Junction> allJunctions) {
        // Find the closest junction to the road's defined start point
        this.startJunction = findClosestJunction(startX, startY, allJunctions);
        // Find the closest junction to the road's defined end point
        this.endJunction = findClosestJunction(endX, endY, allJunctions);

        // Register this road with the found junctions (if any)
        if (startJunction != null) {
            startJunction.connectRoad(this);
        }
        if (endJunction != null) {
            // Connect road to the end junction, even if it's the same as the start (loop road)
            endJunction.connectRoad(this);
        }
    }

    /**
     * Finds the closest junction to a given point (x, y) within a calculated threshold distance.
     * The threshold considers the junction's radius plus a fixed margin to allow for slight
     * inaccuracies in coordinate definitions.
     *
     * @param pointX The x-coordinate of the point to check (typically a road endpoint).
     * @param pointY The y-coordinate of the point to check.
     * @param junctions The list of all potential junctions to check against.
     * @return The closest {@link Junction} within the threshold, or null if none are close enough.
     */
    private Junction findClosestJunction(double pointX, double pointY, List<Junction> junctions) {
        Junction closestJunction = null;
        double minDistanceSquared = Double.MAX_VALUE;
        // Define a margin around the junction's radius for connection eligibility
        double connectionMargin = 15.0; // Pixels

        for (Junction currentJunction : junctions) {
            // Calculate squared distance for efficiency (avoids sqrt)
            double dx = currentJunction.getX() - pointX;
            double dy = currentJunction.getY() - pointY;
            double distanceSquared = dx * dx + dy * dy;

            // Calculate the squared threshold distance = (radius + margin)^2
            double effectiveRadius = currentJunction.getRadius() + connectionMargin;
            double thresholdSquared = effectiveRadius * effectiveRadius;

            // Check if this junction is closer than the current best and within the threshold
            if (distanceSquared < minDistanceSquared && distanceSquared < thresholdSquared) {
                minDistanceSquared = distanceSquared;
                closestJunction = currentJunction;
            }
        }
        // Return the closest junction found within the threshold, or null if none qualified
        return closestJunction;
    }


    /**
     * Renders the visual representation of the road segment, including the road surface
     * and a dashed centerline, onto the provided GraphicsContext.
     * Does not render the individual lanes directly.
     *
     * @param gc The GraphicsContext for drawing.
     */
    public void render(GraphicsContext gc) {
        // Draw the main road surface as a thick gray line
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(width);
        gc.strokeLine(startX, startY, endX, endY);

        // Draw the dashed white centerline for visual separation
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(10, 10); // Set the dash pattern (10 pixels drawn, 10 pixels skipped)
        gc.strokeLine(startX, startY, endX, endY);
        gc.setLineDashes(0); // Reset dash pattern to solid for subsequent drawing operations
    }

    /**
     * Compares this Road object to another object for equality.
     * Two Roads are considered equal if and only if they have the same unique ID.
     *
     * @param o The object to compare against.
     * @return True if the objects represent the same road (based on ID), false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Check for same instance
        if (o == null || getClass() != o.getClass()) return false; // Check for null or different class
        Road road = (Road) o;
        return id == road.id; // Equality is determined solely by the unique ID
    }

    /**
     * Generates a hash code for this Road object.
     * The hash code is based solely on the unique ID, ensuring consistency with {@link #equals(Object)}.
     *
     * @return An integer hash code based on the road's ID.
     */
    @Override
    public int hashCode() {
        // Use Objects.hash for a standard and robust hash code generation based on the ID
        return Objects.hash(id);
    }

    /**
     * Provides a string representation of the Road, primarily for debugging purposes.
     * Includes the road's ID and the IDs of its connected start and end junctions (if any).
     *
     * @return A descriptive string for this Road object.
     */
    @Override
    public String toString() {
        // Format includes hashcode (for identity), ID, and connected junction IDs
        return "Road@" + Integer.toHexString(hashCode()) + "{" +
                "id=" + id +
                ", startJ=" + (startJunction != null ? startJunction.getId() : "null") +
                ", endJ=" + (endJunction != null ? endJunction.getId() : "null") +
                '}';
    }

    // --- Standard Getters ---

    /** @return The unique identifier of this road segment. */
    public int getId() { return id; }
    /** @return The x-coordinate of the road's centerline starting point. */
    public double getStartX() { return startX; }
    /** @return The y-coordinate of the road's centerline starting point. */
    public double getStartY() { return startY; }
    /** @return The x-coordinate of the road's centerline ending point. */
    public double getEndX() { return endX; }
    /** @return The y-coordinate of the road's centerline ending point. */
    public double getEndY() { return endY; }
    /** @return The visual width of the road surface. */
    public double getWidth() { return width; }
    /** @return The calculated length of the road's centerline. */
    public double getLength() {
        // Calculate length dynamically using the Pythagorean theorem
        return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
    }
    /** @return The list of {@link Lane} objects associated with this road (typically forward and backward). */
    public List<Lane> getLanes() { return lanes; }
    /** @return The {@link Junction} connected at the road's start point, or null if not connected. */
    public Junction getStartJunction() { return startJunction; }
    /** @return The {@link Junction} connected at the road's end point, or null if not connected. */
    public Junction getEndJunction() { return endJunction; }

    /**
     * Convenience method to get the forward lane (start -> end direction).
     * @return The forward {@link Lane}, or null if it doesn't exist.
     */
    public Lane getForwardLane() {
        // Use stream API to find the first lane where isForward is true
        return lanes.stream().filter(Lane::isForward).findFirst().orElse(null);
    }

    /**
     * Convenience method to get the backward lane (end -> start direction).
     * @return The backward {@link Lane}, or null if it doesn't exist.
     */
    public Lane getBackwardLane() {
        // Use stream API to find the first lane where isForward is false
        return lanes.stream().filter(l -> !l.isForward()).findFirst().orElse(null);
    }
}