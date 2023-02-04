package ru.yandex.infra.stage.util;

import ru.yandex.infra.stage.HttpServiceMetrics;

public class DummyHttpServiceMetrics implements HttpServiceMetrics  {

    public int requestsCount;
    public int errorResponseCount;
    public int parsingErrorCount;

    @Override
    public void addRequest(Source source) {
        requestsCount++;
    }

    @Override
    public void addErrorResponse(Source source) {
        errorResponseCount++;
    }

    @Override
    public void addParsingError(Source source) {
        parsingErrorCount++;
    }
}
