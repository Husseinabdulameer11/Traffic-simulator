

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class RoadNetwork {
    private List<Road> roads = new ArrayList<>();
    private List<Junction> junctions = new ArrayList<>();

    public RoadNetwork() {

        createRoundabout();
    }

    private void createRoundabout() {

        Junction roundabout = new Junction(400, 300, 80, JunctionType.ROUNDABOUT);
        junctions.add(roundabout);


        Road northRoad = new Road(400, 0, 400, 220);
        Road eastRoad = new Road(580, 300, 800, 300);
        Road southRoad = new Road(400, 380, 400, 600);
        Road westRoad = new Road(0, 300, 320, 300);

        roads.add(northRoad);
        roads.add(eastRoad);
        roads.add(southRoad);
        roads.add(westRoad);


        roundabout.connectRoad(northRoad);
        roundabout.connectRoad(eastRoad);
        roundabout.connectRoad(southRoad);
        roundabout.connectRoad(westRoad);
    }

    public void render(GraphicsContext gc) {

        gc.setLineWidth(30);
        gc.setStroke(Color.LIGHTGRAY);

        for (Road road : roads) {
            road.render(gc);
        }


        for (Junction junction : junctions) {
            junction.render(gc);
        }
    }

    public void disturbTrafficAt(double x, double y) {

    }


    public List<Road> getRoads() {
        return roads;
    }

    public List<Junction> getJunctions() {
        return junctions;
    }
}