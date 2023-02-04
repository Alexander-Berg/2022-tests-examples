package ru.auto.tests.coverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

public class SwaggerCoverageExec {

    private Config config;

    private SwaggerCoverageExec(Config config) {
        this.config = config;
    }

    public static SwaggerCoverageExec swaggerCoverage(Config config) {
        return new SwaggerCoverageExec(config);
    }

    private final static Logger log = Logger.getLogger(SwaggerCoverageExec.class);

    void execute() {
        Set<String> coverage = CoverageUtils.readRequests(config.getReqPath().toFile());

        Set<String> formattedCoverage = new TreeSet<>();
        Set<String> swaggerDoc = new TreeSet<>();
        Set<String> notFound = new TreeSet<>();

        Swagger swaggerExpected = new SwaggerParser().read(config.getSpecPath().toString());
        swaggerExpected.getPaths().forEach((s, path) -> {
            path.getOperationMap().forEach((httpMethod, operation) -> {
                String req = formatReq(httpMethod.name(), s);
                log.info(operation.getTags() + " → " + req);
                swaggerDoc.add(req);
                if (!coverage.contains(req)) {
                    notFound.add(operation.getTags() + " → " + req);
                } else {
                    formattedCoverage.add(operation.getTags() + " → " + req);
                }

            });
        });

        log.info("<{All covered: ");
        formattedCoverage.forEach(log::info);
        log.info("}>");
        log.info("Size expected (swagger): " + swaggerDoc.size());
        int expected = swaggerDoc.size();
        log.info("Size actual: " + coverage.size());
        int actual = coverage.size();
        float percentage = (actual * 100 / expected);
        log.info("Coverage: " + percentage + " %");
        log.info("Without autotests: " + (swaggerDoc.size() - coverage.size()));
        swaggerDoc.removeAll(coverage);
        log.info("<{Need autotests for ");
        notFound.forEach(log::info);
        log.info("}>");

        CoverageData data = CoverageData.coverageData().withDifference(notFound).withPercentage(percentage);
        try {
            dumpToJson(config.getOutputPath(), data);
        } catch (IOException e) {
            log.info("can'n write in file", e);
        }
    }


    private static void dumpToJson(Path output, CoverageData data) throws IOException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(data);
        Files.write(output, json.getBytes());
    }

    private static String formatReq(String httpMethod, String path) {
        return httpMethod + " " + path;
    }
}
