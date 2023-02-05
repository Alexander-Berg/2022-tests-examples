package com.yandex.frankenstein;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommandRunnerTest {

    private static final String CLAZZ = "ClassName";
    private static final String NAME = "commandName";
    private static final String COMMAND_ID = "commandId";

    private final CommandProcessFactory mCommandProcessFactory = mock(CommandProcessFactory.class);
    private final CommandRunner mCommandRunner = new CommandRunner(CLAZZ, mCommandProcessFactory);

    private final CommandsProvider.Argument mFirstArgument = mock(CommandsProvider.Argument.class);
    private final CommandsProvider.Argument mSecondArgument = mock(CommandsProvider.Argument.class);
    private final List<CommandsProvider.Argument> mArguments = Arrays.asList(mFirstArgument, mSecondArgument);

    private final CommandsProvider mCommandsProvider = mock(CommandsProvider.class);

    @Before
    public void setUp() {
        when(mCommandsProvider.add(CLAZZ, NAME, mArguments)).thenReturn(COMMAND_ID);
        CommandRegistry.registerInstance(null, mCommandsProvider, null, null);
    }

    @After
    public void tearDown() {
        CommandRegistry.clearInstance();
    }

    @Test
    public void testStartWithArray() {
        mCommandRunner.start(NAME, mFirstArgument, mSecondArgument);

        verify(mCommandsProvider).add(CLAZZ, NAME, mArguments);
        verify(mCommandProcessFactory).createDefaultCommandProcess(COMMAND_ID);
    }

    @Test
    public void testStartWithList() {
        mCommandRunner.start(NAME, mArguments);

        verify(mCommandsProvider).add(CLAZZ, NAME, mArguments);
        verify(mCommandProcessFactory).createDefaultCommandProcess(COMMAND_ID);
    }

    @Test
    public void testStartSilentlyWithArray() {
        mCommandRunner.startSilently(NAME, mFirstArgument, mSecondArgument);

        verify(mCommandsProvider).add(CLAZZ, NAME, mArguments);
        verify(mCommandProcessFactory).createSilentCommandProcess(COMMAND_ID);
    }

    @Test
    public void testStartSilentlyWithList() {
        mCommandRunner.startSilently(NAME, mArguments);

        verify(mCommandsProvider).add(CLAZZ, NAME, mArguments);
        verify(mCommandProcessFactory).createSilentCommandProcess(COMMAND_ID);
    }
}
