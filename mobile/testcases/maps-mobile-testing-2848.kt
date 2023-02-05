// testcase hash: 78b17194
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2848
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2848", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2848")

internal class MapsMobileTesting2848 : TestCaseBasedTest("Включение слоя Парковки", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAndSpanAtYandex()
            assert("Слой Парковки выключен") {
                assertEqual(pages.startScreen.map.isAnyParkingVisible(), false, "Parkings are not visible when test started")
            }
        }

        perform("Тап на контрол Слои") {
            pages.startScreen.tapLayersButton()
        }

        perform("Тап на Парковки") {
            pages.mapLayersScreen.tapOnParkings()
            pages.mapLayersScreen.close()

            assert(
                """
                Слой Парковки включен
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyParkingVisible(), true, "Parkings are visible")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
