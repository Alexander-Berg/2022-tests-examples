import XCTest

@testable import BeruHealthMetrics

final class GlobalHealthMetricsEventHolder: XCTestCase {

    // MARK: - Base behaviour tests

    func test_shouldCreateNewEvent_whenRecreateCalled() {
        // given
        let holder = HealthMetricsEvent.Holder(event: HealthMetricsEvent(name: .cartCostIsZero, portion: .core))

        // when
        let firstChain = holder.recreate()
        let secondChain = holder.recreate()

        // then
        XCTAssertNotEqual(firstChain, secondChain)
    }

    func test_shouldDropCurrentEvent_whenCancelCalled() {
        // given
        let holder = HealthMetricsEvent.Holder(event: HealthMetricsEvent(name: .cartCostIsZero, portion: .core))

        // when
        holder.recreate()
        holder.cancel()

        // then
        XCTAssertNil(holder.event)
    }

    func test_shouldDropCurrentEvent_whenFinalized() {
        // given
        let holder = HealthMetricsEvent.Holder(event: HealthMetricsEvent(name: .cartCostIsZero, portion: .core))

        // when
        holder.recreate()
        holder.finalizeMetric()

        // then
        XCTAssertNil(holder.event)
    }

    func test_shouldNotHaveEvent_whenRecreationNotCalled() {
        // when
        let holder = HealthMetricsEvent.Holder(event: HealthMetricsEvent(name: .cartCostIsZero, portion: .core))

        // then
        XCTAssertNil(holder.event)
    }

    func test_shouldReturnLastCreatedChain_whenChainCalled() {
        // given
        let holder = HealthMetricsEvent.Holder(event: HealthMetricsEvent(name: .cartCostIsZero, portion: .core))

        // when
        let firstEvent = holder.recreate()
        let secondEvent = holder.event

        // then
        XCTAssertEqual(firstEvent, secondEvent)
    }

    func test_shouldSendChain_whenFinalized() {
        // given
        let stubEvent = StubEvent(name: .cartCostIsZero, portion: .core)
        let holder = HealthMetricsEvent.Holder(event: stubEvent)

        // when
        holder.recreate()
        holder.finalizeMetric()

        // then
        XCTAssertTrue(stubEvent.sendCalled)
    }
}

private class StubEvent: HealthMetricsEvent {

    var sendCalled = false

    override func send() {
        sendCalled = true
    }
}
