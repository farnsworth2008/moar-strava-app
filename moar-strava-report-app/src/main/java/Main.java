import org.springframework.boot.SpringApplication;
import moar.report.MoarReportApp;

public class Main {
  public static void main(final String[] args) throws Exception {
    new SpringApplication(MoarReportApp.class).run(args);
  }
}
