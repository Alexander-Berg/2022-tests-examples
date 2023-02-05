package ru.yandex.yandexmaps.settings.main

import android.content.Context
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import ru.yandex.maps.BaseTest
import ru.yandex.maps.appkit.util.AppUtils
import ru.yandex.maps.appkit.util.LinkUtils
import ru.yandex.yandexmaps.datasync.DataSyncService
import ru.yandex.yandexmaps.datasync.SearchHistoryInteractor
import ru.yandex.yandexmaps.datasync.binding.SharedData
import ru.yandex.yandexmaps.debug.DebugPanelManager
import ru.yandex.yandexmaps.debug.YandexoidResolver
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.routehistory.RouteHistoryItem
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.searchhistory.SearchHistoryItem
import ru.yandex.yandexmaps.settings.SettingsNavigationManager
import utils.Rx2Answers

class MainSettingsPresenterTest : BaseTest() {

    @Mock
    lateinit var navigationManagerMock: SettingsNavigationManager

    @Mock
    lateinit var yandexoidResolver: YandexoidResolver

    @Mock
    lateinit var debugPanelManager: DebugPanelManager

    @Mock
    lateinit var dataSyncServiceMock: DataSyncService

    @Mock
    lateinit var searchHistoryInteractor: SearchHistoryInteractor

    @Mock
    lateinit var routeHistoryItemItemSharedDataMock: SharedData<RouteHistoryItem>

    @Mock
    lateinit var linkUtils: LinkUtils

    @Mock
    lateinit var context: Context

    private lateinit var viewMock: MainSettingsView
    private lateinit var presenter: MainSettingsPresenter

    private var appUtilsMock: MockedStatic<AppUtils>? = null

    override fun setUp() {
        super.setUp()

        viewMock = mock(MainSettingsView::class.java, Rx2Answers.Never)

        whenever(routeHistoryItemItemSharedDataMock.removeAll()).thenReturn(Completable.never())
        whenever(searchHistoryInteractor.clear()).thenReturn(Completable.never())
        whenever(routeHistoryItemItemSharedDataMock.data()).thenReturn(Observable.never())
        whenever(searchHistoryInteractor.data()).thenReturn(Observable.never())
        whenever(dataSyncServiceMock.routeHistory()).thenReturn(routeHistoryItemItemSharedDataMock)
        whenever(yandexoidResolver.isYandexoid).thenReturn(false)
        appUtilsMock = mockStatic(AppUtils::class.java).apply {
            `when`<Boolean> { AppUtils.isAppInstalled(ArgumentMatchers.any(), ArgumentMatchers.any()) }.thenReturn(true)
        }

        presenter = MainSettingsPresenter(
            context,
            navigationManagerMock,
            dataSyncServiceMock,
            searchHistoryInteractor,
            yandexoidResolver,
            debugPanelManager,
            true,
            linkUtils,
        )
    }

    @After
    fun tearDown() {
        appUtilsMock?.close()
        appUtilsMock = null
    }

    @Test
    fun navigateToGeneralSettingsCalledWhenGeneralSelectionsEmitted() {
        whenever(viewMock.generalSelections()).thenReturn(Observable.just(Unit))
        presenter.bind(viewMock)

        verify(navigationManagerMock).navigateToGeneralSettings()
        verifyNoMoreInteractions(navigationManagerMock)
    }

    @Test
    fun navigateToMapSettingsCalledWhenMapSelectionsEmitted() {
        whenever(viewMock.mapSelections()).thenReturn(Observable.just(Unit))
        presenter.bind(viewMock)

        verify(navigationManagerMock).navigateToMapSettings()
        verifyNoMoreInteractions(navigationManagerMock)
    }

    @Test
    fun navigateToRoutesSettingsCalledWhenRoutesSelectionsEmitted() {
        whenever(viewMock.routesSelections()).thenReturn(Observable.just(Unit))
        presenter.bind(viewMock)

        verify(navigationManagerMock).navigateToRoutesSettings()
        verifyNoMoreInteractions(navigationManagerMock)
    }

    @Test
    fun navigateToAboutSettingsCalledWhenAboutSelectionsEmitted() {
        whenever(viewMock.aboutSelections()).thenReturn(Observable.just(Unit))
        presenter.bind(viewMock)

        verify(navigationManagerMock).navigateToAboutSettings()
        verifyNoMoreInteractions(navigationManagerMock)
    }

    @Test
    fun navigateToClearHistoryConfirmationCalledWhenClearSearchHistorySelectionsEmitted() {
        whenever(viewMock.clearSearchHistorySelections()).thenReturn(Observable.just(Unit))
        presenter.bind(viewMock)

        verify(navigationManagerMock).navigateToClearHistoryConfirmation()
        verifyNoMoreInteractions(navigationManagerMock)
    }

    @Test
    fun clearHistoryButtonStateCorrectAfterBind() {
        // Test 1 Non-empty + empty
        whenever(searchHistoryInteractor.data()).thenReturn(Observable.just(listOf(mock(SearchHistoryItem::class.java))))
        whenever(routeHistoryItemItemSharedDataMock.data()).thenReturn(Observable.just(emptyList<RouteHistoryItem>()))
        presenter.bind(viewMock)
        verify(viewMock).setClearHistoryButtonEnabled(ArgumentMatchers.eq(true))

        presenter.unbind(viewMock)
        resetViewMock()

        // Test 2 Empty + non-empty
        whenever(searchHistoryInteractor.data()).thenReturn(Observable.just(emptyList()))
        whenever(routeHistoryItemItemSharedDataMock.data()).thenReturn(Observable.just(listOf(mock(RouteHistoryItem::class.java))))
        presenter.bind(viewMock)
        verify<MainSettingsView>(viewMock).setClearHistoryButtonEnabled(ArgumentMatchers.eq(true))

        presenter.unbind(viewMock)
        resetViewMock()

        // Test 3 Empty + empty
        whenever(routeHistoryItemItemSharedDataMock.data()).thenReturn(Observable.just(emptyList()))
        whenever(searchHistoryInteractor.data()).thenReturn(Observable.just(emptyList()))
        presenter.bind(viewMock)
        verify(viewMock).setClearHistoryButtonEnabled(ArgumentMatchers.eq(false))

        presenter.unbind(viewMock)
        resetViewMock()

        // Test 4 Non-empty + non-empty
        whenever(searchHistoryInteractor.data()).thenReturn(Observable.just(listOf(mock(SearchHistoryItem::class.java))))
        whenever(routeHistoryItemItemSharedDataMock.data()).thenReturn(Observable.just(listOf(mock(RouteHistoryItem::class.java))))
        presenter.bind(viewMock)
        verify<MainSettingsView>(viewMock).setClearHistoryButtonEnabled(ArgumentMatchers.eq(true))
    }

    @Test
    fun debugPanelButtonShowsWhenBuildDebug() {
        presenter = MainSettingsPresenter(
            context,
            navigationManagerMock,
            dataSyncServiceMock,
            searchHistoryInteractor,
            yandexoidResolver,
            debugPanelManager,
            true,
            linkUtils,
        )
        presenter.bind(viewMock)

        verify(viewMock).setDebugPanelButtonVisibility(true)
    }

    private fun resetViewMock() {
        reset(viewMock)
    }
}
