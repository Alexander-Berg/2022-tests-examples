package ru.yandex.yandexmaps.slavery;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import ru.yandex.yandexmaps.app.NavigationManager;
import ru.yandex.yandexmaps.bookmarks.on_map.BookmarkOnMap;
import ru.yandex.yandexmaps.bookmarks.on_map.BookmarksOnMapManager;
import ru.yandex.yandexmaps.bookmarks.MtStopsBookmarkPlacemarkRepository;
import ru.yandex.yandexmaps.events.EventsCommander;
import ru.yandex.yandexmaps.map.LongTapEvent;
import ru.yandex.yandexmaps.map.RxMap;
import ru.yandex.yandexmaps.mapobjectsrenderer.api.RxPlacemarkRenderer;
import ru.yandex.yandexmaps.multiplatform.bookmarks.common.RawBookmark;
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point;
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.places.ImportantPlace;
import ru.yandex.yandexmaps.multiplatform.debug.panel.api.ExperimentManager;
import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperimentKey;
import ru.yandex.yandexmaps.mytransportlayer.MtFavoriteStopsBookmarkRenderer;
import ru.yandex.yandexmaps.mytransportlayer.MtStopBookmarkOnMapState;
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay;
import ru.yandex.yandexmaps.overlays.api.OverlaysState;
import ru.yandex.yandexmaps.overlays.api.OverlaysStateProvider;
import ru.yandex.yandexmaps.overlays.api.overlays.TransportOverlayApi;
import ru.yandex.yandexmaps.common.mapkit.contours.ContoursController;
import ru.yandex.yandexmaps.presentation.common.longtap.SlaveLongTap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MasterPresenterMocks {

    public NavigationManager navigationManager;

    @Mock
    public MasterNavigationManager masterNavigationManager;

    @Mock
    public BookmarksOnMapManager bookmarksOnMapManager;

    @Mock
    public SlaveLongTap.Commander slaveLongTapCommander;

    @Mock
    public RxMap rxMap;

    @Mock
    public ContoursController contoursController;

    @Mock
    public MtFavoriteStopsBookmarkRenderer myTransportStopsRenderer;

    @Mock
    public TransportOverlayApi transportApi;

    @Mock
    public ExperimentManager experimentManager;

    @Mock
    public OverlaysStateProvider overlaysStateProvider;

    @Mock
    public MtStopsBookmarkPlacemarkRepository mtStopsBookmarkPlacemarkRepository;
    public PublishSubject<SlaveLongTap.ClickEvent> longTapMenuSelections = PublishSubject.create();
    public PublishSubject<SlaveView> slaveHidesFromUser = io.reactivex.subjects.PublishSubject.create();
    public PublishSubject<BookmarkOnMap> bookmarksOnMapSelections = PublishSubject.create();
    public PublishSubject<ImportantPlace> myPlacesOnMapSelections = PublishSubject.create();
    public PublishSubject<Point> mapTaps = PublishSubject.create();
    public PublishSubject<RxMap.TapEvent> mapObjectTaps = PublishSubject.create();
    public PublishSubject<LongTapEvent> mapLongTaps = PublishSubject.create();
    @Mock
    RxPlacemarkRenderer<MtStopBookmarkOnMapState> mtStopsBookmarkRenderer;
    @Mock
    public EventsCommander eventsCommander;


    public MasterPresenterMocks() {
        MockitoAnnotations.initMocks(this);

        navigationManager = mock(NavigationManager.class);

        when(rxMap.taps()).thenReturn(mapTaps);
        when(rxMap.longTaps()).thenReturn(mapLongTaps);
        when(rxMap.objectTaps()).thenReturn(mapObjectTaps);
        when(rxMap.map()).thenReturn(Single.never());

        when(slaveLongTapCommander.clicks()).thenReturn(longTapMenuSelections);
        when(masterNavigationManager.slaveHiddenFromUser()).thenAnswer((Answer<Observable<SlaveView>>) invocation -> slaveHidesFromUser);
        when(masterNavigationManager.hasSlaves()).thenReturn(Observable.empty());
        when(bookmarksOnMapManager.bookmarksOnMapSelections()).thenReturn(bookmarksOnMapSelections);
        when(bookmarksOnMapManager.myPlacesOnMapSelections()).thenReturn(myPlacesOnMapSelections);

        when(myTransportStopsRenderer.getPlacemarkClicks()).thenReturn(Observable.empty());
        when(mtStopsBookmarkPlacemarkRepository.isSelected(anyString())).thenReturn(false);

        when(mtStopsBookmarkRenderer.getPlacemarkClicks()).thenReturn(Observable.empty());

        when(transportApi.vehicleClicks()).thenReturn(Observable.empty());
        when(eventsCommander.getOrganizationEventTaps()).thenReturn(Observable.empty());
        when(eventsCommander.getCardEventTaps()).thenReturn(Observable.empty());
        when(eventsCommander.getUrlEventTaps()).thenReturn(Observable.empty());

        when(experimentManager.get(any(KnownExperimentKey.KnownExperimentKeyBoolean.class))).thenReturn(false);

        OverlaysState state = new OverlaysState(EnabledOverlay.None.INSTANCE, Collections.emptyList(), true);
        when(overlaysStateProvider.getCurrentState()).thenReturn(state);
    }

    public MasterPresenter.MasterPresenterDependenciesHolder dependenciesHolder() {
        return new MasterPresenter.MasterPresenterDependenciesHolder(
                navigationManager,
                masterNavigationManager,
                bookmarksOnMapManager,
                slaveLongTapCommander,
                rxMap,
                mtStopsBookmarkRenderer,
                mtStopsBookmarkPlacemarkRepository,
                transportApi,
                eventsCommander,
                experimentManager,
                overlaysStateProvider
        );
    }

}
