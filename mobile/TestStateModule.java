package ru.yandex.market.di.module;

import java.util.List;

import dagger.Module;
import dagger.Provides;
import ru.yandex.market.mocks.State;

@Module
public class TestStateModule {

    private List<State> states;

    public TestStateModule(final List<State> states) {
        this.states = states;
    }

    @Provides
    List<? extends State> provideStates() {
        return states;
    }

}