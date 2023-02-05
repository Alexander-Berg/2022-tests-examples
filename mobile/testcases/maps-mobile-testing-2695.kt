// testcase hash: 2c855dbb
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPointFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2695
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2695", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2695")

internal class MapsMobileTesting2695 : TestCaseBasedTest("Поиск в ведении по маршруту", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {
        perform("Открыт экран ведения по автомобильному маршруту") {
            setLocationAndSpanAtYandex()
            openCarGuidance()
            pages.search.waitForCategoriesDownload()
        }

        perform("Тап на Поиск") {
            pages.startScreen.map.doubleTapOnMap(ScreenPointFactory.invoke(64f, 400f))
            pages.directions.guidance.car.tapSearch()

            assert(
                """
                Открыто меню поиска
                Над меню поиска отображена кнопка голосового ввода
                В меню отображены поисковые категории и кнопка Ещё
                """
            ) {
                assertEqual(pages.search.isVoiceSearchButtonVisible(), true, "voiceSearchButtonVisible assert failed")
            }
        }

        perform("Тап на Где поесть") {
            val categories = pages.search.getVisibleCommonSearchCategories()
            assert("Список категорий не пустой") {
                assert(categories.isNotEmpty(), "Categories list is empty")
            }

            val category = pages.search.getRestaurantsCategory()
            assert("Категория найдена") {
                assertNonNull(category)
            }

            pages.search.tapSearchCategory(category!!)

            assert(
                """
                Произведён поиск организаций вырбранной категории
                Отображены пины организаций вблизи линии маршрута
                """
            ) {
                assertEqual(pages.directions.guidance.car.searchResultsVisible(), true, "searchResultsVisible assert failed")
                assertEqual(pages.directions.guidance.car.searchResultsSearchText(), "Где поесть", "searchResultsSearchText assert failed")
            }
        }

        perform("Закрыть поисковую выдачу") {
            pages.directions.guidance.car.tapCloseSearch()

            assert(
                """
                Поисковая выдача не отображается
                Ведение продолжается
                """
            ) {
                assertEqual(pages.directions.guidance.car.searchResultsVisible(), false, "searchResultsVisible assert failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
