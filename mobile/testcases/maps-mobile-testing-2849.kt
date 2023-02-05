// testcase hash: 4a301207
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2849
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2849", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2849")

internal class MapsMobileTesting2849 : TestCaseBasedTest("Включение слоя Панорамы", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAtYandexAndSpanAtMoscow()
            assert("Слой Панорамы выключен") {
                assertEqual(pages.startScreen.map.isAnyPanoramasVisible(), false, "Panoramas are not visible when test started")
            }
        }

        perform("Тап на контрол Слои") {
            pages.startScreen.tapLayersButton()
        }

        perform("Тап на Панорамы") {
            pages.mapLayersScreen.tapOnPanorama()
            pages.mapLayersScreen.close()

            assert(
                """
                Слой Панорамы включен
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyPanoramasVisible(), true, "Panoramas are visible")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
