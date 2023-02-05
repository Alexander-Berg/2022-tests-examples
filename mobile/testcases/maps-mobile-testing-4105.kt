// testcase hash: 364ec6a5
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4105
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-4105", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4105")

internal class MapsMobileTesting4105 : TestCaseBasedTest("Открытие карточки провязанного директа", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {
        setLocationAtYandexAndSpanAtMoscow()

        perform(
            """
            Открыт экран результатов поиска по запросу Окна рехау на спане МКАД или Московская область
            """
        ) {
            pages.startScreen.tapSearchField()
            pages.search.history.setSearchText("Окна рехау")
            pages.search.suggests.tapSearchButton()
            pages.search.waitForSearchShutterVisible()
            pages.search.results.scrollToSearchResultWithDirectItem()

            assert(
                """
                В поисковой выдаче присутствуют сниппеты с [провязанным](https://jing.yandex-team.ru/files/katurkin/провязанный.jpg) директом
                """
            ) {
                assertEqual(pages.search.results.isSearchResultWithDirectItemVisible(), true, "Search results has no direct items")
            }
        }

        perform("Тап на сниппет с провязанным директом") {

            pages.search.results.clickOnFirstDirectWithoutScrolling()

            assert(
                """
                Открыта карточка организации
                В карточке присутствует блок директа с описанием и подписью Реклама
                """
            ) {
                assertEqual(pages.card.searchResult.isDirectVisible(), true, "Direct is not visible")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
