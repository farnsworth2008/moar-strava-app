package moar.strava.client;

import java.util.List;

public interface DetailedActivity {
  ActivityMap getMap();
  String getName();
  List<SegmentEffort> getSegmentEfforts();
  String getStartDate();
}
