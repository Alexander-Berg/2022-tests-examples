//
//  FiltersSubtests+developer.swift
//  UI Tests
//
//  Created by Aleksey Gotyanov on 10/23/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersSubtests {
    func developer(isVillageDeveloper: Bool = false) {
        let needCheckSamolet = isVillageDeveloper == false
        let isOnlySamolet = needCheckSamolet ? self.onlySamoletIsChecked() : false

        let runKey = #function + "_isVillageDeveloper: \(isVillageDeveloper), isOnlySamoletWasEnabled: \(isOnlySamolet)"
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'Застройщик'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: Self.developerCell(isVillageDeveloper))
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        if isVillageDeveloper {
            self.api.setupVillageDeveloperSearch()
        }
        else {
            self.api.setupDeveloperSearch()
        }

        self.selectDeveloper()

        if needCheckSamolet {
            self.assertThatOnlySamoletIsUnchecked()

            self.api.setupSamoletDeveloperSearch()
            self.selectSamoletDeveloper()
            self.assertThatOnlySamoletIsChecked()
        }

        self.clearSelectedDeveloper()
    }

    // MARK: Private

    private func selectDeveloper() {
        self.selectDeveloper(
            enterText: "Capital group",
            tapOn: "Capital Group",
            validateID: "268706"
        )
    }

    private func selectSamoletDeveloper() {
        self.selectDeveloper(
            enterText: "Самолет",
            tapOn: "Группа «Самолет»",
            validateID: "102320"
        )
    }

    private func selectDeveloper(
        enterText text: String,
        tapOn result: String,
        validateID id: String
    ) {
        let activityTitle = "Проверяем формирование запроса при выборе значения '\(result)' параметра 'Застройщик'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(
                    contain: [
                        URLQueryItem(name: "developerId", value: id),
                    ]
                ),
                handler: {
                    expectation.fulfill()
                }
            )
        }

        self.filtersSteps
            .tapOnDeveloper()
            .enter(developerName: text)
            .tapOnSearchResult(result)

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }

    private func clearSelectedDeveloper() {
        let activityTitle = "Проверяем формирование запроса при удалении выбранного значения 'Застройщик'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .queryItems(
                    notContain: [
                        URLQueryItem(name: "developerId", value: nil)
                    ]
                ),
                handler: {
                    expectation.fulfill()
                }
            )
        }

        self.filtersSteps
            .tapOnDeveloper()
            .clearSelectedValue()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }

    private func onlySamoletIsChecked() -> Bool {
        guard let isOnlySamolet = self.filtersSteps.boolParameterValue(cellAccessibilityIdentifier: Self.onlySamoletCell)
        else {
            XCTFail("unable to find cell 'ЖК от Самолет'")
            return false
        }

        return isOnlySamolet
    }

    private func assertThatOnlySamoletIsUnchecked() {
        XCTAssertFalse(self.onlySamoletIsChecked(), "'ЖК от Самолет' should be unchecked")
    }

    private func assertThatOnlySamoletIsChecked() {
        XCTAssertTrue(self.onlySamoletIsChecked(), "'ЖК от Самолет' should be checked")
    }

    private static func developerCell(_ isVillageDeveloper: Bool) -> String {
        if isVillageDeveloper {
            return "filters.cell.villageDeveloper"
        }
        else {
            return "filters.cell.developer"
        }
    }

    private static let onlySamoletCell = "filters.cell.onlySamolet"
}
