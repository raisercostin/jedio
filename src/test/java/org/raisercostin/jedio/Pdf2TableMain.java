package org.raisercostin.jedio;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import technology.tabula.CommandLineApp;

@SpringBootApplication
public class Pdf2TableMain implements ApplicationRunner {
  @Test
  @Disabled("not actual test")
  void test() {
    Flux<FileAltered> all = Locations.path("d:/work/watched").asChangableLocation().watch();
    all.log("rec").map(x -> {
      if (x.event.kind().name() == "ENTRY_MODIFY") {
        if (x.location().getName().endsWith(".pdf")) {
          extractCsv(x);
        }
      }
      return x;
    }).blockLast();// Duration.ofSeconds(200)
  }

  private void extractCsv(FileAltered x) {
    System.out.println("extract csv from " + x);
    try {
      CommandLineParser parser = new DefaultParser();
      CommandLine line = parser.parse(CommandLineApp.buildOptions(), new String[] { "" });
      CommandLineApp app = new technology.tabula.CommandLineApp(null, line);
      x.location().asWritableFile().rename(x.location().asWritableFile());
      app.extractFileInto(new File(x.location().absoluteAndNormalized()),
          new File(x.location().absoluteAndNormalized() + ".csv"));
      // extractFileInto
      // technology.tabula.CommandLineApp.main(
      // new String[] { , "-o", x.location().absoluteAndNormalized() + ".csv"
      // });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(Pdf2TableMain.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    test();
  }
}
