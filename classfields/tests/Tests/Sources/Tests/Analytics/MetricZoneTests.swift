@testable import AutoRuAnalyticsCore
import XCTest

final class LoggerSpy: AnalyticsLogger {
    private(set) var lastEvent = AnalyticsLoggerEvent(
        name: "",
        rawProperties: [:],
        userProperties: UserProperties()
    )

    func logEvent(_ event: AnalyticsLoggerEvent) {
        lastEvent = event
    }
    func logError(_ event: AnalyticsLoggerErrorEvent) {}
    func userPropertiesChanged(_ event: UserPropertiesChangedEvent) {}
}

struct TestEvent: PartialAnalyticsEvent {
    public var name = ["Event Name", "Event Subname"]
    public var properties: [String: EventProperty] {
        return ["key": "value"]
    }
}

final class MetricZoneTests: XCTestCase {
    private let loggerSpy = LoggerSpy()

    override func setUp() {
        super.setUp()
        Analytics.storage.loggers = [loggerSpy]
    }

    func test_singeComponentZone() {
        // arrange
        let metricZone = MetricZone("Zone 0")

        // action
        TestEvent().log(in: metricZone)

        // assert
        XCTAssertTrue(loggerSpy.lastEvent.name == "Zone 0. Event Name. Event Subname")

        guard let loggedPropeties = loggerSpy.lastEvent.rawProperties as? [String: String]
        else {
            XCTFail()
            return
        }
        XCTAssertEqual(loggedPropeties, ["key": "value"])
    }

    func test_multiZone() {
        // arrange
        let metricZone_0 = MetricZone("Zone 0")
        let metricZone_1 = MetricZone("Zone 1").in(metricZone_0)
        let metricZone_2 = MetricZone("Zone 2").in(metricZone_1)

        // action
        TestEvent().log(in: metricZone_2)

        // assert
        XCTAssertTrue(loggerSpy.lastEvent.name == "Zone 0. Zone 1. Zone 2. Event Name. Event Subname")

        guard let loggedPropeties = loggerSpy.lastEvent.rawProperties as? [String: String]
        else {
            XCTFail()
            return
        }

        XCTAssertEqual(loggedPropeties, ["key": "value"])
    }
}
