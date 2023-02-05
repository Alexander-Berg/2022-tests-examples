import MarketUITestMocks
import Metrics
import XCTest

final class SKUHealthChainTests: LocalMockTestCase {

    func test_shouldSendSKUChain_whenSKUShown() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку цепочки SCREEN_OPENED_MAIN_COMPONENT.SKU_SCREEN")

        var sku: SKUPage!

        "Мокаем состояние".run {
            setupSKUInfoState()
        }

        "Открываем SKU".run {
            sku = goToDefaultSKUPage()
        }

        "Ждем появления экрана".run {
            ybm_wait(forFulfillmentOf: { sku.gallery.element.isVisible })
            sku.collectionView.ybm_swipeCollectionView(
                toFullyReveal: sku.addToCartButton.element,
                inset: sku.stickyViewInset,
                withVelocity: .slow
            )
        }

        guard
            let promoEvent = MetricRecorder.events(from: .health)
            .with(name: "SCREEN_OPENED_MAIN_COMPONENT")
            .with(params: ["portion": "SKU_SCREEN"])
            .first,
            let mainRequestId = promoEvent.parameters["requestId"] as? String
        else {
            XCTFail("Root event was not sent")
            return
        }

        "Проверяем отправленные метрики".ybm_run { _ in
            let requestMetrics = MetricRecorder.events(from: .health).with(name: "REQUEST")
            let requestEvents = requestMetrics.filter { event in
                guard
                    event.parameters["requestId"] as? String != nil,
                    let info = event.parameters["info"] as? [String: Any],
                    let marketReqId = info["xMarketRequestId"] as? String,
                    let urlString = info["url"] as? String,
                    let shortUrlString = info["shortUrl"] as? String,
                    let duration = event.parameters["duration"] as? Int
                else { return false }

                return marketReqId != "unknown" &&
                    URL(string: urlString) != nil &&
                    URL(string: shortUrlString) != nil &&
                    duration > 0
            }

            XCTAssertTrue(checkMetric(name: "DATA_LOAD", with: mainRequestId))
            XCTAssertTrue(checkMetric(name: "RENDER", with: mainRequestId))
            XCTAssertTrue(checkMetric(name: "SCREEN_INIT", with: mainRequestId))
            XCTAssertTrue(checkMetric(name: "BUILD_VIEW_MODELS", with: mainRequestId))
            XCTAssertFalse(requestEvents.isEmpty)
        }
    }
}

// MARK: - Helper Methods

private extension SKUHealthChainTests {

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(offer: .default)
        stateManager?.setState(newState: skuState)
    }

    func checkMetric(name: String, with requestId: String) -> Bool {
        guard
            let event = MetricRecorder.events(from: .health).with(name: name).first,
            let eventRequestId = event.parameters["requestId"] as? String
        else { return false }

        return requestId == eventRequestId
    }
}
