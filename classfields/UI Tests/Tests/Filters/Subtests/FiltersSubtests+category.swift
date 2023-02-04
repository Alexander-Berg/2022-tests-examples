//
//  FiltersSubtests+category.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREFiltersModel
import XCTest

extension FiltersSubtests {
    enum Category {
        case apartment
        case room
        case house
        case lot
        case commercial
        case garage
    }

    func category(_ category: Category) {
        let activityTitle = "Меняем категорию недвижимости на '\(category.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            if self.filtersSteps.category(equalTo: category) {
                expectation.fulfill()
            }
            else {
                self.api.setupSearchCounter(
                    predicate: .contains(queryItem: URLQueryItem(name: "category", value: category.queryItemValue)),
                    handler: { expectation.fulfill() }
                )
                self.filtersSteps.switchToCategory(category)
            }
        }
        
        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.Category {
    var queryItemValue: String {
        switch self {
            case .apartment: return "APARTMENT"
            case .room: return "ROOMS"
            case .house: return "HOUSE"
            case .lot: return "LOT"
            case .commercial: return "COMMERCIAL"
            case .garage: return "GARAGE"
        }
    }

    var readableName: String {
        switch self {
            case .apartment: return "Квартиру"
            case .room: return "Комнату"
            case .house: return "Дом"
            case .lot: return "Участок"
            case .commercial: return "Коммерческую"
            case .garage: return "Гараж"
        }
    }

    var buttonTitle: String {
        switch self {
            case .apartment: return "Квартиру"
            case .room: return "Комнату"
            case .house: return "Дом"
            case .lot: return "Участок"
            case .commercial: return "Коммерческую"
            case .garage: return "Гараж"
        }
    }
}
