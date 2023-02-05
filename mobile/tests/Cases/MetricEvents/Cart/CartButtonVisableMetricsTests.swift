import MarketUITestMocks
import Metrics
import XCTest

class CartButtonVisableMetricsTests: LocalMockTestCase {

    private let eventKey = "ADD-TO-CART-BUTTON_VISIBLE"

    func testShouldSendMetricsWhenMakeOrder() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Отображение кнопки \"В корзину\"")
        Allure.addTitle("Отправка продуктовых метрик при оформлении заказа")

        var morda: MordaPage!
        var feed: FeedPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Basics")
            mockStateManager?.pushState(bundleName: "CartButtonVisableMetrics_Feed")
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
            wait(forVisibilityOf: morda.element)
        }

        try "Проверяем отправку событий отображения кнопки В КОРЗИНУ в виджите истории".ybm_run { _ in
            try checkDefaultHistoryCartButtonEvents()
        }

        "Открываем выдачу".ybm_run { _ in
            let searchPage = morda.searchButton.tap()
            ybm_wait(forVisibilityOf: [searchPage.navigationBar.searchTextField])
            searchPage.navigationBar.searchTextField.tap()
            searchPage.navigationBar.searchTextField.typeText("Протеин")
            MetricRecorder.clear()
            searchPage.navigationBar.searchTextField.typeText("\n")
            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            wait(forVisibilityOf: feedElement)
            feed = FeedPage(element: feedElement)
        }

        try "Проверяем отправку событий".ybm_run { _ in
            try checkFeedCartButtonEvents()
            MetricRecorder.clear()
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Переходим в КМ".ybm_run { _ in
            let snippet = feed.collectionView.cellPage(at: 0)
            snippet.tap()
        }

        try "Проверяем отправку событий".ybm_run { _ in
            try checkDefaultSKUCartButtonEvents()
        }
    }

    // MARK: - Private methods

    private func checkDefaultHistoryCartButtonEvents() throws {
        let events = MetricRecorder.events(from: .appmetrica).with(name: eventKey)

        let proteinOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100902560734" })
        XCTAssertEqual(try XCTUnwrap(proteinOffer.parameters["productId"] as? NSNumber), 658_172_022)
        XCTAssertEqual(try XCTUnwrap(proteinOffer.parameters["wareId"] as? String), "s0TMPCGvs9JIraw-0zZIRQ")
        XCTAssertEqual(try XCTUnwrap(proteinOffer.parameters["price"] as? NSNumber), 1_190)
        XCTAssertEqual(try XCTUnwrap(proteinOffer.parameters["feedId"] as? NSNumber), 0)
        XCTAssertEqual(try XCTUnwrap(proteinOffer.parameters["atSupplierWarehouse"] as? NSNumber), 0)

        let secondProteinOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100953490814" })
        XCTAssertEqual(try XCTUnwrap(secondProteinOffer.parameters["productId"] as? NSNumber), 668_278_053)
        XCTAssertEqual(try XCTUnwrap(secondProteinOffer.parameters["wareId"] as? String), "zm8FK4B__0g6HVlNGQcSaA")
        XCTAssertEqual(try XCTUnwrap(secondProteinOffer.parameters["feedId"] as? NSNumber), 0)
        XCTAssertEqual(try XCTUnwrap(secondProteinOffer.parameters["price"] as? NSNumber), 840)
        XCTAssertEqual(try XCTUnwrap(secondProteinOffer.parameters["atSupplierWarehouse"] as? NSNumber), 0)
    }

    private func checkFeedCartButtonEvents() throws {
        let events = MetricRecorder.events(from: .appmetrica).with(name: eventKey)

        let firstOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100699222310" })
        XCTAssertNotNil(firstOffer.parameters["offerLocalUniqueId"])
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["showUid"] as? String), "16263774137415684001506001")
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["productId"] as? NSNumber), 1_712_454_293)
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["wareId"] as? String), "R-XXnDqmYWClaGLwVUp5Dg")
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["shopId"] as? NSNumber), 431_782)
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["feedId"] as? NSNumber), 475_690)
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["offerId"] as? String), "493303.000132.4600778002303")
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["price"] as? NSNumber), 1_172)
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["oldPrice"] as? NSNumber), 1_450)
        XCTAssertEqual(try XCTUnwrap(firstOffer.parameters["atSupplierWarehouse"] as? NSNumber), 0)

        let secondOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100422054760" })
        XCTAssertNotNil(firstOffer.parameters["offerLocalUniqueId"])
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["showUid"] as? String), "16263774137415684001506002")
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["productId"] as? NSNumber), 1_712_454_293)
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["wareId"] as? String), "bYIJe1LcfoymvizKR6QDfw")
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["shopId"] as? NSNumber), 431_782)
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["feedId"] as? NSNumber), 475_690)
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["offerId"] as? String), "493303.000132.4600778002310")
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["price"] as? NSNumber), 1_149)
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["oldPrice"] as? NSNumber), 1_450)
        XCTAssertEqual(try XCTUnwrap(secondOffer.parameters["atSupplierWarehouse"] as? NSNumber), 0)
    }

    private func checkDefaultSKUCartButtonEvents() throws {
        ybm_wait {
            MetricRecorder.events(from: .appmetrica)
                .with(name: self.eventKey)
                .with(params: ["skuId": "101077347763"])
                .isNotEmpty
        }

        let skuOffer = try XCTUnwrap(
            MetricRecorder.events(from: .appmetrica)
                .with(name: eventKey)
                .with(params: ["skuId": "101077347763"])
                .first
        )
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["showUid"] as? String), "16302313087396618916700001")
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["productId"] as? NSNumber), 722_979_017)
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["wareId"] as? String), "KRAPKDZkKKimNA-4MVm-rA")
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["shopId"] as? NSNumber), 431_782)
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["supplierId"] as? NSNumber), 465_852)
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["feedId"] as? NSNumber), 475_690)
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["offerId"] as? String), "637707.000284.MGJK3RU/A")
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["price"] as? NSNumber), 81_990)
        XCTAssertEqual(try XCTUnwrap(skuOffer.parameters["atSupplierWarehouse"] as? NSNumber), 0)
    }

}
