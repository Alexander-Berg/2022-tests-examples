package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.responses.Response;

public abstract class ResponseTypeBuilder implements FluentBuilder {
    private ResponseBuilder parent;

    protected ResponseTypeBuilder(ResponseBuilder parent) {
        this.parent = parent;
    }

    @Override
    public ResponseBuilder end() {
        return parent;
    }

    abstract protected Response build();
}
