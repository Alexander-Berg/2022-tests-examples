package ru.yandex.yandexmaps.map;

import javax.annotation.NonNullByDefault;


import ru.yandex.yandexmaps.multiplatform.core.map.CameraMove;
import ru.yandex.yandexmaps.multiplatform.core.map.CameraState;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NonNullByDefault
public class CameraMoves {

    public static CameraMove azimuth(float value) {
        final CameraState mock = mock(CameraState.class);
        when(mock.getAzimuth()).thenReturn(value);
        return state(mock);
    }

    public static CameraMove zoom(float value) {
        final CameraState mock = mock(CameraState.class);
        when(mock.getZoom()).thenReturn(value);
        return state(mock);
    }

    public static CameraMove tilt(float value) {
        final CameraState mock = mock(CameraState.class);
        when(mock.getTilt()).thenReturn(value);
        return state(mock);
    }

    private static CameraMove state(CameraState state) {
        final CameraMove mock = mock(CameraMove.class);
        when(mock.getState()).thenReturn(state);
        return mock;
    }





}
