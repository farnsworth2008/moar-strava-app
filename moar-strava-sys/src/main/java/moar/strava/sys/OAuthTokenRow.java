package moar.strava.sys;

import moar.awake.WakeableRow;

public interface OAuthTokenRow
    extends
    WakeableRow.IdColumnAsLong {
  String getAccessToken();
  Long getExpiresAt();
  String getRefreshToken();
  void setAccessToken(String value);
  void setExpiresAt(Long value);
  void setRefreshToken(String value);
}
