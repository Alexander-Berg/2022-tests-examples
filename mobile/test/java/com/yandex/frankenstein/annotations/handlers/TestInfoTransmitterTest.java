package com.yandex.frankenstein.annotations.handlers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestInfoTransmitterTest {

    private static final String TAG = "Some Tag";
    private static final String INFO = "Some Info";

    private final PrintStream mSystemErr = System.err;
    private final PrintStream mErr = mock(PrintStream.class);
    private final TestInfoTransmitter mTransmitter = new TestInfoTransmitter();

    @Before
    public void setUp() {
        System.setErr(mErr);
    }

    @After
    public void tearDown() {
        System.setErr(mSystemErr);
    }

    @Test
    public void testTransmit() {
        mTransmitter.transmit(TAG);

        verify(mErr).println(String.format("%s: ", TAG));
    }

    @Test
    public void testTransmitWithInfo() {
        mTransmitter.transmit(TAG, INFO);

        verify(mErr).println(String.format("%s: %s", TAG, INFO));
    }
}
