// testcase hash: 6aedddda
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2691
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2691", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2691")

internal class MapsMobileTesting2691 : TestCaseBasedTest("Поиск маршрута по запросу пользователя", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("") {
            setLocationAndSpanAtYandex()
        }

        perform("Произвести поиск по запросу маршрута, например, трамвай 12") {
            pages.startScreen.tapSearchField()
            pages.search.history.setSearchText("Трамвай 12")
            pages.search.suggests.tapSearchButton()

            assert(
                """
                Открыта миникарточка маршрута Трамвай 12
                На спане выделен маршрут ОТ
                Маршрутная линия полностью вмещается на спане карты
                """
            ) {
                assertEqual(pages.search.results.card.transportCardTitle(), "Трамвай 12", "Wrong value for search result card title!")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
