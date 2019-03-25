package moar.strava.sys;

import moar.awake.WakeableRow;

public interface PlaceBoundaryRow
    extends
    WakeableRow {
  Long getPlaceId();
  Long getPointId();
  Integer getSeq();
  void setPlaceId(Long value);
  void setPointId(Long value);
  void setSeq(Integer value);
}
