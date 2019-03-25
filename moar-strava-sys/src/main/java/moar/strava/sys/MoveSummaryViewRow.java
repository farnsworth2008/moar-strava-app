package moar.strava.sys;

public interface MoveSummaryViewRow {
  Long getAthleteId();
  String getCountry();
  Double getMeters();
  Boolean getOffRoad();
  String getPlaceName();
  String getState();
  void setAthleteId(Long value);
}