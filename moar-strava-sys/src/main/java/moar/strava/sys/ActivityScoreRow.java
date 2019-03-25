package moar.strava.sys;

import moar.awake.WakeableRow;

public interface ActivityScoreRow
    extends
    WakeableRow.IdColumnAsAutoLong {
  Long getActivityId();
  void setActivityId(Long value);
  void setOffRoadMiles(Double value);
  void setRoadMiles(Double value);
  void setTotalMiles(Double value);
}
