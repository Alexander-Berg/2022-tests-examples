import MarketUnitTestHelpers
import SwiftyJSON
import XCTest

@testable import BeruHealthMetrics
@testable import Metrics

final class HealthEventTests: XCTestCase {

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
        let event = HealthEvent(
            name: HealthEventName("testName"),
            portion: HealthEventPortion.trustScreen,
            level: HealthEventLevel.warning
        )

        // when
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

        let event = HealthEvent(
            name: HealthEventName("testName", message: message),
            portion: HealthEventPortion.trustScreen,
            level: HealthEventLevel.warning
        )
        event.externalDataProvider = provider

        // when
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

        let event = HealthEvent(
            name: HealthEventName("testName", message: message),
            portion: HealthEventPortion.trustScreen,
            level: HealthEventLevel.warning
        )
        event.externalDataProvider = provider

        // when
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

    func test_shouldBeEventWithTypeLog_whenDefaultEventCreated() {
        // given
        let logEventType = HealthEventType.log

        // when
        let event = HealthEvent(
            name: HealthEventName("testName"),
            portion: HealthEventPortion.trustScreen,
            level: HealthEventLevel.warning
        )

        // then
        XCTAssertEqual(event.type.value, logEventType.value)
    }
}
