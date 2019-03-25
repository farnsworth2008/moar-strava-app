package moar.strava.client;

public interface RefreshTokenResponse {
  String getAccessToken();
  Long getExpiresAt();
  Long getExpiresIn();
  String getRefreshToken();
  void setExpiresAt(Long expiresAt);
}
