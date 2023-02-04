//
//  FiltersSubtests+objectType.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 10.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    enum ObjectType {
        case all
        case village
        case secondary
    }

    func objectType(_ type: ObjectType) {
        let activityTitle = "Меняем тип объекта (участка или дома) на '\(type.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(contain: type.queryItems),
                handler: { expectation.fulfill() }
            )
            self.filtersSteps.tapOnObjectTypeButton(type)
        }

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.ObjectType {
    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .all:
                identifier = "Все"
            case .village:
                identifier = "Коттеджный пос."
            case .secondary:
                identifier = "Вторичка"
        }
        return "YRESegmentedControl-" + identifier
    }

    var queryItems: Set<URLQueryItem> {
        var result: Set<URLQueryItem> = []
        switch self {
            case .all:
                result.insert(URLQueryItem(name: "objectType", value: "OFFER"))

            case .village:
                result.insert(URLQueryItem(name: "objectType", value: "VILLAGE"))

            case .secondary:
                result.insert(URLQueryItem(name: "objectType", value: "OFFER"))
                result.insert(URLQueryItem(name: "primarySale", value: "NO"))
        }
        return result
    }

    var readableName: String {
        switch self {
            case .all: return "Все"
            case .village: return "Коттеджный пос."
            case .secondary: return "Вторичка"
        }
    }
}
