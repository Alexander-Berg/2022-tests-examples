package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.responses.ModeType;
import ru.auto.tests.commons.mountebank.http.responses.Proxy;
import ru.auto.tests.commons.mountebank.http.responses.Response;

public class ProxyBuilder extends ResponseTypeBuilder {

    private ModeType mode;
    private String to;

    protected ProxyBuilder(ResponseBuilder responseBuilder) {
        super(responseBuilder);
    }

    public ProxyBuilder mode(ModeType modeType) {
        mode = modeType;
        return this;
    }

    public ProxyBuilder to(String url) {
        to = url;
        return this;
    }

    @Override
    protected Response build() {
        return new Proxy().withMode(mode).withTo(to);
    }
}
