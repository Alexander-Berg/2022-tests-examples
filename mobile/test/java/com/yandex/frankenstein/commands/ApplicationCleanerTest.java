package com.yandex.frankenstein.commands;

import com.yandex.frankenstein.CommandProcess;
import com.yandex.frankenstein.CommandRunner;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ApplicationCleanerTest {

    private final CommandRunner mCommandRunner = mock(CommandRunner.class);
    private final CommandProcess mCommandProcess = mock(CommandProcess.class);
    private final ApplicationCleaner mApplicationCleaner = new ApplicationCleaner(mCommandRunner);

    @Test
    public void testCleanCache() {
        when(mCommandRunner.start("cleanCache")).thenReturn(mCommandProcess);

        mApplicationCleaner.cleanCache();
        verify(mCommandProcess).waitFor();
    }

    @Test
    public void testCleanKeychain() {
        when(mCommandRunner.start("cleanKeychain")).thenReturn(mCommandProcess);

        mApplicationCleaner.cleanKeychain();
        verify(mCommandRunner).start("cleanKeychain");
        verifyZeroInteractions(mCommandRunner);
    }
}
