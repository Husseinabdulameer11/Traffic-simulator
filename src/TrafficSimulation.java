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
import javafx.scene.image.Image;

public class TrafficSimulation extends Application {

    private Canvas simulationCanvas;
    private GraphicsContext gc;
    private double time = 0;
    private RoadNetwork roadNetwork;
    private VehicleManager vehicleManager;


    private double totalInflow = 3600;
    private double mainroadPercentage = 100;
    private double timelapseSpeed = 20;
    private double maxAcceleration = 1.5;
    private double maxSpeed = 50;
    private double timeGap = 1.4;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();


        simulationCanvas = new Canvas(800, 600);
        gc = simulationCanvas.getGraphicsContext2D();


        roadNetwork = new RoadNetwork();
        vehicleManager = new VehicleManager(roadNetwork);


        VBox controlPanel = createControlPanel();


        root.setCenter(simulationCanvas);
        root.setBottom(controlPanel);


        setupInteractionHandlers();

        startSimulation();

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("Traffic Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        Label flowLabel = new Label("Traffic Flow and General");
        flowLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        HBox inflowBox = new HBox(10);
        Label inflowLabel = new Label("Total Inflow");
        Slider inflowSlider = new Slider(0, 7200, totalInflow);
        Label inflowValue = new Label(totalInflow + " veh/h");
        inflowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            totalInflow = newVal.doubleValue();
            inflowValue.setText(String.format("%.0f veh/h", totalInflow));
            vehicleManager.setInflow(totalInflow);
        });
        inflowBox.getChildren().addAll(inflowLabel, inflowSlider, inflowValue);


        Label behaviorLabel = new Label("Car-Following Behavior");
        behaviorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        panel.getChildren().addAll(
                flowLabel,
                inflowBox,
                behaviorLabel
        );

        HBox buttonBox = new HBox(10);
        Button pauseButton = new Button("Pause");
        Button infoButton = new Button("Info");
        buttonBox.getChildren().addAll(pauseButton, infoButton);
        panel.getChildren().add(buttonBox);

        return panel;
    }

    private void setupInteractionHandlers() {
        simulationCanvas.setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            roadNetwork.disturbTrafficAt(x, y);
        });

        simulationCanvas.setOnMouseDragged(e -> {
            double x = e.getX();
            double y = e.getY();
        });
    }

    private void startSimulation() {
        new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                time += elapsedSeconds * timelapseSpeed;

                vehicleManager.update(elapsedSeconds * timelapseSpeed);

                render();
            }
        }.start();
    }

    private void render() {
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());

        roadNetwork.render(gc);

        vehicleManager.render(gc);


        gc.setFill(Color.WHITE);
        gc.fillRect(10, 10, 150, 30);
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("Time=%.1f s", time), 20, 30);
    }

    public static void main(String[] args) {
        launch(args);
    }
}