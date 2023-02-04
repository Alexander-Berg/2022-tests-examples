//
//  FiltersSubtests+buildingSeries.swift
//  UI Tests
//
//  Created by Timur Guliamov on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    func buildingSeries() {
        self.selectSeries()
        self.clearSelectedSeries()
    }

    // MARK: Private

    private func selectSeries() {
        self.selectSeries(enterText: "1-51", tapOn: "1-510", validateID: "663298")
    }

    private func selectSeries(enterText text: String, tapOn result: String, validateID id: String) {
        let activityTitle = "Проверяем формирование запроса при выборе значения '\(result)' параметра 'Серия дома'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(contain: [URLQueryItem(name: "buildingSeriesId", value: id)]),
                handler: { expectation.fulfill() }
            )
        }

        self.filtersSteps
            .tapOnBuildingSeries()
            .enter(developerName: text)
            .tapOnSearchResult(result)

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout * 2)
        XCTAssert(result)
    }

    private func clearSelectedSeries() {
        let activityTitle = "Проверяем формирование запроса при удалении выбранного значения 'Серия дома'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(notContain: [URLQueryItem(name: "buildingSeriesId", value: nil)]),
                handler: { expectation.fulfill() }
            )
        }

        self.filtersSteps
            .tapOnBuildingSeries()
            .clearSelectedValue()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout * 2)
        XCTAssert(result)
    }
}
