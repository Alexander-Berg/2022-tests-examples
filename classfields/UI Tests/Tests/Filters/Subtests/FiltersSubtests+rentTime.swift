//
//  FilterSubtests+rentTime.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 07.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    enum RentTime {
        case short
        case long
    }

    func rentTime(_ type: RentTime) {
        let activityTitle = "Меняем срок аренды на '\(type.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(contain: type.queryItems),
                handler: { expectation.fulfill() }
            )
            self.filtersSteps.tapOnRentTimeButton(type)
        }

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.RentTime {
    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .short:
                identifier = "Посуточно"
            case .long:
                identifier = "Длительно"
        }
        return "YRESegmentedControl-" + identifier
    }

    var queryItems: Set<URLQueryItem> {
        var result: Set<URLQueryItem> = []
        switch self {
            case .short:
                result.insert(URLQueryItem(name: "rentTime", value: "SHORT"))

            case .long:
                result.insert(URLQueryItem(name: "rentTime", value: "LARGE"))
        }
        return result
    }

    var readableName: String {
        switch self {
            case .short: return "Посуточная"
            case .long: return "Длительная"
        }
    }
}
