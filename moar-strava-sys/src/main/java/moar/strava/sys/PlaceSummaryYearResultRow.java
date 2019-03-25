package moar.strava.sys;

public interface PlaceSummaryYearResultRow {
  Long getAthleteId();
  Boolean getDetail();
  String getResult();
  Integer getYear();
  void setAthleteId(Long value);
  void setDetail(Boolean value);
  void setMilliseconds(Long value);
  void setResult(String value);
  void setYear(Integer value);
}
