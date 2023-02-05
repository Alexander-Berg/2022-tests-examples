// testcase hash: -15402dab
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6785
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-6785", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6785")

internal class MapsMobileTesting6785 : TestCaseBasedTest("Открытие рекламного блока геопродукта через серп", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {
        perform("Отображается поисковая выдача по запросу Где поесть В поисковой выдаче отображаются организации с геопродуктом") {
            setLocationAtYandexAndSpanAtMoscow()
            pages.search.waitForCategoriesDownload()
            pages.startScreen.tapSearchTabButton()

            assert(
                """
                Открыт экран поиска
                """
            ) {
                assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
            }

            val categories = pages.search.getVisibleCommonSearchCategories()
            assert("Список категорий не пустой") {
                assert(categories.isNotEmpty(), "Categories list is empty")
            }

            val category = pages.search.getRestaurantsCategory()
            assert("Категория найдена") {
                assertNonNull(category)
            }

            pages.search.tapSearchCategory(category!!)

            assert(
                """
                Открыт экран результатов поиска по запросу Где поесть
                """
            ) {
                assertEqual(pages.search.results.hasSearchResults(), true)
            }
        }

        perform("Тап на рекламный блок организации которая имеет со знаком '%'") {
            pages.search.results.clickOnFirstGeoproduct()

            assert(
                """
                Открылась карточка организации
                Открылась карточка "Подробности акции"
                В карточке отображаются условия акции
                """
            ) {
                assertEqual(pages.search.results.isAdDetailsSheetShown(), true)
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
