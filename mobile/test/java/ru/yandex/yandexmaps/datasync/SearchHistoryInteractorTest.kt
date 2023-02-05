package ru.yandex.yandexmaps.datasync

import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import ru.yandex.yandexmaps.datasync.binding.SharedData
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.searchhistory.SearchHistoryItem

class SearchHistoryInteractorTest {

    @Mock
    lateinit var sharedData: SharedData<SearchHistoryItem>

    @Mock
    lateinit var dataSyncService: DataSyncService

    private lateinit var interactor: SearchHistoryInteractor

    @Before
    fun setUp() {
        openMocks(this)
        `when`(dataSyncService.searchHistory()).thenReturn(sharedData)
        interactor = SearchHistoryInteractor(dataSyncService)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throw_whenTryingToAddInvalidItem() {
        interactor.addOrUpdate(
            SearchHistoryItem(
                recordId = null,
                searchText = "   \t\t\t  \t\t\t   ",
                displayText = "",
                lastUsed = 0,
                uri = null
            )
        )
    }

    @Test
    fun filterInvalidItems_onDataResponse() {
        val valid1 = SearchHistoryItem(
            recordId = null,
            searchText = "aaa",
            displayText = "bbb",
            lastUsed = 0,
            uri = null
        )
        val valid2 = valid1.copy(displayText = "ccc")
        val invalid1 = valid1.copy(displayText = "   ")
        val invalid2 = valid1.copy(searchText = "\t\t")

        `when`(sharedData.data(false)).thenReturn(Observable.just(listOf(valid1, invalid1, valid2, invalid2)))

        //noinspection unchecked
        interactor.data()
            .test()
            .assertResult(listOf(valid1, valid2))
            .dispose()
    }
}
