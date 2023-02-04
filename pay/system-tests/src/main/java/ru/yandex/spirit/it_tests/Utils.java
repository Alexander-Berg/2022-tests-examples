package ru.yandex.spirit.it_tests;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import static io.restassured.RestAssured.given;

@UtilityClass
public class Utils {
    public static File getFile(String filename) {
        val loader = Utils.class.getClassLoader();
        return new File(loader.getResource(filename).getFile());
    }

    @SneakyThrows
    public static File getOrCreateFileInWINDOWS1251(String filename, String content) {
        val loader = Utils.class.getClassLoader();
        if (loader.getResource(filename) == null) {
            File f = new File(filename);
            f.getParentFile().mkdirs();
            f.createNewFile();
            f.setWritable(true);
            PrintWriter writer = new PrintWriter(filename, "WINDOWS-1251");
            writer.print(content);
            writer.close();
            return f;
        }
        return new File(loader.getResource(filename).getFile());
    }

    @SneakyThrows
    public static String get_secret(String path) {
        Path pathSecret = Paths.get(path);
        String secret = Files.readAllLines(pathSecret).get(0);
        return secret;
    }

    public static RequestSpecification prepare_request_specification(
            String baseUrl, Map<String, Object> multiparts, Optional<String> body
    ) {
        RequestSpecification request = given().accept(ContentType.JSON)
                .baseUri(baseUrl)
                .filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        if (multiparts.isEmpty()) {
            request.contentType(ContentType.JSON);
        }

        if (body.isPresent()) {
            request.body(body.get());
        }

        for (val entry : multiparts.entrySet()) {
            if (entry.getValue() instanceof  File) {
                request = request.multiPart(entry.getKey(), (File) entry.getValue());
            } else {
                request = request.multiPart(entry.getKey(), (String) entry.getValue());
            }
        }

        return request;
    }
}
