// testcase hash: -3b56efe9
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2692
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2692", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2692")

internal class MapsMobileTesting2692 : TestCaseBasedTest("Поиск организации по запросу пользователя", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("") {
            setLocationAndSpanAtYandex()
        }

        perform("Произвести поиск по запросу ТЦ Европейский") {
            pages.startScreen.tapSearchField()
            pages.search.history.setSearchText("ТЦ Европейский")
            pages.search.suggests.tapSearchButton()

            assert(
                """
                На спане карты - поднятый пин торгового центра Европейский
                Открыта миникарточка организации
                """
            ) {
                assertEqual(pages.search.results.card.placecardTitle(), "Европейский", "Wrong value for search result card title!")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
