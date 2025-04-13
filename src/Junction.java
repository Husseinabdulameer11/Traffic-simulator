import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the different types of junctions in the simulation.
 * Defined internally in Junction.java for diagnostic purposes.
 */
enum JunctionType {
    ROUNDABOUT,
    TRAFFIC_LIGHT
}

/**
 * Represents a junction (intersection or roundabout) in the road network.
 * For TRAFFIC_LIGHT junctions, it manages the association between incoming lanes
 * and their corresponding traffic lights.
 * Uses the internally defined JunctionType enum.
 */
public class Junction {
    private int id;
    private double x, y;
    private double radius;
    private JunctionType type; // Bruker enum definert over i *denne* filen
    private List<Road> connectedRoads = new ArrayList<>();

    // Fields specific to TRAFFIC_LIGHT junctions
    private TrafficLightCycleManager cycleManager = null;
    private Map<Lane, TrafficLight> lightsForIncomingLanes = new HashMap<>(); // Initialize map


    /**
     * Synchronization logic for junction access
     * Ensures thread-safe entry to junctions by allowing only one vehicle at a time.
     * The 'junctionLock' is used for mutual exclusion, and 'occupied' tracks junction availability.
     */
    private final Object junctionLock = new Object();
    private boolean occupied = false;

    private static int nextId = 0;

    public Junction(double x, double y, double radius, JunctionType type) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
    }

    /**
     * Allows a vehicle to enter the junction.
     * Used to ensure thread coordination and prevent collisions.
     * This method blocks until the vehicle is granted access (no other vehicle is inside, green light)
     */

    public void enterJunction() {
        synchronized (junctionLock) {
            try {
                while (occupied) {
                    junctionLock.wait();
                }
                occupied = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Must be called when the vehicle leaves the junction
     * to allow others to enter.
     */

    public void leaveJunction() {
        synchronized (junctionLock) {
            occupied = false;
            junctionLock.notifyAll();
        }
    }

    public void connectRoad(Road road) {
        if (!connectedRoads.contains(road)) {
            connectedRoads.add(road);
        }
    }

    public void setupTrafficLights(TrafficLightCycleManager manager, Map<Lane, TrafficLight> lightMap) {
        if (this.type == JunctionType.TRAFFIC_LIGHT) {
            this.cycleManager = manager;
            this.lightsForIncomingLanes = new HashMap<>(lightMap);

            System.out.println("J" + id + " setupTrafficLights completed. Map size: " + this.lightsForIncomingLanes.size());
        } else {
            System.err.println("Warning: Attempted to set up traffic lights for non-traffic-light junction " + id);
        }
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.DARKGRAY);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        switch (type) {
            case ROUNDABOUT:
                gc.setFill(Color.SEAGREEN);
                gc.fillOval(x - radius * 0.6, y - radius * 0.6, radius * 1.2, radius * 1.2);
                break;
            case TRAFFIC_LIGHT:
                break;
        }
    }

    public TrafficLight getTrafficLightForLane(Lane incomingLane) {
        if (type != JunctionType.TRAFFIC_LIGHT || lightsForIncomingLanes == null) return null;
        if (incomingLane == null) return null;

        TrafficLight foundLight = lightsForIncomingLanes.get(incomingLane);

        if (foundLight == null) {
            System.err.println("\n--- TRAFFIC LIGHT LOOKUP FAILED (Junction.getTrafficLightForLane) ---");
            System.err.println(" Junction ID: J" + this.id);
            System.err.println(" Lookup Lane: " + incomingLane
                    + " | Road ID: " + (incomingLane.getRoad() != null ? incomingLane.getRoad().getId() : "null Road!")
                    + " | isForward: " + incomingLane.isForward()
                    + " | hashCode: " + incomingLane.hashCode());
            System.err.println(" Map keys currently stored in J" + this.id + " (" + lightsForIncomingLanes.size() + " entries):");
            boolean potentialMatchFound = false;
            if (lightsForIncomingLanes.isEmpty()) {
                System.err.println("   Map is empty!");
            } else {
                for (Lane keyLane : lightsForIncomingLanes.keySet()) {
                    if (keyLane == null) {
                        System.err.println("   - Map Key: NULL!");
                        continue;
                    }
                    System.err.print("   - Map Key: " + keyLane
                            + " | Road ID: " + (keyLane.getRoad() != null ? keyLane.getRoad().getId() : "null Road!")
                            + " | isForward: " + keyLane.isForward()
                            + " | hashCode: " + keyLane.hashCode());
                    boolean isEqual = keyLane.equals(incomingLane);
                    System.err.println(" | equals(lookup)? " + isEqual);
                    if (isEqual) {
                        potentialMatchFound = true;
                    }
                }
            }
            if (potentialMatchFound) {
                System.err.println(" DIAGNOSIS: An EQUAL key exists in the map, but HashMap.get() returned null.");
                System.err.println("            This strongly suggests an issue with the hashCode() implementation ");
                System.err.println("            (hashCode changes after object is put in map, or inconsistent hashCode/equals).");
            } else {
                System.err.println(" DIAGNOSIS: No key currently in the map is considered equal to the lookup Lane.");
                System.err.println("            Check if the correct Lane object was added during setupTrafficLights,");
                System.err.println("            or if the Lane object used by the Vehicle is different.");
            }
            System.err.println("---------------------------------------------------------------------\n");
        }

        return foundLight;
    }

    // Getters
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public JunctionType getType() { return type; }
    public List<Road> getConnectedRoads() { return connectedRoads; }
    public TrafficLightCycleManager getCycleManager() { return cycleManager; }
}
