// testcase hash: 37e64870
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2842
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2842", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2842")

internal class MapsMobileTesting2842 : TestCaseBasedTest("Включение слоя Транспорт", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт главный экран приложения
            Слой Транспорта выключен
            """
        ) {
            setSpanAtMoscow()
        }

        perform("Тап на таб Транспорт") {
            pages.startScreen.tapMasstransitTabButton()

            assert(
                """
                Таб Транспорт активен
                На карте включен слой Транспорт
                На карте отображаются движущиеся метки ОТ
                """
            ) {
                assertEqual(pages.startScreen.map.isMtLayerEnabled(), true, "Enable transport assert failed")
                assertEqual(pages.startScreen.map.isAnyMtVehicleInVisibleRegion(), true, "Transport not visible assert failed")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
