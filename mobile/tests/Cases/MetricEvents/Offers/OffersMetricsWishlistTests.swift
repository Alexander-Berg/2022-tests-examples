import MarketUITestMocks
import Metrics
import XCTest

class OffersMetricsWishlistTests: OffersMetricsBaseTestCase {

    func testShouldSendOffersMetricsOnWishlist() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами в избранном")

        var cell: FeedSnippetPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setWishlistItems(items: [.default])
            stateManager?.setState(newState: wishlistState)
        }

        "Переходим в избранное".ybm_run { _ in
            let profile = goToProfile()
            wait(forExistanceOf: profile.wishlist.element)

            let wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)

            cell = wishlist.wishlistItem(at: 0)
            wait(forExistanceOf: cell.element)
        }

        "Тапаем по сниппету".ybm_run { _ in
            let sku = cell.tap()
            wait(forExistanceOf: sku.element)
        }

        try "Проверяем отправку метрик".ybm_run { _ in
            try checkEvents()
            MetricRecorder.clear()
        }
    }

    // MARK: - Private Methods

    private func checkEvents() throws {
        let expectedParameters: [AnyHashable: AnyHashable] = [
            "supplierId": "465852", "price": 81_990, "productId": 722_979_017,
            "wareId": "KRAPKDZkKKimNA-4MVm-rA", "shopId": 431_782,
            "feedId": 475_690, "showUid": "16302313087396618916700001", "offerId": "637707.000284.MGJK3RU/A"
        ]

        let listSnippetVisible = try getFirstEvent(
            with: "WISHLIST-PAGE_RESULTS_SNIPPET-LIST_SNIPPET_OFFER_SHOW_VISIBLE",
            skuId: "101077347763"
        )
        try check(parameters: listSnippetVisible.parameters, expectedParameters: expectedParameters)

        let snippetVisible = try getFirstEvent(with: "WISHLIST-PAGE_SNIPPET_OFFER_SHOW_VISIBLE", skuId: "101077347763")
        try check(parameters: snippetVisible.parameters, expectedParameters: expectedParameters)

        let snippetNavigate = try getFirstEvent(
            with: "WISHLIST-PAGE_SNIPPET_OFFER_SHOW_NAVIGATE",
            skuId: "101077347763"
        )
        try check(parameters: snippetNavigate.parameters, expectedParameters: expectedParameters)
    }

}
