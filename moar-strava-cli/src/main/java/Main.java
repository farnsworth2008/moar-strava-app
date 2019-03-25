import static com.mashape.unirest.http.Unirest.shutdown;
import static moar.sugar.Sugar.nonNull;
import moar.strava.cli.StravaCli;

public class Main {

  public static void main(String[] args) throws Exception {
    try {
      var c = 1;
      var command = args.length <= c ? null : args[c++];
      var profile = args.length <= c ? null : args[c++];
      command = nonNull(command, "save-points");
      profile = nonNull(profile, "");
      profile = profile.isEmpty() ? "" : "_" + profile;
      new StravaCli(command, profile).run();
    } finally {
      shutdown();
    }
  }

}
