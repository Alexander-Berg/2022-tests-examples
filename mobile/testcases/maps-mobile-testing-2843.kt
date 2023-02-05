// testcase hash: 465bda49
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2843
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2843", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2843")

internal class MapsMobileTesting2843 : TestCaseBasedTest("Открытие карточки маршрута", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            Слой Транспорта включен
            На карте присутствуют метки ОТ
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
        }

        perform("Тап по метке ОТ на карте") {

            pages.startScreen.map.tapOnAnyMtVehicleOnMap()

            assert(
                """
                Открыта карточка маршрута ОТ
                Выделена линия маршрута
                Спан следует за меткой ОТ
                Метки других маршрутов не отображаются
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyMtVehicleInVisibleRegion(), true, "Transport not visible assert failed")
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Transport line not visible assert failed")
                assertEqual(pages.search.results.card.transportCardTitle().isNotBlank(), true, "Wrong value for search result card title!")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
