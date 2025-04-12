// Filnavn: TrafficSimulation.java
// (Ingen package-setning)

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
// import javafx.scene.image.Image; // Currently unused

/**
 * Main application class for the traffic simulation.
 * Sets up the JavaFX stage, canvas, control panel, and manages the main simulation loop
 * using an {@code AnimationTimer}. Initializes and coordinates the {@code RoadNetwork}
 * and {@code VehicleManager}.
 */
public class TrafficSimulation extends Application {

    // --- Simulation Components ---
    private Canvas simulationCanvas; // The drawing surface
    private GraphicsContext gc; // The graphics context for drawing on the canvas
    private RoadNetwork roadNetwork; // Manages roads, junctions, lights
    private VehicleManager vehicleManager; // Manages vehicle spawning, updates, and rendering

    // --- Simulation State ---
    private double time = 0; // Accumulated simulation time in seconds
    private boolean isPaused = false; // Flag to control pausing the simulation loop
    private AnimationTimer simulationTimer; // Handles the main update and render loop

    // --- User-Adjustable Parameters ---
    private double totalInflow = 1000; // Initial traffic inflow rate (vehicles per hour)
    private double timelapseSpeed = 1.0; // Simulation speed multiplier (1.0 = real-time)

    /**
     * The main entry point for the JavaFX application.
     * Sets up the primary stage, scene graph, initializes simulation components,
     * and starts the simulation loop.
     *
     * @param primaryStage The primary stage for this application, onto which
     *                     the application scene can be set.
     */
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane(); // Main layout container

        // Initialize the drawing canvas and its graphics context
        simulationCanvas = new Canvas(700, 350); // Set preferred canvas size
        gc = simulationCanvas.getGraphicsContext2D();

        // Initialize simulation core components
        initializeSimulation();

        // Create the control panel with sliders and buttons
        VBox controlPanel = createControlPanel();

        // Arrange components in the main layout
        root.setCenter(simulationCanvas); // Simulation view in the center
        root.setBottom(controlPanel);     // Controls at the bottom

        // Set up basic mouse interaction handlers (currently placeholders)
        setupInteractionHandlers();

        // Create the main scene
        Scene scene = new Scene(root); // Scene size will adapt to layout

        // Configure and show the primary stage (window)
        primaryStage.setTitle("Traffic Simulation"); // Window title
        primaryStage.setScene(scene);
        // Ensure simulation stops cleanly when the window is closed
        primaryStage.setOnCloseRequest(e -> stopSimulation());
        primaryStage.show();

        // Start the main simulation loop
        startSimulationLoop();
    }

    /**
     * Initializes or resets the core simulation objects (RoadNetwork, VehicleManager).
     */
    private void initializeSimulation() {
        time = 0; // Reset simulation time
        // Create the road network based on canvas size
        roadNetwork = new RoadNetwork(simulationCanvas.getWidth(), simulationCanvas.getHeight());
        // Create the vehicle manager, linking it to the network
        vehicleManager = new VehicleManager(roadNetwork);
        // Set the initial inflow rate from the current parameter value
        vehicleManager.setInflow(totalInflow);
        isPaused = false; // Ensure simulation is not paused initially
    }


    /**
     * Creates the VBox containing the user interface controls (sliders, buttons).
     * @return The VBox node for the control panel.
     */
    private VBox createControlPanel() {
        VBox panel = new VBox(10); // Vertical box with 10px spacing
        panel.setPadding(new Insets(15)); // Padding around the controls
        panel.setStyle("-fx-background-color: #EEEEEE;"); // Light gray background

        // --- Traffic Flow Controls ---
        Label flowTitle = new Label("Traffic Flow");
        flowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Slider inflowSlider = new Slider(0, 7200, totalInflow); // Range: 0 to 7200 veh/h
        Label inflowValueLabel = new Label(String.format("%.0f veh/h", totalInflow));
        // Link slider changes to the inflow parameter and update the label/VehicleManager
        inflowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            totalInflow = newVal.doubleValue();
            inflowValueLabel.setText(String.format("%.0f veh/h", totalInflow));
            if (vehicleManager != null) { // Check if manager is initialized
                vehicleManager.setInflow(totalInflow);
            }
        });
        // Arrange inflow controls horizontally
        HBox inflowBox = new HBox(10, new Label("Total Inflow:"), inflowSlider, inflowValueLabel);
        inflowSlider.setPrefWidth(200); // Give slider reasonable width

        // --- Simulation Speed Controls ---
        Label speedTitle = new Label("Simulation Speed");
        speedTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Slider timelapseSlider = new Slider(0.1, 10.0, timelapseSpeed); // Range: 0.1x to 10x speed
        Label timelapseValueLabel = new Label(String.format("%.1fx", timelapseSpeed));
        // Link slider changes to the timelapseSpeed parameter and update the label
        timelapseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            timelapseSpeed = newVal.doubleValue();
            timelapseValueLabel.setText(String.format("%.1fx", timelapseSpeed));
        });
        // Arrange speed controls horizontally
        HBox timelapseBox = new HBox(10, new Label("Timelapse:"), timelapseSlider, timelapseValueLabel);
        timelapseSlider.setPrefWidth(200);

        // --- Action Buttons ---
        Button pauseButton = new Button("Pause"); // Initial text
        pauseButton.setOnAction(e -> togglePause(pauseButton));

        Button restartButton = new Button("Restart");
        restartButton.setOnAction(e -> restartSimulation(pauseButton)); // Pass pauseButton to reset text

        Button infoButton = new Button("Info");
        infoButton.setOnAction(e -> showInfoDialog());

        // Arrange buttons horizontally
        HBox buttonBox = new HBox(20, pauseButton, restartButton, infoButton); // Spacing between buttons
        buttonBox.setPadding(new Insets(10, 0, 0, 0)); // Add padding above buttons

        // Add all control sections to the main panel
        panel.getChildren().addAll(
                flowTitle, inflowBox,
                new Separator(javafx.geometry.Orientation.HORIZONTAL), // Visual separator
                speedTitle, timelapseBox,
                new Separator(javafx.geometry.Orientation.HORIZONTAL),
                buttonBox
        );

        return panel;
    }

    /**
     * Toggles the paused state of the simulation.
     * Stops or starts the {@code AnimationTimer} and updates the pause button text.
     * @param pauseButton The Button used for pausing/resuming.
     */
    private void togglePause(Button pauseButton) {
        isPaused = !isPaused;
        if (simulationTimer != null) { // Check if timer exists
            if (isPaused) {
                simulationTimer.stop();
                pauseButton.setText("Resume");
            } else {
                // To avoid time jump after pause, reset the last update time in the timer
                // This requires the timer variable to be accessible (which it is here)
                // We can reset it upon starting again (or handle it within the timer's handle method)
                // For simplicity now, just restart (will cause time jump on resume)
                // A better approach involves tracking pause duration.
                simulationTimer.start(); // Restart the timer
                pauseButton.setText("Pause");
            }
        }
    }

    /**
     * Restarts the simulation by stopping the current timer, re-initializing
     * the simulation state (time, network, vehicles), and starting a new timer.
     * Resets the pause button text.
     * @param pauseButton The pause/resume button to reset its text.
     */
    private void restartSimulation(Button pauseButton) {
        // Stop the current simulation loop if it's running
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        // Re-initialize the core simulation components
        initializeSimulation();
        // Start the simulation loop again
        startSimulationLoop();
        // Reset the pause button text
        if (pauseButton != null) {
            pauseButton.setText("Pause");
        }
    }

    /**
     * Displays an information dialog about the simulation.
     */
    private void showInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Simulation Info");
        alert.setHeaderText("Traffic Simulation"); // Version could be added here
        alert.setContentText("A basic traffic simulation demonstrating lane following, " +
                "traffic lights, and simple car-following behavior.\n\n" +
                "- Uses JavaFX Canvas for rendering.\n" +
                "- Implements traffic light cycles.\n" +
                "- Vehicles attempt to follow IDM principles.\n\n" +
                "(Developed for educational purposes)");
        alert.showAndWait();
    }


    /**
     * Sets up event handlers for mouse interactions on the canvas (currently placeholders).
     */
    private void setupInteractionHandlers() {
        simulationCanvas.setOnMouseClicked(e -> {
            // Example: Could be used to get info about clicked location or vehicle
            // roadNetwork.disturbTrafficAt(e.getX(), e.getY());
            System.out.println("Canvas clicked at: (" + e.getX() + ", " + e.getY() + ")");
        });

        // simulationCanvas.setOnMouseDragged(e -> { /* Potential future use */ });
    }

    /**
     * Creates and starts the main {@code AnimationTimer} which drives the simulation loop.
     */
    private void startSimulationLoop() {
        simulationTimer = new AnimationTimer() {
            // Store the timestamp of the last update to calculate delta time
            private long lastUpdateNanos = 0;

            /**
             * This method is called by the JavaFX framework approximately 60 times per second.
             * It calculates the time elapsed since the last call, updates the simulation state,
             * and triggers rendering.
             * @param nowNanos The current time in nanoseconds.
             */
            @Override
            public void handle(long nowNanos) {
                // Initialize lastUpdateNanos on the first frame
                if (lastUpdateNanos == 0) {
                    lastUpdateNanos = nowNanos;
                    return;
                }

                // Calculate time elapsed since the last frame in seconds
                double elapsedSeconds = (nowNanos - lastUpdateNanos) / 1_000_000_000.0;
                lastUpdateNanos = nowNanos;

                // --- Frame Skipping / Stability ---
                // Prevent unusually large time steps (e.g., after resuming from pause or lag)
                // from causing instability by skipping the update for that frame.
                final double maxElapsedTime = 0.5; // Max allowed time step (seconds)
                if (elapsedSeconds > maxElapsedTime) {
                    System.out.println("Large timestep detected (" + String.format("%.3f", elapsedSeconds) + "s), skipping frame to maintain stability.");
                    return; // Skip this frame's update
                }

                // Apply the timelapse speed multiplier to the elapsed time
                double simulationDeltaTime = elapsedSeconds * timelapseSpeed;

                // Accumulate the total simulation time
                time += simulationDeltaTime;

                // --- Update Simulation Logic ---
                // Update the state of all traffic lights based on their cycle managers
                roadNetwork.updateTrafficLights(simulationDeltaTime * 1000); // Manager expects milliseconds

                // Update all vehicles (movement, acceleration, lane changes, light checking)
                vehicleManager.update(simulationDeltaTime); // Vehicle logic uses seconds

                // --- Render the updated state ---
                render();
            }
        };
        simulationTimer.start(); // Begin the animation loop
    }

    /**
     * Stops the simulation loop when the application is closing.
     */
    private void stopSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        System.out.println("Simulation stopped.");
        // Potential place to save simulation state if needed
    }


    /**
     * Renders the current state of the simulation onto the canvas.
     * Clears the canvas, then draws the road network, vehicles, and overlay information.
     */
    private void render() {
        // Clear canvas with background color
        gc.setFill(Color.SEAGREEN);
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());

        // Render the static road network elements (roads, junctions, lights)
        roadNetwork.render(gc);

        // Render the dynamic elements (vehicles)
        vehicleManager.render(gc);

        // Render overlay information (time, vehicle count)
        gc.setFill(Color.WHITE);
        gc.fillRect(5, 5, 180, 40); // Background box for text
        gc.setFill(Color.BLACK);
        gc.setFont(new javafx.scene.text.Font("Arial", 12));
        gc.fillText(String.format("Time: %.1f s", time), 10, 20);
        gc.fillText(String.format("Vehicles: %d", vehicleManager.getVehicles().size()), 10, 35);
    }

    /**
     * The main method, launching the JavaFX application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        launch(args);
    }
}