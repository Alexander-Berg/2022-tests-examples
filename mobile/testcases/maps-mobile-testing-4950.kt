// testcase hash: -670d5203
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.extensions.toNativePoint
import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPositionFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.helpers.KnownLocations

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4950
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-4950", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4950")

internal class MapsMobileTesting4950 : TestCaseBasedTest("Построение веломаршрута", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("") {
            setLocationAndSpanAtYandex()
        }

        perform("Тап на таб Маршруты") {
            pages.startScreen.tapDirectionsTabButton()

            assert("Открылся экран построения маршрута") {
                assertEqual(pages.directions.routeVariants.isDirectionsScreenShown(), true, "Directions screen is not shown")
            }
        }

        perform("Выбрать тип транспортного средства - Велосипед") {
            pages.directions.routeVariants.tapOnBikeTab()
        }

        perform("Тап на поле Куда") {
            pages.directions.wayPointsSelection.tapToField()

            assert("Открыт экран выбора конечной точки") {
                assertEqual(pages.directions.wayPointsSelection.isSelectOnMapShown(), true, "Select on map is not shown")
            }
        }

        perform("Тап на \"Указать на карте\"") {
            pages.directions.wayPointsSelection.tapToSelectOnMap()

            assert("Открылся экран выбора точки на карте") {
                assertEqual(pages.directions.selectPointOnMap.isScreenShown(), true, "Select on map screen is not shown")
            }
        }

        perform("Указать любое место на карте") {
            pages.startScreen.map.setCameraPosition(
                CameraPositionFactory.createCameraPosition(KnownLocations.YANDEX_AURORA.toNativePoint(), 16.0f, 0.0f, 0.0f)
            )
        }

        perform("Тап Готово") {
            pages.directions.selectPointOnMap.tapOnReady()

            assert("Маршрут построен") {
                assertEqual(pages.directions.routeVariants.isDirectionsScreenShown(), true, "Directions screen is not shown")
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Any polyline is not visible")
            }
        }

        perform("Тап Поехали") {
            pages.directions.routeVariants.tapLetsGoButton()

            assert("Открыт экран ведения по веломаршруту") {
                assertEqual(pages.directions.guidance.pedestrian.timeViewVisible(), true, "Bike guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
