import XCTest

class OffersMetricsCartTests: OffersMetricsBaseTestCase {

    func testShouldSendOffersMetricsOnCart() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами в корзине")

        disable(toggles: FeatureNames.cartRedesign)

        mockDefault()

        var cart: CartPage!
        var cell: CartPage.CartItem!

        "Переходим в корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Скроллим до скролл бокса и обратно".ybm_run { _ in
            cell = cart.cartItem(at: .zero)
            wait(forExistanceOf: cell.element)
            cart.collectionView.swipe(to: .down, until: cart.historyWidget.container.element.isVisible)
            cart.collectionView.swipe(to: .up, until: cell.element.isVisible)
        }

        "Тапаем по товару".ybm_run { _ in
            let sku = cell.tap()
            ybm_wait(forFulfillmentOf: { sku.element.isVisible })
        }

        try "Чекаем отправку метрик".ybm_run { _ in
            try checkEvents()
        }
    }

    // MARK: - Private Methods

    private func checkEvents() throws {
        let expectedParameters: [AnyHashable: AnyHashable] = [
            "skuId": "100812315808",
            "showUid": "16268585037651240585506000",
            "productId": 612_787_165,
            "wareId": "fugqcBen6bRo8G0AIJQYbg",
            "shopId": 1_095_449,
            "supplierId": "1095449",
            "feedId": 1_015_257,
            "offerId": "2358",
            "price": 15_998
        ]

        let itemVisible = try getFirstEvent(with: "CART-PAGE_BOX_ITEM_OFFER_SHOW_VISIBLE", skuId: "100812315808")
        try check(parameters: itemVisible.parameters, expectedParameters: expectedParameters)

        let itemNavigate = try getFirstEvent(with: "CART-PAGE_BOX_ITEM_OFFER_SHOW_NAVIGATE", skuId: "100812315808")
        try check(parameters: itemNavigate.parameters, expectedParameters: expectedParameters)

        let snippetVisible = try getFirstEvent(
            with: "CART-PAGE_SCROLLBOX_SNIPPET_OFFER_SHOW_VISIBLE",
            skuId: "100902560734"
        )
        try check(parameters: snippetVisible.parameters, expectedParameters: [
            "skuId": "100902560734",
            "productId": 658_172_022,
            "modelId": 658_172_022,
            "price": 1_190
        ])
    }

}
