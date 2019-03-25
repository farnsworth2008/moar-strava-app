package moar.strava.client;

public interface SummaryClub {
  String getCity();
  String getCountry();
  String getCoverPhoto();
  String getCoverPhotoSmall();
  Boolean getFeatured();
  Long getId();
  long getMemberCount();
  String getName();
  Boolean getPrivate();
  String getProfileMedium();
  String getSportType();
  String getState();
  String getUrl();
  Boolean getVerified();
}
