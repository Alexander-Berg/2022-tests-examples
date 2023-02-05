// testcase hash: -427a6dda
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2814
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2814", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2814")

internal class MapsMobileTesting2814 : TestCaseBasedTest("Построение веломаршрута через карточку топонима", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыта карточка топонима
            """
        ) {
            setLocationAndSpanAtYandex()
            openToponymCard()
        }

        perform("Тап на Маршрут в action-баре") {

            pages.card.searchResult.tapRoute()

            assert(
                """
                Открыт экран выбора вариантов маршрута
                Отображаются альтернативные варианты маршрута (при наличии)
                """
            ) {
                // empty - checked next while clicking on variants and let's go button
            }
        }

        perform("Выбрать тип маршрута - На велосипеде") {
            pages.directions.routeVariants.tapOnBikeTab()
            pages.directions.routeVariants.tapOnEachVariant()
            pages.directions.routeVariants.tapOnFirstVariant()
        }

        perform("Перейти в ведение") {
            pages.directions.routeVariants.tapLetsGoButton()
            assert(
                """
                Открыт экран ведения по веломаршруту
                """
            ) {
                assertEqual(pages.directions.guidance.bike.timeViewVisible(), true, "Bike guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
