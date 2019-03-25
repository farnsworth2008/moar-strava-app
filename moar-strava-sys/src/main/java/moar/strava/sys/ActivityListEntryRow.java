package moar.strava.sys;

import moar.awake.WakeableRow;

public interface ActivityListEntryRow
    extends
    WakeableRow {
  Long getActivityId();
  Long getActivityListId();
  void setActivityId(Long value);
  void setActivityListId(Long value);
}
