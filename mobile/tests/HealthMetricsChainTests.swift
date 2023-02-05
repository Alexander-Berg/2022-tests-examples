import MarketUnitTestHelpers
import SwiftyJSON
import XCTest

@testable import BeruHealthMetrics
@testable import Metrics

final class HealthMetricsChainTests: XCTestCase {

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        MetricRecorder.isRecording = true
    }

    override func tearDown() {
        super.tearDown()
        MetricRecorder.isRecording = false
        MetricRecorder.clear()
    }

    // MARK: - Base behaviour

    func test_shouldSendProperParameters() {
        // given
        let event = HealthMetricsChain(name: HealthEventName("name", message: "message"), portion: .core)

        // when
        event.start()
        event.send()

        // then
        wait {
            MetricRecorder.events(from: .health)
                .with(name: event.name.value)
                .with(params: [
                    "requestId": event.requestID,
                    "duration": 0,
                    "startTime": event.timestamp.secondsSince1970,
                    "timestamp": event.timestamp.secondsSince1970,
                    "startTimeInMs": event.timestamp.millisecondsSince1970,
                    "timestampInMs": event.timestamp.millisecondsSince1970,
                    "level": event.level.value,
                    "portion": event.portion.value,
                    "type": event.type.value
                ])
                .isNotEmpty
        }
    }

    func test_shouldSendAdditionalUserInfoByDefault() {
        // given
        let provider = HealthEvent.ExternalDataProvider()
        provider.uuid = { "123123123" }
        provider.uid = { "qwerty" }
        provider.muid = { "test_muid" }

        let message = "test_message"

        let event = HealthMetricsChain(name: HealthEventName("name", message: message), portion: .core)
        event.externalDataProvider = provider

        // when
        event.start()
        event.send()

        // then
        var sentEvent: MetricRecorderEvent?
        wait {
            sentEvent = MetricRecorder.events.first { recordedEvent in
                guard let name = recordedEvent.parameters["name"] else { return false }
                return name == event.name.value
            }
            return sentEvent != nil
        }
        guard let sentEvent = sentEvent else {
            XCTFail("Event not sent")
            return
        }

        let info = JSON(sentEvent.parameters)["info"]
        XCTAssertEqual(info["uuid"].stringValue, provider.uuid())
        XCTAssertEqual(info["muid"].stringValue, provider.muid())
        XCTAssertEqual(info["uid"].stringValue, provider.uid())
        XCTAssertEqual(info["message"].stringValue, message)
    }

    func test_shouldSendAdditionalInfo_whenInfoAppended() {
        // given
        let provider = HealthEvent.ExternalDataProvider()
        provider.uuid = { "123123123" }
        provider.uid = { "qwerty" }
        provider.muid = { "test_muid" }

        let additionalInfoEntry_1 = (key: "some1", val: "qwery1")
        let additionalInfoEntry_2 = (key: "some2", val: "qwery2")

        let message = "test_message"

        let event = HealthMetricsChain(name: HealthEventName("name", message: message), portion: .core)
        event.externalDataProvider = provider

        // when
        event.start()
        event.addInfo(withKey: additionalInfoEntry_1.key, value: additionalInfoEntry_1.val)
        event.addInfo(withKey: additionalInfoEntry_2.key, value: additionalInfoEntry_2.val)
        event.send()

        // then
        var sentEvent: MetricRecorderEvent?
        wait {
            sentEvent = MetricRecorder.events.first { recordedEvent in
                guard let name = recordedEvent.parameters["name"] else { return false }
                return name == event.name.value
            }
            return sentEvent != nil
        }
        guard let sentEvent = sentEvent else {
            XCTFail("Event not sent")
            return
        }

        let info = JSON(sentEvent.parameters)["info"]
        XCTAssertEqual(info["uuid"].stringValue, provider.uuid())
        XCTAssertEqual(info["muid"].stringValue, provider.muid())
        XCTAssertEqual(info["uid"].stringValue, provider.uid())
        XCTAssertEqual(info["message"].stringValue, message)
        XCTAssertEqual(info[additionalInfoEntry_1.key].stringValue, additionalInfoEntry_1.val)
        XCTAssertEqual(info[additionalInfoEntry_2.key].stringValue, additionalInfoEntry_2.val)
    }

    func test_shouldAppendSuffixToName() {
        // given
        let suffix = "_MAIN_COMPONENT"

        // when
        let event = HealthMetricsChain(name: .screenOpened, portion: .cartScreen)

        // then
        XCTAssertTrue(event.name.value.hasSuffix(suffix))
    }

    // MARK: - Chain subevents tests

    func test_shouldUpdateSubEventRequestId_whenSubEventAdded() {
        // given
        let chain = HealthMetricsChain(name: .screenOpened, portion: .core)
        let subEvent = HealthMetricsEvent(name: .getUUID, portion: .core)
        let initialRequestId = subEvent.requestID

        // when
        chain.addEvent(subEvent)

        // then
        XCTAssertNotEqual(subEvent.requestID, initialRequestId)
        XCTAssertEqual(subEvent.requestID, chain.requestID)
    }

    func test_shouldUpdateSubEventStartTime_whenSubEventAddedAfterChainStart() {
        // given
        let chain = HealthMetricsChain(name: .screenOpened, portion: .core)
        let subEvent = HealthMetricsEvent(name: .getUUID, portion: .core)
        let initialStartTime = subEvent.startTime

        // when
        chain.start()
        chain.addEvent(subEvent)
        subEvent.send()
        chain.send()

        // then
        XCTAssertNotEqual(subEvent.startTime, initialStartTime)
        XCTAssertEqual(subEvent.startTime, chain.startTime)
    }

    func test_shouldUpdateSubEventStartTime_whenSubEventAddedBeforeChainStart() {
        // given
        let chain = HealthMetricsChain(name: .screenOpened, portion: .core)
        let subEvent = HealthMetricsEvent(name: .getUUID, portion: .core)
        let initialStartTime = subEvent.startTime

        // when
        chain.addEvent(subEvent)
        chain.start()
        subEvent.send()
        chain.send()

        // then
        XCTAssertNotEqual(subEvent.startTime, initialStartTime)
        XCTAssertEqual(subEvent.startTime, chain.startTime)
    }

    // MARK: - Pulse integration tests

    func test_shouldReportToPulseWithProperName() {
        // given
        let stubPulseReporter = StubPulseReporter()
        let event = HealthMetricsChain(name: .screenOpened, portion: .cartScreen)
        let properPulseName = event.name.value.replacingOccurrences(
            of: "_" + Cosntants.chainSuffix,
            with: "." + event.portion.value
        )
        event.pulseReporter = stubPulseReporter

        // when
        event.start()
        event.send()

        // then
        XCTAssertNotNil(stubPulseReporter.reportedMetrics.first { $0.name == properPulseName })
    }

    func test_shouldReportToPulseDurationInSeconds() {
        // given
        let stubPulseReporter = StubPulseReporter()
        let event = HealthMetricsChain(name: .screenOpened, portion: .cartScreen)
        event.pulseReporter = stubPulseReporter

        // when
        event.start()
        doHeavyOperation()
        event.send()

        // then
        guard let sentPulseEvent = stubPulseReporter.reportedMetrics.first else {
            XCTFail("Pulse metric should be sent")
            return
        }

        XCTAssertEqual(sentPulseEvent.duration, TimeInterval(event.durationInMs) / 1_000)
    }

    // MARK: - Private

    private func doHeavyOperation() {
        var counter = 0
        for _ in 0 ... 1_000_000 {
            for _ in 0 ... 1 {
                counter += 1
            }
        }
    }
}

// MARK: - Nested types

extension HealthMetricsChainTests {

    enum Cosntants {
        static let chainSuffix = "MAIN_COMPONENT"
    }

}

private class StubPulseReporter: PulseMetricsReporter {

    struct Event {
        let name: String
        let duration: TimeInterval
    }

    var reportedMetrics: [Event] = []

    override func reportMetric(name: String, duration: TimeInterval) {
        reportedMetrics.append(Event(name: name, duration: duration))
    }
}
