// testcase hash: 139dd8fd
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2418
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2418", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2418")

internal class MapsMobileTesting2418 : TestCaseBasedTest("Поиск по брендированной категории", TEST_CASE_LINK) {

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
                Открыт экран результатов поиска по брендированной категории
                """
            ) {
                assertEqual(pages.search.results.hasSearchResults(), true, "No search results")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
