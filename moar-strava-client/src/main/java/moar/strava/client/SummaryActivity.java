package moar.strava.client;

import java.util.List;

public interface SummaryActivity {
  MetaAthlete getAthlete();
  Double getDistance();
  Double getElapsedTime();
  List<Double> getEndLatlng();
  Long getId();
  ActivityMap getMap();
  Double getMovingTime();
  String getName();
  String getStartDate();
  List<Double> getStartLatlng();
  Double getTotalElevationGain();
  String getType();
}
