

import javafx.scene.canvas.GraphicsContext;
// import javafx.scene.paint.Color; // Color not directly used in this class's logic
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors; // Needed if using streams, though not currently used in getRandomEntryLane

/**
 * Manages the collection of roads, junctions, and traffic control elements (lights, managers)
 * within the simulation environment. Responsible for creating the network layout,
 * connecting components, and providing access to network elements.
 */
public class RoadNetwork {
    // Lists holding the core network components
    private final List<Road> roads = new ArrayList<>();
    private final List<Junction> junctions = new ArrayList<>();
    // Lists specific to traffic light control and rendering
    private final List<TrafficLight> trafficLights = new ArrayList<>(); // All individual light instances
    private final List<TrafficLightCycleManager> lightCycleManagers = new ArrayList<>(); // Managers controlling cycles
    // Random number generator for choosing lanes/roads
    private final Random random = new Random();

    /**
     * Constructs the RoadNetwork, creating the initial layout and connecting components.
     *
     * @param canvasWidth  The width of the simulation canvas, used for layout calculations.
     * @param canvasHeight The height of the simulation canvas, used for layout calculations.
     */
    public RoadNetwork(double canvasWidth, double canvasHeight) {
        // --- Network Topology Creation ---
        // Select ONE method here to define the road network layout.
        // createRoundabout(canvasWidth, canvasHeight);
        createTrafficLightIntersection(canvasWidth, canvasHeight);
        // createGridNetwork(canvasWidth, canvasHeight); // Another example layout

        // --- Component Connection ---
        // After creating roads and junctions, establish logical connections between them.
        // This step is crucial for navigation (finding start/end junctions).
        for (Road road : roads) {
            road.connectToJunctions(this.junctions); // Pass the complete list of junctions
        }

        // --- Logging ---
        // Log summary information after setup.
        System.out.println("RoadNetwork initialized:");
        System.out.println("  - Junctions: " + junctions.size());
        System.out.println("  - Roads: " + roads.size());
        System.out.println("  - TrafficLight Cycle Managers: " + lightCycleManagers.size());
        System.out.println("  - TrafficLight Instances: " + trafficLights.size());
    }

    // --- LAYOUT CREATION METHODS ---

    /**
     * Creates a simple roundabout layout. (Example, currently inactive)
     * @param w Canvas width.
     * @param h Canvas height.
     */
    private void createRoundabout(double w, double h) {
        System.out.println("Creating Roundabout Layout...");
        double radius = 80;
        Junction roundabout = new Junction(w / 2, h / 2, radius, JunctionType.ROUNDABOUT);
        junctions.add(roundabout);

        double approachOffset = radius;
        roads.add(new Road(w / 2, 0, w / 2, h / 2 - approachOffset)); // North
        roads.add(new Road(w / 2 + approachOffset, h / 2, w, h / 2)); // East
        roads.add(new Road(w / 2, h / 2 + approachOffset, w / 2, h)); // South
        roads.add(new Road(0, h / 2, w / 2 - approachOffset, h / 2)); // West
    }

    /**
     * Creates a standard four-way intersection controlled by traffic lights
     * located at the center of the simulation area.
     * Initializes the necessary roads, junction, traffic lights, and cycle manager.
     *
     * @param w Canvas width.
     * @param h Canvas height.
     */
    private void createTrafficLightIntersection(double w, double h) {
        System.out.println("Creating Traffic Light Intersection Layout...");
        double junctionX = w / 2;
        double junctionY = h / 2;
        double junctionRadius = 40; // Visual size of the intersection area

        // 1. Create the central Junction
        Junction centerJunction = new Junction(junctionX, junctionY, junctionRadius, JunctionType.TRAFFIC_LIGHT);
        junctions.add(centerJunction);
        // System.out.println(" Added Junction J" + centerJunction.getId() + " at (" + junctionX + "," + junctionY + ")");

        // 2. Create the four Roads approaching the junction
        // Coordinates end just outside the junction's visual radius.
        Road northRoad = new Road(junctionX, 0, junctionX, junctionY - junctionRadius); // ID 0
        Road eastRoad = new Road(junctionX + junctionRadius, junctionY, w, junctionY);   // ID 1
        Road southRoad = new Road(junctionX, junctionY + junctionRadius, junctionX, h); // ID 2
        Road westRoad = new Road(0, junctionY, junctionX - junctionRadius, junctionY); // ID 3
        roads.add(northRoad);
        roads.add(eastRoad);
        roads.add(southRoad);
        roads.add(westRoad);
        // System.out.println(" Added 4 roads potentially connected to J" + centerJunction.getId());

        // 3. Create and configure the TrafficLightCycleManager for the junction
        long greenTimeMillis = 7000;
        long yellowTimeMillis = 1500;
        long redTimeMillis = greenTimeMillis + yellowTimeMillis; // Ensure proper clearance time

        // Set initial states: N/S RED, E/W GREEN
        TrafficLightCycleManager manager = new TrafficLightCycleManager(
                centerJunction.getId(), LightState.RED, LightState.GREEN,
                greenTimeMillis, yellowTimeMillis, redTimeMillis
        );
        lightCycleManagers.add(manager); // Add manager to the network's list for updates

        // 4. Create individual TrafficLights for each incoming lane and map them
        Map<Lane, TrafficLight> lightMap = new HashMap<>();

        // --- Setup Lights for each Incoming Lane ---
        // North approach: Southbound traffic on NorthRoad uses the ForwardLane
        Lane incomingN = northRoad.getForwardLane();
        if (incomingN != null) {
            TrafficLight lightN = createAndPlaceLightForLane(incomingN, true); // Vertical light
            lightMap.put(incomingN, lightN); // Map Lane object -> Light object
            manager.addNsLight(lightN);       // Assign to North/South control group
        } else { System.err.println("Error: Could not get ForwardLane for North Road!"); }

        // East approach: Westbound traffic on EastRoad uses the BackwardLane
        Lane incomingE = eastRoad.getBackwardLane();
        if (incomingE != null) {
            TrafficLight lightE = createAndPlaceLightForLane(incomingE, false); // Horizontal light
            lightMap.put(incomingE, lightE);
            manager.addEwLight(lightE);       // Assign to East/West control group
        } else { System.err.println("Error: Could not get BackwardLane for East Road!"); }

        // South approach: Northbound traffic on SouthRoad uses the BackwardLane
        Lane incomingS = southRoad.getBackwardLane();
        if (incomingS != null) {
            TrafficLight lightS = createAndPlaceLightForLane(incomingS, true); // Vertical light
            lightMap.put(incomingS, lightS);
            manager.addNsLight(lightS);       // Assign to N/S group
        } else { System.err.println("Error: Could not get BackwardLane for South Road!"); }

        // West approach: Eastbound traffic on WestRoad uses the ForwardLane
        Lane incomingW = westRoad.getForwardLane();
        if (incomingW != null) {
            TrafficLight lightW = createAndPlaceLightForLane(incomingW, false); // Horizontal light
            lightMap.put(incomingW, lightW);
            manager.addEwLight(lightW);       // Assign to E/W group
        } else { System.err.println("Error: Could not get ForwardLane for West Road!"); }

        // 5. Finalize the junction setup by providing the manager and the light map
        centerJunction.setupTrafficLights(manager, lightMap);
        System.out.println("Traffic light system configured for J" + centerJunction.getId());
    }

    /**
     * Helper method to create a {@link TrafficLight} instance and calculate its
     * appropriate position near the end of an incoming lane.
     * The light is typically placed slightly before the junction boundary and offset
     * to the right side of the lane's direction of travel.
     *
     * @param incomingLane The lane for which the light is being created.
     * @param vertical     True if the light display should be arranged vertically, false for horizontal.
     * @return The newly created and positioned {@link TrafficLight} object.
     */
    private TrafficLight createAndPlaceLightForLane(Lane incomingLane, boolean vertical) {
        // Determine the coordinates of the lane's endpoint (entry point to the junction area)
        double laneEndX = incomingLane.isForward() ? incomingLane.getEndX() : incomingLane.getStartX();
        double laneEndY = incomingLane.isForward() ? incomingLane.getEndY() : incomingLane.getStartY();

        // Calculate the normalized direction vector *along* the lane towards the junction
        double dirX, dirY;
        double laneStartX = incomingLane.isForward() ? incomingLane.getStartX() : incomingLane.getEndX();
        double laneStartY = incomingLane.isForward() ? incomingLane.getStartY() : incomingLane.getEndY();
        dirX = laneEndX - laneStartX;
        dirY = laneEndY - laneStartY;
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        // Normalize the direction vector
        if (len > 0) {
            dirX /= len;
            dirY /= len;
        }

        // Calculate a perpendicular vector pointing to the right of the lane's direction
        double perpX = -dirY; // Rotate (dirX, dirY) by -90 degrees
        double perpY = dirX;

        // Define placement parameters relative to the lane's end point
        double distBefore = 15.0; // How many pixels before the exact end point to place the light
        double offsetRight = 15.0; // How many pixels to the right of the lane centerline

        // Calculate the final position for the traffic light's center
        double lightX = laneEndX - dirX * distBefore + perpX * offsetRight;
        double lightY = laneEndY - dirY * distBefore + perpY * offsetRight;

        // Create the TrafficLight instance. Initial state is arbitrary as the manager controls it.
        TrafficLight light = new TrafficLight(LightState.RED, lightX, lightY, vertical);
        // Add the created light to the network's list for rendering purposes
        trafficLights.add(light);
        return light;
    }

    // --- RENDERING AND UPDATE METHODS ---

    /**
     * Renders all components of the road network onto the provided GraphicsContext.
     * The rendering order ensures proper layering (roads -> junctions -> lights).
     *
     * @param gc The {@link GraphicsContext} to draw on.
     */
    public void render(GraphicsContext gc) {
        // Draw roads first (typically the bottom layer)
        for (Road road : roads) {
            road.render(gc);
        }
        // Draw junctions on top of roads
        for (Junction junction : junctions) {
            junction.render(gc);
        }
        // Draw traffic lights last (on top of junctions/roads)
        for (TrafficLight light : trafficLights) {
            light.render(gc);
        }
    }

    /**
     * Updates the state of all registered {@link TrafficLightCycleManager} instances.
     * This progresses the light cycles based on the elapsed time.
     *
     * @param deltaTimeMillis The time elapsed since the last update call, in milliseconds.
     */
    public void updateTrafficLights(double deltaTimeMillis) {
        for (TrafficLightCycleManager manager : lightCycleManagers) {
            manager.update(deltaTimeMillis);
        }
    }

    /** Placeholder method for potential future interactions like simulating incidents. */
    public void disturbTrafficAt(double x, double y) {
        // Future implementation could modify traffic flow or vehicle behavior near this point.
    }

    // --- UTILITY METHODS ---

    /**
     * Finds a random {@link Lane} that serves as an entry point into the network.
     * Entry points are defined as lanes originating from the end of a {@link Road}
     * that is not connected to a {@link Junction}.
     *
     * @return A randomly selected entry Lane, or null if no suitable entry lanes are found.
     */
    public Lane getRandomEntryLane() {
        List<Lane> possibleEntryLanes = new ArrayList<>();
        for (Road r : roads) {
            // Check if road start is unconnected AND road end is connected (lane leads into network)
            if (r.getStartJunction() == null && r.getEndJunction() != null) {
                Lane forward = r.getForwardLane();
                if (forward != null) possibleEntryLanes.add(forward);
            }
            // Check if road end is unconnected AND road start is connected (lane leads into network)
            if (r.getEndJunction() == null && r.getStartJunction() != null) {
                Lane backward = r.getBackwardLane();
                if (backward != null) possibleEntryLanes.add(backward);
            }
        }

        if (possibleEntryLanes.isEmpty()) {
            // Log a warning if no lanes meeting the criteria were found.
            System.err.println("Warning: No valid entry lanes found in the network configuration!");
            // Avoid fallback to potentially invalid lanes. Return null if no clear entry point.
            return null;
        }
        // Return a random lane from the list of identified valid entry points.
        return possibleEntryLanes.get(random.nextInt(possibleEntryLanes.size()));
    }

    /**
     * Determines the next appropriate {@link Lane} for a vehicle exiting a {@link Junction}.
     * This simplified version selects a random valid exit road (excluding the arrival road)
     * and returns the lane on that road leading away from the current junction.
     * Does not implement specific turn maneuvers (left/right/straight).
     *
     * @param arrivalLane The {@link Lane} the vehicle arrived at the junction on. Cannot be null.
     * @param junction    The {@link Junction} the vehicle is currently at. Cannot be null.
     * @return The next {@link Lane} object to enter, or null if no valid exit is found (e.g., dead end).
     */
    public Lane getNextLane(Lane arrivalLane, Junction junction) {
        // Basic validation of inputs
        if (junction == null || arrivalLane == null) {
            // Log error if called with invalid arguments
            System.err.println("Error: getNextLane called with null arrivalLane or junction. Cannot determine next lane.");
            return null;
        }

        // Get the list of roads connected to the junction
        List<Road> possibleExitRoads = new ArrayList<>(junction.getConnectedRoads());
        // Remove the road the vehicle just arrived on to prevent immediate U-turns
        possibleExitRoads.remove(arrivalLane.getRoad());

        // Check if there are any valid exit roads remaining
        if (possibleExitRoads.isEmpty()) {
            // This junction is effectively a dead end from the arrival direction
            // System.out.println("Vehicle arrived at dead end at J" + junction.getId() + " from Road " + arrivalLane.getRoad().getId());
            return null;
        }

        // Select one of the available exit roads randomly
        Road nextRoad = possibleExitRoads.get(random.nextInt(possibleExitRoads.size()));

        // Determine which lane on the chosen exit road leads *away* from the current junction
        // If the next road starts at this junction, the forward lane moves away.
        if (nextRoad.getStartJunction() == junction) {
            return nextRoad.getForwardLane();
        }
        // If the next road ends at this junction, the backward lane moves away.
        else if (nextRoad.getEndJunction() == junction) {
            return nextRoad.getBackwardLane();
        } else {
            // This indicates a configuration error: the selected 'nextRoad'
            // should be connected to this 'junction' at either its start or end.
            System.err.println("Error in getNextLane: Selected next Road " + nextRoad.getId()
                    + " appears incorrectly connected (or not connected) to Junction " + junction.getId());
            return null;
        }
    }


    // --- Standard Getters ---

    /** @return The list of all {@link Road} objects in the network. */
    public List<Road> getRoads() { return roads; }
    /** @return The list of all {@link Junction} objects in the network. */
    public List<Junction> getJunctions() { return junctions; }
    /** @return The list of all individual {@link TrafficLight} instances used for rendering. */
    public List<TrafficLight> getTrafficLights() { return trafficLights; }
    /** @return The list of all {@link TrafficLightCycleManager} instances controlling the lights. */
    public List<TrafficLightCycleManager> getLightCycleManagers() { return lightCycleManagers; }
}