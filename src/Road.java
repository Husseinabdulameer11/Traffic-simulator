
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Viktig for equals/hashCode

public class Road {
    private final int id; // Unik ID for hver vei
    private double startX, startY, endX, endY;
    private double width = 30;
    private List<Lane> lanes = new ArrayList<>();
    private Junction startJunction = null;
    private Junction endJunction = null;

    private static int nextId = 0; // Statisk teller for IDer

    public Road(double startX, double startY, double endX, double endY) {
        this.id = nextId++; // Tildel unik ID
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        createLanes();
    }

    private void createLanes() {
        lanes.clear();
        lanes.add(new Lane(this, true));  // Forward direction
        lanes.add(new Lane(this, false)); // Backward direction
    }

    /**
     * Connects this road to the nearest junctions at its start and end points.
     * This method should be called after all junctions have been created.
     * @param allJunctions A list of all junctions in the network.
     */
    public void connectToJunctions(List<Junction> allJunctions) {
        this.startJunction = findClosestJunction(startX, startY, allJunctions);
        this.endJunction = findClosestJunction(endX, endY, allJunctions);
        if (startJunction != null) startJunction.connectRoad(this);
        // Connect end junction only if it's different from the start junction,
        // or connect anyway if it's the same (e.g., loop road).
        if (endJunction != null) {
            endJunction.connectRoad(this);
        }

        // Debugging: Print connection results
        // System.out.println("Road " + id + " connected: StartJ=" + (startJunction != null ? startJunction.getId() : "null")
        //                  + ", EndJ=" + (endJunction != null ? endJunction.getId() : "null"));
    }

    /**
     * Finds the closest junction to a given point (x, y) within a reasonable threshold.
     * @param pointX The x-coordinate of the point (typically a road endpoint).
     * @param pointY The y-coordinate of the point.
     * @param junctions The list of all junctions to search through.
     * @return The closest Junction, or null if none are within the threshold.
     */
    private Junction findClosestJunction(double pointX, double pointY, List<Junction> junctions) {
        Junction closest = null;
        double minDistSq = Double.MAX_VALUE;
        double fixedThreshold = 15.0; // Allow up to 15 pixels distance from junction radius edge

        for (Junction j : junctions) {
            double dx = j.getX() - pointX;
            double dy = j.getY() - pointY;
            double distSq = dx * dx + dy * dy;

            // Calculate the squared distance threshold based on junction radius + fixed threshold
            double radiusWithThreshold = j.getRadius() + fixedThreshold;
            double thresholdSq = radiusWithThreshold * radiusWithThreshold;

            // Check if the point is within the threshold and closer than previous candidates
            if (distSq < minDistSq && distSq < thresholdSq) {
                minDistSq = distSq;
                closest = j;
            }
        }

        // Optional Debugging:
         /*
         if (closest == null) {
             System.out.println("--> findClosestJunction at (" + String.format("%.1f",pointX) + "," + String.format("%.1f",pointY) + ") found NO junction within threshold (minDist=" + String.format("%.1f", Math.sqrt(minDistSq)) + ")");
          } else {
             System.out.println("--> findClosestJunction at (" + String.format("%.1f",pointX) + "," + String.format("%.1f",pointY) + ") found J" + closest.getId() + " (dist=" + String.format("%.1f",Math.sqrt(minDistSq))+")");
          }
         */
        return closest;
    }


    public void render(GraphicsContext gc) {
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(width);
        gc.strokeLine(startX, startY, endX, endY);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(10, 10);
        gc.strokeLine(startX, startY, endX, endY);
        gc.setLineDashes(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Road road = (Road) o;
        return id == road.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // Provide a more informative toString for debugging
        return "Road@" + Integer.toHexString(hashCode()) + "{" +
                "id=" + id +
                ", start=(" + String.format("%.1f", startX) + "," + String.format("%.1f", startY) + ")" +
                ", end=(" + String.format("%.1f", endX) + "," + String.format("%.1f", endY) + ")" +
                ", startJ=" + (startJunction != null ? startJunction.getId() : "null") +
                ", endJ=" + (endJunction != null ? endJunction.getId() : "null") +
                '}';
    }

    // Getters
    public int getId() { return id; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public double getWidth() { return width; }
    public double getLength() {
        return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
    }
    public List<Lane> getLanes() { return lanes; }
    public Junction getStartJunction() { return startJunction; }
    public Junction getEndJunction() { return endJunction; }

    public Lane getForwardLane() {
        return lanes.stream().filter(Lane::isForward).findFirst().orElse(null);
    }
    public Lane getBackwardLane() {
        return lanes.stream().filter(l -> !l.isForward()).findFirst().orElse(null);
    }
}