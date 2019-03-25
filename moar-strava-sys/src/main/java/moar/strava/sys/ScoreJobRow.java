package moar.strava.sys;

import moar.awake.WakeableRow;

public interface ScoreJobRow
    extends
    WakeableRow.IdColumnAsLong {
  void setDetail(Boolean value);
  void setYear(Integer value);
}
