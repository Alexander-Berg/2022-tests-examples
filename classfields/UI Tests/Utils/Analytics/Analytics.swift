//
//  Analytics.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import Combine
import YRECoreUtils

final class Analytics<Event: AnalyticsEvent & Equatable> {
    init(
        system: AnalyticsSystem<Event>,
        dynamicStubs: HTTPDynamicStubs
    ) {
        self.system = system
        self.dynamicStubs = dynamicStubs
        self.matchedPairs = []
        self.recordedEvents = []
        self.cancelBag = []

        self.registerEvents()
    }

    func expect(event: Event, times: Int) -> XCTestExpectation {
        let predicate = Predicate.match(event: event)

        let description = """
        Отправка события в \(self.system.name).
        Событие: \(event.description)
        Количество: \(times)
        """

        let expectation = XCTestExpectation(description: description)
        expectation.expectedFulfillmentCount = times
        expectation.assertForOverFulfill = true

        self.$recordedEvents
            .sink(receiveValue: { [weak self, weak expectation] receivedEvents in
                guard let strongSelf = self else { return }

                var matchedPairs: [MatchedPair] = []
                for (index, event) in receivedEvents.enumerated() {
                    let matchedEvent = MatchedPair.MatchedEvent(index: index, event: event)
                    let pair = MatchedPair(expectation: expectation, matchedEvent: matchedEvent)

                    guard predicate.matches(event), !strongSelf.matchedPairs.contains(pair) else { continue }

                    matchedPairs.append(pair)
                }

                guard matchedPairs.isNotEmpty, let expectation = expectation else { return }

                matchedPairs.forEach({ pair in
                    let event = pair.matchedEvent.event
                    let activityName = "Событие \"\(event.name)\" удовлетворяет предикату \(predicate.description)"

                    XCTContext.runActivity(named: activityName) { _ in
                        expectation.fulfill()

                        strongSelf.matchedPairs.append(pair)
                    }
                })
            })
            .store(in: &self.cancelBag)

        return expectation
    }

    func removeAllPreviousEvents() {
        self.recordedEvents.removeAll()
        self.matchedPairs.removeAll()
        self.cancelBag.removeAll()
    }

    // MARK: - Private

    private struct MatchedPair: Equatable {
        /// We need index of event because the same event can occur more than once and we need count all of them.
        struct MatchedEvent: Equatable {
            let index: Int
            let event: Event
        }

        weak var expectation: XCTestExpectation?
        let matchedEvent: MatchedEvent
    }

    private let system: AnalyticsSystem<Event>
    @Published
    private var recordedEvents: [Event]
    private var matchedPairs: [MatchedPair]
    private let dynamicStubs: HTTPDynamicStubs
    private var cancelBag: CancelBag

    func registerEvents() {
        self.system.setup(self.dynamicStubs, { [weak self] event in
            guard let strongSelf = self else { return }
            strongSelf.recordedEvents.append(event)
        })
    }
}

struct AnalyticsSystem<Event: AnalyticsEvent> {
    let name: String
    let setup: (HTTPDynamicStubs, @escaping (Event) -> Void) -> Void

    static var metrica: AnalyticsSystem<MetricaAnalyticsEvent> {
        .init(
            name: "Metrica",
            setup: AnalyticsAPIStubConfigurator.setupMetrica
        )
    }

    static var eventLog: AnalyticsSystem<EventLogAnalyticsEvent> {
        .init(
            name: "Event Log",
            setup: AnalyticsAPIStubConfigurator.setupEventLog
        )
    }

    static var adjust: AnalyticsSystem<AdjustAnalyticsEvent> {
        .init(
            name: "Adjust",
            setup: AnalyticsAPIStubConfigurator.setupAdjust
        )
    }
}
