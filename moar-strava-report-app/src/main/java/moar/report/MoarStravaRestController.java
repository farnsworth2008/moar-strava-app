package moar.report;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.format;
import static moar.strava.client.StravaClient.STRAVA_AUTH_URL;
import static moar.strava.client.StravaClient.STRAVA_TOKEN_URL;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mashape.unirest.http.exceptions.UnirestException;
import moar.strava.client.StravaClient;
import moar.strava.sys.MoarStravaSys;

@RequestMapping("/")
@RestController
public class MoarStravaRestController {

  private final MoarStravaSys sys = new MoarStravaSys();

  @GetMapping("/auth")
  void authorize(HttpServletResponse response) throws IOException {
    StringBuilder url = new StringBuilder();
    url.append(STRAVA_AUTH_URL);
    url.append("?response_type=code");
    url.append("&approval_prompt=auto");
    url.append("&scope=activity:read");
    url.append(format("&client_id=%s", StravaClient.STRAVA_CLIENT_ID));
    url.append(format("&redirect_uri=%s", "https://do.moar.club/code"));
    response.sendRedirect(url.toString());
  }

  @GetMapping("/code")
  void code(HttpServletResponse response, @RequestParam String code) throws IOException, UnirestException {
    var url = new StringBuilder(STRAVA_TOKEN_URL);
    url.append(format("?client_id=%s", StravaClient.STRAVA_CLIENT_ID));
    url.append(format("&client_secret=%s", StravaClient.STRAVA_CLIENT_SECRET));
    url.append(format("&code=%s", code));
    url.append(format("&grant_type=%s", "authorization_code"));
    var responsePost = post(url.toString()).asJson();
    if (responsePost.getStatus() != 200) {
      var status = format("status=%s", responsePost.getStatus());
      response.sendRedirect(format("#status=%s", status));
    } else {
      var responseJson = responsePost.getBody();
      var object = responseJson.getObject();
      JSONObject athlete = object.getJSONObject("athlete");
      var userId = athlete.getLong("id");
      var oAuthToken = sys.getOAuthTokenRow(userId);
      oAuthToken.setAccessToken(object.getString("access_token"));
      oAuthToken.setRefreshToken(object.getString("refresh_token"));
      oAuthToken.setExpiresAt(0L);
      sys.upsert(oAuthToken);
      response.sendRedirect(format("/score/%d", userId));
    }
  }

  @GetMapping("/score/{user}")
  void score(HttpServletResponse response, @PathVariable("user") Long userId) throws IOException, UnirestException {
    sys.startScoreAthlete(userId);
    response.sendRedirect(format("/#user=%d", userId));
  }

  @GetMapping("/results/{user}")
  String state(@PathVariable("user") Long userId) throws IOException {
    return sys.getResults(userId);
  }
}
