import MarketUITestMocks
import Metrics
import UIUtils
import XCTest

class OpenScreenMetricsTests: LocalMockTestCase {
    func testShouldSendMetrics_SKU() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Открытие экрана")
        Allure.addTitle("Страничка SKU")

        var sku: SKUPage!
        let backButton = NavigationBarPage.current.backButton

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.withAlternativeOffer))
            stateManager?.setState(newState: skuState)
        }

        "Мокаем ответы".run {
            mockStateManager?.pushState(bundleName: "OpenScreenMetricsTests_SKU")
        }

        "Открываем карточку товара".run {
            sku = goToDefaultSKUPage()
        }

        "Открываем предложения от всех продавцов".run {
            let showAllButton = sku.alternativeOffers.showAllButton
            sku.collectionView.swipe(to: .down, untilVisible: showAllButton)
            showAllButton.tap()
            wait(forInvisibilityOf: showAllButton)
        }

        "Идем обратно".run {
            backButton.tap()
            wait(forVisibilityOf: sku.element)
        }

        "Открываем характеристики".run {
            sku.collectionView.swipe(
                to: .down,
                untilVisible: sku.specsDetailsButton.element,
                times: 15,
                avoid: [
                    .keyboard,
                    .navigationBar,
                    .other(element: CompactOfferViewPage.current.element, edge: .maxYEdge)
                ]
            )
            let specs = sku.specsDetailsButton.tap()
            wait(forVisibilityOf: specs.element)
        }

        try "Чекаем метрики".run {
            try checkOpenScreenEvent(name: "SKU")
            try checkOpenScreenEvent(name: "SKU_ALL-OFFERS")
            try checkOpenScreenEvent(name: "CHARACTERISTICS")
        }
    }

    func testShouldSendMetrics_Search() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Открытие экрана")
        Allure.addTitle("Поиск")

        var search: SearchPage!
        var feed: FeedPage!

        "Открываем поисковый экран".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            let morda = root.tabBar.mordaPage
            search = morda.searchButton.tap()
        }

        "Мокаем состояние введенного текста \"красный\" и выдачу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OpenScreenMetricsTests_Search")
        }

        "Вводим текст \"красный\" и переходим на выдачу".ybm_run { _ in
            search.navigationBar.searchTextField.ybm_clearAndEnterText("красный" + "\n")

            ybm_wait(forFulfillmentOf: { FeedPage.current.element.isVisible })
            feed = FeedPage.current
        }

        "Свапаем уточнения категории".ybm_run { _ in
            let indexPath = IndexPath(item: 2, section: 0)
            let clarifyCategory = feed.collectionView.categoriesCollectionView.cellElement(at: indexPath)
            feed.collectionView.categoriesCollectionView.element.swipe(to: .right, untilVisible: clarifyCategory)
        }

        try "Чекаем метрики".run {
            try checkOpenScreenEvent(name: "SEARCH")
            try checkOpenScreenEvent(name: "SEARCH_RESULT")

        }
    }

    // MARK: - Private methods

    @discardableResult
    private func checkOpenScreenEvent(name: String) throws -> MetricRecorderEvent {
        try XCTUnwrap(
            MetricRecorder.events(from: .appmetrica).with(name: "OPEN-PAGE_VISIBLE")
                .with(params: ["pageName": name]).first
        )
    }
}

private extension CustomSKUConfig {
    static let withAlternativeOffer = modify(
        CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
    ) {
        $0.alternativeOfferIds = ["bNolttGchovLo4iQr7Q6cA"]
        $0.specs = .pharma
    }

}
