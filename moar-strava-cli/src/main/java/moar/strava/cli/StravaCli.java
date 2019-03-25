package moar.strava.cli;
import static java.lang.String.format;
import static java.lang.System.out;
import static moar.ansi.Ansi.red;
import static moar.sugar.Sugar.nonNull;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import moar.ansi.StatusLine;
import moar.strava.sys.MoarStravaSys;

public class StravaCli {
  private final String command;

  private final MoarStravaSys sys;

  public StravaCli(String command, String profile) {
    var status = new StatusLine("Creating Moar Strava System");
    sys = new MoarStravaSys(profile);
    status.remove();
    this.command = command;
  }

  private String getCurrentFormattedTime() {
    var now = LocalTime.now();
    var timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    String currentFormatedTime = now.format(timeFormat);
    return currentFormatedTime;
  }

  protected void loadKml(StatusLine status, File loadFile) {
    var loadFilename = loadFile.getName();
    var points = sys.getGeo().readKml2(loadFile);
    var placeName = loadFilename.replaceAll("\\.kml$", "");
    var country = "";
    var state = "";
    var county = "";
    status.set(loadFilename);
    for (var point : points) {
      var placePointRow = sys.upsertPointRow(point, sys.getGeoPointD5Id(point), null);
      var placeRow = sys.getPlaceRow(placePointRow.getPlaceId());
      country = nonNull(placeRow.getCountry(), country);
      state = nonNull(placeRow.getState(), state);
      county = nonNull(placeRow.getCounty(), county);
      var hasCountry = !country.isEmpty();
      boolean hasState = !state.isEmpty();
      boolean hasCounty = !county.isEmpty();
      if (hasCountry && hasState && hasCounty) {
        // We have what we need, we can exit now
        break;
      }
    }
    var placeId = sys.getPlaceId(placeName, country, state, county);
    var placeRow = sys.getPlaceRow(placeId);
    if (!placeRow.getBoundary()) {
      placeRow.setBoundary(true);
      sys.upsert(placeRow);
      sys.invalidatePlaceRow(placeRow.getId());
    }
    sys.deletePlaceBoundaryRows(placeId);
    var seq = new AtomicInteger();
    for (var point : points) {
      var row = sys.definePlaceBoundaryRow();
      row.setSeq(seq.getAndIncrement());
      row.setPointId(sys.getGeoPointD5Id(point));
      row.setPlaceId(placeId);
      sys.upsert(row);
    }
  }

  void loadPlaces() {
    var loadDir = new File("load");

    var loadFiles = loadDir.listFiles();
    if (loadFiles == null || loadFiles.length == 0) {
      return;
    }

    var status = new StatusLine(loadFiles.length, "Loading Places");
    for (var loadFile : loadFiles) {
      var loadFilename = loadFile.getName();
      if (loadFilename.endsWith(".kml")) {
        loadKml(status, loadFile);
      }
      status.completeOne();
    }
    status.remove();
    outTimestamp("Completed KML loading");
  }

  protected void outTimestamp(String msg) {
    out.println(format("%s " + msg, getCurrentFormattedTime()));
  }

  public void run() {
    if ("load-places".equals(command)) {
      loadPlaces();
    } else {
      out.println(red(format("Unknown command: %s", command)));
    }
  }

}