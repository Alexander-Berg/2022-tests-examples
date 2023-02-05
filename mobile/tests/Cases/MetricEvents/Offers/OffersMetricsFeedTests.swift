import XCTest

class OffersMetricsFeedTests: OffersMetricsBaseTestCase {
    private let skuId = "2000086706989"
    private let expectedParameters: [AnyHashable: AnyHashable] = [
        "supplierId": "10456106", "price": 35_900, "productId": 2_000_086_706_989,
        "wareId": "v-FNHPZKXZNsQhmyb4ngyA", "shopId": 10_456_106,
        "feedId": 200_709_583, "showUid": "16250404304782811722806003", "offerId": "2116608"
    ]

    func testShouldSendOffersMetricsOnCatalog() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами в каталоге")

        mockDefault()

        "Мокаем состояние".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.fapiNavigationTree
            mockStateManager?.pushState(bundleName: "OffersMetrics_Feed")
        }

        var catalog: CatalogPage!
        var feed: FeedPage!

        "Переходим в каталог".run {
            catalog = goToCatalog()
            wait(forVisibilityOf: catalog.departmentCell(at: IndexPath(item: 0, section: 0)).element)
        }

        "Переходим в подкатегорию".run {
            let subcategory = catalog.departmentTitle(matching: "Для школы и офиса")
            catalog.collectionView.ybm_swipeCollectionView(toFullyReveal: subcategory.element)

            let subcategoryPage = subcategory.tap()
            let subcategoryTitle = subcategoryPage.subcategoryTitle(matching: "Письменные принадлежности")
            wait(forVisibilityOf: subcategoryTitle.element)

            let subSubCategoryPage = subcategoryTitle.goToSubcategory()
            let subSubCategoryTitle = subSubCategoryPage.subcategoryTitle(matching: "Ручки")
            wait(forVisibilityOf: subSubCategoryTitle.element)

            feed = subSubCategoryTitle.goToFeed()
            wait(forVisibilityOf: feed.element)
        }

        try "Проверяем отправку метрик".ybm_run { _ in
            try checkCatalogEvents()
        }
    }

    func testShouldSendOffersMetricsOnSearch() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами в поиске")

        mockDefault()

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OffersMetrics_Feed")
        }

        var feed: FeedPage!

        "Открываем поиск".run {
            feed = goToFeed(with: "query")
            wait(forExistanceOf: feed.element)
        }

        "Тапаем по первому сниппету".run {
            let snippet = feed.collectionView.cellPage(at: 0)
            feed.element.ybm_swipeCollectionView(toFullyReveal: snippet.element)
            let sku = snippet.tap()
            wait(forExistanceOf: sku.element)
        }

        try "Проверяем отправку метрик".run {
            try checkSearchEvents()
        }
    }

    // MARK: - Private Methods

    private func checkCatalogEvents() throws {
        let snippetVisible = try getFirstEvent(with: "LIST_SNIPPET_OFFER_SHOW_VISIBLE", skuId: skuId)
        try check(parameters: snippetVisible.parameters, expectedParameters: expectedParameters)

        let listSnippetVisible = try getFirstEvent(
            with: "LIST_RESULTS_SNIPPET-LIST_SNIPPET_OFFER_SHOW_VISIBLE",
            skuId: skuId
        )
        try check(parameters: listSnippetVisible.parameters, expectedParameters: expectedParameters)
    }

    private func checkSearchEvents() throws {
        let listSnippetVisibile = try getFirstEvent(
            with: "SEARCH_RESULTS_SNIPPET-LIST_SNIPPET_OFFER_SHOW_VISIBLE",
            skuId: skuId
        )
        try check(parameters: listSnippetVisibile.parameters, expectedParameters: expectedParameters)

        let listSnippetNavigate = try getFirstEvent(
            with: "SEARCH_RESULTS_SNIPPET-LIST_SNIPPET_OFFER_SHOW_NAVIGATE",
            skuId: skuId
        )
        try check(parameters: listSnippetNavigate.parameters, expectedParameters: expectedParameters)
    }

}
