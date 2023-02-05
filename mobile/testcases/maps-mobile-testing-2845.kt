// testcase hash: 187d8fd9
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2845
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2845", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2845")

internal class MapsMobileTesting2845 : TestCaseBasedTest("Переход из карточки остановки в карточку маршрута", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыта карточка остановки
            Присутствует список маршрутов ОТ
            """
        ) {
            setSpanAtStops()
            pages.startScreen.map.tapOnAnyMtStopOnMap()
            assert(
                """
                    Присутствует список маршрутов ОТ
                """
            ) {
                assertEqual(pages.card.stationCard.hasTransportItems(), true, "Transport items in card visible assert failed")
            }
        }

        perform("Тап на любой маршрут ОТ в списке") {
            pages.card.stationCard.tapTransportItem()
            assert(
                """
                Открытка карточка маршрута ОТ
                """
            ) {
                assertEqual(pages.card.transportCard.titleText().isNotBlank(), true, "Show any transport assert failed")
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Transport line not visible assert failed")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
