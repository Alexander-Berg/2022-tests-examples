// testcase hash: -2a994036
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperiments.taxiNativeOrderCard
import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.TaxiNativeOrderCard
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ExperimentInfo
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2809
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-2809", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-2809")

internal class MapsMobileTesting2809 : TestCaseBasedTest(
    name = "Построение маршрута на такси через карточку организации",
    link = TEST_CASE_LINK,
    experiments = listOf(ExperimentInfo(taxiNativeOrderCard, TaxiNativeOrderCard.ALWAYS_OPEN)),
) {

    override fun TestCaseDsl.run() {

        perform(
            """
            Выключен эксперимент taxi_native_order_in_taxi_tab
            Открыта карточка организации
            """
        ) {
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
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Taxi route is visible assert failed")
            }
        }

        perform("Выбрать тип маршрута - На такси") {
            pages.directions.routeVariants.tapOnTaxiTab()
            pages.directions.routeVariants.tapOnEachTaxiVariant()
            pages.directions.routeVariants.tapOnFirstTaxiVariant()

            assert(
                """
                Отображается маршрут такси
                Присутствует кнопка логина и вызова такси
                """
            ) {
                assertEqual(pages.startScreen.map.isAnyPolylineOnMapVisible(), true, "Taxi route is visible assert failed")
                assert(pages.directions.routeVariants.hasLoginButtonInTaxiSnippet(), "Login and call taxi button is visible assert failed")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
