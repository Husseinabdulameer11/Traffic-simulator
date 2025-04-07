# OBJ2100 Oblig 2 (Vår 2025): Trafikksimulering - Gruppe 1

Dette prosjektet er en del av Obligatorisk Oppgave 2 i kurset OBJ2100 Objektorientert Programmering 2 ved **Universitetet i Sørøst-Norge (USN)**.

## Oversikt

Prosjektet implementerer en trafikksimulering i Java med JavaFX for visualisering. Målet er å simulere trafikkflyt i et veikryss-system, med fokus på realistisk biloppførsel, kjørefelt-disiplin, og håndtering av trafikklys. Koden demonstrerer objektorienterte prinsipper og håndtering av samtidige hendelser i en grafisk simuleringskontekst ved hjelp av `AnimationTimer`.

Simuleringen viser kjøretøy som navigerer et veinettverk, følger tilordnede kjørefelt, holder avstand til bilen foran (basert på IDM-prinsipper), og responderer på trafikklyssignaler ved kryss.

## Gruppemedlemmer (Gruppe 1)

*   Anas Mahamoud Hussein
*   Martin Pettersen
*   Hussein Khudhaer Abdul-Ameer
*   Khalif Cali Maxamed

*(Merk: Detaljert ansvarsfordeling skal inkluderes i det separate PDF-dokumentet ved innlevering).*

## Kurskontekst

*   **Kurs:** OBJ2100 Objektorientert Programmering 2
*   **Universitet:** Universitetet i Sørøst-Norge (USN)
*   **Periode:** Vår 2025
*   **Oppgave:** Obligatorisk Oppgave 2 - Prosjekt 1: Trafikksimulering

## Funksjonalitet

### Obligatoriske Funksjoner Implementert:

*   **Viktige Trafikkenheter:**
    *   Implementerer et sentralt, fireveis **lyskryss** (`JunctionType.TRAFFIC_LIGHT`) der biler og trafikklys samhandler. *(Merk: Selv om oppgaven krevde minst 3 kryss, fokuserer denne implementasjonen på mekanismene i ett fullt fungerende lyskryss).*
    *   **Trafikklys** (`TrafficLight`, `TrafficLightCycleManager`) veksler korrekt mellom RØDT, GULT, og GRØNT i synkroniserte sykluser for N/S og E/W retninger.
    *   Flere (konfigurerbart antall via `VehicleManager`) **kjøretøy** (`Vehicle`) opererer uavhengig, med tilfeldige farger og typer (Bil/Lastebil).
    *   Kjøretøy følger definerte **kjørefelt** (`Lane`) med korrekt høyrekjøring (offset fra veiens midtlinje).
    *   Kjøretøy velger neste vei **tilfeldig** ved kryss (forenklet ruting).
*   **Kontroll av Trafikkflyt:**
    *   Kjøretøy holder avstand til bilen foran ved hjelp av en **bilfølgemodell** (inspirert av IDM - Intelligent Driver Model).
    *   Kjøretøy **stopper korrekt** for rødt og gult lys ved det implementerte lyskrysset, før de når selve kryssområdet.
    *   Kollisjoner i kryss unngås primært gjennom trafikklyssystemet og at kun én retning har grønt om gangen.
    *   Fastlåste situasjoner (deadlocks) i det sentrale krysset unngås via den styrte lyssyklusen.
*   **Håndtering av Feil (Stabilitet):**
    *   Programmet bruker JavaFX `AnimationTimer` for en jevn og stabil hovedløkke.
    *   Grunnleggende feilhåndtering og logging av advarsler/feil til konsoll er implementert (f.eks. ved manglende lys-oppsett).

### Valgfrie Funksjoner Implementert:

*(Merk av med [X] for de dere faktisk har fullført - minimum én kreves)*

*   **[X] Logging eller Visualisering:**
    *   **Visualisering:** Hele simuleringen er visualisert grafisk ved hjelp av JavaFX Canvas. Kjøretøybevegelser, kjørefelt, kryss og trafikklysenes tilstand vises dynamisk.
    *   **Logging:** Systemet bruker `System.out.println` / `System.err.println` for å logge nøkkelhendelser (oppstart, lysbytter, feil) til konsollen.
*   **[X] Andre Funksjoner - GUI Parameterjustering:**
    *   Brukergrensesnittet tillater sanntidsjustering av:
        *   **Total Inflow:** Kontrollerer hvor mange kjøretøy som genereres per time.
        *   **Timelapse Speed:** Justerer simuleringshastigheten.
*   **[ ] Andre Funksjoner - Kjøretøyprioritering:** (Ikke implementert)
*   **[ ] Andre Funksjoner - Flere Kjørefelt per Vei:** (Ikke implementert - kun ett felt per retning)
*   **[ ] Andre Funksjoner - Justerbar Lysvarighet via GUI:** (Ikke implementert)
*   **[ ] Andre Funksjoner - Adaptiv Lysstyring:** (Ikke implementert)
*   **[ ] Dataserialisering:** (Ikke implementert)

## Teknologistabel

*   **Språk:** Java (JDK 17+)
*   **Grafikk/GUI:** JavaFX (Canvas API)
*   **IDE:** [Fyll inn IDE, f.eks. IntelliJ IDEA, Eclipse, VS Code]

## Oppsett og Kjøring

1.  **Forutsetninger:**
    *   Java Development Kit (JDK) versjon 17 eller nyere.
    *   JavaFX SDK riktig konfigurert for ditt system/IDE. Se [OpenJFX Getting Started](https://openjfx.io/openjfx-docs/).
2.  **Klon Repository:** `git clone [URL til deres repo]`
3.  **Åpne i IDE:** Åpne prosjektmappen i din IDE.
4.  **Konfigurer JavaFX (om nødvendig):**
    *   Legg til JavaFX i prosjektets biblioteker/avhengigheter.
    *   Sett opp VM Options i Run Configuration hvis du ikke bruker et modulbasert prosjekt (anbefales for JavaFX):
        *   Eksempel: `--module-path /path/to/javafx-sdk-XX/lib --add-modules javafx.controls,javafx.graphics` (Bytt ut path og versjon).
5.  **Bygg Prosjektet:** Bruk IDEens byggefunksjon (f.eks. `Build -> Build Project`).
6.  **Kjør Applikasjonen:** Kjør `main`-metoden i `TrafficSimulation.java`.

## Prosjektstruktur (Nøkkelklasser)

*   `TrafficSimulation`: JavaFX `Application`, GUI, hovedløkke (`AnimationTimer`).
*   `RoadNetwork`: Holder styr på veier, kryss, lys; bygger layout.
*   `VehicleManager`: Oppretter, oppdaterer, rendrer og fjerner kjøretøy.
*   `Vehicle`: Individuelt kjøretøy; tilstand, fysikk (IDM), oppførsel.
*   `Junction`: Veikryss; kobler veier, håndterer lys (via manager).
*   `Road`: Veisegment; inneholder `Lane`s.
*   `Lane`: Ett kjørefelt; geometri, retning.
*   `TrafficLight`: Visuell representasjon av et R/G/Y-lys.
*   `TrafficLightCycleManager`: Styrer timingen for et helt kryss' lys.
*   `LightState`: Enum for lysets tilstand (RED, YELLOW, GREEN).
*   `JunctionType`: Enum for krysstype (TRAFFIC_LIGHT, ROUNDABOUT).

## Innleveringsinformasjon (For Gruppe 1)

*   **Innleveringsfiler:** `Gruppe1.zip` (kildekode), `Gruppe1.pdf` (dokumentasjon), `Gruppe1.mp4` (demo-video).
*   **Frist:** Søndag 13. april 2025, kl. 23:59.