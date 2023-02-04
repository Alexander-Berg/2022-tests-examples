package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.responses.Response;

public class ResponseBuilder implements FluentBuilder {
    private StubBuilder parent;
    private ResponseTypeBuilder builder;

    protected ResponseBuilder(StubBuilder stubBuilder) {
        this.parent = stubBuilder;
    }

    public IsBuilder is() {
        builder = new IsBuilder(this);
        return (IsBuilder) builder;
    }

    public ProxyBuilder proxy() {
        builder = new ProxyBuilder(this);
        return (ProxyBuilder) builder;
    }

    public InjectBuilder inject() {
        builder = new InjectBuilder(this);
        return (InjectBuilder) builder;
    }

    @Override
    public StubBuilder end() {
        return parent;
    }

    protected Response build() {
        if(builder != null) return builder.build();

        return new IsBuilder(this).build();
    }
}
