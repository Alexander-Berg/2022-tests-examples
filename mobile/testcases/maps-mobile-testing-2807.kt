// testcase hash: -3132e80a
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2807
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2807", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2807")

internal class MapsMobileTesting2807 : TestCaseBasedTest("Построение пешего маршрута через карточку организации", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("Открыта карточка организации") {
            setLocationAndSpanAtYandex()

            val searchText = "ГУМ"
            findUsingSearchTab(searchText)

            assert("Открыта карточка организации") {
                assertEqual(
                    pages.card.searchResult.placecardTitle(), searchText,
                    "Wrong value for search result card title!"
                )
            }
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
