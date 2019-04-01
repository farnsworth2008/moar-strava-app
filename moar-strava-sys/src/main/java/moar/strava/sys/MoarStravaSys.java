package moar.strava.sys;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.Float.parseFloat;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static moar.awake.InterfaceUtil.createLoadingCache;
import static moar.awake.InterfaceUtil.use;
import static moar.geo.GeoFactory.getGeoService;
import static moar.sugar.MoarDateUtil.parse8601Date;
import static moar.sugar.MoarDateUtil.toDate;
import static moar.sugar.MoarJson.getMoarJson;
import static moar.sugar.MoarStringUtil.cleanWithOnlyAscii;
import static moar.sugar.Sugar.asRuntimeException;
import static moar.sugar.Sugar.has;
import static moar.sugar.Sugar.nonNull;
import static moar.sugar.Sugar.require;
import static moar.sugar.Sugar.retryable;
import static moar.sugar.Sugar.silently;
import static moar.sugar.Sugar.swallow;
import static moar.sugar.thread.MoarThreadSugar.$;
import static org.apache.commons.lang3.StringUtils.join;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.AtomicDouble;
import com.zaxxer.hikari.HikariDataSource;
import moar.awake.WokeResultSet;
import moar.awake.WokenWithSession;
import moar.driver.Driver;
import moar.geo.GeoDescription;
import moar.geo.GeoPoint;
import moar.geo.GeoService;
import moar.strava.client.ActivityMap;
import moar.strava.client.BaseAthlete;
import moar.strava.client.DetailedActivity;
import moar.strava.client.Segment;
import moar.strava.client.StravaClient;
import moar.strava.client.SummaryActivity;
import moar.sugar.MoarDateUtil;
import moar.sugar.MoarLogger;
import moar.sugar.PropertyAccessor;
import moar.sugar.thread.MoarAsyncProvider;

public class MoarStravaSys {

  private final MoarLogger log = new MoarLogger(MoarStravaSys.class);
  private final ArrayList<Place> places = new ArrayList<Place>();
  protected final DataSource ds;
  protected final WokenWithSession<ActivityDetailRow> activityDetailRepo;
  protected final WokenWithSession<PlaceBoundaryRow> placeBoundryRepo;
  protected final LoadingCache<Long, GeoPointD5Row> pointCache;
  protected final LoadingCache<Long, PlaceRow> placeCache;
  protected final LoadingCache<Long, SegmentRow> segmentCache;
  private final GeoService geo = getGeoService();
  private final Set<String> types = new HashSet<>();
  private final MoarAsyncProvider runAsync = $(100);
  private final MoarAsyncProvider scanAsync = $(2);
  private final MoarAsyncProvider scoreAsync = $(2);

  private final ConcurrentHashMap<Long, AtomicReference<String>> scoreJobStatusMap = new ConcurrentHashMap<>();
  private final AtomicDouble detailRate = new AtomicDouble();
  private final AtomicDouble activityRate = new AtomicDouble();
  private final AtomicDouble geoRate = new AtomicDouble();
  private final AtomicDouble gpsRate = new AtomicDouble();
  private final AtomicDouble movRate = new AtomicDouble();
  private final AtomicLong stravaAvailable = new AtomicLong();
  private final AtomicLong stravaAvailableDaily = new AtomicLong();
  private final AtomicLong retryCount = new AtomicLong();

  public MoarStravaSys() {
    this("");
  }

  public MoarStravaSys(String profile) {
    int maxSize = 100000;
    ds = createDataSource(profile);
    geo.setDescribeRateLimit(10);
    activityDetailRepo = use(ActivityDetailRow.class).of(ds);
    placeBoundryRepo = use(PlaceBoundaryRow.class).of(ds);
    pointCache = createLoadingCache(ds, maxSize, GeoPointD5Row.class);
    placeCache = createLoadingCache(ds, maxSize, PlaceRow.class);
    segmentCache = createLoadingCache(ds, maxSize, SegmentRow.class);
    loadPlaces();
  }

  private boolean addResult(Long athleteId, ArrayList<Object> years, int year) {
    WokenWithSession<PlaceSummaryYearResultRow> repo = use(PlaceSummaryYearResultRow.class).of(ds);
    PlaceSummaryYearResultRow resultRow = repo.where(row -> {
      row.setAthleteId(athleteId);
      row.setYear(year);
    }).find();
    if (resultRow == null) {
      return false;
    }
    years.add(new Object[] { year, resultRow.getDetail(), getMoarJson().fromJson(resultRow.getResult()) });
    return true;
  }

  private void appendCounter(StringBuilder msg, String label, String value) {
    msg.append(format("<br>%s %s", label, value));
  }

  protected DataSource createDataSource(String profile) {
    return silently(() -> {
      PropertyAccessor props = new PropertyAccessor();
      var mysql = props.getString("MOAR_STRAVA_MYSQL" + profile);
      var password = props.getString("MOAR_STRAVA_MYSQL_PASSWORD" + profile);
      var username = props.getString("MOAR_STRAVA_MYSQL_USERNAME" + profile);
      var jdbcUrl = format("moar:main:jdbc:mysql://%s:3306/moar_strava_sys", mysql);
      jdbcUrl += "?allowPublicKeyRetrieval=true";
      jdbcUrl += "&useSSL=false";
      jdbcUrl += "&verifyServerCertificate=false&rewriteBatchedStatements=true&serverTimezone=UTC";
      return createDataSource(jdbcUrl, username, password);
    }).get();
  }

  private DataSource createDataSource(String jdbcUrl, String username, String password) {
    HikariDataSource ds = new HikariDataSource();
    ds.setDriverClassName(Driver.class.getName());
    ds.setJdbcUrl(jdbcUrl);
    ds.setUsername(username);
    ds.setPassword(password);
    return ds;
  }

  public PlaceBoundaryRow definePlaceBoundaryRow() {
    return use(PlaceBoundaryRow.class).of(ds).define();
  }

  public PlaceRow definePlaceRow() {
    return use(PlaceRow.class).of(ds).define();
  }

  public void deletePlaceBoundaryRows(Long placeId) {
    retryCount.addAndGet(retryable(100, () -> {
      use(ds).executeSql("delete from place_boundary where place_id = ?", placeId);
    }));
  }

  private void deleteScoreJobRow(Long userId) {
    use(ScoreJobRow.class).of(ds).id(userId).delete();
  }

  public DataSource getDataSource() {
    return ds;
  }

  private String getDisplayName(MoveSummaryViewRow item) {
    var displayName = "";
    if (has(item.getPlaceName())) {
      displayName = item.getPlaceName() + " " + item.getState();
    } else {
      var displayBuilder = new StringBuilder();
      Boolean offRoad = has(item.getOffRoad());
      displayBuilder.append(offRoad ? "Other" : "Road");
      displayBuilder.append(" ");
      displayBuilder.append(nonNull(item.getState(), item.getCountry(), "Unknown"));
      displayName = displayBuilder.toString();
    }
    return displayName;
  }

  public GeoService getGeo() {
    return geo;
  }

  private Long getGeoPointD3Id(GeoPoint p) {
    var lat = parseFloat(format("%.3f", p.getLat()));
    var lon = parseFloat(format("%.3f", p.getLon()));
    return getGeoPointD5Id(lat, lon);
  }

  private Long getGeoPointD4Id(GeoPoint p) {
    var lat = parseFloat(format("%.4f", p.getLat()));
    var lon = parseFloat(format("%.4f", p.getLon()));
    return getGeoPointD5Id(lat, lon);
  }

  private long getGeoPointD5Id(float latF, float lonF) {
    var lat = (long) (abs(latF) * 100000L);
    if (latF > 0) {
      lat += 100000000L;
    }
    var lon = (long) (abs(lonF) * 100000L);
    if (lonF > 0) {
      lon += 100000000L;
    }
    return lat * 1000000000L + lon;
  }

  public Long getGeoPointD5Id(GeoPoint p) {
    return getGeoPointD5Id(p.getLat(), p.getLon());
  }

  /**
   * Get GeoPoint from pointId
   * <p>
   * Example:<br>
   *
   * <pre>
   * ID: 103585593007878534
   * Step 1: 103585593    007878534
   * Step 2: +035.85593   -078.78534
   * </pre>
   *
   * @param pointId
   * @return
   */
  private GeoPoint getGeoPointFromD5Id(Long pointId) {
    var lat = pointId / 1000000000L;
    var lon = pointId - lat * 1000000000L;
    if (lat > 100000000) {
      lat -= 100000000;
    } else {
      lat *= -1;
    }
    if (lon > 100000000) {
      lon -= 100000000;
    } else {
      lon *= -1;
    }
    var latF = (float) lat / 100000;
    var lonF = (float) lon / 100000;
    return getGeo().point(latF, lonF, null);
  }

  private GeoPoint getMovePoint(MoveRow moveRow) {
    return getGeo().point(moveRow.getLat(), moveRow.getLon(), null);
  }

  public List<MoveRow> getMovesWithoutPoints(long batchSize) {
    var sql = new StringBuilder();
    sql.append("select [*] ");
    sql.append("from move MoveRow ");
    sql.append("left join geo_point_d5 g ");
    sql.append("on g.id = MoveRow.point_id ");
    sql.append("where g.id is null limit ?");
    return use(MoveRow.class).of(ds).list(sql.toString(), batchSize);
  }

  public OAuthTokenRow getOAuthTokenRow(Long userId) {
    var repo = use(OAuthTokenRow.class).of(ds);
    var row = repo.id(userId).find();
    if (row == null) {
      row = repo.define();
      row.setId(userId);
    }
    return row;
  }

  public Long getPlaceId(String placeName, String country, String state, String county) {
    var finalCounty = county;
    var finalState = state;
    var finalPlaceName = placeName;
    var finalCountry = country;
    Long placeId = use(PlaceRow.class).of(ds).upsert(placeRow -> {
      placeRow.setPlaceName(finalPlaceName);
      placeRow.setCounty(finalCounty);
      placeRow.setState(finalState);
      placeRow.setCountry(finalCountry);
      placeRow.setBoundary(false);
    }).getId();
    placeCache.invalidate(placeId);
    return placeId;
  }

  public PlaceRow getPlaceRow(Long placeId) {
    var placeRow = swallow(() -> placeCache.get(placeId));
    if (placeRow == null) {
      return null;
    }
    if (placeRow.getId() == null) {
      return null;
    }
    return placeRow;
  }

  public List<MoveSummaryViewRow> getPlaceSummaryList(Long athleteId) {
    return use(MoveSummaryViewRow.class).of(ds).where(row -> {
      row.setAthleteId(athleteId);
    }).list();
  }

  public List<MoveSummaryYearViewRow> getPlaceSummaryList(Long athleteId, int year) {
    return use(MoveSummaryYearViewRow.class).of(ds).where(row -> {
      row.setAthleteId(athleteId);
      row.setYear(year);
    }).list();
  }

  public WokeResultSet<GeoPointD5Row> getPoints() {
    var result = use(GeoPointD5Row.class).of(ds).iterator("where true");
    return result;
  }

  public String getResults(Long userId) {
    var repo = use(AthleteRow.class).of(ds);
    var athleteRow = repo.where(row -> row.setId(userId)).find();
    if (athleteRow == null) {
      return "{}";
    }
    var athleteId = athleteRow.getId();

    var map = new HashMap<String, Object>();
    var years = new ArrayList<Object>();
    var year = LocalDate.now().getYear();
    while (addResult(athleteId, years, year)) {
      year--;
    }
    year = -1;
    addResult(athleteId, years, year);
    var job = scoreJobStatusMap.get(userId);
    var statusMsg = job == null ? "" : job.get();
    StringBuilder msg = new StringBuilder();
    msg.append(statusMsg);
    msg.append("<p>");
    appendCounter(msg, "ACT", format("%.1f/min", activityRate.get()));
    appendCounter(msg, "DES", format("%d", geo.getDescribeCount()));
    appendCounter(msg, "DET", format("%.1f/min", detailRate.get()));
    appendCounter(msg, "GEO", format("%.1f/sec", geoRate.get()));
    appendCounter(msg, "GPS", format("%.1f/sec", gpsRate.get()));
    appendCounter(msg, "JOB", format("%d", scoreJobStatusMap.size()));
    appendCounter(msg, "MOV", format("%.1f/sec", movRate.get()));
    appendCounter(msg, "RET", format("%d", retryCount.get()));
    appendCounter(msg, "SAD", format("%d", stravaAvailableDaily.get()));
    appendCounter(msg, "SAV", format("%d", stravaAvailable.get()));
    msg.append("</p>");
    map.put("status", msg.toString());
    map.put("years", years);
    map.put("running", job != null);
    getMoarJson().getGson().toJson(map);
    return getMoarJson().getGson().toJson(map);
  }

  private String getTypeDisplay(String type) {
    var convert = UPPER_CAMEL.to(LOWER_UNDERSCORE, type);
    var split = convert.split("_");
    for (var i = 0; i < split.length; i++) {
      split[i] = StringUtils.capitalize(split[i]);
    }
    var typeDisplay = join(split, " ");
    return typeDisplay;
  }

  private ScoreJobRow insertScoreJobRow(Long userId) {
    deleteScoreJobRow(userId);
    return use(ScoreJobRow.class).of(ds).id(userId).insert(row -> row.setYear(-1));
  }

  public void invalidatePlaceRow(Long placeId) {
    placeCache.invalidate(placeId);
  }

  private void loadPlaces() {
    var placeRows = use(PlaceRow.class).of(ds).where(row -> row.setBoundary(true)).list();
    places.clear();
    for (var placeRow : placeRows) {
      var boundery = new ArrayList<GeoPoint>();
      var placeId = placeRow.getId();
      var placeBoundryRows = placeBoundryRepo.where(row -> row.setPlaceId(placeId)).list("seq");
      for (var placeBoundryRow : placeBoundryRows) {
        boundery.add(getGeoPointFromD5Id(placeBoundryRow.getPointId()));
      }
      var placeName = placeRow.getPlaceName();
      var country = placeRow.getCountry();
      var state = placeRow.getState();
      var county = placeRow.getCounty();
      var place = new Place(placeId, placeName, country, state, county, boundery);
      places.add(place);
    }
  }

  private void recordSegments(Long athleteId, DetailedActivity detail) {
    var segmentEfforts = detail.getSegmentEfforts();
    var segmentMap = new HashMap<Long, Segment>();
    for (var effort : segmentEfforts) {
      var segment = effort.getSegment();
      segmentMap.put(segment.getId(), segment);
    }
    var segments = segmentMap.values();
    for (var seg : segments) {
      SegmentRow segRow = require(() -> segmentCache.get(seg.getId()));
      if (segRow.getId() == null) {
        segRow.setId(seg.getId());
        segRow.setName(seg.getName());
        segRow.setActivityType(seg.getActivityType());
        segRow.setAverageGrade(seg.getAverageGrade());
        segRow.setClimbCategory(seg.getClimbCategory());
        segRow.setDistance(seg.getDistance());
        segRow.setElevationHigh(seg.getElevationHigh());
        segRow.setElevationLow(seg.getElevationLow());
        segRow.setStartLatitude(seg.getStartLatitude());
        segRow.setStartLongitude(seg.getStartLongitude());
        segRow.setMaximumGrade(seg.getMaximumGrade());
        use(SegmentRow.class).of(ds).upsert(segRow);
        segmentCache.invalidate(segRow.getId());
      }
      use(AthleteSegmentRow.class).of(ds).upsert(row -> {
        row.setAthleteId(athleteId);
        row.setSegmentId(seg.getId());
      });
    }
  }

  private boolean refreshAccessToken(StravaClient strava) {
    stravaAvailable.lazySet(strava.getAvailable());
    stravaAvailableDaily.lazySet(strava.getAvailableDaily());
    var epochSecond = MoarDateUtil.getEpochSecond();
    var tokenRepo = use(OAuthTokenRow.class).of(ds);
    var tokenRow = tokenRepo.id(strava.getId()).find();
    if (strava.getExpiresAt() > epochSecond) {
      return false;
    }
    var refreshToken = tokenRow.getRefreshToken();
    var refresh = strava.refreshToken(refreshToken);
    var newAccessToken = refresh.getAccessToken();
    if (newAccessToken.equals(tokenRow.getAccessToken())) {
      return false;
    }
    // we got a new access token, it might work now.
    tokenRow.setAccessToken(refresh.getAccessToken());
    tokenRow.setRefreshToken(refresh.getRefreshToken());
    tokenRow.setExpiresAt(refresh.getExpiresAt());
    tokenRepo.update(tokenRow);
    return true;
  }

  private void runScoreJob(AtomicReference<String> statusMsg, ScoreJobRow scoreJobRow) {
    statusMsg.set("<p>Creating Strava Client</p>");
    var userId = scoreJobRow.getId();
    if (userId == null) {
      return;
    }
    var tokenRepo = use(OAuthTokenRow.class).of(ds);
    var tokenRow = tokenRepo.id(userId).find();
    var strava = new StravaClient(Long.valueOf(userId), tokenRow.getAccessToken(), tokenRow.getExpiresAt());
    while (true) {
      try {
        scoreStrava(statusMsg, strava, scoreJobRow);
        break;
      } catch (Throwable t) {
        log.warn("runScoreJob problem", userId, t);
        if (!refreshAccessToken(strava)) {
          // we did not get a new access token don't bother retry
          throw asRuntimeException(t);
        }
        continue;
      }
    }
  }

  private void scoreActivity(AtomicReference<String> statusMsg, StravaClient strava, ActivitySummaryRow activity,
      int year, boolean detail) {
    var activityId = activity.getId();
    var activityDetailRow = activityDetailRepo.id(activityId).find();
    if (activityDetailRow != null) {
      return;
    }

    Long athleteId = activity.getAthleteId();
    types.add(activity.getType());
    var summaryPolyline = activity.getSummaryPolyline();
    if (detail) {
      refreshAccessToken(strava);
      var activityDetail = strava.fetchActivity(activityId);
      recordSegments(athleteId, activityDetail);
      var detailPolyline = swallow(() -> {
        ActivityMap map = activityDetail.getMap();
        return map == null ? null : map.getPolyline();
      });
      if (detailPolyline != null) {
        var detailPoints = getGeo().decode(detailPolyline);
        var scoreDistance = scoreMoves(athleteId, activity, detailPoints, year, detail);
        activityDetailRepo.upsert(row -> {
          row.setId(activityId);
          row.setPolyline(detailPolyline);
          row.setScoredDistance(scoreDistance);
        });
      } else {
        activityDetailRepo.upsert(row -> {
          row.setId(activityId);
          row.setPolyline("");
          row.setScoredDistance(0d);
        });
      }
    } else {
      if (summaryPolyline != null) {
        var summaryPoints = getGeo().decode(summaryPolyline);
        scoreMoves(athleteId, activity, summaryPoints, year, detail);
      }
    }
  }

  private void scoreAthlete(AtomicReference<String> statusMsg, StravaClient strava, BaseAthlete athlete,
      ScoreJobRow scoreJobRow) throws InterruptedException, ExecutionException {
    var createdAt = parse8601Date(athlete.getCreatedAt()).get();
    LocalDateTime now = LocalDateTime.now();
    var currentYear = now.getYear();
    var createdYear = createdAt.getYear();
    for (int y = currentYear; y >= createdYear; y--) {
      int year = y;
      scoreYear(scanAsync, statusMsg, strava, athlete, year, scoreJobRow, false);
    }
    for (int y = currentYear; y >= createdYear; y--) {
      int year = y;
      scoreYear(scoreAsync, statusMsg, strava, athlete, year, scoreJobRow, true);
    }
  }

  private double scoreMoves(Long athleteId, ActivitySummaryRow activity, List<GeoPoint> points, int year,
      boolean detail) {
    if (points.isEmpty()) {
      return 0d;
    }
    var scoredDistance = new AtomicDouble();
    var p = points.get(0);
    retryCount.addAndGet(retryable(100, () -> {
      use(ds).executeSql("delete from move where activity_id=?", activity.getId());
    }));
    var moveRepo = use(MoveRow.class).of(ds);
    var loopSize = points.size();
    var geoElap = 0L;
    var gpsElap = 0L;
    var movElap = new AtomicLong();
    for (int i = 0; i < loopSize; i++) {
      GeoPointD5Row d5Row = null;
      var geoStart = nanoTime();
      var point = points.get(i);
      var meters = geo.meters(p, point);
      p = point;
      scoredDistance.addAndGet(meters);
      var seq = i;
      var movement = (float) meters;
      var pointRowId = getGeoPointD5Id(p);

      for (var place : places) {
        if (place.contains(point)) {
          d5Row = upsertPointRow(point, pointRowId, place.getPlaceId());
          break;
        }
      }

      geoElap += nanoTime() - geoStart;

      if (d5Row == null) {
        var gpsStart = nanoTime();
        d5Row = upsertPointRow(point, pointRowId, null);
        gpsElap += nanoTime() - gpsStart;
      }

      var pointRow = d5Row;
      var moveStart = nanoTime();
      retryCount.addAndGet(retryable(3, () -> {
        moveRepo.upsert(row -> {
          row.setActivityId(activity.getId());
          row.setAthleteId(athleteId);
          row.setYear(year);
          row.setSeq(seq);
          row.setPointId(pointRowId);
          row.setOffRoad(pointRow.getOffRoad());
          row.setPlaceId(pointRow.getPlaceId());
          row.setMeters(movement);
          row.setLat(point.getLat());
          row.setLon(point.getLon());
        });
      }));
      movElap.addAndGet(nanoTime() - moveStart);
    }
    if (detail && loopSize > 100) {
      var geoSeconds = SECONDS.convert(geoElap, NANOSECONDS);
      var gpsSeconds = SECONDS.convert(gpsElap, NANOSECONDS);
      var movSeconds = SECONDS.convert(movElap.get(), NANOSECONDS);
      if (geoSeconds != 0) {
        geoRate.lazySet((double) loopSize / geoSeconds);
      }
      if (gpsSeconds != 0) {
        gpsRate.lazySet((double) loopSize / gpsSeconds);
      }
      if (movSeconds != 0) {
        movRate.lazySet((double) loopSize / movSeconds);
      }
    }
    return scoredDistance.get();
  }

  private void scoreStrava(AtomicReference<String> statusMsg, StravaClient strava, ScoreJobRow scoreJobRow)
      throws InterruptedException, ExecutionException {
    statusMsg.set("<p>Connecting</p>");
    refreshAccessToken(strava);
    BaseAthlete athlete = strava.fetchAthlete();
    statusMsg.set("<p>Preparing (1)</p>");
    use(AthleteRow.class).of(ds).upsert(row -> {
      row.setId(athlete.getId());
      row.setCity(athlete.getCity());
      row.setCountry(athlete.getCountry());
      row.setCreatedAt(toDate(parse8601Date(athlete.getCreatedAt()).getOrThrow()));
      row.setFirstname(athlete.getFirstname());
      row.setLastname(athlete.getLastname());
      row.setProfile(athlete.getProfile());
      row.setProfileMedium(athlete.getProfileMedium());
      row.setSex(athlete.getSex());
      row.setState(athlete.getState());
    });
    statusMsg.set("<p>Preparing (2)</p>");
    scoreAthlete(statusMsg, strava, athlete, scoreJobRow);
    storeAllTimePlaceSummaryResultRow(statusMsg, athlete, athlete.getId());
  }

  private void scoreYear(MoarAsyncProvider async, AtomicReference<String> statusMsg, StravaClient strava,
      BaseAthlete athlete, int year, ScoreJobRow scoreJobRow, boolean detail)
      throws InterruptedException, ExecutionException {
    var scoreType = detail ? "Score" : "Scan";
    statusMsg.set(format("<p>Checking %s %d</p>", scoreType, year));
    var resultRepo = use(PlaceSummaryYearResultRow.class).of(ds);
    var resultRow = resultRepo.where(row -> {
      row.setAthleteId(athlete.getId());
      row.setYear(year);
    }).find();
    if (resultRow != null) {
      var detailNotNeeded = detail == false;
      var hasDetail = resultRow.getDetail();
      if (hasDetail || detailNotNeeded) {
        return;
      }
    }
    statusMsg.set(format("<p>Queued %s %d</p>", scoreType, year));
    $(async, $(), () -> {
      statusMsg.set(format("<p>Starting (1) %s %d</p>", scoreType, year));
      scoreJobRow.setYear(year);
      scoreJobRow.setDetail(detail);
      use(ScoreJobRow.class).of(ds).update(scoreJobRow);
      statusMsg.set(format("<p>Starting (2) %s %d</p>", scoreType, year));
      var start = currentTimeMillis();
      var yearDate = LocalDate.of(year, 1, 1);
      var after = yearDate.atStartOfDay().toEpochSecond(UTC);
      var before = yearDate.plusYears(1).atStartOfDay().toEpochSecond(UTC);
      Consumer<ActivityListRow> activityListCriteria = row -> {
        row.setAthleteId(athlete.getId());
        row.setBefore(before);
        row.setAfter(after);
        row.setYear(year);
      };

      var activityListRepo = use(ActivityListRow.class).of(ds);
      var entryRepo = use(ActivityListEntryRow.class).of(ds);
      var activityList = activityListRepo.where(activityListCriteria).find();
      statusMsg.set(format("<p>Starting (3) %s %d</p>", scoreType, year));
      if (activityList == null) {
        statusMsg.set(format("<p>Preparing %s %d</p>", scoreType, year));
        activityList = activityListRepo.upsert(activityListCriteria);
        var activityListId = activityList.getId();
        statusMsg.set(format("<p>Fetching %s %d</p>", scoreType, year));
        refreshAccessToken(strava);
        var activities = strava.fetchActivities(before, after);
        var activitiesSize = activities.size();
        for (int i = 0; i < activitiesSize; i++) {
          var activity = activities.get(i);
          var activityRow = upsertActivitySummary(activity);
          entryRepo.upsert(row -> {
            row.setActivityId(activityRow.getId());
            row.setActivityListId(activityListId);
          });
        }
      }

      statusMsg.set(format("<p>%s %d</p>", scoreType, year));
      var activityListId = activityList.getId();
      var activityEntries = entryRepo.where(row -> row.setActivityListId(activityListId)).list();
      var activitySummaryRepo = use(ActivitySummaryRow.class).of(ds);
      var loopStart = currentTimeMillis();
      int activitiesSize = activityEntries.size();
      for (int i = 0; i < activitiesSize; i++) {
        var activityEntry = activityEntries.get(i);
        var activityId = activityEntry.getActivityId();
        var row = activitySummaryRepo.id(activityId).find();

        scoreActivity(statusMsg, strava, row, year, detail);

        var loopCount = i + 1;
        var loopElap = currentTimeMillis() - loopStart;
        var loopMinute = (double) loopElap / 1000 / 60;
        var rate = loopElap == 0 ? 0 : loopCount / loopMinute;
        if (loopElap != 0) {
          if (detail) {
            detailRate.lazySet(rate);
          } else {
            activityRate.lazySet(rate);
          }
        }
        StringBuilder msg = new StringBuilder();
        msg.append("<p>");
        msg.append(scoreType);
        msg.append(" ");
        msg.append(format("%d", year));
        msg.append("</p><p>");
        msg.append("Activities");
        msg.append(" ");
        msg.append("<br>");
        msg.append(format("(%d of %d)", loopCount, activitiesSize));
        msg.append("<br>");
        msg.append(format("%.1f/min", rate));
        msg.append("</p>");
        statusMsg.set(msg.toString());
      }
      storePlaceSummaryResultRow(statusMsg, athlete.getId(), year, detail, start);
    }).get();
  }

  public void startScoreAthlete(Long userId) {
    synchronized (scoreJobStatusMap) {
      if (scoreJobStatusMap.containsKey(userId)) {
        return;
      }
      scoreJobStatusMap.put(userId, new AtomicReference<String>());
      var statusMsg = scoreJobStatusMap.get(userId);
      statusMsg.set("<p>Starting</p>");
      var futures = $();
      $(runAsync, futures, () -> {
        try {
          var scoreJobRow = insertScoreJobRow(userId);
          try {
            runScoreJob(statusMsg, scoreJobRow);
          } finally {
            deleteScoreJobRow(userId);
          }
        } catch (Throwable t) {
          log.error(t);
          throw asRuntimeException(t);
        } finally {
          scoreJobStatusMap.remove(userId);
        }
      });
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void storeAllTimePlaceSummaryResultRow(AtomicReference<String> statusMsg, BaseAthlete athlete, Long athleteId)
      throws InterruptedException, ExecutionException {
    statusMsg.set("<p>Checking<br>(all-time)</p>");
    var resultRepo = use(PlaceSummaryYearResultRow.class).of(ds);
    var resultRow = resultRepo.where(row -> {
      row.setAthleteId(athlete.getId());
      row.setYear(-1);
    }).find();
    if (resultRow != null) {
      if (resultRow.getDetail()) {
        return;
      }
    }
    statusMsg.set("<p>Queuing to Score<br>(all-time)</p>");
    $(scoreAsync, $(), () -> {
      var start = currentTimeMillis();
      statusMsg.set("<p>Scoring<br>(all-time)</p>");
      var list = getPlaceSummaryList(athlete.getId());
      statusMsg.set("<p>Storing<br>(all-time)</p>");
      var resultList = new ArrayList<Map<String, Object>>();
      for (var item : list) {
        var map = new HashMap(use(item).toMap());
        map.put("displayName", getDisplayName(item));
        map.remove("athleteId");
        var meters = (BigDecimal) map.remove("meters");
        map.put("miles", (int) (meters.doubleValue() * 0.000621371));
        resultList.add(map);
      }
      var result = getMoarJson().getGson().toJson(resultList);
      var elap = currentTimeMillis() - start;
      use(PlaceSummaryYearResultRow.class).of(ds).upsert(row -> {
        row.setAthleteId(athleteId);
        row.setYear(-1);
        row.setDetail(true);
        row.setResult(result);
        row.setMilliseconds(elap);
      });
    }).get();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void storePlaceSummaryResultRow(AtomicReference<String> statusMsg, Long athleteId, int year, boolean detail,
      long start) {
    var scoreType = detail ? "Score" : "Scan";
    statusMsg.set(format("<p>Summerize %s %d</p>", scoreType, year));
    var list = getPlaceSummaryList(athleteId, year);
    statusMsg.set(format("<p>Store %s %d</p>", scoreType, year));
    var resultList = new ArrayList<Map<String, Object>>();
    for (var item : list) {
      var map = new HashMap(use(item).toMap());
      map.put("displayName", getDisplayName(item));
      map.remove("athleteId");
      map.remove("year");
      var meters = (BigDecimal) map.remove("meters");
      map.put("miles", (int) (meters.doubleValue() * 0.000621371));
      resultList.add(map);
    }
    var result = getMoarJson().getGson().toJson(resultList);
    var repo = use(PlaceSummaryYearResultRow.class).of(ds);
    StringBuilder sql = new StringBuilder();
    sql.append("delete ");
    sql.append("from place_summary_year_result ");
    sql.append("where ");
    sql.append("athlete_id=? ");
    sql.append("and year=?");
    retryCount.addAndGet(retryable(100, () -> {
      use(ds).executeSql(sql.toString(), athleteId, year);
    }));
    var elap = currentTimeMillis() - start;
    repo.insert(row -> {
      row.setAthleteId(athleteId);
      row.setYear(year);
      row.setResult(result);
      row.setDetail(detail);
      row.setMilliseconds(elap);
    });
  }

  public void upsert(OAuthTokenRow oAuthToken) {
    WokenWithSession<OAuthTokenRow> repo = use(OAuthTokenRow.class).of(ds);
    repo.delete(oAuthToken);
    repo.insert(oAuthToken);
  }

  public void upsert(PlaceBoundaryRow row) {
    use(PlaceBoundaryRow.class).of(ds).upsert(row);
  }

  public void upsert(PlaceRow placeRow) {
    use(PlaceRow.class).of(ds).upsert(placeRow);
  }

  private ActivitySummaryRow upsertActivitySummary(SummaryActivity activity) {
    var activityName = cleanWithOnlyAscii(activity.getName());
    var athleteId = activity.getAthlete().getId();
    var map = activity.getMap();
    var summaryPolyline = map == null ? null : map.getSummaryPolyline();
    var activityRow = use(ActivitySummaryRow.class).of(ds).upsert(row -> {
      row.setId(activity.getId());
      row.setAthleteId(athleteId);
      row.setDistance(activity.getDistance());
      row.setElapsedTime(activity.getElapsedTime());
      row.setMovingTime(activity.getMovingTime());
      row.setSummaryPolyline(summaryPolyline);
      row.setType(getTypeDisplay(activity.getType()));
      row.setName(activityName);
      row.setStartDate(toDate(parse8601Date(activity.getStartDate()).getOrThrow()));
    });
    return activityRow;
  }

  public GeoPointD5Row upsertPointRow(GeoPoint point, Long pointId, Long placeId) {
    var pointRow = swallow(() -> pointCache.get(pointId));
    if (pointRow != null && pointRow.getId() != null) {
      if (placeId == null) {
        return pointRow;
      } else {
        if (placeId.equals(pointRow.getPlaceId())) {
          return pointRow;
        }
      }
    }
    try {
      var pointRepo = use(GeoPointD5Row.class).of(ds);
      for (var place : places) {
        if (place.contains(point)) {
          return upsertPointRow(pointId, 0L, true, place.getPlaceId());
        }
      }
      var placeName = "";
      var d3PointId = getGeoPointD3Id(point);
      var d3PointRow = swallow(() -> pointCache.get(d3PointId));
      var d4PointId = getGeoPointD4Id(point);
      var d4PointRow = swallow(() -> pointCache.get(d4PointId));
      if (d4PointRow.getId() == null) {
        if (d3PointRow.getId() == null) {
          getGeo().describe(point);
          GeoDescription desc = point.getDescription();
          var county = nonNull(desc.getCounty(), "").replaceAll(" County$", "");
          var state = desc.getState();
          var country = desc.getCountry();
          var offRoad = desc.getRoad() == null;
          var pointPlaceId = getPlaceId(placeName, country, state, county);
          if (!d3PointId.equals(pointId)) {
            upsertPointRow(d3PointId, pointId, offRoad, pointPlaceId);
            pointCache.invalidate(d3PointId);
          }
          if (!d4PointId.equals(pointId)) {
            upsertPointRow(d4PointId, pointId, offRoad, pointPlaceId);
            pointCache.invalidate(d4PointId);
          }
          var result = upsertPointRow(pointId, null, offRoad, pointPlaceId);
          pointCache.invalidate(pointId);
          return result;
        }
        Long inferred = nonNull(d3PointRow.getInferred(), d3PointRow.getId());
        d4PointRow = pointRepo.upsert(row -> {
          row.setId(d4PointId);
          row.setOffRoad(d3PointRow.getOffRoad());
          row.setPlaceId(d3PointRow.getPlaceId());
          row.setInferred(inferred);
        });
      }

      var placeRow = getPlaceRow(d4PointRow.getPlaceId());
      var county = placeRow.getCounty();
      var state = placeRow.getState();
      var country = placeRow.getCountry();
      var offRoad = d4PointRow.getOffRoad();
      var finalPlaceId = getPlaceId(placeName, country, state, county);
      Long inferred = nonNull(d4PointRow.getInferred(), d4PointRow.getId());
      return pointRepo.upsert(row -> {
        row.setId(pointId);
        row.setOffRoad(offRoad);
        row.setPlaceId(finalPlaceId);
        row.setInferred(inferred);
      });
    } finally {
      pointCache.invalidate(pointId);
    }
  }

  private GeoPointD5Row upsertPointRow(Long pointId, Long inferred, Boolean offRoad, Long finalPlaceId) {
    var pointRepo = use(GeoPointD5Row.class).of(ds);
    return pointRepo.upsert(row -> {
      row.setId(pointId);
      row.setOffRoad(offRoad);
      row.setPlaceId(finalPlaceId);
      row.setInferred(inferred);
    });
  }

  public void upsertPoints(List<MoveRow> moveList) {
    var listSize = moveList.size();
    format("%d Points", listSize);
    require(() -> {
      var targetBatchSize = 100;
      var batch = new AtomicInteger();
      getGeo().resetStats();
      for (int i = 0; i < moveList.size(); i++) {
        var isLast = i == moveList.size() - 1;
        var moveRow = moveList.get(i);
        GeoPoint movePoint = getMovePoint(moveRow);
        upsertPointRow(movePoint, moveRow.getPointId(), null);
        if (isLast || batch.incrementAndGet() >= targetBatchSize) {
          batch.set(0);
        }
      }
    });
  }

}