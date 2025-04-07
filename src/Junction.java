

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

    private static int nextId = 0;

    /**
     * Constructs a new Junction.
     * @param x The x-coordinate of the junction center.
     * @param y The y-coordinate of the junction center.
     * @param radius The visual radius of the junction area.
     * @param type The type of the junction (e.g., JunctionType.TRAFFIC_LIGHT).
     */
    public Junction(double x, double y, double radius, JunctionType type) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
    }

    /**
     * Connects a road to this junction. Called by Road.connectToJunctions.
     * @param road The road to connect.
     */
    public void connectRoad(Road road) {
        if (!connectedRoads.contains(road)) {
            connectedRoads.add(road);
        }
    }

    /**
     * Sets up the traffic light system for this junction.
     * This should only be called for junctions of type TRAFFIC_LIGHT.
     * @param manager The TrafficLightCycleManager controlling this junction's lights.
     * @param lightMap A map associating each incoming Lane object with its specific TrafficLight object.
     */
    public void setupTrafficLights(TrafficLightCycleManager manager, Map<Lane, TrafficLight> lightMap) {
        if (this.type == JunctionType.TRAFFIC_LIGHT) {
            this.cycleManager = manager;
            // Store the provided map. Ensure the keys (Lane objects) have proper equals/hashCode.
            this.lightsForIncomingLanes = new HashMap<>(lightMap); // Use a copy for safety

            // Debugging: Print setup confirmation and map contents
            System.out.println("J" + id + " setupTrafficLights completed. Map size: " + this.lightsForIncomingLanes.size());
             /* // Optional: Print keys during setup for comparison later
             System.out.println("  Stored light map keys at setup:");
             for (Lane key : this.lightsForIncomingLanes.keySet()) {
                  System.out.println("    - Key: " + key + " | hashCode: " + key.hashCode());
             }
             */

        } else {
            System.err.println("Warning: Attempted to set up traffic lights for non-traffic-light junction " + id);
        }
    }


    /**
     * Renders the junction area on the canvas.
     * @param gc The GraphicsContext to draw on.
     */
    public void render(GraphicsContext gc) {
        // Draw the base circle/oval for the junction area
        gc.setFill(Color.DARKGRAY);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // Render type-specific details
        switch (type) { // 'type' bruker nå den internt definerte enum'en
            case ROUNDABOUT:
                gc.setFill(Color.SEAGREEN); // Match background
                gc.fillOval(x - radius * 0.6, y - radius * 0.6, radius * 1.2, radius * 1.2);
                break;
            case TRAFFIC_LIGHT:
                break;
        }
        // Note: Individual traffic lights are rendered in RoadNetwork.render()
    }

    /**
     * Gets the specific TrafficLight object associated with a given incoming Lane.
     * Includes detailed debugging output if the lookup fails.
     * @param incomingLane The Lane object representing the lane the vehicle is approaching on.
     * @return The associated TrafficLight object, or null if none is found (which indicates an error).
     */
    public TrafficLight getTrafficLightForLane(Lane incomingLane) {
        // Basic validation
        if (type != JunctionType.TRAFFIC_LIGHT || lightsForIncomingLanes == null) { // Sammenligner med intern enum
            return null;
        }
        if (incomingLane == null) {
            return null;
        }

        // Attempt the lookup in the map
        TrafficLight foundLight = lightsForIncomingLanes.get(incomingLane);

        // --- DETAILED DEBUGGING BLOCK (If lookup failed) ---
        if (foundLight == null) {
            System.err.println("\n--- TRAFFIC LIGHT LOOKUP FAILED (Junction.getTrafficLightForLane) ---");
            System.err.println(" Junction ID: J" + this.id);
            System.err.println(" Lookup Lane: " + incomingLane
                    + " | Road ID: " + (incomingLane.getRoad() != null ? incomingLane.getRoad().getId() : "null Road!") // Added null check
                    + " | isForward: " + incomingLane.isForward()
                    + " | hashCode: " + incomingLane.hashCode());
            System.err.println(" Map keys currently stored in J" + this.id + " (" + lightsForIncomingLanes.size() + " entries):");
            boolean potentialMatchFound = false;
            if (lightsForIncomingLanes.isEmpty()) {
                System.err.println("   Map is empty!");
            } else {
                for (Lane keyLane : lightsForIncomingLanes.keySet()) {
                    if (keyLane == null) { // Check for null keys
                        System.err.println("   - Map Key: NULL!");
                        continue;
                    }
                    System.err.print("   - Map Key: " + keyLane
                            + " | Road ID: " + (keyLane.getRoad() != null ? keyLane.getRoad().getId() : "null Road!") // Added null check
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
        // --- END DEBUGGING BLOCK ---

        return foundLight;
    }


    // Getters
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public JunctionType getType() { return type; } // Returnerer typen (intern enum)
    public List<Road> getConnectedRoads() { return connectedRoads; }
    public TrafficLightCycleManager getCycleManager() { return cycleManager; }
}