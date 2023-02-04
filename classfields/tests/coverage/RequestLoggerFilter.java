package ru.auto.tests.coverage;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class RequestLoggerFilter implements OrderedFilter {

    private Path outputPath;

    public RequestLoggerFilter(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        String method = requestSpec.getMethod();
        String path = requestSpec.getUserDefinedPath();
        try (FileWriter fileWriter = new FileWriter(outputPath.toFile(), true)) {
            fileWriter.write(String.format("%s %s\n", method, path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ctx.next(requestSpec, responseSpec);
    }
}