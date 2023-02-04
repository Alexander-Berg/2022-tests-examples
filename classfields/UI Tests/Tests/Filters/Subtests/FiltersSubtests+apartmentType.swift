//
//  FilterSubtests+apartmentType.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 07.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    enum ApartmentType {
        case all
        case newbuilding
        case secondary
    }

    func apartmentType(_ type: ApartmentType) {
        let activityTitle = "Меняем тип квартиры на '\(type.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(contain: type.queryItems),
                handler: { expectation.fulfill() }
            )
            self.filtersSteps.tapOnApartmentTypeButton(type)
        }

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.ApartmentType {
    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .all:
                identifier = "Все"
            case .newbuilding:
                identifier = "Новостройка"
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

            case .newbuilding:
                result.insert(URLQueryItem(name: "objectType", value: "NEWBUILDING"))

            case .secondary:
                result.insert(URLQueryItem(name: "objectType", value: "OFFER"))
                result.insert(URLQueryItem(name: "newFlat", value: "NO"))
        }
        return result
    }

    var readableName: String {
        switch self {
            case .all: return "Все"
            case .newbuilding: return "Новостройка"
            case .secondary: return "Вторичка"
        }
    }
}
