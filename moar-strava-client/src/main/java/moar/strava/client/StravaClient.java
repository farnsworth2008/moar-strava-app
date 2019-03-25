package moar.strava.client;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.lang.Thread.sleep;
import static java.time.ZoneOffset.UTC;
import static moar.awake.InterfaceUtil.use;
import static moar.oauth.OAuthClientFactory.getOAuthClient;
import static moar.sugar.MoarJson.getMoarJson;
import static moar.sugar.Sugar.require;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moar.oauth.OAuthClient;
import moar.sugar.CallableVoid;

public class StravaClient {
  public static final String STRAVA_TOKEN_URL = "https://www.strava.com/oauth/token";
  public static final String STRAVA_AUTH_URL = "https://www.strava.com/oauth/authorize";
  public static final String STRAVA_CLIENT_ID = getenv("MOAR_STRAVA_CLIENT_ID");
  public static final String STRAVA_CLIENT_SECRET = getenv("MOAR_STRAVA_CLIENT_SECRET");
  private final OAuthClient client;
  private final Long id;

  public StravaClient(Long id, String token, long expiresAt) {
    this.id = id;
    client = getOAuthClient(token, expiresAt, "https://www.strava.com/api/v3");
    client.setThrottleWhen(200);
    client.setThrottleRate(.1);
    on429(() -> {
      var secondsToWait = 10;
      for (var i = 0; i < secondsToWait; i++) {
        sleep(1000);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public List<SummaryActivity> fetchActivities(Long before, Long after) {
    var perPage = 200;
    var pageList = new ArrayList<Map<String, Object>>();
    var activityList = new ArrayList<SummaryActivity>();
    var page = 0;
    do {
      var resource = "/athlete/activities";
      var request = new StringBuilder();
      request.append(resource);
      request.append(format("?per_page=%d", perPage));
      request.append(format("&page=%d", ++page));
      request.append(format("&before=%d", before));
      request.append(format("&after=%d", after));
      var result = client.get(List.class, request.toString());
      pageList = (ArrayList<Map<String, Object>>) result.getOrThrow();
      for (var item : pageList) {
        var activity = use(SummaryActivity.class).of(item);
        activityList.add(activity);
      }
    } while (pageList.size() >= perPage);
    return activityList;
  }

  @SuppressWarnings({ "unchecked" })
  public DetailedActivity fetchActivity(Long id) {
    var request = format("/activities/%s", id);
    var result = client.get(HashMap.class, request);
    var activityMap = result.get();
    return use(DetailedActivity.class).of(activityMap);
  }

  @SuppressWarnings("unchecked")
  public BaseAthlete fetchAthlete() {
    var resource = "/athlete";
    var map = client.get(HashMap.class, resource).getOrThrow();
    if ("Authorization Error".equals(map.get("message"))) {
      throw new AuthorizationError();
    }
    return use(DetailedAthlete.class).of(map);
  }

  @SuppressWarnings("unchecked")
  public List<SummaryActivity> fetchClubActivities(Long clubId) {
    var perPage = 200;
    var pageList = new ArrayList<Map<String, Object>>();
    var activityList = new ArrayList<SummaryActivity>();
    var page = 0;
    do {
      var resource = format("/clubs/%d/activities", clubId);
      var request = new StringBuilder();
      request.append(resource);
      request.append(format("?per_page=%d", perPage));
      request.append(format("&page=%d", ++page));
      var result = client.get(List.class, request.toString());
      pageList = (ArrayList<Map<String, Object>>) result.getOrThrow();
      for (var item : pageList) {
        var activity = use(SummaryActivity.class).of(item);
        activityList.add(activity);
      }
    } while (pageList.size() >= perPage);
    return activityList;
  }

  @SuppressWarnings("unchecked")
  public List<SummaryAthlete> fetchClubMembers(Long clubId) {
    var perPage = 200;
    var pageList = new ArrayList<Map<String, Object>>();
    var athleteList = new ArrayList<SummaryAthlete>();
    var page = 0;
    do {
      var resource = format("/clubs/%d/members", clubId);
      var request = new StringBuilder();
      request.append(resource);
      request.append(format("?per_page=%d", perPage));
      request.append(format("&page=%d", ++page));
      var result = client.get(List.class, request.toString());
      pageList = (ArrayList<Map<String, Object>>) result.getOrThrow();
      for (var item : pageList) {
        var athlete = use(SummaryAthlete.class).of(item);
        athleteList.add(athlete);
      }
    } while (pageList.size() >= perPage);
    return athleteList;
  }

  @SuppressWarnings("unchecked")
  public List<SummaryClub> fetchClubs() {
    var perPage = 200;
    var pageList = new ArrayList<Map<String, Object>>();
    var clubList = new ArrayList<SummaryClub>();
    var page = 0;
    do {
      var resource = "/athlete/clubs";
      var request = new StringBuilder();
      request.append(resource);
      request.append(format("?per_page=%d", perPage));
      request.append(format("&page=%d", ++page));
      var result = client.get(List.class, request.toString());
      pageList = (ArrayList<Map<String, Object>>) result.getOrThrow();
      for (var item : pageList) {
        var club = use(SummaryClub.class).of(item);
        clubList.add(club);
      }
    } while (pageList.size() >= perPage);
    return clubList;
  }

  public Integer getAvailable() {
    return client.getAvailable();
  }

  public Integer getAvailableDaily() {
    return client.getAvailableDaily();
  }

  public long getExpiresAt() {
    return client.getExpiresAt();
  }

  public Long getId() {
    return id;
  }

  public String getXRateDesc() {
    return client.getXRateDesc();
  }

  public void on429(CallableVoid call) {
    client.on429(call);
  }

  @SuppressWarnings("unchecked")
  public RefreshTokenResponse refreshToken(String refreshToken) {
    var epochSecond = LocalDateTime.now().atZone(UTC).toInstant().getEpochSecond();
    var url = new StringBuilder(STRAVA_TOKEN_URL);
    url.append(format("?client_id=%s", STRAVA_CLIENT_ID));
    url.append(format("&client_secret=%s", STRAVA_CLIENT_SECRET));
    url.append(format("&refresh_token=%s", refreshToken));
    url.append(format("&grant_type=%s", "refresh_token"));
    String response = require(() -> post(url.toString()).asString().getBody());
    var map = getMoarJson().getGson().fromJson(response, HashMap.class);
    var result = use(RefreshTokenResponse.class).of(map);
    int expiresEarly = 30 * 60;
    var expiresIn = result.getExpiresIn() - expiresEarly;
    var expiresAt = epochSecond + expiresIn;
    result.setExpiresAt(expiresAt);
    client.setAccessToken(result.getAccessToken(), expiresAt);
    return result;
  }
}
