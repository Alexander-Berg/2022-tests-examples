package com.yandex.frankenstein;

import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LogTest {

    private static final String TAG = "tag";
    private static final String MESSAGE = "message";

    private final Throwable mThrowable = mock(Throwable.class);

    @Test
    public void testVerbose() {
        Log.v(TAG, MESSAGE);

        verify(Logger.getLogger(TAG)).log(Level.FINE, MESSAGE);
    }

    @Test
    public void testVerboseWithThrowable() {
        Log.v(TAG, MESSAGE, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.FINE, MESSAGE, mThrowable);
    }

    @Test
    public void testDebug() {
        Log.d(TAG, MESSAGE);

        verify(Logger.getLogger(TAG)).log(Level.CONFIG, MESSAGE);
    }

    @Test
    public void testDebugWithThrowable() {
        Log.d(TAG, MESSAGE, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.CONFIG, MESSAGE, mThrowable);
    }

    @Test
    public void testInfo() {
        Log.i(TAG, MESSAGE);

        verify(Logger.getLogger(TAG)).log(Level.INFO, MESSAGE);
    }

    @Test
    public void testInfoWithThrowable() {
        Log.i(TAG, MESSAGE, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.INFO, MESSAGE, mThrowable);
    }

    @Test
    public void testWarn() {
        Log.w(TAG, MESSAGE);

        verify(Logger.getLogger(TAG)).log(Level.WARNING, MESSAGE);
    }

    @Test
    public void testWarnWithThrowable() {
        Log.w(TAG, MESSAGE, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.WARNING, MESSAGE, mThrowable);
    }

    @Test
    public void testWarnWithThrowableOnly() {
        Log.w(TAG, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.WARNING, "", mThrowable);
    }

    @Test
    public void testError() {
        Log.e(TAG, MESSAGE);

        verify(Logger.getLogger(TAG)).log(Level.SEVERE, MESSAGE);
    }

    @Test
    public void testErrorWithThrowable() {
        Log.e(TAG, MESSAGE, mThrowable);

        verify(Logger.getLogger(TAG)).log(Level.SEVERE, MESSAGE, mThrowable);
    }
}
