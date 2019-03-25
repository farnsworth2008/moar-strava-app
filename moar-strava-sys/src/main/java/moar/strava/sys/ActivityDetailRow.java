package moar.strava.sys;

import moar.awake.WakeableRow;

public interface ActivityDetailRow
    extends
    WakeableRow.IdColumnAsLong {
  String getPolyline();
  void setPolyline(String value);
  void setScoredDistance(Double value);
}
