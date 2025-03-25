

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Road {
    private double startX, startY, endX, endY;
    private double width = 30;
    private List<Lane> lanes = new ArrayList<>();

    public Road(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;


        createLanes();
    }

    private void createLanes() {

        lanes.add(new Lane(this, true));  // Forward direction
        lanes.add(new Lane(this, false)); // Reverse direction
    }

    public void render(GraphicsContext gc) {

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(width);
        gc.strokeLine(startX, startY, endX, endY);


        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(10, 10);
        gc.strokeLine(startX, startY, endX, endY);
        gc.setLineDashes(0);
    }


    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public double getLength() {
        return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
    }
    public List<Lane> getLanes() { return lanes; }
}