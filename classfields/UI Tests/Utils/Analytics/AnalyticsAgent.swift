//
//  AnalyticsAgent.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import Swifter
import XCTest

final class AnalyticsAgent {
    /// Shared instance is needed to store all events from app's start.
    static let shared: AnalyticsAgent = .init()

    func expect(event: MetricaAnalyticsEvent, times: Int = 1) -> XCTestExpectation {
        return self.metricaAnalytics.expect(event: event, times: times)
    }

    func expect(event: EventLogAnalyticsEvent, times: Int = 1) -> XCTestExpectation {
        return self.eventLogAnalytics.expect(event: event, times: times)
    }

    func expect(event: AdjustAnalyticsEvent, times: Int = 1) -> XCTestExpectation {
        return self.adjustAnalytics.expect(event: event, times: times)
    }

    func setUp(using dynamicStubs: HTTPDynamicStubs) {
        self.metricaAnalytics = .init(system: .metrica, dynamicStubs: dynamicStubs)
        self.eventLogAnalytics = .init(system: .eventLog, dynamicStubs: dynamicStubs)
        self.adjustAnalytics = .init(system: .adjust, dynamicStubs: dynamicStubs)
    }

    /// Remove all earlier received events and stop validate them.
    /// Call this when you need to check event with the same name in different places
    /// e.g., event "Позвонить" in Site Card and Offer Card.
    func removeAllPreviousEvents() {
        self.metricaAnalytics?.removeAllPreviousEvents()
        self.eventLogAnalytics?.removeAllPreviousEvents()
        self.adjustAnalytics?.removeAllPreviousEvents()
    }

    private var metricaAnalytics: Analytics<MetricaAnalyticsEvent>!
    private var eventLogAnalytics: Analytics<EventLogAnalyticsEvent>!
    private var adjustAnalytics: Analytics<AdjustAnalyticsEvent>!

    private init() {}
}
