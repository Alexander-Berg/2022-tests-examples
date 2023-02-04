//
//  FiltersSubtests+onlySamolet.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.07.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    func onlySamolet() {
        let runKey = #function
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'ЖК от Самолет'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                let accessibilityIdentifier = "filters.cell.onlySamolet"
                self.filtersSteps.isCellPresented(accessibilityIdentifier: accessibilityIdentifier)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        let activityTitle = "Проверяем формирование запроса при переключении параметра 'ЖК от Самолет'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(
                    contain: [
                        URLQueryItem(name: "onlySamolet", value: "YES"),
                        URLQueryItem(name: "developerId", value: "102320"),
                    ]
                ),
                handler: {
                    expectation.fulfill()
                }
            )
        }

        self.filtersSteps.tapOnBoolParameterCell(accessibilityIdentifier: "filters.cell.onlySamolet")

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}
