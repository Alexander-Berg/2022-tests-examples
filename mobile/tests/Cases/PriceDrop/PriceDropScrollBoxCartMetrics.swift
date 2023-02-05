import MarketUITestMocks
import Metrics
import XCTest

final class PriceDropScrollBoxCartMetrics: LocalMockTestCase {

    override func setUp() {
        super.setUp()

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.addToCartEnablePriceDropFeature)
        enable(toggles: FeatureNames.mordaRedesign)
        disable(toggles: FeatureNames.cartRedesign)
        var defaultState = DefaultState()
        defaultState.setExperiments(experiments: [.priceDropExp])
        stateManager?.setState(newState: defaultState)
    }

    func testCartScrollboxWidgetsAppearance() {
        Allure.addEpic("Корзина скроллбокс прайсдропа")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем метрику прайсдропа в скроллбоксе в корзине")

        // Вспомогательные функции

        var cartPage: CartPage!
        var priceDropPopup: PriceDropPopupPage!
        var snippetPage: PriceDropPopupSnippetPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "PriceDropCartScrollbox")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.compactSummary.element)
        }

        "Проверяем виджет метрику открытия попапа пд".ybm_run { _ in
            MetricRecorder.clear()
            let title = cartPage.priceDropWidget.title
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: title)
            let showMoreButton = cartPage.priceDropWidget.showMoreButton
            showMoreButton.tap()
            priceDropPopup = PriceDropPopupPage.currentPopup
            wait(forVisibilityOf: priceDropPopup.element)
            ybm_wait {
                let openEvents = MetricRecorder.events(from: .appmetrica).with(name: "PRICE_DROP_LANDING_VISIBLE")
                let visibleEvents = MetricRecorder.events(from: .appmetrica)
                    .with(name: "PRICE_DROP_LANDING_SNIPPET_OFFER_VISIBLE")
                    .with(params: [
                        "skuId": "100395908063",
                        "price": 258,
                        "oldPrice": 358,
                        "merchPrice": 272,
                        "index": 1
                    ])
                return visibleEvents.count == 1 && openEvents.count == 1
            }
        }

        "Добавление в корзину и переход по сниппету метрика".ybm_run { _ in
            MetricRecorder.clear()
            snippetPage = priceDropPopup.collectionView.cellPage(at: IndexPath(item: 1, section: 2))
            _ = snippetPage.cartButton.tap()
            let addEvent = MetricRecorder.events(from: .appmetrica).with(name: "PRICE_DROP_LANDING_SNIPPET_OFFER_CLICK")
            XCTAssertEqual(addEvent.count, 1)
            _ = snippetPage.tap()
            ybm_wait {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "PRICE_DROP_LANDING_SNIPPET_OFFER_NAVIGATE")
                    .with(params: [
                        "skuId": "100395908063",
                        "price": 258,
                        "oldPrice": 358,
                        "merchPrice": 272,
                        "index": 1
                    ]).count == 1

            }
        }
    }
}
