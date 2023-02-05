// testcase hash: 432e48c7
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPointFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2820
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2820", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2820")

internal class MapsMobileTesting2820 : TestCaseBasedTest("Построение ОТ-маршрута через лонг-тап", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAtYandexAndSpanAtMoscow()
        }

        perform("Лонг-тап по карте") {
            pages.startScreen.map.longTapOnMap(ScreenPointFactory.invoke(400f, 400f))
        }

        perform("Тап на Сюда") {
            pages.mapLongTapScreen.tapToButton()

            assert(
                """
                Открыт экран выбора вариантов маршрута
                Отображаются альтернативные варианты маршрута (при наличии)
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Route line not visible assert failed")
            }
        }

        perform("Выбрать тип маршрута - ОТ") {
            pages.directions.routeVariants.tapOnMtTab()
        }

        perform("Посмотреть все альтернативные  маршруты (если есть)") {
            pages.directions.routeVariants.mt.tapAndBackForAllMtVariants()

            assert("Маршруты отображаются корректно.") {
            }
        }

        perform("Перейти в ведение") {
            pages.directions.routeVariants.mt.tapOnFirstMtVariant()
            pages.directions.routeVariants.mt.details.tapLetsGoButton()

            assert(
                """
                Открыт экран ведения по ОТ-маршруту
                """
            ) {
                assertEqual(pages.directions.guidance.mt.tripTimeViewVisible(), true, "Mt guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
