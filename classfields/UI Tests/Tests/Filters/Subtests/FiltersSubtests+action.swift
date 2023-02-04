//
//  FiltersSubtests+action.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREFiltersModel
import XCTest

extension FiltersSubtests {
    enum Action {
        case buy
        case rent
    }

    func action(_ action: Action) {
        let activityTitle = "Меняем тип сделки на '\(action.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            if self.filtersSteps.action(equalTo: action) {
                expectation.fulfill()
            }
            else {
                self.api.setupSearchCounter(
                    predicate: .contains(queryItem: URLQueryItem(name: "type", value: action.queryItemValue)),
                    handler: { expectation.fulfill() }
                )
                self.filtersSteps.switchToAction(action)
            }
        }
        
        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.Action {
    var queryItemValue: String {
        switch self {
            case .buy: return "SELL"
            case .rent: return "RENT"
        }
    }

    var readableName: String {
        switch self {
            case .buy: return "Купить"
            case .rent: return "Снять"
        }
    }

    var buttonTitle: String {
        switch self {
            case .buy: return "Купить"
            case .rent: return "Снять"
        }
    }
}
