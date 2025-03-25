
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

enum JunctionType {
    ROUNDABOUT,
    TRAFFIC_LIGHT,
    PRIORITY
}

public class Junction {
    private double x, y;
    private double radius;
    private JunctionType type;
    private List<Road> connectedRoads = new ArrayList<>();

    public Junction(double x, double y, double radius, JunctionType type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
    }

    public void connectRoad(Road road) {
        connectedRoads.add(road);
    }

    public void render(GraphicsContext gc) {

        switch (type) {
            case ROUNDABOUT:
                gc.setFill(Color.LIGHTGRAY);
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);


                gc.setFill(Color.DARKGREEN);
                gc.fillOval(x - radius * 0.7, y - radius * 0.7, radius * 1.4, radius * 1.4);
                break;

            case TRAFFIC_LIGHT:

                break;

            case PRIORITY:
                break;
        }
    }


    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public JunctionType getType() { return type; }
    public List<Road> getConnectedRoads() { return connectedRoads; }
}