package com.yandex.frankenstein.steps;

import com.yandex.frankenstein.settings.ServerSettings;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MockWebServerRuleTest {

    private static final String HOST = "42.100.42.100";
    private static final int PORT = 4242;
    private static final long TIMEOUT = 100;

    @Mock private ServerSettings mServerSettings;
    @Mock private MockWebServer mMockWebServer;
    @Mock private Supplier<MockWebServer> mMockWebServerSupplier;
    @Mock private Dispatcher mDispatcher;
    @Mock private Dispatcher mNewDispatcher;
    @Mock private RecordedRequest mRequest;

    private MockWebServerRule mMockWebServerRule;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mServerSettings.getListenHost()).thenReturn(HOST);
        when(mServerSettings.getPort()).thenReturn(PORT);
        when(mServerSettings.getTimeout()).thenReturn(TIMEOUT);

        when(mMockWebServerSupplier.get()).thenReturn(mMockWebServer);

        mMockWebServerRule = new MockWebServerRule(mServerSettings, mDispatcher, mMockWebServerSupplier);
        mMockWebServerRule.shutdown();
        clearInvocations(mMockWebServer, mMockWebServerSupplier);
    }

    @Test
    public void testStart() throws IOException {
        mMockWebServerRule.start();

        verify(mMockWebServer).setDispatcher(mDispatcher);
        verify(mMockWebServer).start(InetAddress.getByName(HOST), PORT);
    }

    @Test
    public void testStartTwice() throws IOException {
        mMockWebServerRule.start();
        mMockWebServerRule.start();

        verify(mMockWebServerSupplier, times(1)).get();
        verify(mMockWebServer, times(1)).setDispatcher(mDispatcher);
        verify(mMockWebServer, times(1)).start(InetAddress.getByName(HOST), PORT);
    }

    @Test
    public void testTakeRequest() throws InterruptedException {
        when(mMockWebServer.takeRequest(TIMEOUT, TimeUnit.SECONDS)).thenReturn(mRequest);
        mMockWebServerRule.start();

        assertThat(mMockWebServerRule.takeRequest()).isEqualTo(mRequest);
    }

    @Test(expected = IllegalStateException.class)
    public void testTakeRequestWithoutStart() throws InterruptedException {
        when(mMockWebServer.takeRequest(TIMEOUT, TimeUnit.SECONDS)).thenReturn(mRequest);

        mMockWebServerRule.takeRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void testTakeRequestAfterShutdown() throws InterruptedException {
        when(mMockWebServer.takeRequest(TIMEOUT, TimeUnit.SECONDS)).thenReturn(mRequest);
        mMockWebServerRule.start();
        mMockWebServerRule.shutdown();

        mMockWebServerRule.takeRequest();
    }

    @Test
    public void testTakeRequestSwallowsInterruptedException() throws InterruptedException {
        when(mMockWebServer.takeRequest(TIMEOUT, TimeUnit.SECONDS)).thenThrow(InterruptedException.class);
        mMockWebServerRule.start();

        assertThat(mMockWebServerRule.takeRequest()).isNull();
    }

    @Test
    public void testSetDispatcher() {
        mMockWebServerRule.start();
        mMockWebServerRule.setDispatcher(mNewDispatcher);

        verify(mMockWebServer).setDispatcher(mNewDispatcher);
    }

    @Test
    public void testSetDispatcherWithoutStart() {
        mMockWebServerRule.setDispatcher(mNewDispatcher);

        verifyZeroInteractions(mMockWebServer);
    }

    @Test
    public void testShutdown() throws IOException {
        mMockWebServerRule.start();
        mMockWebServerRule.shutdown();

        verify(mMockWebServer).shutdown();
    }

    @Test
    public void testShutdownWithoutStart() {
        mMockWebServerRule.shutdown();

        verifyZeroInteractions(mMockWebServer);
    }

    @Test
    public void testShutdownSwallowsIOException() throws IOException {
        doThrow(IOException.class).when(mMockWebServer).shutdown();

        mMockWebServerRule.start();
        mMockWebServerRule.shutdown();
    }

    @Test
    public void testToString() {
        final String string = mMockWebServerRule.toString();

        assertThat(string).isEqualTo(String.format("MockWebServer[%d]", PORT));
    }
}
