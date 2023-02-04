package ru.auto.tests.commons.mountebank.fluent;

import ru.auto.tests.commons.mountebank.http.imposters.Imposter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ImposterBuilder {
    private List<StubBuilder> stubBuilders = newArrayList();

    public static ImposterBuilder anImposter() {
        return new ImposterBuilder();
    }

    public StubBuilder stub() {
        StubBuilder child = new StubBuilder(this);
        stubBuilders.add(child);
        return child;
    }

    public Imposter build() {
        Imposter imposter = new Imposter();
        for (StubBuilder stubBuilder : stubBuilders) {
            imposter.addStub(stubBuilder.build());
        }
        return imposter;
    }
}
