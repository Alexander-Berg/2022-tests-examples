// testcase hash: 78b17194
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPointFactory
import ru.yandex.yandexmaps.multiplatform.mapkit.map.obtainZoom
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6267
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-6267", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6267")

internal class MapsMobileTesting6267 : TestCaseBasedTest("Увеличение зума карты дабл-тапом", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            """
        ) {
            setLocationAtYandexAndSpanAtMoscow()
        }
        val position = pages.startScreen.map.getCameraPosition()
        val previousZoom = position?.obtainZoom() ?: 0

        perform("Дабл-тап одним пальцем по карте") {
            pages.startScreen.map.doubleTapOnMap(ScreenPointFactory.invoke(400.0f, 400.0f))
        }

        assert(
            """
            Зум увеличился на 1
            """
        ) {
            val actualZoom = pages.startScreen.map.getCameraPosition()?.obtainZoom() ?: 0
            assert(
                actualZoom.toDouble() > previousZoom.toDouble(),
                "zoom before double tap is $previousZoom\n" +
                    "zoom after is $actualZoom"
            )
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
