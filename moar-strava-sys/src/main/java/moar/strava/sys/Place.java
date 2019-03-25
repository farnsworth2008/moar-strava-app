package moar.strava.sys;

import static moar.geo.GeoFactory.getGeoService;
import java.util.List;
import moar.geo.GeoPoint;

public class Place {
  private final List<GeoPoint> boundery;
  private final String name;
  private final String county;
  private final String state;
  private final String country;
  private final Long placeId;

  public Place(Long placeId, String name, String country, String state, String county, List<GeoPoint> boundery) {
    this.placeId = placeId;
    this.name = name;
    this.county = county;
    this.boundery = boundery;
    this.country = country;
    this.state = state;
  }

  public boolean contains(GeoPoint point) {
    return getGeoService().inside(point, boundery);
  }

  public List<GeoPoint> getBoundery() {
    return boundery;
  }

  public String getCountry() {
    return country;
  }

  public String getCounty() {
    return county;
  }

  public String getName() {
    return name;
  }

  public Long getPlaceId() {
    return placeId;
  }

  public String getState() {
    return state;
  }

  @Override
  public String toString() {
    return name;
  }
}
