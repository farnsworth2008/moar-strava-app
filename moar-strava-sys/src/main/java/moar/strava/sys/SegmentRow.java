package moar.strava.sys;

import moar.awake.WakeableRow;

public interface SegmentRow
    extends
    WakeableRow.IdColumnAsLong {
  String getActivityType();
  Double getAverageGrade();
  Double getClimbCategory();
  Double getDistance();
  Double getElevationHigh();
  Double getElevationLow();
  Double getMaximumGrade();
  String getName();
  Double getStartLatitude();
  Double getStartLongitude();

  void setActivityType(String value);
  void setAverageGrade(Double value);
  void setClimbCategory(Double value);
  void setDistance(Double value);
  void setElevationHigh(Double value);
  void setElevationLow(Double value);
  void setMaximumGrade(Double value);
  void setName(String value);
  void setStartLatitude(Double value);
  void setStartLongitude(Double value);

}
