// testcase hash: 594fcb7e
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPointFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2821
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2821", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2821")

internal class MapsMobileTesting2821 : TestCaseBasedTest("Построение пешего маршрута через лонг-тап", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAtYandexAndSpanAtMoscow()
        }

        perform("Лонг-тап по карте") {
            pages.startScreen.map.longTapOnMap(ScreenPointFactory.invoke(64f, 400f))
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

        perform("Выбрать тип маршрута - Пешком") {
            pages.directions.routeVariants.tapOnPedestrianTab()
            pages.directions.routeVariants.tapOnEachVariant()
            pages.directions.routeVariants.tapOnFirstVariant()
        }

        perform("Перейти в ведение") {
            pages.directions.routeVariants.tapLetsGoButton()

            assert(
                """
                Открыт экран ведения по пешему маршруту
                """
            ) {
                assertEqual(pages.directions.guidance.pedestrian.timeViewVisible(), true, "Pedestrian guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
