

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

enum VehicleType {
    CAR(4.5, 2.0),
    TRUCK(8.0, 2.5);

    private final double length;
    private final double width;

    VehicleType(double length, double width) {
        this.length = length;
        this.width = width;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
}

public class Vehicle {
    private VehicleType type;
    private Color color;
    private Lane currentLane;
    private double position;
    private double speed;
    private double maxSpeed = 50;
    private double acceleration = 0;
    private double maxAcceleration = 1.5;
    private double x, y;
    private double angle;

    public Vehicle(VehicleType type, Color color, Lane lane) {
        this.type = type;
        this.color = color;
        this.currentLane = lane;
        this.position = 0;
        this.speed = maxSpeed * 0.5 + Math.random() * maxSpeed * 0.5;
        updatePosition();
    }

    public void update(double deltaTime) {
        Vehicle ahead = findVehicleAhead();

        if (ahead != null) {
            double distance = calculateDistance(ahead);
            double safeDistance = speed * 1.4;

            if (distance < safeDistance) {
                acceleration = -maxAcceleration;
            } else if (distance > safeDistance * 1.5) {
                acceleration = maxAcceleration;
            } else {

                acceleration = 0;
            }
        } else {
            if (speed < maxSpeed) {
                acceleration = maxAcceleration;
            } else {
                acceleration = 0;
            }
        }

        speed += acceleration * deltaTime * 3.6;
        speed = Math.max(0, Math.min(speed, maxSpeed));

        double distanceTraveled = speed * deltaTime / 3.6;
        position += distanceTraveled / currentLane.getLength();

        if (position >= 1.0) {

            position = 0;
        }

        updatePosition();
    }

    private void updatePosition() {

        Road road = currentLane.getRoad();
        boolean isForward = currentLane.isForward();

        double startX = road.getStartX();
        double startY = road.getStartY();
        double endX = road.getEndX();
        double endY = road.getEndY();


        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx*dx + dy*dy);


        dx /= length;
        dy /= length;

        double perpX = -dy;
        double perpY = dx;


        double offset = isForward ? -5 : 5;


        startX += perpX * offset;
        startY += perpY * offset;
        endX += perpX * offset;
        endY += perpY * offset;


        if (isForward) {
            x = startX + (endX - startX) * position;
            y = startY + (endY - startY) * position;
            angle = Math.atan2(endY - startY, endX - startX);
        } else {
            x = endX + (startX - endX) * position;
            y = endY + (startY - endY) * position;
            angle = Math.atan2(startY - endY, startX - endX);
        }
    }

    private Vehicle findVehicleAhead() {

        return null; // Placeholder
    }

    private double calculateDistance(Vehicle ahead) {

        double dx = ahead.getX() - x;
        double dy = ahead.getY() - y;
        return Math.sqrt(dx*dx + dy*dy) - type.getLength() / 2 - ahead.getType().getLength() / 2;
    }

    public void render(GraphicsContext gc) {

        gc.save();


        gc.translate(x, y);


        gc.rotate(Math.toDegrees(angle));


        gc.setFill(color);
        double length = type.getLength() * 2;
        double width = type.getWidth() * 2;
        gc.fillRect(-length/2, -width/2, length, width);


        if (type == VehicleType.CAR) {
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(-length/2 + length*0.2, -width/2 + width*0.2, length*0.3, width*0.6);
        }


        gc.restore();
    }


    public double getX() { return x; }
    public double getY() { return y; }
    public VehicleType getType() { return type; }
    public double getSpeed() { return speed; }
}