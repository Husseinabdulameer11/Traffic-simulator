// Filnavn: RoadNetwork.java
// (Ingen package-setning)

import javafx.scene.canvas.GraphicsContext;
// import javafx.scene.paint.Color; // Color is not directly used here
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages the collection of roads, junctions, and traffic control elements (lights, managers)
 * within the simulation environment. Responsible for creating the network layout and
 * providing access to network components.
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
        // Select and execute one method to build the desired network topology
        // createRoundabout(canvasWidth, canvasHeight); // Example: Roundabout layout
        createTrafficLightIntersection(canvasWidth, canvasHeight); // Example: Single traffic light intersection
        // createGridNetwork(canvasWidth, canvasHeight); // Example: Grid layout

        // After creating all roads and junctions, establish the connections between them
        for (Road road : roads) {
            road.connectToJunctions(this.junctions); // Pass the complete list of junctions
        }
        // Log summary information about the created network
        // System.out.println("RoadNetwork initialized with " + junctions.size() + " junctions and " + roads.size() + " roads.");
        // System.out.println(lightCycleManagers.size() + " TrafficLightCycleManagers created.");
        // System.out.println(trafficLights.size() + " individual TrafficLights created.");
    }

    // --- LAYOUT CREATION METHODS ---

    /**
     * Example method to create a simple roundabout layout. (Currently inactive)
     * @param w Canvas width.
     * @param h Canvas height.
     */
    private void createRoundabout(double w, double h) {
        System.out.println("Creating Roundabout Layout...");
        // Define junction properties
        double radius = 80;
        Junction roundabout = new Junction(w / 2, h / 2, radius, JunctionType.ROUNDABOUT);
        junctions.add(roundabout);

        // Define roads connecting to the roundabout periphery
        double approachOffset = radius; // Distance from center to road end point
        roads.add(new Road(w / 2, 0, w / 2, h / 2 - approachOffset)); // North approach
        roads.add(new Road(w / 2 + approachOffset, h / 2, w, h / 2)); // East approach
        roads.add(new Road(w / 2, h / 2 + approachOffset, w / 2, h)); // South approach
        roads.add(new Road(0, h / 2, w / 2 - approachOffset, h / 2)); // West approach
    }

    /**
     * Creates a standard four-way intersection controlled by traffic lights at the center of the canvas.
     * @param w Canvas width.
     * @param h Canvas height.
     */
    private void createTrafficLightIntersection(double w, double h) {
        System.out.println("Creating Traffic Light Intersection Layout...");
        double junctionX = w / 2;
        double junctionY = h / 2;
        double junctionRadius = 40; // Visual radius of the intersection area

        // Create the central junction
        Junction centerJunction = new Junction(junctionX, junctionY, junctionRadius, JunctionType.TRAFFIC_LIGHT);
        junctions.add(centerJunction);
        // System.out.println(" Added Junction J" + centerJunction.getId() + " at (" + junctionX + "," + junctionY + ")");

        // Create the four roads approaching the junction
        // Road coordinates end slightly before the junction center based on its radius
        Road northRoad = new Road(junctionX, 0, junctionX, junctionY - junctionRadius); // Road ID 0
        Road eastRoad = new Road(junctionX + junctionRadius, junctionY, w, junctionY);   // Road ID 1
        Road southRoad = new Road(junctionX, junctionY + junctionRadius, junctionX, h); // Road ID 2
        Road westRoad = new Road(0, junctionY, junctionX - junctionRadius, junctionY); // Road ID 3
        roads.add(northRoad);
        roads.add(eastRoad);
        roads.add(southRoad);
        roads.add(westRoad);
        // System.out.println(" Added 4 roads potentially connected to J" + centerJunction.getId());

        // Create the Traffic Light Cycle Manager for this junction
        long greenTimeMillis = 7000;    // Duration of green light phase
        long yellowTimeMillis = 1500;   // Duration of yellow light phase
        long redTimeMillis = greenTimeMillis + yellowTimeMillis; // Red phase duration matches the other direction's green+yellow

        // Initialize light states (e.g., North/South start RED, East/West start GREEN)
        TrafficLightCycleManager manager = new TrafficLightCycleManager(
                centerJunction.getId(), LightState.RED, LightState.GREEN,
                greenTimeMillis, yellowTimeMillis, redTimeMillis
        );
        lightCycleManagers.add(manager); // Add manager to the network's list

        // Prepare a map to associate specific incoming lanes with their traffic lights
        Map<Lane, TrafficLight> lightMap = new HashMap<>();

        // --- Setup Lights for each Incoming Lane ---
        // Create, place, and map a light for each lane entering the junction.

        // North approach: Vehicles travel SOUTH on the NORTH road (using the ForwardLane)
        Lane incomingN = northRoad.getForwardLane();
        if (incomingN != null) {
            TrafficLight lightN = createAndPlaceLightForLane(incomingN, true); // Vertical light stack
            lightMap.put(incomingN, lightN); // Map this specific lane object to its light
            manager.addNsLight(lightN);       // Assign this light to the North/South control group
            // System.out.println("  Added Light " + lightN.getId() + " for North approach (Lane Hash: " + incomingN.hashCode() + ")");
        } else { System.err.println("Error: Could not get ForwardLane for North Road!"); }

        // East approach: Vehicles travel WEST on the EAST road (using the BackwardLane)
        Lane incomingE = eastRoad.getBackwardLane();
        if (incomingE != null) {
            TrafficLight lightE = createAndPlaceLightForLane(incomingE, false); // Horizontal light stack
            lightMap.put(incomingE, lightE);
            manager.addEwLight(lightE);       // Assign this light to the East/West control group
            // System.out.println("  Added Light " + lightE.getId() + " for East approach (Lane Hash: " + incomingE.hashCode() + ")");
        } else { System.err.println("Error: Could not get BackwardLane for East Road!"); }

        // South approach: Vehicles travel NORTH on the SOUTH road (using the BackwardLane)
        Lane incomingS = southRoad.getBackwardLane();
        if (incomingS != null) {
            TrafficLight lightS = createAndPlaceLightForLane(incomingS, true); // Vertical light stack
            lightMap.put(incomingS, lightS);
            manager.addNsLight(lightS);       // Assign to N/S group
            // System.out.println("  Added Light " + lightS.getId() + " for South approach (Lane Hash: " + incomingS.hashCode() + ")");
        } else { System.err.println("Error: Could not get BackwardLane for South Road!"); }

        // West approach: Vehicles travel EAST on the WEST road (using the ForwardLane)
        Lane incomingW = westRoad.getForwardLane();
        if (incomingW != null) {
            TrafficLight lightW = createAndPlaceLightForLane(incomingW, false); // Horizontal light stack
            lightMap.put(incomingW, lightW);
            manager.addEwLight(lightW);       // Assign to E/W group
            // System.out.println("  Added Light " + lightW.getId() + " for West approach (Lane Hash: " + incomingW.hashCode() + ")");
        } else { System.err.println("Error: Could not get ForwardLane for West Road!"); }

        // --- Finalize Junction Setup ---
        // Provide the junction with its cycle manager and the map linking lanes to lights
        centerJunction.setupTrafficLights(manager, lightMap);
    }

    /**
     * Helper method to create and position a {@link TrafficLight} object
     * appropriately for an incoming lane at an intersection.
     *
     * @param incomingLane The lane for which the light is being created.
     * @param vertical     True if the light display should be vertical, false for horizontal.
     * @return The newly created and positioned {@link TrafficLight} object.
     */
    private TrafficLight createAndPlaceLightForLane(Lane incomingLane, boolean vertical) {
        // Determine the end coordinates of the lane (where it enters the junction area)
        double laneEndX = incomingLane.isForward() ? incomingLane.getEndX() : incomingLane.getStartX();
        double laneEndY = incomingLane.isForward() ? incomingLane.getEndY() : incomingLane.getStartY();

        // Calculate the normalized direction vector *along* the lane, pointing towards the junction
        double dirX, dirY;
        double laneStartX = incomingLane.isForward() ? incomingLane.getStartX() : incomingLane.getEndX();
        double laneStartY = incomingLane.isForward() ? incomingLane.getStartY() : incomingLane.getEndY();
        dirX = laneEndX - laneStartX; // Vector along the lane
        dirY = laneEndY - laneStartY;
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0) { // Normalize the direction vector
            dirX /= len;
            dirY /= len;
        }

        // Calculate the perpendicular vector pointing to the right relative to the lane's direction
        double perpX = -dirY; // Rotate (dirX, dirY) -90 degrees
        double perpY = dirX;

        // Define placement parameters: distance before the junction and offset to the side
        double distBefore = 15.0; // Place the light slightly before the lane physically ends
        double offsetRight = 15.0; // Place the light to the right side of the approaching lane

        // Calculate the final position for the traffic light center
        double lightX = laneEndX - dirX * distBefore + perpX * offsetRight;
        double lightY = laneEndY - dirY * distBefore + perpY * offsetRight;

        // Create the TrafficLight instance at the calculated position
        // The initial state passed here is temporary; the CycleManager will set the correct state.
        TrafficLight light = new TrafficLight(LightState.RED, lightX, lightY, vertical);
        trafficLights.add(light); // Add to the network's list for rendering
        return light;
    }

    // --- RENDERING AND UPDATE METHODS ---

    /**
     * Renders all components of the road network (roads, junctions, traffic lights).
     * Order matters for layering (roads first, then junctions, then lights).
     * @param gc The GraphicsContext to draw on.
     */
    public void render(GraphicsContext gc) {
        for (Road road : roads) {
            road.render(gc);
        }
        for (Junction junction : junctions) {
            junction.render(gc);
        }
        for (TrafficLight light : trafficLights) {
            light.render(gc);
        }
    }

    /**
     * Updates the state of all traffic light cycles in the network.
     * @param deltaTimeMillis The time elapsed since the last update, in milliseconds.
     */
    public void updateTrafficLights(double deltaTimeMillis) {
        for (TrafficLightCycleManager manager : lightCycleManagers) {
            manager.update(deltaTimeMillis);
        }
    }

    /** Placeholder method for future interaction features. */
    public void disturbTrafficAt(double x, double y) {
        // Could be used to simulate incidents or manually trigger events
    }

    // --- UTILITY METHODS ---

    /**
     * Finds a random lane that serves as an entry point to the network.
     * Entry points are lanes originating from road ends not connected to any junction.
     * @return A randomly selected entry Lane, or null if none exist or roads are empty.
     */
    public Lane getRandomEntryLane() {
        List<Lane> possibleEntryLanes = new ArrayList<>();
        for (Road r : roads) {
            // Check if the road start is unconnected -> forward lane is an entry
            if (r.getStartJunction() == null) {
                Lane forward = r.getForwardLane();
                if (forward != null) possibleEntryLanes.add(forward);
            }
            // Check if the road end is unconnected -> backward lane is an entry
            if (r.getEndJunction() == null) {
                Lane backward = r.getBackwardLane();
                if (backward != null) possibleEntryLanes.add(backward);
            }
        }

        if (possibleEntryLanes.isEmpty()) {
            // Fallback if no clear entry lanes are defined (e.g., closed loop network)
            System.err.println("Warning: No designated entry lanes found in the network!");
            if (roads.isEmpty()) return null;
            // As a last resort, pick a random lane from a random road
            Road randomRoad = roads.get(random.nextInt(roads.size()));
            List<Lane> randomLanes = randomRoad.getLanes();
            return randomLanes.isEmpty() ? null : randomLanes.get(random.nextInt(randomLanes.size()));
        }
        // Return a random lane from the list of identified entry points
        return possibleEntryLanes.get(random.nextInt(possibleEntryLanes.size()));
    }

    /**
     * Determines the next lane for a vehicle exiting a junction.
     * Current implementation selects a random valid exit road (not U-turn)
     * and returns the corresponding lane leading away from the junction.
     * Does not yet support specific turning maneuvers (left/right/straight).
     *
     * @param arrivalLane The lane the vehicle arrived at the junction on.
     * @param junction    The junction the vehicle is currently at.
     * @return The next Lane object to enter, or null if no valid exit is found.
     */
    public Lane getNextLane(Lane arrivalLane, Junction junction) {
        if (junction == null || arrivalLane == null) {
            System.err.println("Error: getNextLane called with null arrivalLane or junction.");
            return null;
        }

        // Get potential exit roads, excluding the road the vehicle just arrived on
        List<Road> possibleExitRoads = new ArrayList<>(junction.getConnectedRoads());
        possibleExitRoads.remove(arrivalLane.getRoad());

        if (possibleExitRoads.isEmpty()) {
            // This junction is a dead end from this arrival direction
            //System.out.println("Vehicle reached dead end at J" + junction.getId());
            return null;
        }

        // Select one of the possible exit roads randomly
        Road nextRoad = possibleExitRoads.get(random.nextInt(possibleExitRoads.size()));

        // Determine which lane on the nextRoad leads *away* from the current junction
        // If the next road *starts* at this junction, take the forward lane.
        if (nextRoad.getStartJunction() == junction) {
            return nextRoad.getForwardLane();
        }
        // If the next road *ends* at this junction, take the backward lane.
        else if (nextRoad.getEndJunction() == junction) {
            return nextRoad.getBackwardLane();
        } else {
            // This indicates a topology error: the selected 'next road' isn't
            // actually connected to the current junction correctly in its own data.
            System.err.println("Error in getNextLane: Road " + nextRoad.getId()
                    + " selected as exit from J" + junction.getId() + ", but it's not connected correctly.");
            return null;
        }
    }


    // --- Standard Getters ---

    /** @return The list of all roads in the network. */
    public List<Road> getRoads() { return roads; }
    /** @return The list of all junctions in the network. */
    public List<Junction> getJunctions() { return junctions; }
    /** @return The list of all individual traffic light visual instances. */
    public List<TrafficLight> getTrafficLights() { return trafficLights; }
    /** @return The list of all traffic light cycle managers. */
    public List<TrafficLightCycleManager> getLightCycleManagers() { return lightCycleManagers; }
}