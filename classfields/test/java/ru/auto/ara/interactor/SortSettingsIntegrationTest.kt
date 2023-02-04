package ru.auto.ara.interactor

import android.content.Context
import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.data.preferences.MemoryDefaultPreferences
import ru.auto.ara.plugin.ClearCachePlugin
import ru.auto.ara.presentation.presenter.feed.factory.SortItemFactory
import ru.auto.ara.util.Clock
import ru.auto.ara.util.android.StringsProvider
import ru.auto.core_ui.util.Consts
import ru.auto.data.interactor.SortSettingsInteractor
import ru.auto.data.model.SortType
import ru.auto.data.model.search.Order
import ru.auto.data.model.search.Sort
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.data.repository.SortSettingsRepository
import ru.auto.data.repository.createDefaultSortOptionsProvider
import ru.auto.data.util.SortUtils
import ru.auto.test.core.RxTestAndroid
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class SortSettingsIntegrationTest: RxTestAndroid() {

    private val defaultPreferences = MemoryDefaultPreferences()

    private val context: Context = mock()

    private val strings: StringsProvider = mock()

    private val interactor: SortSettingsInteractor by lazy {
        val optionsRepo = SortItemFactory(strings)
        val repo = SortSettingsRepository(
            MemoryReactivePrefsDelegate(), Gson(), true,
            createDefaultSortOptionsProvider(optionsRepo, isSavedFeedSearch = false)
        )
        SortSettingsInteractor(repo, optionsRepo, SortUtils)
    }

    private val cachePlugin = ClearCachePlugin(defaultPreferences) { interactor }

    @Before
    fun setUp() {
        val time = START_DATE.time
        defaultPreferences.setSessionTimestamp(time)
        setupClock(time)
        whenever(strings.get(any())).thenReturn("")
    }

    @Test
    fun `should not clear selected sort when less then day passed`() {
        interactor.updateSort(SORT_TYPE, SELECTED_SORT).await()
        cachePlugin.onSafeSetup(context)
        assertEquals(SELECTED_SORT, interactor.observeSort(SORT_TYPE).toBlocking().first())
    }

    @Test
    fun `should clear selected sort when day passed`() {
        interactor.updateSort(SORT_TYPE, SELECTED_SORT).await()
        setupClock(START_DATE.time + Consts.DAY_MS + 1L)
        cachePlugin.onSafeSetup(context)
        assertEquals(DEFAULT_SORT, interactor.observeSort(SORT_TYPE).toBlocking().first())
    }

    private fun setupClock(time: Long) {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = time
        }
    }

    companion object {
        private val DEFAULT_SORT = Sort(Order.DESC, "fresh_relevance_1")
        private val SELECTED_SORT = Sort(Order.DESC, "price")
        private val SORT_TYPE = SortType.AUTO
        private val START_DATE = Date()
    }
}
