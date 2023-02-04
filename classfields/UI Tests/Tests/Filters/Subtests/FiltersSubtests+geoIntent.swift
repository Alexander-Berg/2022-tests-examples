//
//  FiltersSubtests+geoIntent.swift
//  UI Tests
//
//  Created by Aleksey Gotyanov on 10/23/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

extension FiltersSubtests.Subtest {
    struct GeoIntentOptions {
        enum Target: String {
            case plainGeoIntent
            case drawGeoIntent
            case commuteGeoIntent
        }

        let targets: Set<Target>

        var runKey: String {
            self.targets.map { $0.rawValue }.joined(separator: "_")
        }

        static func resetGeoIntentAlertShouldBeShown(for targets: Set<Target> = []) -> Self {
            Self.init(targets: targets)
        }

        func alertShouldBeShown(for target: Target) -> Bool {
            self.targets.contains(target)
        }
    }
}

extension FiltersSubtests {
    func geoIntent(_ options: Subtest.GeoIntentOptions) {
        let runKey = #function + "_\(options.runKey)"
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки выбора региона"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: FiltersGeoIntentAccessibilityIdentifiers.plainCellIdentifier)
                self.filtersSteps.isCellPresented(accessibilityIdentifier: FiltersGeoIntentAccessibilityIdentifiers.drawCellIdentifier)
                self.filtersSteps.isCellPresented(accessibilityIdentifier: FiltersGeoIntentAccessibilityIdentifiers.commuteCellIdentifier)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        self.checkPlainGeoIntent(options)
        self.checkDrawGeoIntent(options)
        self.checkCommuteGeoIntent(options)
    }

    // MARK: Private

    private func checkPlainGeoIntent(_ options: Subtest.GeoIntentOptions) {
        XCTContext.runActivity(named: "Проверка открытия экрана выбора гео") { _ -> Void in
            let resetGeoIntentAlertSteps = self.filtersSteps
                .tapOnPlainGeoIntentField()

            if options.alertShouldBeShown(for: .plainGeoIntent) {
                resetGeoIntentAlertSteps
                    .screenIsPresented()
                    .tapOnCancelAlertButton()
            }
            else {
                resetGeoIntentAlertSteps
                    .screenIsDismissed()

                GeoIntentSteps()
                    .tapOnBackButton()
            }
        }
    }

    private func checkDrawGeoIntent(_ options: Subtest.GeoIntentOptions) {
        XCTContext.runActivity(named: "Проверка открытия экрана для выбора гео по кнопке \"Нарисовать область на карте\"") { _ -> Void in
            let resetGeoIntentAlertSteps = self.filtersSteps
                .tapOnDrawGeoIntentButton()

            if options.alertShouldBeShown(for: .drawGeoIntent) {
                resetGeoIntentAlertSteps
                    .screenIsPresented()
                    .tapOnCancelAlertButton()
            }
            else {
                resetGeoIntentAlertSteps
                    .screenIsDismissed()

                DrawGeoIntentSteps()
                    .tapOnCloseButton()
            }
        }
    }

    private func checkCommuteGeoIntent(_ options: Subtest.GeoIntentOptions) {
        XCTContext.runActivity(named: "Проверка открытия экрана для выбора гео по кнопке \"Время на дорогу\"") { _ -> Void in
            let resetGeoIntentAlertSteps = self.filtersSteps
                .tapOnCommute()

            if options.alertShouldBeShown(for: .commuteGeoIntent) {
                resetGeoIntentAlertSteps
                    .screenIsPresented()
                    .tapOnCancelAlertButton()
            }
            else {
                resetGeoIntentAlertSteps.screenIsDismissed()

                CommuteSteps()
                    .pressBackButton()
            }
        }
    }
}
