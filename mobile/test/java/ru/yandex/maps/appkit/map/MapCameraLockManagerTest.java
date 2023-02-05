package ru.yandex.maps.appkit.map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.yandexmaps.common.map.MapCameraLock;
import ru.yandex.yandexmaps.map.RxMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapCameraLockManagerTest {


    @Mock
    RxMap rxMap;
    @Mock
    Map map;

    MapCameraLockManager lockManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(rxMap.get()).thenReturn(map);
        lockManager = new MapCameraLockManager(rxMap);
    }

    @Test
    public void mapFreezes_whenAnyLockHasAcquired() {
        lockManager.addLock(mock(MapCameraLock.class));

        verify(map, only()).setFreezeCameraToPlacemarkMoves(true);
    }

    @Test
    public void mapUnfreezes_whenAllLocksHasReleased() {
        final MapCameraLock lock = mock(MapCameraLock.class);
        lockManager.addLock(lock);
        lockManager.removeLock(lock);

        verify(map, times(1)).setFreezeCameraToPlacemarkMoves(false);
    }

    @Test
    public void mapNotUnfreeze_whenManagerHasAtLeastOneLock() {
        final MapCameraLock lock = mock(MapCameraLock.class);
        lockManager.addLock(mock(MapCameraLock.class));
        lockManager.addLock(lock);
        lockManager.removeLock(lock);

        verify(map, never()).setFreezeCameraToPlacemarkMoves(false);
    }

    @Test
    public void mapUnfreezes_whenMapAndOtherLocksPresented() {
        final MapCameraLock oldLock = mock(MapCameraLock.class);
        final MapCameraLock newLock = mock(MapCameraLock.class);

        lockManager.addLock(oldLock);
        lockManager.addLock(newLock);

        verify(map, times(1)).setFreezeCameraToPlacemarkMoves(true);

        lockManager.removeLock(newLock);
        lockManager.removeLock(oldLock);

        verify(map, times(1)).setFreezeCameraToPlacemarkMoves(false);

    }

}