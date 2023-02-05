package com.yandex.frankenstein;

import com.yandex.frankenstein.steps.CommandStep;
import org.junit.After;
import org.junit.Test;

import static com.yandex.frankenstein.CommandRegistry.callbacks;
import static com.yandex.frankenstein.CommandRegistry.commandStep;
import static com.yandex.frankenstein.CommandRegistry.commands;
import static com.yandex.frankenstein.CommandRegistry.results;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CommandRegistryTest {

    private final CommandStep mCommandStep = mock(CommandStep.class);
    private final CommandsProvider mCommandsProvider = mock(CommandsProvider.class);
    private final ResultsProvider mResultsProvider = mock(ResultsProvider.class);
    private final CallbacksDispatcher mCallbacksDispatcher = mock(CallbacksDispatcher.class);

    @After
    public void tearDown() {
        CommandRegistry.clearInstance();
    }

    @Test
    public void testDefaultInstance() {
        assertThat(commandStep()).isNull();
        assertThat(commands()).isNull();
        assertThat(results()).isNull();
        assertThat(callbacks()).isNull();
    }

    @Test
    public void testRegisterInstance() {
        CommandRegistry.registerInstance(mCommandStep, mCommandsProvider, mResultsProvider, mCallbacksDispatcher);

        assertThat(commandStep()).isEqualTo(mCommandStep);
        assertThat(commands()).isEqualTo(mCommandsProvider);
        assertThat(results()).isEqualTo(mResultsProvider);
        assertThat(callbacks()).isEqualTo(mCallbacksDispatcher);
    }

    @Test
    public void testClearInstance() {
        CommandRegistry.registerInstance(mCommandStep, mCommandsProvider, mResultsProvider, mCallbacksDispatcher);
        CommandRegistry.clearInstance();

        assertThat(commandStep()).isNull();
        assertThat(commands()).isNull();
        assertThat(results()).isNull();
        assertThat(callbacks()).isNull();
    }
}
