package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.predicates.Predicate;
import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PredicateTypeBuilder implements FluentBuilder {
    private StubBuilder parent;
    private List<PredicateValueBuilder> childPredicateValueBuilders = newArrayList();

    protected PredicateTypeBuilder(StubBuilder stubBuilder) {
        this.parent = stubBuilder;
    }

    public PredicateValueBuilder equals() {
        return getHttpMatcherBuilder(PredicateType.EQUALS);
    }

    public PredicateValueBuilder deepEquals() {
        return getHttpMatcherBuilder(PredicateType.DEEP_EQUALS);
    }

    public PredicateValueBuilder contains() {
        return getHttpMatcherBuilder(PredicateType.CONTAINS);
    }

    public PredicateValueBuilder startsWith() {
        return getHttpMatcherBuilder(PredicateType.STARTS_WITH);
    }

    public PredicateValueBuilder endsWith() {
        return getHttpMatcherBuilder(PredicateType.ENDS_WITH);
    }

    public PredicateValueBuilder matches() {
        return getHttpMatcherBuilder(PredicateType.MATCHES);
    }

    public PredicateValueBuilder exists() {
        return getHttpMatcherBuilder(PredicateType.EXISTS);
    }

    private PredicateValueBuilder getHttpMatcherBuilder(PredicateType type) {
        PredicateValueBuilder predicateValueBuilder = new PredicateValueBuilder(this, type);
        childPredicateValueBuilders.add(predicateValueBuilder);
        return predicateValueBuilder;
    }

    @Override
    public StubBuilder end() {
        return parent;
    }

    protected List<Predicate> build() {
        List<Predicate> predicates = newArrayList();
        for(PredicateValueBuilder predicateValueBuilder : childPredicateValueBuilders) {
            predicates.add(predicateValueBuilder.build());
        }
        return predicates;
    }
}
