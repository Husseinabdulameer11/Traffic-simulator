
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
// import javafx.scene.shape.Circle; // Not needed when drawing directly on GraphicsContext

/**
 * Represents a single visual traffic light unit (typically containing red, yellow, and green lamps).
 * This class is responsible for rendering the light based on its current state.
 * The state itself (RED, YELLOW, GREEN) is controlled externally, usually by a
 * {@link TrafficLightCycleManager}. This class does not manage timing or state transitions.
 */
public class TrafficLight {

    private final int id; // Unique identifier for this specific light instance
    private volatile LightState currentState; // The current state (RED, YELLOW, GREEN)
    private final double x, y; // The center coordinates for positioning the light fixture
    private final boolean vertical; // Orientation of the lamp arrangement (true for vertical stack, false for horizontal)

    // Color constants for active (ON) and inactive (OFF) states of the lamps
    private static final Color RED_ON = Color.RED;
    private static final Color RED_OFF = Color.DARKRED.deriveColor(0, 1, 0.4, 1); // Dimmed red
    private static final Color YELLOW_ON = Color.YELLOW;
    private static final Color YELLOW_OFF = Color.DARKGOLDENROD.deriveColor(0, 1, 0.4, 1); // Dimmed yellow
    private static final Color GREEN_ON = Color.LIMEGREEN;
    private static final Color GREEN_OFF = Color.DARKGREEN.deriveColor(0, 1, 0.4, 1); // Dimmed green

    // --- Visual dimension constants for rendering ---
    private static final double LIGHT_RADIUS = 6.0; // Radius of each individual lamp circle
    private static final double LIGHT_SPACING = LIGHT_RADIUS * 2.5; // Center-to-center distance between lamps
    private static final double BOX_PADDING = 3.0; // Padding inside the black housing box

    // Calculated dimensions for the housing box based on orientation
    private static final double BOX_WIDTH_HORIZONTAL = (LIGHT_RADIUS + LIGHT_SPACING) * 2 + BOX_PADDING * 2;
    private static final double BOX_HEIGHT_HORIZONTAL = LIGHT_RADIUS * 2 + BOX_PADDING * 2;
    private static final double BOX_WIDTH_VERTICAL = LIGHT_RADIUS * 2 + BOX_PADDING * 2;
    private static final double BOX_HEIGHT_VERTICAL = (LIGHT_RADIUS + LIGHT_SPACING) * 2 + BOX_PADDING * 2;
    // --- End of visual constants ---

    // Static counter to generate unique IDs for each TrafficLight instance
    private static int nextId = 0;

    /**
     * Constructs a new visual TrafficLight instance.
     *
     * @param initialState The initial {@link LightState} the light should display.
     * @param x            The center x-coordinate where the light fixture will be drawn.
     * @param y            The center y-coordinate where the light fixture will be drawn.
     * @param vertical     True to arrange lamps vertically (top to bottom: R, Y, G),
     *                     false for horizontal arrangement (left to right: R, Y, G).
     */
    public TrafficLight(LightState initialState, double x, double y, boolean vertical) {
        this.id = nextId++; // Assign a unique ID
        this.currentState = initialState;
        this.x = x;
        this.y = y;
        this.vertical = vertical;
        // System.out.println("Created visual TrafficLight " + id + " at (" + x + "," + y + ")");
    }

    /**
     * Updates the current state of this traffic light.
     * This method is intended to be called by an external controller (e.g., TrafficLightCycleManager)
     * to change which lamp appears active. This method is thread-safe due to `volatile` state
     * and the synchronized block, though direct calls from multiple threads are not expected
     * with the CycleManager pattern.
     *
     * @param newState The new {@link LightState} to display.
     */
    public synchronized void setState(LightState newState) {
        // Avoid unnecessary updates if the state hasn't changed
        if (this.currentState == newState || newState == null) {
            return;
        }
        this.currentState = newState;
        // Note: Visual update happens during the render() call, not here directly.
    }

    /**
     * Gets the current state of this traffic light.
     * This is used by vehicles to check the light status. Marked volatile for visibility across threads.
     *
     * @return The current {@link LightState} (RED, YELLOW, or GREEN).
     */
    public LightState getCurrentState() {
        return currentState;
    }

    /**
     * Renders this traffic light onto the provided {@link GraphicsContext}.
     * Draws the housing box and the three lamps (red, yellow, green), coloring
     * them according to the {@code currentState}.
     *
     * @param gc The {@link GraphicsContext} to draw on.
     */
    public void render(GraphicsContext gc) {
        // Determine dimensions and positions based on orientation
        double boxX, boxY, boxW, boxH;         // Housing box properties
        double redX, redY, yellowX, yellowY, greenX, greenY; // Lamp center coordinates

        if (vertical) {
            boxW = BOX_WIDTH_VERTICAL;
            boxH = BOX_HEIGHT_VERTICAL;
            // Calculate top-left corner of the box based on center (x, y)
            boxX = x - boxW / 2;
            boxY = y - boxH / 2;
            // Calculate lamp centers relative to the main center (x, y)
            redX = x;    redY = y - LIGHT_SPACING; // Top lamp
            yellowX = x; yellowY = y;             // Middle lamp
            greenX = x;  greenY = y + LIGHT_SPACING; // Bottom lamp
        } else { // Horizontal arrangement
            boxW = BOX_WIDTH_HORIZONTAL;
            boxH = BOX_HEIGHT_HORIZONTAL;
            boxX = x - boxW / 2;
            boxY = y - boxH / 2;
            redX = x - LIGHT_SPACING; redY = y; // Left lamp
            yellowX = x;             yellowY = y; // Middle lamp
            greenX = x + LIGHT_SPACING; greenY = y; // Right lamp
        }

        // Draw the black housing box with rounded corners
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 5, 5); // Small corner radius

        // Draw the three lamp circles, coloring based on the current state
        gc.setFill(currentState == LightState.RED ? RED_ON : RED_OFF);
        gc.fillOval(redX - LIGHT_RADIUS, redY - LIGHT_RADIUS, LIGHT_RADIUS * 2, LIGHT_RADIUS * 2);

        gc.setFill(currentState == LightState.YELLOW ? YELLOW_ON : YELLOW_OFF);
        gc.fillOval(yellowX - LIGHT_RADIUS, yellowY - LIGHT_RADIUS, LIGHT_RADIUS * 2, LIGHT_RADIUS * 2);

        gc.setFill(currentState == LightState.GREEN ? GREEN_ON : GREEN_OFF);
        gc.fillOval(greenX - LIGHT_RADIUS, greenY - LIGHT_RADIUS, LIGHT_RADIUS * 2, LIGHT_RADIUS * 2);
    }

    // --- Standard Getters ---

    /** @return The unique ID assigned to this traffic light instance. */
    public int getId() { return id; }
    /** @return The center x-coordinate of this traffic light fixture. */
    public double getX() { return x; }
    /** @return The center y-coordinate of this traffic light fixture. */
    public double getY() { return y; }
}