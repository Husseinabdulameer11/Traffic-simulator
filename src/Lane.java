

public class Lane {
    private Road road;
    private boolean isForward;

    public Lane(Road road, boolean isForward) {
        this.road = road;
        this.isForward = isForward;
    }

    public Road getRoad() {
        return road;
    }

    public boolean isForward() {
        return isForward;
    }

    public double getLength() {
        return road.getLength();
    }
}