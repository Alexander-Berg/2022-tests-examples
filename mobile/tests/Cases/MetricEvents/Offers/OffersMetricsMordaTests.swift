import Metrics
import XCTest

class OffersMetricsMordaTests: OffersMetricsBaseTestCase {

    func testShouldSendOffersMetricsOnMorda() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("События с офферами")
        Allure.addTitle("Отправка продуктовых метрик сязанных с офферами с морды")

        var morda: MordaPage!

        mockDefault()

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
            wait(forExistanceOf: morda.historyWidget.container.element)
        }

        "Клацаем по сниппету и возвращаемся".ybm_run { _ in
            let snippet = morda.historyWidget.container.cellPage(at: .init(row: .zero, section: .zero))
            let skuPage = snippet.tap()
            wait(forExistanceOf: skuPage.element)
            skuPage.navigationBar.backButton.tap()
        }

        try "Чекаем отправку метрик".ybm_run { _ in
            try checkEvents()
        }
    }

    // MARK: - Private Methods

    private func checkEvents() throws {
        let events = MetricRecorder.events(from: .appmetrica)
            .with(name: "CMS-PAGE_SCROLLBOX_SNIPPET_OFFER_SHOW_VISIBLE")

        let proteinOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100902560734" })
        try check(parameters: proteinOffer.parameters, expectedParameters: [
            "productId": 658_172_022,
            "modelId": 658_172_022,
            "wareId": "s0TMPCGvs9JIraw-0zZIRQ",
            "price": 1_190
        ])

        let secondProteinOffer = try XCTUnwrap(events.first { $0.parameters["skuId"] as? String == "100953490814" })
        try check(parameters: secondProteinOffer.parameters, expectedParameters: [
            "productId": 668_278_053,
            "modelId": 668_278_053,
            "wareId": "zm8FK4B__0g6HVlNGQcSaA",
            "price": 840
        ])

        let event = try XCTUnwrap(
            MetricRecorder.events(from: .appmetrica)
                .with(name: "CMS-PAGE_SCROLLBOX_SNIPPET_OFFER_SHOW_NAVIGATE").first
        )
        try check(parameters: event.parameters, expectedParameters: [
            "skuId": "100902560734",
            "productId": 658_172_022,
            "modelId": 658_172_022,
            "price": 1_190
        ])
    }

}
