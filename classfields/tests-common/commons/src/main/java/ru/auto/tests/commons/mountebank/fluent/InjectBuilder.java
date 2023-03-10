package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.responses.Inject;

public class InjectBuilder extends ResponseTypeBuilder {
    private String function = "";

    public InjectBuilder(ResponseBuilder responseBuilder) {
        super(responseBuilder);
    }

    public InjectBuilder function(String function) {
        this.function = function;
        return this;
    }

    @Override
    protected Inject build() {
        return new Inject().withFunction(function);
    }
}
