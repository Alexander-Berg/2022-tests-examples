package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2846
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2846", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2846")

internal class MapsMobileTesting2846 : TestCaseBasedTest("Переход из карточки маршрута в карточку остановки", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Слой Транспорта включен
            Открыта карточка маршрута
            """
        ) {
            setSpanAtMoscow()
            pages.startScreen.tapMasstransitTabButton()

            assert(
                """
                На карте присутствуют метки ОТ
                """
            ) {
                assertEqual(pages.startScreen.map.isMtLayerEnabled(), true, "Enable transport assert failed")
                assertEqual(pages.startScreen.map.isAnyMtVehicleInVisibleRegion(), true, "Transport not visible assert failed")
            }

            pages.startScreen.map.tapOnAnyMtVehicleOnMap()
        }

        perform("Тап на остановку из карточки маршрута") {

            pages.card.transportCard.tapSummaryCard()
            pages.card.transportCard.tapFirstStation()

            assert(
                """
                Открытка карточка остановки
                """
            ) {
                assertEqual(pages.card.stationCard.titleText().isNotBlank(), true, "Show station card failed")
            }
        }

        perform("Закрыть карточку оставноки") {

            pages.card.stationCard.tapCloseCard()

            assert(
                """
                Вернулись на карточку маршрута
                """
            ) {
                assertEqual(pages.card.transportCard.titleText().isNotBlank(), true, "Show transport card failed")
            }
        }

        perform("Закрыть карточку маршрута") {
            pages.card.transportCard.tapCloseCard()
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
