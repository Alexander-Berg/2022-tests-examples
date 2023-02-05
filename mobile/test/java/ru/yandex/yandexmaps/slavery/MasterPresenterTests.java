package ru.yandex.yandexmaps.slavery;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.maps.utils.Stub;
import ru.yandex.yandexmaps.bookmarks.on_map.BookmarkOnMap;
import ru.yandex.yandexmaps.multiplatform.bookmarks.common.RawBookmark;
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.places.ImportantPlace;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MasterPresenterTests {

    @Mock
    private MasterView view;

    private MasterPresenterMocks mocks;

    private MasterPresenter<MasterView> presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mocks = new MasterPresenterMocks();

        presenter = new MasterPresenter<>(mocks.dependenciesHolder());
    }

    @Test
    public void bindUnbind__withoutErrors() {
        presenter.bind(view);
        presenter.unbind(view);
    }

    @Test
    public void bookmarkOnMapSelected__navigatesToBookmarkCard() {
        presenter.bind(view);
        final BookmarkOnMap bookmark = Stub.bookmark();
        mocks.bookmarksOnMapSelections.onNext(bookmark);
        presenter.unbind(view);

        verify(mocks.masterNavigationManager).navigateToBookmarkOnMapCard(bookmark);
    }

    @Test
    public void placeOnMapSelected__navigatesToPlaceCard() {
        final ImportantPlace place = mock(ImportantPlace.class);
        presenter.bind(view);
        mocks.myPlacesOnMapSelections.onNext(place);
        presenter.unbind(view);

        verify(mocks.masterNavigationManager).navigateToMyPlaceOnMapCard(place);
    }

}
