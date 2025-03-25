

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VehicleManager {
    private List<Vehicle> vehicles = new ArrayList<>();
    private RoadNetwork roadNetwork;
    private double inflow = 3600;
    private Random random = new Random();
    private double spawnTimer = 0;

    public VehicleManager(RoadNetwork roadNetwork) {
        this.roadNetwork = roadNetwork;
    }

    public void update(double deltaTime) {
        spawnTimer += deltaTime;
        double spawnInterval = 3600.0 / inflow;

        if (spawnTimer >= spawnInterval) {
            spawnVehicle();
            spawnTimer = 0;
        }

        List<Vehicle> toRemove = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            vehicle.update(deltaTime);

            if (isOutOfBounds(vehicle)) {
                toRemove.add(vehicle);
            }
        }

        vehicles.removeAll(toRemove);
    }

    private void spawnVehicle() {

        List<Road> roads = roadNetwork.getRoads();
        if (roads.isEmpty()) return;

        Road entryRoad = roads.get(random.nextInt(roads.size()));
        Lane entryLane = entryRoad.getLanes().get(random.nextInt(entryRoad.getLanes().size()));

        VehicleType type = random.nextDouble() < 0.8 ? VehicleType.CAR : VehicleType.TRUCK;
        Color color = getRandomColor();

        Vehicle vehicle = new Vehicle(type, color, entryLane);
        vehicles.add(vehicle);
    }

    private Color getRandomColor() {

        Color[] colors = {
                Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.ORANGE, Color.PURPLE, Color.BROWN, Color.BLACK
        };
        return colors[random.nextInt(colors.length)];
    }

    private boolean isOutOfBounds(Vehicle vehicle) {
        double x = vehicle.getX();
        double y = vehicle.getY();


        return x < -50 || x > 850 || y < -50 || y > 650;
    }

    public void render(GraphicsContext gc) {
        for (Vehicle vehicle : vehicles) {
            vehicle.render(gc);
        }
    }

    public void setInflow(double inflow) {
        this.inflow = inflow;
    }
}