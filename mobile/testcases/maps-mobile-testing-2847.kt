// testcase hash: 1d8f3fd7
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2847
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2847", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2847")

internal class MapsMobileTesting2847 : TestCaseBasedTest("Включение слоя Пробки", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAndSpanAtYandex()
            assert("Слой Пробки выключен") {
                assertEqual(pages.startScreen.map.isAnyTrafficLinesVisible(), false, "Traffic is not visible when test started")
            }
        }

        perform("Тап на контрол Слои") {
            pages.startScreen.tapLayersButton()
        }

        perform("Тап на Пробки") {
            pages.mapLayersScreen.tapOnTraffic()
            pages.mapLayersScreen.close()

            assert(
                """
                Слой Пробки включен
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyTrafficLinesVisible(), true, "Traffic is visible")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
