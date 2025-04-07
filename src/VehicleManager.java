
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator; // Bruk iterator for sikker fjerning
import java.util.List;
import java.util.Random;

public class VehicleManager {
    private List<Vehicle> vehicles = new ArrayList<>();
    private RoadNetwork roadNetwork;
    private double inflow = 1000; // Endret startverdi for testing
    private Random random = new Random();
    private double spawnTimer = 0;
    private double inflowRateVehiclesPerSecond; // Kalkulert rate

    // Farger fra gruppens kode
    private static final Color[] PREDEFINED_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.ORANGE, Color.PURPLE, Color.BROWN, Color.BLACK,
            Color.CYAN, Color.MAGENTA, Color.PINK, Color.GRAY
    };

    public VehicleManager(RoadNetwork roadNetwork) {
        this.roadNetwork = roadNetwork;
        setInflow(this.inflow); // Kalkulerer raten ved oppstart
    }

    public void update(double deltaTime) { // deltaTime i sekunder
        // 1. Spawn nye kjøretøy
        if (inflowRateVehiclesPerSecond > 0) {
            spawnTimer += deltaTime;
            double spawnInterval = 1.0 / inflowRateVehiclesPerSecond; // Tid mellom spawns i sekunder
            // Spawn flere hvis deltaTime er stor nok
            while (spawnTimer >= spawnInterval) {
                spawnVehicle();
                spawnTimer -= spawnInterval;
            }
        }

        // 2. Oppdater eksisterende kjøretøy og fjern de som er utenfor
        // Bruk iterator for å trygt kunne fjerne elementer mens vi itererer
        Iterator<Vehicle> iterator = vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            vehicle.update(deltaTime);

            // Bruk gruppens logikk for å fjerne biler som kjører utenfor definerte grenser
            if (isOutOfBounds(vehicle)) {
                System.out.println("Removing vehicle " + System.identityHashCode(vehicle) + " (out of bounds)");
                iterator.remove(); // Fjern trygt
            }
            // TODO: Vurder å fjerne biler som når en "exit"-lane i stedet/i tillegg
        }
    }

    private void spawnVehicle() {
        // Få en tilfeldig innkjørings-lane fra RoadNetwork
        Lane entryLane = roadNetwork.getRandomEntryLane();
        if (entryLane == null) {
            // System.err.println("Could not find entry lane to spawn vehicle.");
            return; // Ingen steder å spawne
        }

        // --- Enkel kollisjonssjekk ved spawn ---
        double requiredSpace = VehicleType.CAR.getLength() * 1.5; // Minimum plass ved start
        for (Vehicle existing : vehicles) {
            if (existing.getCurrentLane() == entryLane) {
                // Hvor langt er den eksisterende bilen fra starten av lanen?
                double distanceIntoLane = existing.getPosition() * entryLane.getLength();
                if (distanceIntoLane < requiredSpace) {
                    // System.out.println("Spawn prevented on lane: Collision detected near start.");
                    return; // Ikke spawn hvis det er en bil rett ved starten
                }
            }
        }
        // --- Slutt kollisjonssjekk ---


        // Velg type og farge
        VehicleType type = random.nextDouble() < 0.8 ? VehicleType.CAR : VehicleType.TRUCK; // 80% biler
        Color color = getRandomColor();

        // --- VIKTIG: Send med referanser til 'this' og 'roadNetwork' ---
        Vehicle vehicle = new Vehicle(type, color, entryLane, this, roadNetwork);
        vehicles.add(vehicle);
        // System.out.println("Spawned vehicle on lane associated with road starting at ("+entryLane.getRoad().getStartX()+","+entryLane.getRoad().getStartY()+")");
    }

    // Hentet fra gruppens kode
    private Color getRandomColor() {
        return PREDEFINED_COLORS[random.nextInt(PREDEFINED_COLORS.length)];
    }

    // Hentet fra gruppens kode
    private boolean isOutOfBounds(Vehicle vehicle) {
        double x = vehicle.getX();
        double y = vehicle.getY();
        // Bruk gjerne canvas størrelse her hvis tilgjengelig, ellers hardkodet
        double margin = 50; // Ekstra margin utenfor skjermen
        double canvasWidth = 1000; // Antatt bredde
        double canvasHeight = 750; // Antatt høyde
        return x < -margin || x > canvasWidth + margin || y < -margin || y > canvasHeight + margin;
    }

    public void render(GraphicsContext gc) {
        for (Vehicle vehicle : vehicles) {
            vehicle.render(gc);
        }
    }

    // Oppdaterer raten når inflow endres
    public void setInflow(double vehiclesPerHour) {
        this.inflow = vehiclesPerHour; // Lagre den opprinnelige verdien om ønskelig
        if (this.inflow <= 0) {
            this.inflowRateVehiclesPerSecond = 0;
        } else {
            // Kalkuler rate i biler per sekund
            this.inflowRateVehiclesPerSecond = this.inflow / 3600.0;
        }
        // System.out.println("VehicleManager inflow set to " + this.inflow + " veh/h (" + String.format("%.3f", this.inflowRateVehiclesPerSecond) + " veh/s)");
    }

    // --- VIKTIG METODE: Finn bilen foran på samme lane ---
    public Vehicle findVehicleAheadOnLane(Vehicle subjectVehicle, Lane lane) {
        Vehicle leader = null;
        double minPositiveDistanceFraction = Double.POSITIVE_INFINITY; // Se etter minste positive posisjonsforskjell

        for (Vehicle other : vehicles) {
            // Hopp over oss selv og biler på andre lanes
            if (other == subjectVehicle || other.getCurrentLane() != lane) {
                continue;
            }

            // Beregn forskjell i posisjonsbrøk (0.0 til 1.0)
            double positionDifference = other.getPosition() - subjectVehicle.getPosition();

            // Vi leter etter den nærmeste bilen som har en STØRRE posisjonsbrøk
            if (positionDifference > 0.0001) { // Legg inn liten epsilon for flyttall
                if (positionDifference < minPositiveDistanceFraction) {
                    minPositiveDistanceFraction = positionDifference;
                    leader = other;
                }
            }
            // Denne logikken håndterer IKKE wraparound (f.eks. for rundkjøring)
        }
        return leader; // Returnerer null hvis ingen er foran
    }

    // Getter for antall biler (brukes i Main for info)
    public List<Vehicle> getVehicles() {
        return vehicles;
    }
}