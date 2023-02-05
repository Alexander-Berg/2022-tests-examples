// testcase hash: 1af92daf
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2844
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2844", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2844")

internal class MapsMobileTesting2844 : TestCaseBasedTest("Открытие карточки остановки", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            "Открыт главный экран приложения"
        ) {
            setSpanAtStops()
            assert(
                "Показываются остановки"
            ) {
                assertEqual(pages.startScreen.map.isAnyMtStopVisible(), true, "Stops visible assert failed")
            }
        }

        perform("Тап на остановку ОТ") {
            pages.startScreen.map.tapOnAnyMtStopOnMap()
            assert(
                """
                Открыта миникарточка остановки
                Присутствует список маршрутов ОТ
                На карте отображаются метки ОТ маршрутов, проходящих через эту остановку
                """
            ) {
                assertEqual(pages.card.stationCard.hasTransportItems(), true, "Transport items in card visible assert failed")
                setSpanAtMoscow()
                assertEqual(pages.startScreen.map.isAnyMTVehicleVisible(), true, "Transport enabled assert failed")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
