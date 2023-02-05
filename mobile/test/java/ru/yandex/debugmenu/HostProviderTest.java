package ru.yandex.debugmenu;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;

public class HostProviderTest {
    private static final String HOST_FIRST = "host_first";
    private static final String HOST_SECOND = "host_second";
    private static final String HOST_THIRD = "host_third";

    private static final String HOST_DEFAULT = HOST_FIRST;

    private HostList hostList;
    private PreferredHostStorage debugPrefs;

    @Before
    public void runBeforeAnyTest() {
        debugPrefs = Mockito.mock(PreferredHostStorage.class);
        Mockito.when(debugPrefs.getHost(HOST_DEFAULT)).thenReturn(HOST_DEFAULT);

        hostList = Mockito.mock(HostList.class);
        Mockito.when(hostList.getHostList()).thenReturn(new String[]{HOST_FIRST, HOST_SECOND, HOST_THIRD});
        Mockito.when(hostList.defaultHost()).thenReturn(HOST_DEFAULT);
    }

    @Test
    public void checkCurrentBeforeAnyChanges() {
        HostProvider hostProvider = new HostProvider(debugPrefs, hostList);
        assertEquals(HOST_DEFAULT, hostProvider.getCurrentHost());
    }

    @Test
    public void checkCurrentAfterSetNewValue() {
        HostProvider hostProvider = new HostProvider(debugPrefs, hostList);
        hostProvider.setCurrentHost(HOST_THIRD);

        Mockito.verify(debugPrefs, Mockito.times(1)).saveHost(HOST_THIRD);
        assertEquals(HOST_THIRD, hostProvider.getCurrentHost());
    }

}
