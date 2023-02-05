package ru.yandex.yandexmaps.map;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;

import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.maps.BaseTest;
import ru.yandex.maps.appkit.common.PreferenceStorage;
import ru.yandex.yandexmaps.common.mapkit.map.Animations;
import ru.yandex.maps.appkit.map.CameraController;
import ru.yandex.maps.appkit.map.CameraDebugPreferences;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CameraControllerTest extends BaseTest {
    private CameraController cameraController;

    @Mock
    RxMap rxMap;

    @Mock
    PreferenceStorage preferencesInterface;

    @Mock
    CameraDebugPreferences cameraDebugPreferences;

    @Mock
    Map map;

    @Mock
    MapView mapView;

    @Mock
    Map.CameraCallback cameraCallback;

    private final Point somePoint = new Point(0.5, 0.5);
    private final CameraPosition someCameraPosition = new CameraPosition(somePoint, 0, 0, 0);

    @Override
    public void setUp() {
        super.setUp();
        when(rxMap.cameraMoves()).thenReturn(io.reactivex.Observable.never());
        when(map.getCameraPosition()).thenReturn(someCameraPosition);
        cameraController = new CameraController(rxMap, preferencesInterface, cameraDebugPreferences);
        cameraController.init(map, mapView);
        cameraController.setMapReady(true);
        cameraController.setMapSize(1, 1);
    }

    @Test
    public void callCallbackOnBadPoints() {
        final Animation animation = Animations.CAMERA;
        final ScreenPoint someScreenPoint = new ScreenPoint(100f, 100f);

        when(mapView.worldToScreen(any())).thenReturn(null);
        cameraController.moveCenterWithVectorNotImportant(somePoint, somePoint, animation, cameraCallback);
        verify(cameraCallback, times(1)).onMoveFinished(false);

        reset(cameraCallback);

        when(mapView.worldToScreen(any())).thenReturn(someScreenPoint);
        when(mapView.screenToWorld(any())).thenReturn(null);
        cameraController.moveCenterWithVectorNotImportant(somePoint, somePoint, animation, cameraCallback);
        verify(cameraCallback, times(1)).onMoveFinished(false);
    }
}
