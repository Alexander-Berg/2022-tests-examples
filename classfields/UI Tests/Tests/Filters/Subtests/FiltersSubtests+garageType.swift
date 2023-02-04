//
//  FilterSubtests+garageType.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 10.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    enum GarageType: CaseIterable {
        case box
        case garage
        case parkingPlace
    }


    func garageTypes() {
        for (index, item) in GarageType.allCases.enumerated() {
            let activityTitle = "Добавляем опцию '\(item.readableName)'"

            let expectation = XCTestExpectation(description: activityTitle)
            XCTContext.runActivity(named: activityTitle) { _ in
                self.checkGarageTypeButton(index: index, garageType: item, select: true, completion: {
                    expectation.fulfill()
                })
            }
            let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
            XCTAssert(result)
        }

        for (index, item) in GarageType.allCases.enumerated().reversed() {
            let activityTitle = "Убираем опцию '\(item.readableName)'"

            let expectation = XCTestExpectation(description: activityTitle)
            XCTContext.runActivity(named: activityTitle) { _ in
                self.checkGarageTypeButton(index: index, garageType: item, select: false, completion: {
                    expectation.fulfill()
                })
            }
            let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
            XCTAssert(result)
        }
    }

    // MARK: - Private

    private func checkGarageTypeButton(
        index: Int,
        garageType: GarageType,
        select: Bool,
        completion: @escaping () -> Void
    ) {
        let garageTypeQueryKey = "garageType"

        let selectedGarageTypes = GarageType.allCases.prefix(through: index + (select ? 0 : -1))
        let deselectedGarageTypes = GarageType.allCases.suffix(from: index + 1)

        let requiredQueryItems = selectedGarageTypes.flatMap { $0.queryItems }
        let prohibitedQueryItemsKeys = selectedGarageTypes.isEmpty ? [garageTypeQueryKey] : []
        let deselectedGarageTypeQueryItems = deselectedGarageTypes.flatMap { $0.queryItems }
        let prohibitedQueryItems = selectedGarageTypes.isEmpty ? [] : deselectedGarageTypeQueryItems

        self.api.setupSearchCounter(
            predicate: .queryItems(
                contain: Set(requiredQueryItems),
                notContainKeys: Set(prohibitedQueryItemsKeys),
                notContain: Set(prohibitedQueryItems)
            ),
            handler: completion
        )

        self.filtersSteps.tapOnGarageTypeButton(garageType)
    }
}

extension FiltersSubtests.GarageType {
    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .box:
                identifier = "Бокс"
            case .garage:
                identifier = "Гараж"
            case .parkingPlace:
                identifier = "Машиноместо"
        }
        return "YRESegmentedControl-" + identifier
    }

    var queryItems: Set<URLQueryItem> {
        var result: Set<URLQueryItem> = []
        switch self {
            case .box:
                result.insert(URLQueryItem(name: "garageType", value: "BOX"))

            case .garage:
                result.insert(URLQueryItem(name: "garageType", value: "GARAGE"))

            case .parkingPlace:
                result.insert(URLQueryItem(name: "garageType", value: "PARKING_PLACE"))
        }
        return result
    }

    var readableName: String {
        switch self {
            case .box: return "Бокс"
            case .garage: return "Гараж"
            case .parkingPlace: return "Машиноместо"
        }
    }
}
