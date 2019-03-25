package moar.strava.sys;

import java.util.Date;
import moar.awake.WakeableRow;

public interface ActivitySummaryRow
    extends
    WakeableRow.IdColumnAsLong {

  Long getAthleteId();
  Double getDistance();
  String getName();
  Date getStartDate();
  String getSummaryPolyline();
  String getType();
  void setAthleteId(Long value);
  void setDistance(Double value);
  void setElapsedTime(Double value);
  void setMovingTime(Double value);
  void setName(String value);
  void setStartDate(Date value);
  void setSummaryPolyline(String value);
  void setType(String value);

}
