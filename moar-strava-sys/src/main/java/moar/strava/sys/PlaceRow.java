package moar.strava.sys;

import moar.awake.WakeableRow;

public interface PlaceRow
    extends
    WakeableRow.IdColumnAsAutoLong {
  Boolean getBoundary();
  String getCountry();
  String getCounty();
  String getPlaceName();
  String getState();
  Boolean setBoundary(boolean value);
  void setCountry(String value);
  void setCounty(String value);
  void setPlaceName(String value);
  void setState(String value);
}
