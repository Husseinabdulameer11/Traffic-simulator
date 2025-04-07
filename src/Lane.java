

import java.util.Objects; // Required for Objects.hash and Objects.equals

/**
 * Represents a single lane of traffic along a {@link Road}.
 * A lane has its own geometry (start/end points) offset from the road's centerline,
 * determined by the driving direction ({@code isForward}) and a predefined offset.
 * Implements {@code equals()} and {@code hashCode()} for correct use as keys in Maps (e.g., in Junction).
 */
public class Lane {
    // The parent Road segment this lane belongs to.
    private final Road road;
    // Directionality flag: true if the lane follows the Road's start->end direction,
    // false if it follows the end->start direction.
    private final boolean isForward;
    // Calculated geometric properties of the lane centerline.
    private double length;
    private double startX, startY, endX, endY;
    private double angleRad; // Angle of the lane in radians (used for vehicle orientation)

    // Constant defining the distance from the Road's centerline to this lane's centerline.
    private static final double LANE_OFFSET = 7.5; // Pixels

    /**
     * Constructs a new Lane associated with a given Road.
     * Calculates the lane's geometry based on the road and direction.
     *
     * @param road      The parent {@link Road} object. Cannot be null.
     * @param isForward True if this lane follows the road's start-to-end direction,
     *                  false for the end-to-start direction.
     */
    public Lane(Road road, boolean isForward) {
        // Input validation
        if (road == null) {
            throw new IllegalArgumentException("Road cannot be null when creating a Lane.");
        }
        this.road = road;
        this.isForward = isForward;
        // Calculate and store the lane's specific geometry upon creation.
        calculateGeometry();
    }

    /**
     * Calculates the start/end coordinates, length, and angle of this lane
     * based on the parent road's geometry and the {@code LANE_OFFSET}.
     * This is called internally during construction.
     */
    private void calculateGeometry() {
        // Get parent road's centerline endpoints
        double rStartX = road.getStartX();
        double rStartY = road.getStartY();
        double rEndX = road.getEndX();
        double rEndY = road.getEndY();

        // Calculate road vector and length
        double dx = rEndX - rStartX;
        double dy = rEndY - rStartY;
        this.length = Math.sqrt(dx * dx + dy * dy);

        // Handle potential zero-length roads to avoid division by zero errors
        if (this.length < 0.01) {
            this.startX = rStartX;
            this.startY = rStartY;
            this.endX = rEndX;
            this.endY = rEndY;
            this.angleRad = 0; // Assign a default angle
            // Optionally log a warning here if zero-length roads are unexpected
            return;
        }

        // Calculate normalized direction vector of the parent road (start -> end)
        double dirX = dx / length;
        double dirY = dy / length;

        // Calculate a perpendicular vector pointing 90 degrees counter-clockwise (to the left)
        // relative to the road's direction vector (start -> end).
        double perpX = -dirY;
        double perpY = dirX;

        // Determine the offset magnitude based on the lane's direction relative to the road.
        // Using a right-hand traffic rule: forward lanes are offset to the right,
        // backward lanes are also offset to their right (which is left relative to the road's forward direction).
        double actualOffset = isForward ? LANE_OFFSET : -LANE_OFFSET;

        // Apply the offset perpendicular to the road's direction to find the lane's endpoints.
        this.startX = rStartX + perpX * actualOffset;
        this.startY = rStartY + perpY * actualOffset;
        this.endX = rEndX + perpX * actualOffset;
        this.endY = rEndY + perpY * actualOffset;


        // Calculate the lane's angle in radians, crucial for vehicle orientation.
        // The angle depends on the actual direction of travel along this specific lane.
        if (isForward) {
            // Angle of the vector from lane start point to lane end point
            this.angleRad = Math.atan2(this.endY - this.startY, this.endX - this.startX);
        } else {
            // Angle of the vector from the effective start (road end) to effective end (road start)
            // This ensures vehicles traveling backward are oriented correctly.
            this.angleRad = Math.atan2(this.startY - this.endY, this.startX - this.endX);
        }
    }

    /**
     * Calculates the world coordinates (x, y) for a given fractional position along this lane's centerline.
     *
     * @param positionFraction The position along the lane, where 0.0 represents the start
     *                         and 1.0 represents the end of the lane's travel direction.
     *                         Values outside the range [0.0, 1.0] are clamped.
     * @return A double array containing the [x, y] coordinates.
     */
    public double[] getPointAt(double positionFraction) {
        // Clamp the input fraction to ensure it's within the valid range [0.0, 1.0].
        double actualPos = Math.max(0.0, Math.min(1.0, positionFraction));

        // Handle zero-length lanes gracefully by returning the start coordinate.
        if (length < 0.01) {
            return new double[]{startX, startY};
        }

        // Perform linear interpolation between the lane's start and end points
        // based on the direction of travel (isForward).
        double currentX, currentY;
        if (isForward) {
            // Interpolate from startX, startY towards endX, endY
            currentX = startX + (endX - startX) * actualPos;
            currentY = startY + (endY - startY) * actualPos;
        } else {
            // For backward lanes, interpolate from the effective start (endX, endY)
            // towards the effective end (startX, startY).
            currentX = endX + (startX - endX) * actualPos;
            currentY = endY + (startY - endY) * actualPos;
        }
        return new double[]{currentX, currentY};
    }

    /**
     * Gets the {@link Junction} located at the end of this lane's direction of travel.
     * This relies on the junction connections previously established in the parent {@link Road}.
     *
     * @return The {@link Junction} at the destination end of this lane,
     *         or null if the corresponding road end is not connected to a junction.
     */
    public Junction getEndingJunction() {
        // If traveling forward (start->end on road), the ending junction is the road's endJunction.
        // If traveling backward (end->start on road), the ending junction is the road's startJunction.
        return isForward ? road.getEndJunction() : road.getStartJunction();
    }

    /**
     * Gets the {@link Junction} located at the start of this lane's direction of travel.
     * This relies on the junction connections previously established in the parent {@link Road}.
     *
     * @return The {@link Junction} at the origin end of this lane,
     *         or null if the corresponding road start is not connected to a junction.
     */
    public Junction getStartingJunction() {
        // If traveling forward (start->end on road), the starting junction is the road's startJunction.
        // If traveling backward (end->start on road), the starting junction is the road's endJunction.
        return isForward ? road.getStartJunction() : road.getEndJunction();
    }

    /**
     * Compares this Lane to another object for equality.
     * Two Lanes are considered equal if they belong to the same Road
     * and have the same directionality (isForward).
     * Relies on the parent Road having a proper equals() implementation (e.g., based on ID).
     *
     * @param o The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Same object instance
        if (o == null || getClass() != o.getClass()) return false; // Null or different class
        Lane otherLane = (Lane) o;
        // Check both directionality and equality of the parent road
        return isForward == otherLane.isForward && Objects.equals(road, otherLane.road);
    }

    /**
     * Generates a hash code for this Lane object.
     * The hash code is based on the parent Road object and the directionality (isForward),
     * consistent with the fields used in the {@link #equals(Object)} method.
     * Relies on the parent Road having a proper hashCode() implementation.
     *
     * @return An integer hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(road, isForward);
    }

    /**
     * Provides a string representation of the Lane, useful for debugging.
     *
     * @return A string describing the lane, including its internal hashcode, parent road ID, and direction.
     */
    @Override
    public String toString() {
        // Include relevant info for debugging map lookups
        return "Lane@" + Integer.toHexString(hashCode()) + "{" +
                "roadId=" + (road != null ? road.getId() : "null") +
                ", isForward=" + isForward +
                '}';
    }

    // --- Standard Getters ---

    /** @return The parent {@link Road} this lane belongs to. */
    public Road getRoad() { return road; }
    /** @return True if this lane follows the road's start-to-end direction, false otherwise. */
    public boolean isForward() { return isForward; }
    /** @return The calculated length of this lane's centerline. */
    public double getLength() { return length; }
    /** @return The x-coordinate of the lane's starting point. */
    public double getStartX() { return startX; }
    /** @return The y-coordinate of the lane's starting point. */
    public double getStartY() { return startY; }
    /** @return The x-coordinate of the lane's ending point. */
    public double getEndX() { return endX; }
    /** @return The y-coordinate of the lane's ending point. */
    public double getEndY() { return endY; }
    /** @return The angle of the lane in radians, based on its direction of travel. */
    public double getAngleRad() { return angleRad; }
}