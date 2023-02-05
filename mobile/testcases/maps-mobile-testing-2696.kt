// testcase hash: -257cb941
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperiments.searchScreenWithHistory
import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPointFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ExperimentInfo
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2696
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2696", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2696")

internal class MapsMobileTesting2696 : TestCaseBasedTest(
    name = "Поиск в ведении по маршруту из категории Еще",
    link = TEST_CASE_LINK,
    experiments = listOf(ExperimentInfo(searchScreenWithHistory, false)),
) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Включен эксперимент no_toolbar_in_guidance
            Открыт экран ведения по автомобильному маршруту
            """
        ) {
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
                assertEqual(pages.search.isVoiceSearchButtonVisible(), true, "isVoiceSearchButtonVisible assert failed")
                // assertEqual(pages.search.areMoreCommonSearchCategoriesAvailable(), true, "moreButtonVisible assert failed")
            }
        }

// TODO не работатет эксперимент searchScreenWithHistory
//        perform("Тап на Ещё") {
//            pages.search.showMoreCommonSearchCategories()
//
//            assert("Открыт экран поиска") {
//                assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
//            }
//        }

        perform("Произвести поиск по валидному запросу (например, банк)") {
            pages.search.tapSearchField()
            pages.search.history.setSearchText("банк")
            pages.search.suggests.tapSearchButton()

            assert(
                """
                Произведён поиск организаций по запросу
                Отображены пины организаций вблизи линии маршрута
                """
            ) {
                assertEqual(pages.directions.guidance.car.searchResultsVisible(), true, "searchResultsVisible assert failed")
                assertEqual(pages.directions.guidance.car.searchResultsSearchText(), "банк", "searchResultsSearchText assert failed")
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

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
