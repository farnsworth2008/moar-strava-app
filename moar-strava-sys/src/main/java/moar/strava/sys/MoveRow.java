package moar.strava.sys;

import moar.awake.WakeableRow;

public interface MoveRow
    extends
    WakeableRow {
  Long getActivityId();
  Long getAthleteId();
  Float getLat();
  Float getLon();
  Float getMeters();
  Boolean getOffRoad();
  Long getPlaceId();
  Long getPointId();
  Integer getSeq();
  Integer getYear();
  void setActivityId(Long value);
  void setAthleteId(Long value);
  void setLat(Float value);
  void setLon(Float value);
  void setMeters(Float value);
  void setOffRoad(Boolean offRoad);
  void setPlaceId(Long placeId);
  void setPointId(Long value);
  void setSeq(Integer value);
  void setYear(Integer year);
}
