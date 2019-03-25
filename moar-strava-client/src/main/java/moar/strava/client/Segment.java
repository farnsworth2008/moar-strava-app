package moar.strava.client;

public interface Segment {
  String getActivityType();
  Double getAverageGrade();
  String getCity();
  Double getClimbCategory();
  String getCountry();
  Double getDistance();
  Double getElevationHigh();
  Double getElevationLow();
  Double getEndLatitude();
  Double getEndLongitude();
  Long getId();
  Double getMaximumGrade();
  String getName();
  Boolean getStarred();
  Double getStartLatitude();
  Double getStartLongitude();
  String getState();
}
