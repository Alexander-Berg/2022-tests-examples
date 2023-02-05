// testcase hash: 534414c0
package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

// https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6784
private val TEST_CASE_LINK = TestCaseLink("TEST-CASE-6784", "https://testpalm.yandex-team.ru/testcase/maps-mobile-testing-6784")

internal class MapsMobileTesting6784 : TestCaseBasedTest("Открытие рекламного блока геопродукта через карточку", TEST_CASE_LINK) {

    override fun TestCaseDsl.run() {

        perform("Отображается поисковая выдача по запросу Где поесть") {
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

        perform("Тап на организацию с геопродуктом который имеется рекламный блок со знаком '%'") {

            pages.search.results.openTopSearchResultWithAd()
            assert(
                """
                Открылась карточка организации
                В карточке присутствует рекламный блок со знаком '%'
                """
            ) {
                assert(pages.search.results.card.hasGeoProductAd(), "Organization doesn't have geo product ad")
            }
        }

        perform("Тап на рекламный рекламный блок") {
            pages.search.results.card.tapAdBlock()
            assert(
                """
                Отображается карточка "Подробности акции"
                В карточке отображаются условия акции
                """
            ) {
                assert(pages.search.results.card.isAdDetailsSheetShown(), "Ad details sheet is not shown")
            }
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)
}
