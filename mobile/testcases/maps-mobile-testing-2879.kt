// testcase hash: -64f6658e
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2879
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2879", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2879")

internal class MapsMobileTesting2879 : TestCaseBasedTest("Ведение в режиме Навигатор", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Происходит симуляция движения без ведения
            Открыт главный экран приложения
            """
        ) {
            setLocationAndSpanAtYandex()
        }

        perform("Тап на таб Навигатор") {

            pages.startScreen.tapNavigationTabButton()

            assert(
                """
                Происходит ведение в режиме Навигатор
                """
            ) {
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
