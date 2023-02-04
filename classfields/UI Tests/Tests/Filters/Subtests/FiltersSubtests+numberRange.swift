//
//  Created by Alexey Aleshkov on 24/07/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension FiltersSubtests {
    enum NumberRangeParameter: String {
        struct Value {
            let from: String
            let to: String
        }

        struct Configuration {
            let parameter: NumberRangeParameter
            let value: Value
            let expectationScreenshotID: String
        }

        // "Общая площадь"
        case totalArea
        // "Площадь кухни"
        case kitchenArea
        // "Площадь комнаты"
        case livingSpace
        // "Площадь дома"
        case houseArea
        // "Площадь дома" (Коттеджный поселок)
        case villageHouseArea
        // "Площадь участка"
        case lotArea
        // "Площадь участка с сепаратором"
        case lotAreaWithSeparator
        // "Площадь"
        case commercialArea
        // "Площадь участка"
        case commercialLotArea
        // "Этаж"
        case floor
        // "Год постройки"
        case builtYear
        // "Цена"
        case price
        // Этажей в доме
        case houseFloors
    }

    func numberRangeParameter(_ parameter: NumberRangeParameter,
                              _ value: NumberRangeParameter.Value,
                              _ expectationScreenshotID: String) {
        let runKey = #function + "_" + parameter.runKey
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки '\(parameter.readableName)'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: parameter.accessibilityIdentifier)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        // Test Close button for picker
        // @coreshock: Commented to speed-up testing
        // let pickerToClose = self.filtersSteps
        //    .openNumberRangePicker(accessibilityIdentifier: parameter.accessibilityIdentifier)
        // pickerToClose
        //    .isScreenPresented(with: parameter.readableName)
        //    .close()
        //    .isScreenClosed()

        let expectation = XCTestExpectation(description: "Проверка формирования запроса при задании всех значений в пикере '\(parameter.readableName)' главного фильтра.")

        let fromKey = parameter.isSuffixSearilizedParameter ? "\(parameter.queryItemKey)Min" : "min\(parameter.queryItemKey.capitalized)"
        let toKey = parameter.isSuffixSearilizedParameter ? "\(parameter.queryItemKey)Max" : "max\(parameter.queryItemKey.capitalized)"

        self.api.setupSearchCounter(
            predicate: .queryItems(
                contain: [
                    URLQueryItem(name: fromKey, value: value.from),
                    URLQueryItem(name: toKey, value: value.to),
                ]
            ),
            handler: {
                expectation.fulfill()
            }
        )

        let numberRangePicker = self.filtersSteps
            .openNumberRangePicker(for: parameter)
        numberRangePicker
            .isScreenPresented(with: parameter.readableName)
            .clear()
            .enter(.init(from: value.from, to: value.to))
            .apply()
            .isScreenClosed()

         self.filtersSteps.compareWithCellScreenshot(
             cellID: parameter.accessibilityIdentifier,
             screenshotID: expectationScreenshotID
         )

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.NumberRangeParameter {
    var runKey: String {
        self.rawValue
    }

    var accessibilityIdentifier: String {
        let identifier: String
        switch self {
            case .totalArea: identifier = "areaTotal"
            case .kitchenArea: identifier = "areaKitchen"
            case .livingSpace: identifier = "areaLiving"
            case .houseArea: identifier = "houseArea"
            case .villageHouseArea: identifier = "villageHouseArea"
            case .lotArea: identifier = "lotArea"
            case .lotAreaWithSeparator: identifier = "lotArea"
            case .commercialArea: identifier = "areaTotal"
            case .commercialLotArea: identifier = "lotArea"
            case .floor: identifier = "floor"
            case .builtYear: identifier = "buildYear"
            case .price: identifier = "price"
            case .houseFloors: identifier = "houseFloors"
        }
        return "filters.cell." + identifier
    }

    var queryItemKey: String {
        switch self {
            case .totalArea: return "area"
            case .kitchenArea: return "kitchenSpace"
            case .livingSpace: return "livingSpace"
            case .houseArea: return "area"
            case .villageHouseArea: return "houseArea"
            case .lotArea: return "lotArea"
            case .lotAreaWithSeparator: return "lotArea"
            case .commercialArea: return "area"
            case .commercialLotArea: return "lotArea"
            case .floor: return "floor"
            case .builtYear: return "builtYear"
            case .price: return "price"
            case .houseFloors: return "floors"
        }
    }

    var readableName: String {
        switch self {
            case .totalArea: return "Общая площадь"
            case .kitchenArea: return "Площадь кухни"
            case .livingSpace: return "Площадь комнаты"
            case .houseArea: return "Площадь дома"
            case .villageHouseArea: return "Площадь дома"
            case .lotArea: return "Площадь участка"
            case .lotAreaWithSeparator: return "Площадь участка"
            case .commercialArea: return "Площадь"
            case .commercialLotArea: return "Площадь участка"
            case .floor: return "Этаж"
            case .builtYear: return "Год постройки"
            case .price: return "Цена"
            case .houseFloors: return "Этажей в доме"
        }
    }

    fileprivate var isSuffixSearilizedParameter: Bool {
        switch self {
            case .totalArea,
                 .kitchenArea,
                 .livingSpace,
                 .houseArea,
                 .villageHouseArea,
                 .lotArea,
                 .lotAreaWithSeparator,
                 .commercialArea,
                 .commercialLotArea,
                 .floor,
                 .builtYear,
                 .price:
                return true

            case .houseFloors:
                return false
        }
    }
}
