// testcase hash: 2ea8a50f
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.map.obtainTilt
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6702
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-6702", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6702")

internal class MapsMobileTesting6702 : TestCaseBasedTest("Изменение наклона карты через кнопку слоев", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {
        perform(
            """
            Вертикальная ориентация устройства
            Карта отображается в 2D-режиме
            Открыт главный экран приложения
            """
        ) {
            setPortraitOrientation()
            setLocationAndSpanAtYandex()

            assert("Карта отображается в 2D-режиме") {
                val currentTilt = pages.startScreen.map.getCameraPosition()?.obtainTilt()
                assertNonNull(currentTilt, "Couldn't obtain camera tilt")
                assertEqual(
                    currentTilt,
                    .0f,
                    "Camera is not in 2D mode. Camera tilt is $currentTilt."
                )
            }
        }

        perform(
            """
            Тап на контролл слоев
            Тап на пункт 3D-режим
            """
        ) {
            pages.startScreen.tapLayersButton()
            pages.mapLayersScreen.tapOnTilt()

            assert(
                """
                Иконка "3D-режим" подсвечена
                Видимая часть Карты отображается в 3D
                """
            ) {
                // Skipping assertion 'Иконка "3D-режим" подсвечена'
                // https://st.yandex-team.ru/MAPSMOBILETEST-3548#62de86e7e3bab95f306c6315

                val currentTilt = pages.startScreen.map.getCameraPosition()?.obtainTilt() ?: .0f
                assert(currentTilt > .0f, "Camera is not in 3D mode. Camera tilt is $currentTilt.")
            }
        }

        perform("Закрыть экран \"Слои\"") {
            pages.mapLayersScreen.close()

            assert("Карта отображается в 3D") {
                val currentTilt = pages.startScreen.map.getCameraPosition()?.obtainTilt() ?: .0f
                assert(currentTilt > .0f, "Camera is not in 3D mode. Camera tilt is $currentTilt.")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
