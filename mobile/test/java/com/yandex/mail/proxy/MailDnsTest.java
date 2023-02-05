package com.yandex.mail.proxy;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.util.BaseIntegrationTest;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Dns;

import static com.yandex.mail.proxy.BlockManager.State.ALLOWED;
import static com.yandex.mail.proxy.BlockManager.State.BLOCKED;
import static com.yandex.mail.proxy.BlockManager.State.UNKNOWN;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class MailDnsTest extends BaseIntegrationTest {

    @NonNull
    private final List<InetAddress> defaultAddresses = listOf(mock(InetAddress.class));

    @NonNull
    private final List<InetAddress> customAddresses = listOf(mock(InetAddress.class));

    @NonNull
    private final String hostName = "yandex.ru";

    @Mock
    @SuppressWarnings("NullableProblems")   // before
    @NonNull
    private Dns defaultResolver;

    @Mock
    @SuppressWarnings("NullableProblems")   // before
    @NonNull
    BlockManager blockManager;

    @Mock
    @SuppressWarnings("NullableProblems")   // before
    @NonNull
    MailProxyManager proxyManager;

    @Before
    public void beforeEachTest() throws IOException, JSONException {
        MockitoAnnotations.initMocks(this);
        when(defaultResolver.lookup(hostName)).thenReturn(defaultAddresses);
        when(proxyManager.lookup(hostName)).thenReturn(customAddresses);
    }

    @Test
    public void lookup_callsBlockManager() throws UnknownHostException {
        MailDns mailDnsUkraine = new MailDns(blockManager, proxyManager, true, defaultResolver);
        mailDnsUkraine.lookup(hostName);
        verify(blockManager).refreshBlockedStateIfNeeded(true);

        MailDns mailDns = new MailDns(blockManager, proxyManager, false, defaultResolver);
        mailDns.lookup(hostName);
        verify(blockManager).refreshBlockedStateIfNeeded(false);
    }

    @Test
    public void lookup_callsRefreshMappingOnlyIfSavedBlockStateBlocked() throws UnknownHostException {
        MailDns mailDns = new MailDns(blockManager, proxyManager, true, defaultResolver);

        when(blockManager.getBlockedState()).thenReturn(UNKNOWN);
        mailDns.lookup(hostName);
        verify(proxyManager, never()).refreshMapping();

        when(blockManager.getBlockedState()).thenReturn(ALLOWED);
        mailDns.lookup(hostName);
        verify(proxyManager, never()).refreshMapping();

        when(blockManager.getBlockedState()).thenReturn(BLOCKED);
        mailDns.lookup(hostName);
        verify(proxyManager).refreshMapping();
    }

    @Test
    public void lookup_callsProxyManagerOnlyIfSavedBlockStateBlocked() throws UnknownHostException {
        MailDns mailDns = new MailDns(blockManager, proxyManager, true, defaultResolver);

        when(blockManager.getBlockedState()).thenReturn(UNKNOWN);
        assertThat(mailDns.lookup(hostName)).isEqualTo(defaultAddresses);

        when(blockManager.getBlockedState()).thenReturn(ALLOWED);
        assertThat(mailDns.lookup(hostName)).isEqualTo(defaultAddresses);

        when(blockManager.getBlockedState()).thenReturn(BLOCKED);
        assertThat(mailDns.lookup(hostName)).isEqualTo(customAddresses);
    }
}
