package ru.yandex.market.mvp.strategy;

import moxy.MvpView;
import moxy.viewstate.ViewCommand;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.yandex.market.base.presentation.core.mvp.strategy.SingleTagStrategyDelegate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class SingleTagLoadDataSourceTypeDelegateTest {

    private static final String TAG = "tag";

    private static final List<ViewCommand<MvpView>> INITIAL_COMMANDS = Arrays.asList(
            new DummyViewCommand(),
            new DummyViewCommand(TAG),
            new AnotherDummyViewCommand(),
            new AnotherDummyViewCommand(TAG)
    );

    private SingleTagStrategyDelegate delegate;

    @Before
    public void setUp() {
        delegate = new SingleTagStrategyDelegate();
    }

    @Test
    public void testRemoveCommandsWithSameClassWhenBothTagsAreEmpty() {
        final List<ViewCommand<MvpView>> commands = new ArrayList<>(INITIAL_COMMANDS);
        delegate.apply(commands, new DummyViewCommand());
        assertThat(commands, equalTo(INITIAL_COMMANDS.subList(1, 4)));
    }

    @Test
    public void testRemoveCommandsWithSameTagWhenAtLeastOneTagPresent() {
        final List<ViewCommand<MvpView>> commands = new ArrayList<>(INITIAL_COMMANDS);
        delegate.apply(commands, new DummyViewCommand(TAG));

        final List<ViewCommand<MvpView>> expectedCommands = new ArrayList<>();
        expectedCommands.add(INITIAL_COMMANDS.get(0));
        expectedCommands.add(INITIAL_COMMANDS.get(2));
        assertThat(commands, equalTo(expectedCommands));
    }

    @Test
    public void testDoNothingWhenCurrentStateIsNull() {
        delegate.apply(null, new DummyViewCommand(TAG));
    }

    @Test
    public void testDoNothingWhenCommandIsNull() {
        final List<ViewCommand<MvpView>> commands = new ArrayList<>(INITIAL_COMMANDS);
        delegate.apply(commands, null);
        assertThat(commands, equalTo(INITIAL_COMMANDS));
    }
}