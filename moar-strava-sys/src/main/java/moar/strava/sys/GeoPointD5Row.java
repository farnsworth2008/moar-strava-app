package moar.strava.sys;

import moar.awake.WakeableRow;

public interface GeoPointD5Row
    extends
    WakeableRow.IdColumnAsLong {
  Long getInferred();
  Boolean getOffRoad();
  Long getPlaceId();
  void setInferred(Long value);
  void setOffRoad(Boolean value);
  void setPlaceId(Long value);
}
