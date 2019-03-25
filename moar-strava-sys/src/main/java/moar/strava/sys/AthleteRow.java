package moar.strava.sys;

import java.util.Date;
import moar.awake.WakeableRow;

public interface AthleteRow
    extends
    WakeableRow.IdColumnAsLong {
  void setCity(String value);
  void setCountry(String value);
  void setCreatedAt(Date value);
  void setFirstname(String value);
  void setLastname(String value);
  void setProfile(String value);
  void setProfileMedium(String value);
  void setSex(String value);
  void setState(String value);
}
