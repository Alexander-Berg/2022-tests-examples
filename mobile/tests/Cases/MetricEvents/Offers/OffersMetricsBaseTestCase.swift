import Metrics
import XCTest

class OffersMetricsBaseTestCase: LocalMockTestCase {

    // MARK: - Internal Methods

    func mockDefault() {
        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OffersMetrics")
        }
    }

    func check(parameters: [AnyHashable: AnyHashable], expectedParameters: [AnyHashable: AnyHashable]) throws {
        try expectedParameters.forEach {
            let value = try XCTUnwrap(parameters[$0.key])
            let expectedValue = try XCTUnwrap($0.value)
            XCTAssertEqual(
                value,
                expectedValue,
                "\($0.key) has unexpected value of \(value). Expected: \(expectedValue)"
            )
        }
        XCTAssertNotNil(parameters["offerLocalUniqueId"])
    }

    func getFirstEvent(with name: String, skuId: String) throws -> MetricRecorderEvent {
        try XCTUnwrap(MetricRecorder.events(from: .appmetrica).with(name: name).with(params: ["skuId": skuId]).first)
    }
}
