// testcase hash: -23bb8e44
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-1830
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-1830", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-1830")

internal class MapsMobileTesting1830 : TestCaseBasedTest("Поиск топонима по тапу на саджест", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("Открыт главный экран приложения") {
            setLocationAndSpanAtYandex()
        }

        perform("Тап на таб Поиск") {

            pages.startScreen.tapSearchTabButton()

            assert("Открыт экран поиска") {
                assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
            }
        }

        perform("Вводить в поле поиска адрес существующего здания до появления саджеста с названием искомой улицы") {
            pages.search.tapSearchField()
            pages.search.history.setSearchText("Льва Тол")

            assert("Отображается саджест с названием улицы, без номера дома") {
                assertEqual(pages.search.suggests.hasSuggestWithText("улица Льва Толстого"), true, "4. Wrong value for hasSuggests!")
            }
        }

        perform("Тап на саджест без номера дома") {
            pages.search.suggests.openSuggestWithText("улица Льва Толстого")

            assert(
                """
                В поисковой строке адрес с названием улицы из саджеста
                Отображаются саджесты с номером дома
                """
            ) {
                assertEqual(pages.search.suggests.getSearchText(), "улица Льва Толстого ", "6. Wrong search text!")
                assertEqual(pages.search.suggests.hasSuggestWithText("улица Льва Толстого, 16"), true, "6. Wrong value for hasSuggests!")
            }
        }

        perform("Тап на любомй саджест с номером дома") {
            pages.search.suggests.openSuggestWithText("улица Льва Толстого, 16")

            assert(
                """
                Произведён поиск по выбранному адресу
                На спане - поднятый пин топонима
                Открыта миникарточка топонима
                """
            ) {
                assertEqual(pages.search.results.card.placecardTitle(), "улица Льва Толстого, 16", "Wrong value for search result card title!")
            }
        }
    }

    override fun status() = Status.STABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
