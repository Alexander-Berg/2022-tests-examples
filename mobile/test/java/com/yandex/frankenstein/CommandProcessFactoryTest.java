package com.yandex.frankenstein;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandProcessFactoryTest {

    private static final String COMMAND_ID = "commandId";
    private final CommandProcessFactory mCommandProcessFactory = new CommandProcessFactory();

    @Test
    public void testCreateDefaultCommandProcess() {
        final CommandProcess commandProcess = mCommandProcessFactory.createDefaultCommandProcess(COMMAND_ID);
        assertThat(commandProcess.getCommandId()).isEqualTo(COMMAND_ID);
    }

    @Test
    public void testCreateSilentCommandProcess() {
        final CommandProcess commandProcess = mCommandProcessFactory.createSilentCommandProcess(COMMAND_ID);
        assertThat(commandProcess.getCommandId()).isEqualTo(COMMAND_ID);
    }
}
