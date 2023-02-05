package ru.yandex.yandexmaps.guidance.car.search.menu;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import kotlin.Unit;
import ru.yandex.yandexmaps.guidance.annotations.Muter;
import ru.yandex.yandexmaps.routes.state.GuidanceSearchQuery;
import ru.yandex.yandexmaps.routes.state.SearchType;
import ru.yandex.yandexmaps.speechkit.SpeechKitService;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class QuickSearchPresenterTest {

    private static final String SPEECH_KIT_RESULT_STRING = "Test String";

    private PublishSubject<GuidanceSearchQuery> searchesInterceptor = PublishSubject.create();

    private SlaveQuickSearch.CommanderInternal commanderInternal = new SlaveQuickSearch.CommanderInternal() {
        @NonNull
        @Override
        public Disposable subscribeToQuickSearches(@NonNull io.reactivex.Observable<GuidanceSearchQuery> searches) {
            return searches.subscribe(searchesInterceptor::onNext);
        }

        @NonNull
        @Override
        public Disposable subscribeToMoreClicks(@NonNull io.reactivex.Observable<Unit> clicks) {
            return Disposables.disposed();
        }
    };

    @Mock
    private SpeechKitService speechKitService;

    @Mock
    private Muter muter;

    @Mock
    private QuickSearchInteractor interactor;

    @Mock
    private GuidanceQuickSearchAnalyticsCenter analyticsCenter;

    @Mock
    private QuickSearchView view;

    private PublishSubject<Unit> voiceClicks = PublishSubject.create();
    private PublishSubject<QuickSearchCategory> categorySelections = PublishSubject.create();
    private PublishSubject<Unit> moreActions = PublishSubject.create();

    private QuickSearchPresenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(voiceClicks).when(view).voiceClicks();
        doReturn(categorySelections).when(view).categorySelections();
        doReturn(moreActions).when(view).moreAction();

        when(speechKitService.recognizeWhen(Matchers.any(), Matchers.any(), anyInt(), Matchers.any()))
                .thenReturn(Observable.just(SPEECH_KIT_RESULT_STRING));

        presenter = new QuickSearchPresenter(commanderInternal, speechKitService, muter, interactor, analyticsCenter);
    }

    @Test
    public void voiceSearchSubmittedToCommander() {
        TestObserver<GuidanceSearchQuery> testSubscriber = searchesInterceptor.test();

        presenter.bind(view);
        voiceClicks.onNext(Unit.INSTANCE);
        presenter.unbind(view);

        testSubscriber.assertValueCount(1).assertValue(new GuidanceSearchQuery(SPEECH_KIT_RESULT_STRING, SPEECH_KIT_RESULT_STRING, SearchType.VOICE));
    }

}
