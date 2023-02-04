import MarketUnitTestHelpers
import SwiftyJSON
import XCTest

@testable import BeruHealthMetrics
@testable import Metrics

final class HealthMetricsEventTests: XCTestCase {

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        MetricRecorder.isRecording = true
        MetricRecorder.clear()
        HealthMetricsEvent.appLifecycleObserver.resignActiveDate = Date()
    }

    // MARK: - Base behaviour

    func test_shouldSendProperParameters() {
        // given
        let event = HealthMetricsEvent(name: HealthEventName("name", message: "message"), portion: .core)

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

        let event = HealthMetricsEvent(name: HealthEventName("name", message: message), portion: .core)
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

        let event = HealthMetricsEvent(name: HealthEventName("name", message: message), portion: .core)
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

    func test_shouldBeEventWithTypeMetrika_whenDefaultEventCreated() {
        // given
        let logEventType = HealthEventType.metrika

        // when
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // then
        XCTAssertEqual(event.type.value, logEventType.value)
    }

    func test_shouldCollectDuration_whenMeasuringHeavyOperation() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // when
        event.start()
        doHeavyOperation()
        event.send()

        // then
        XCTAssertTrue(event.durationInMs > 0)
    }

    func test_shouldCollectDurationInMS_whenMeasuringHeavyOperation() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // when
        let startDate = Date()
        event.start()
        doHeavyOperation()
        let endDate = Date()
        event.send()

        // then
        XCTAssertTrue(event.durationInMs > Int(endDate.timeIntervalSince(startDate)))
    }

    func test_shouldSendProperDuration_whenMeasuringHeavyOperation() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // when
        event.start()
        doHeavyOperation()
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

        let eventBody = JSON(sentEvent.parameters)
        XCTAssertEqual(eventBody["duration"].intValue, event.durationInMs)
    }

    func test_shouldCollectZeroDuration_whenMeasuringInstantOperation() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // when
        event.start()
        event.send()

        // then
        // 0 мс может не сработать на агентах
        XCTAssertLessThanOrEqual(event.durationInMs, 1)
    }

    func test_shouldSendProperStartTimeAndTimeStamp_whenDefaultCaseUsed() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

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

        let eventBody = JSON(sentEvent.parameters)
        XCTAssertEqual(eventBody["startTime"].int, event.startTime?.secondsSince1970)
        XCTAssertEqual(eventBody["timestamp"].intValue, event.timestamp.secondsSince1970)
        XCTAssertEqual(eventBody["startTimeInMs"].intValue, event.startTime?.millisecondsSince1970)
        XCTAssertEqual(eventBody["timestampInMs"].intValue, event.timestamp.millisecondsSince1970)
    }

    func test_shouldSendSameStartTimeAndTimestamp_whenNotInsideChain() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)

        // when
        event.start()
        event.send()

        // then
        XCTAssertEqual(event.startTime ?? Date(), event.timestamp)
    }

    func test_appResignActive() {
        // given
        let event = HealthMetricsEvent(name: .screenOpened, portion: .cartScreen)
        HealthMetricsEvent.appLifecycleObserver.resignActiveDate = Date(timeIntervalSinceNow: 100)

        // when
        event.start()
        event.send()

        // then
        XCTAssertTrue(MetricRecorder.events(from: .health).isEmpty)
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
