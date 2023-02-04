//
//  FiltersSubtests+metroDistance.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 19.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

extension FiltersSubtests {
    enum TimeToMetroOption {
        case empty
        case option_5
        case option_10
        case option_15
        case option_20
        case option_25
        case option_30
        case option_45
        case option_60
    }

    func metroDistance(options: Set<TimeToMetroOption>) {
        let runKey = #function
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'Время до метро'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: Self.metroDistanceCell)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        var prevItem = TimeToMetroOption.empty
        var prevDistanceType: FiltersMetroDistancePickerSteps.DistanceType = .byFoot

        XCTContext.runActivity(named: "Проверка кнопки 'Отмена' на пикере 'Время до метро'") { _ -> Void in
            let picker = self.filtersSteps
                .tapOnMetroDistanceCell()
            picker
                .isPickerPresented()
                .tapOnRow(TimeToMetroOption.option_60.readableValue)
                .tapOnCloseButton()
                .isPickerClosed()
            self.filtersSteps
                .singleSelectParameter(with: Self.metroDistanceCell, hasValue: TimeToMetroOption.empty.readableValue)
        }

        [FiltersMetroDistancePickerSteps.DistanceType.byFoot, .transport].forEach { distanceType in
            XCTContext.runActivity(
                named: "Проверяем опции для времени до метро '\(distanceType.readableValue)'"
            ) { _ -> Void in
                self.selectOptions(options: options,
                                   for: distanceType,
                                   prevItem: &prevItem,
                                   prevDistanceType: &prevDistanceType)
            }
        }
    }

    private func selectOptions(options: Set<TimeToMetroOption>,
                               for distanceType: FiltersMetroDistancePickerSteps.DistanceType,
                               prevItem: inout TimeToMetroOption,
                               prevDistanceType: inout FiltersMetroDistancePickerSteps.DistanceType) {
        for item in options {
            let expectation = XCTestExpectation(
                description: "Проверка формирования запроса для выбранного значения"
                + " '\(item.readableValue) пешком' в пикере 'Время до метро' главного фильтра."
            )

            let expectedReadableValue = item != TimeToMetroOption.empty
                ? "\(item.readableValue) \(distanceType.readableValue)"
                : "\(item.readableValue)"
            let predicate: Predicate<HttpRequest>

            if item == TimeToMetroOption.empty {
                predicate = .queryItems(notContainKeys: ["timeToMetro", "metroTransport"])
            }
            else {
                let timeToMetroItem = URLQueryItem(name: "timeToMetro", value: item.queryValue)
                let metroTransportItem = URLQueryItem(name: "metroTransport", value: distanceType.queryValue)

                let isPrevItemNotEmpty = prevItem != TimeToMetroOption.empty
                let notContaints: Set<URLQueryItem> = isPrevItemNotEmpty
                    ? [URLQueryItem(name: "timeToMetro", value: prevItem.queryValue)]
                    : []
                predicate = .queryItems(contain: [timeToMetroItem, metroTransportItem], notContain: notContaints)
            }

            let responseHandler: () -> Void = {
                expectation.fulfill()
            }

            // Stub for parameter value expection
            self.api.setupSearchCounter(
                predicate: predicate,
                handler: responseHandler
            )

            XCTContext.runActivity(
                named: "Проверяем, что выбранное значение '\(expectedReadableValue)' передается в поисковый запрос по нажатию на 'Готово'"
            ) { _ -> Void in
                let picker = self.filtersSteps
                    .tapOnMetroDistanceCell()

                picker
                    .isPickerPresented()
                    // FIXME: https://st.yandex-team.ru/VSAPPS-8876
//                    .compareWithScreenshot(identifier: prevDistanceType.screenShotPrefix + prevItem.queryValue)
                    .switchTo(distanceType: distanceType)

                prevDistanceType = distanceType

                picker
                    .tapOnRow(item.readableValue)
                    // FIXME: https://st.yandex-team.ru/VSAPPS-8876
//                    .compareWithScreenshot(identifier: distanceType.screenShotPrefix + item.queryValue)
                    .tapOnApplyButton()
                let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
                XCTAssert(result)
            }

            XCTContext.runActivity(
                named: "Проверяем, что отображаемое значение фильтра поменялось на '\(expectedReadableValue)'"
            ) { _ -> Void in
                self.filtersSteps
                    .singleSelectParameter(with: Self.metroDistanceCell, hasValue: expectedReadableValue)
            }

            prevItem = item
        }
    }

    private static let metroDistanceCell = "filters.cell.timeToMetro"
}

extension FiltersMetroDistancePickerSteps.DistanceType {
    fileprivate var queryValue: String {
        switch self {
            case .byFoot:       return "ON_FOOT"
            case .transport:    return "ON_TRANSPORT"
        }
    }

    fileprivate var screenShotPrefix: String {
        switch self {
            case .byFoot:       return "ON_FOOT_"
            case .transport:    return "ON_TRANSPORT_"
        }
    }

    fileprivate var  readableValue: String {
        switch self {
            case .byFoot:       return "пешком"
            case .transport:    return "на транспорте"
        }
    }
}

extension FiltersSubtests.TimeToMetroOption {
    var readableValue: String {
        switch self {
            case .empty:     return "Неважно"
            case .option_5:  return "5 минут"
            case .option_10: return "10 минут"
            case .option_15: return "15 минут"
            case .option_20: return "20 минут"
            case .option_25: return "25 минут"
            case .option_30: return "30 минут"
            case .option_45: return "45 минут"
            case .option_60: return "1 час"
        }
    }

    var queryValue: String {
        switch self {
            case .empty:     return ""
            case .option_5:  return "5"
            case .option_10: return "10"
            case .option_15: return "15"
            case .option_20: return "20"
            case .option_25: return "25"
            case .option_30: return "30"
            case .option_45: return "45"
            case .option_60: return "60"
        }
    }
}
