//
//  FilterSubtests+villageOfferType.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    enum VillageOfferType: CaseIterable {
        case cottage
        case townhouse
        case land
    }

    func villageOfferTypes(types: [VillageOfferType], configurations: [PriceConfiguration]) {
        var selectedVillageOfferTypes = [VillageOfferType]()
        var deselectedVillageOfferTypes = VillageOfferType.allCases

        for item in types {
            let activityTitle = "Добавляем опцию '\(item.readableName)'"

            XCTContext.runActivity(named: activityTitle) { _ in
                let expectation = XCTestExpectation(description: activityTitle)

                self.checkVillageOfferTypeButton(
                    villageOfferType: item,
                    selectedVillageOfferTypes: &selectedVillageOfferTypes,
                    deselectedVillageOfferTypes: &deselectedVillageOfferTypes,
                    select: true,
                    completion: ({
                        expectation.fulfill()
                    })
                )

                let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
                XCTAssert(result)
            }
        }

        let skipPriceType = deselectedVillageOfferTypes.isEmpty
            || selectedVillageOfferTypes.isEmpty
            || deselectedVillageOfferTypes == [.cottage]
            || deselectedVillageOfferTypes == [.townhouse]

        self.price(configurations: configurations, skipPriceType: skipPriceType)

        for item in types.reversed() {
            let activityTitle = "Убираем опцию '\(item.readableName)'"

            XCTContext.runActivity(named: activityTitle) { _ in
                let expectation = XCTestExpectation(description: activityTitle)

                self.checkVillageOfferTypeButton(
                    villageOfferType: item,
                    selectedVillageOfferTypes: &selectedVillageOfferTypes,
                    deselectedVillageOfferTypes: &deselectedVillageOfferTypes,
                    select: false,
                    completion: ({
                        expectation.fulfill()
                    })
                )

                let result = XCTWaiter.yreWait(for: [expectation], timeout: 2 * Constants.timeout)
                XCTAssert(result)
            }
        }
    }

    // MARK: - Private

    private func checkVillageOfferTypeButton(
        villageOfferType: VillageOfferType,
        selectedVillageOfferTypes: inout [VillageOfferType],
        deselectedVillageOfferTypes: inout [VillageOfferType],
        select: Bool,
        completion: @escaping () -> Void
    ) {
        if select {
            selectedVillageOfferTypes.append(villageOfferType)
            deselectedVillageOfferTypes.removeAll { $0 == villageOfferType }
        }
        else {
            deselectedVillageOfferTypes.append(villageOfferType)
            selectedVillageOfferTypes.removeAll { $0 == villageOfferType }
        }

        let villageOfferTypeQueryKey = "villageOfferType"

        let requiredQueryItems = selectedVillageOfferTypes.map { $0.queryItem }
        let prohibitedQueryItemsKeys = selectedVillageOfferTypes.isEmpty ? [villageOfferTypeQueryKey] : []
        let deselectedVillageOfferTypeQueryItems = deselectedVillageOfferTypes.map { $0.queryItem }
        let prohibitedQueryItems = selectedVillageOfferTypes.isEmpty ? [] : deselectedVillageOfferTypeQueryItems

        self.api.setupSearchCounter(
            predicate: .queryItems(
                contain: Set(requiredQueryItems),
                notContainKeys: Set(prohibitedQueryItemsKeys),
                notContain: Set(prohibitedQueryItems)
            ),
            handler: completion
        )

        self.filtersSteps.tapOnVillageOfferTypeButton(villageOfferType)
    }
}

extension FiltersSubtests.VillageOfferType {
    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .cottage:
                identifier = "Дом"
            case .townhouse:
                identifier = "Таунхаус"
            case .land:
                identifier = "Участок"
        }
        return "YRESegmentedControl-" + identifier
    }

    var queryItem: URLQueryItem {
        var result: URLQueryItem
        switch self {
            case .cottage:
                result = URLQueryItem(name: "villageOfferType", value: "COTTAGE")
            case .townhouse:
                result = URLQueryItem(name: "villageOfferType", value: "TOWNHOUSE")
            case .land:
                result = URLQueryItem(name: "villageOfferType", value: "LAND")
        }
        return result
    }

    var readableName: String {
        switch self {
            case .cottage: return "Дом"
            case .townhouse: return "Таунхаус"
            case .land: return "Участок"
        }
    }
}
