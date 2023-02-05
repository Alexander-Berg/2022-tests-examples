package com.yandex.frankenstein.commands;

import com.yandex.frankenstein.CommandProcess;
import com.yandex.frankenstein.CommandRunner;
import com.yandex.frankenstein.CommandsProvider.Argument;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TimeManagerTest {

    private static final double TIME = 4.2;
    private static final int TIMEZONE_OFFSET = 10800000;
    private static final long DELTA_HOURS = 8;
    private static final long MILLIS_IN_HOUR = 3600 * 1000;

    private final CommandRunner mCommandRunner = mock(CommandRunner.class);
    private final CommandProcess mCommandProcess = mock(CommandProcess.class);
    private final TimeManager mTimeManager = new TimeManager(mCommandRunner);

    @Test
    public void testGetTime() {
        when(mCommandRunner.start("getTime")).thenReturn(mCommandProcess);
        when(mCommandProcess.getDoubleResult()).thenReturn(TIME);

        assertThat(mTimeManager.getTime()).isEqualTo(TIME);
    }

    @Test
    public void testGetTimezoneOffset() {
        when(mCommandRunner.start("getTimezoneOffset")).thenReturn(mCommandProcess);
        when(mCommandProcess.getIntResult()).thenReturn(TIMEZONE_OFFSET);

        assertThat(mTimeManager.getTimezoneOffset()).isEqualTo(TIMEZONE_OFFSET);
    }

    @Test
    public void testUpdateTime() {
        final Argument argument = new Argument("delta", DELTA_HOURS * MILLIS_IN_HOUR);
        when(mCommandRunner.start(eq("updateTime"), refEq(argument))).thenReturn(mCommandProcess);

        mTimeManager.updateTime(DELTA_HOURS, TimeUnit.HOURS);
        verify(mCommandProcess).waitFor();
    }

    @Test
    public void testResetTime() {
        when(mCommandRunner.start("resetTime")).thenReturn(mCommandProcess);
        mTimeManager.resetTime();

        verify(mCommandRunner).start("resetTime");
        verifyZeroInteractions(mCommandProcess);
    }
}
