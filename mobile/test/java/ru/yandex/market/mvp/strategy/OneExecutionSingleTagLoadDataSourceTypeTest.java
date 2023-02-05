package ru.yandex.market.mvp.strategy;

import moxy.MvpView;
import moxy.viewstate.ViewCommand;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.base.presentation.core.mvp.strategy.OneExecutionSingleTagStrategy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class OneExecutionSingleTagLoadDataSourceTypeTest {

    private OneExecutionSingleTagStrategy strategy;

    @Before
    public void setUp() {
        strategy = new OneExecutionSingleTagStrategy();
    }

    @Test
    public void testAddsCommandToStateBeforeApplyAndRemovesAfter() {
        final List<ViewCommand<MvpView>> currentState = new ArrayList<>();
        final ViewCommand<MvpView> command = new DummyViewCommand();

        strategy.beforeApply(currentState, command);
        assertThat(currentState, contains(command));

        strategy.afterApply(currentState, command);
        assertThat(currentState, empty());
    }
}