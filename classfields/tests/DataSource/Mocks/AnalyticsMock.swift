//
//  AnalyticsMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 13.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAnalytics

final class AnalyticsMock: NSObject, AnalyticsProtocol {
    let report: XCTestExpectation = .init(description: "Did report")

    func report(_ event: YREAnalyticsEvent) {
        self.report.fulfill()
    }
}
