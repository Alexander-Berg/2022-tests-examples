// testcase hash: 333bbefb
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2806
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2806", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2806")

internal class MapsMobileTesting2806 : TestCaseBasedTest("Построение ОТ-маршрута через карточку организации", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыта карточка организации
            """
        ) {
            setLocationAndSpanAtYandex()

            val searchText = "ГУМ"
            findUsingSearchTab(searchText)
        }

        perform("Тап на Маршрут в action-баре") {

            pages.card.searchResult.tapRoute()

            assert(
                """
                Открыт экран выбора вариантов маршрута
                Отображаются альтернативные варианты маршрута (при наличии)
                """
            ) {}
        }

        perform("Выбрать тип маршрута - ОТ") {
            pages.directions.routeVariants.tapOnMtTab()
        }

        perform("Посмотреть все альтернативные маршруты (если есть)") {
            pages.directions.routeVariants.mt.tapAndBackForAllMtVariants()

            assert("Маршруты отображаются корректно.") {
                // empty
            }
        }

        perform("Перейти в ведение") {
            pages.directions.routeVariants.mt.tapOnFirstMtVariant()
            pages.directions.routeVariants.mt.details.tapLetsGoButton()

            assert("Открыт экран ведения по ОТ-маршруту") {
                assertEqual(pages.directions.guidance.mt.tripTimeViewVisible(), true, "Mt guidance visible assertion failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
