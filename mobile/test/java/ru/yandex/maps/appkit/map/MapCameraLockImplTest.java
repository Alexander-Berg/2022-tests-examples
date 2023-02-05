package ru.yandex.maps.appkit.map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class MapCameraLockImplTest {

    @Mock
    private MapCameraLockManager mapCameraLockManager;

    @InjectMocks
    MapCameraLockImpl mapCameraLock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void acquirePassSelfToManager() throws Exception {
        mapCameraLock.acquire(getClass());
        verify(mapCameraLockManager).addLock(mapCameraLock);
    }

    @Test
    public void releasePassSelfToManager() throws Exception {
        mapCameraLock.release();
        verify(mapCameraLockManager).removeLock(mapCameraLock);
    }

}