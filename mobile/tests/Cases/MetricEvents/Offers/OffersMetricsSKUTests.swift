import MarketUITestMocks
import XCTest

class OffersMetricsSKUTests: OffersMetricsBaseTestCase {

    func testShouldSendOffersMetricsOnFeed() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами на КТ")

        mockDefault()

        var morda: MordaPage!
        var sku: SKUPage!
        var snippet: SnippetPage!

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(
                offer: modify(FAPIOffer.default) { $0.delivery = .pharma },
                model: .default
            )
            stateManager?.setState(newState: skuState)
        }

        "Открываем морду".run {
            morda = goToMorda()
        }

        "Клацаем по сниппету и скроллим до скроллбокса".run {
            snippet = morda.historyWidget.container.cellPage(at: .init(row: .zero, section: .zero))
            sku = snippet.tap()
            wait(forExistanceOf: sku.element)
            sku.collectionView.swipe(to: .down, untilVisible: sku.analogs)
        }

        try "Проверяем отправку метрик".run {
            try checkEvents()
        }
    }

    // MARK: - Private Methods

    private func checkEvents() throws {
        let expectedParameters: [AnyHashable: AnyHashable] = [
            "supplierId": "465852",
            "sellerId": "465852",
            "productId": 722_979_017,
            "wareId": "KRAPKDZkKKimNA-4MVm-rA",
            "shopId": 431_782,
            "feedId": 475_690,
            "showUid": "16302313087396618916700001",
            "offerId": "637707.000284.MGJK3RU/A"
        ]
        let deliveryOptions = try getFirstEvent(
            with: "PRODUCT_OFFER-CONDITIONS_DELIVERY-OPTION_VISIBLE",
            skuId: "101077347763"
        )
        try check(parameters: deliveryOptions.parameters, expectedParameters: expectedParameters)

        let pageVisible = try getFirstEvent(with: "PRODUCT_PAGE_OFFER_SHOW_VISIBLE", skuId: "101077347763")
        try check(parameters: pageVisible.parameters, expectedParameters: expectedParameters)

        let alsoViewed = try getFirstEvent(with: "PRODUCT_ALSOVIEWED_SNIPPET_OFFER_SHOW_VISIBLE", skuId: "100902560734")
        try check(parameters: alsoViewed.parameters, expectedParameters: [
            "supplierId": "465852",
            "sellerId": "465852",
            "productId": 658_172_022,
            "wareId": "s0TMPCGvs9JIraw-0zZIRQ",
            "shopId": 431_782,
            "feedId": 475_690,
            "showUid": "16302313087396618916700001",
            "offerId": "637707.000284.MGJK3RU/A"
        ])
    }

}
