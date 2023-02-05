// testcase hash: 78696918
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.extensions.toNativePoint
import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPositionFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.helpers.KnownLocations

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4952
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-4952", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-4952")

internal class MapsMobileTesting4952 : TestCaseBasedTest("Построение маршрута на ОТ", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("Открыт главный экран приложения") {
            setLocationAndSpanAtYandex()
        }

        perform("Тап на таб Маршруты") {
            pages.startScreen.tapDirectionsTabButton()

            assert("Открылся экран построения маршрута") {
                assertEqual(pages.directions.routeVariants.isDirectionsScreenShown(), true, "Directions screen is not shown")
            }
        }

        perform("Выбрать тип транспортного средства - ОТ") {
            pages.directions.routeVariants.tapOnMtTab()
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

            assert("Открыт экран выбора вариантов маршрута") {
                assertEqual(pages.directions.routeVariants.isDirectionsScreenShown(), true, "Directions screen is not shown")
            }
        }

        perform("Тап Посмотреть на карте у любого варианта маршрута") {
            pages.directions.routeVariants.mt.tapOnFirstMtVariant()

            assert("Открыт экран обзора маршрута") {
                // empty - checked on tapping on let's go button
            }
        }

        perform("Пролистать в конец экрана") {
            // empty - done while tapping on let's go button
        }

        perform("Тап на кнопку Поехали") {
            pages.directions.routeVariants.mt.details.tapLetsGoButton()

            assert("Открыт экран ведения по ОТ-маршруту") {
                assertEqual(pages.directions.guidance.mt.tripTimeViewVisible(), true, "Mt guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
