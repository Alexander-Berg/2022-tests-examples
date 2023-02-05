package ru.yandex.disk.remote;

public interface MockResponses {

    String RESPONSE_SUCCESS = "" +
            "{\n" +
            "  \"status\": \"success\"\n" +
            "}";

    String RESPONSE_IN_PROGRESS = "" +
            "{\n" +
            "  \"status\": \"in-progress\"\n" +
            "}";

    String RESPONSE_UNKNOWN = "" +
            "{\n" +
            "  \"status\": \"unknown\"\n" +
            "}";
}
