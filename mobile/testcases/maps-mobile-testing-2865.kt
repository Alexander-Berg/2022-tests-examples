// testcase hash: 14f60f60
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2865
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2865", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2865")

internal class MapsMobileTesting2865 : TestCaseBasedTest("Авторизация в приложении", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Открыт экран Меню
            На устройстве добавлен яндексовый аккаунт
            Пользователь не авторизован
            """
        ) {
            login()
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
