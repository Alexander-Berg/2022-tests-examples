// testcase hash: -12b6c32e
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchCategory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6697
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-6697", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6697")

internal class MapsMobileTesting6697 : TestCaseBasedTest("Открытие карточки брендированной организации", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {
        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAndSpanAtYandex()
            pages.search.waitForCategoriesDownload()
        }

        perform("Тап на таб Поиск") {
            pages.startScreen.tapSearchTabButton()

            assert(
                """
                Открыт экран поиска
                """
            ) {
                assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
            }
        }

        perform("Произвести поиск по брендированной категории") {
            searchByAdCategoryUntilResultsAppeared()

            assert(
                """
                Отображается список поисковой выдачи с поисковыми сниппетами
                Отображаются пины поисковой выдачи на карте
                """
            ) {
                assertEqual(pages.search.results.hasSearchResults(), true, "No search results")
            }
        }

        perform(
            """
            Тап на сниппет в списке организаций
            """
        ) {
            pages.search.results.clickFirstSearchResult()

            assert("Список поисковой выдачи скрывается Открывается карточка организации Пин организации становится выделеным") {
                assertEqual(pages.card.searchResult.placecardTitle().isNotEmpty(), true)
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
