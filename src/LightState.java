
import javafx.scene.paint.Color; // Required for storing color values

/**
 * Enumeration defining the possible states of a traffic light (RED, YELLOW, GREEN).
 * Each state is associated with a specific {@link Color} for rendering purposes.
 */
public enum LightState {
    /** The RED state, typically indicating stop. */
    RED(Color.RED),
    /** The YELLOW state, typically indicating prepare to stop or clear intersection. */
    YELLOW(Color.YELLOW),
    /** The GREEN state, typically indicating proceed. Uses LIMEGREEN for better visibility. */
    GREEN(Color.LIMEGREEN);

    // The JavaFX Color associated with this traffic light state.
    private final Color displayColor;

    /**
     * Private constructor to associate a color with each enum constant.
     * @param color The {@link Color} for this state.
     */
    LightState(Color color) {
        this.displayColor = color;
    }

    /**
     * Gets the JavaFX {@link Color} associated with this traffic light state.
     * Used for rendering the visual representation of the light.
     * @return The {@link Color} corresponding to this state.
     */
    public Color getColor() {
        return displayColor;
    }
}