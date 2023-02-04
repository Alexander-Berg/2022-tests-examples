@file:Suppress("ForbiddenImport") // fails test if not set correctly
package ru.auto.feature.manual

import android.app.Application
import android.content.res.Resources
import com.yandex.mobile.verticalcore.utils.AppHelper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.R
import ru.auto.ara.presentation.presenter.manual.ManualController
import ru.auto.ara.router.Navigator
import ru.auto.ara.router.command.ShowWebViewCommand
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.statistics.AnalystManager
import ru.auto.ara.util.statistics.StatEvent
import ru.auto.ara.viewmodel.manual.ManualViewModel


@RunWith(AllureRunner::class) class ManualControllerTest {

    private val mockApp: Application = mock()
    private val mockResources: Resources = mock()
    private val mockRouter: Navigator = mock()
    private val mockStringsProvider: StringsProvider = mock {
        on { get(R.string.manual_page_01_title) } doReturn "Звоним продавцу"
        on { get(R.string.manual_page_02_title) } doReturn "Готовимся к осмотру"
        on { get(R.string.manual_page_03_title) } doReturn "Проверяем по базам"
        on { get(R.string.manual_page_04_title) } doReturn "Проверяем ПТС"
        on { get(R.string.manual_page_05_title) } doReturn "Осматриваем кузов"
        on { get(R.string.manual_page_06_title) } doReturn "Осматриваем салон"
        on { get(R.string.manual_title) } doReturn "Учебник"
        on { get(R.string.manual_message) } doReturn "Больше статей\\nпро покупку авто"
        on { get(R.string.manual_hint) } doReturn "Проверьте перед покупкой"
    }
    private val mockAnalytics: AnalystManager = mock()
    private val fromTestAnalytics = "from_test"

    private val manualController = ManualController(
        router = mockRouter,
        stringsProvider = mockStringsProvider,
        analystManager = mockAnalytics,
        analyticsMsgOpenFrom = fromTestAnalytics,
    )
    private val manualsPagesItems: Map<ManualViewModel, String> = mapOf(
        ManualViewModel.Page(
            title = R.string.manual_page_01_title,
            text = R.string.manual_page_01_text,
            image = R.drawable.uchebnik_01
        ) to "https://mag.auto.ru/article/how-to-call",
        ManualViewModel.Page(
            title = R.string.manual_page_02_title,
            text = R.string.manual_page_02_text,
            image = R.drawable.uchebnik_02
        ) to "https://mag.auto.ru/article/check-preparation",
        ManualViewModel.Page(
            title = R.string.manual_page_03_title,
            text = R.string.manual_page_03_text,
            image = R.drawable.uchebnik_03
        ) to "https://mag.auto.ru/article/check-bases",
        ManualViewModel.Page(
            title = R.string.manual_page_04_title,
            text = R.string.manual_page_04_text,
            image = R.drawable.uchebnik_04
        ) to "https://mag.auto.ru/article/check-pts",
        ManualViewModel.Page(
            title = R.string.manual_page_05_title,
            text = R.string.manual_page_05_text,
            image = R.drawable.uchebnik_05
        ) to "https://mag.auto.ru/article/check-exterior",
        ManualViewModel.Page(
            title = R.string.manual_page_06_title,
            text = R.string.manual_page_06_text,
            image = R.drawable.uchebnik_06
        ) to "https://mag.auto.ru/article/check-interior",
        ManualViewModel.Page(
            title = R.string.manual_message,
            text = null,
            image = R.drawable.uchebnik_more
        ) to "https://mag.auto.ru/theme/uchebnik",
        ManualViewModel.Title to "https://mag.auto.ru/theme/uchebnik",
    )

    @Before
    fun presetAppHelper() {
        whenever(mockApp.resources).thenReturn(mockResources)
        whenever(mockApp.applicationContext).thenReturn(mockApp)
        AppHelper.setupApp(mockApp)
    }

    @Test
    fun `should get correct manual items list`() {
        val manualItems = manualController.getCardManual().items
        manualItems shouldHaveSize 7
        manualItems shouldContainExactly manualsPagesItems.keys.minus(ManualViewModel.Title)
    }

    @Test
    fun `should open correct webview when clicking manual items`() {
        manualsPagesItems.forEach { (vm, url) ->
            manualController.onManualClicked(vm)
            argumentCaptor<Map<String, Any>>().apply {
                verify(mockAnalytics).logEvent(
                    eq(StatEvent.EVENT_GO_TO_MANUAL),
                    capture()
                )

                firstValue[fromTestAnalytics] shouldBe mockStringsProvider[vm.title]
            }
            argumentCaptor<ShowWebViewCommand>().apply {
                verify(mockRouter, times(1)).perform(capture())
                firstValue.url shouldBe url
            }
            reset(mockRouter, mockAnalytics)
        }
    }

}
