package moar.strava.sys;

import moar.awake.WakeableRow;

public interface ActivityListRow
    extends
    WakeableRow.IdColumnAsAutoLong {
  void setAfter(Long value);
  void setAthleteId(Long value);
  void setBefore(Long value);
  void setYear(Integer year);
}
