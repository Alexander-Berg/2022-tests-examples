//
//  Created by Alexey Aleshkov on 11/09/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Swifter
import YREAccessibilityIdentifiers

extension FiltersSubtests {
    typealias CommercialType = FiltersCommercialTypePickerSteps.CommercialType

    private static let pickerName: String = "Объект (тип коммерческой недвижимости)"
    private static let fieldName: String = "commercialType"

    func commercialTypeCancellation() {
        XCTContext.runActivity(named: "Проверка кнопки 'Отмена' на пикере '\(Self.pickerName)'", block: { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: Self.commercialTypeCell
            )
            let previousLabels = cell.staticTexts.allElementsBoundByIndex.map({ $0.label })

            let picker = self.filtersSteps
                .tapOnCommercialTypeCell()
            picker
                .isScreenPresented()
                .select(commercialType: .land)
                .tapOnCloseButton()
                .isScreenClosed()

            let currentLabels = cell.staticTexts.allElementsBoundByIndex.map({ $0.label })

            XCTAssert(previousLabels == currentLabels)
        })
    }

    func commercialType(_ options: CommercialType) {
        let label = options.readableLabel

        let expectation = XCTestExpectation(
            description: "Проверка формирования запроса для выбранного значения"
            + " '\(label)' в пикере '\(Self.pickerName)' главного фильтра."
        )

        let fieldName = Self.fieldName
        let predicate: Predicate<HttpRequest>
        switch options {
            case .noMatter:
                predicate = .queryItems(notContainKeys: [fieldName])

            case .land, .others:
                let queryValues = options.queryValues ?? []
                let queryItems = queryValues.map({ URLQueryItem(name: fieldName, value: $0) })
                let absentQueryValues = CommercialType.allQueryValues.subtracting(queryValues)
                let absentQueryItems = absentQueryValues.map({ URLQueryItem(name: fieldName, value: $0) })
                predicate = .queryItems(contain: Set(queryItems), notContain: Set(absentQueryItems))
        }

        let responseHandler: () -> Void = {
            expectation.fulfill()
        }

        // Stub for parameter value expectation
        self.api.setupSearchCounter(
            predicate: predicate,
            handler: responseHandler
        )

        XCTContext.runActivity(
            named: "Проверяем, что выбранное значение '\(label)' передается в поисковый запрос по нажатию на 'Готово'",
            block: { _ -> Void in
                let picker = self.filtersSteps
                    .tapOnCommercialTypeCell()

                picker
                    .isScreenPresented()
                    .select(commercialType: options)
                    .tapOnApplyButton()
                    .isScreenClosed()

                let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
                XCTAssert(result)
            }
        )

        XCTContext.runActivity(
            named: "Проверяем, что отображаемое значение фильтра поменялось на '\(label)'",
            block: { _ -> Void in
                // Invalid query - string identifier '...' exceeds maximum length of 128 characters.
                let cell = ElementsProvider.obtainElement(
                    identifier: Self.commercialTypeCell
                )
                cell.yreEnsureExistsWithTimeout()
                let predicate = NSPredicate(format: "label == %@", label)
                let label = cell.staticTexts.element(matching: predicate)
                XCTAssert(label.exists)
            }
        )
    }

    private static let commercialTypeCell = "filters.cell.commercialType"
}

extension FiltersSubtests.CommercialType {
    var readableLabel: String {
        switch self {
            case .noMatter, .land:
                return self.readableValue

            case .others(let values):
                let texts = values.sorted().map({ $0.readableValue })
                let result = texts.joined(separator: ", ")
                return result
        }
    }
}

extension FiltersSubtests.CommercialType {
    static var allQueryValues: Set<String> {
        let types: [FiltersSubtests.CommercialType] = [
            .land,
            .others(FiltersSubtests.CommercialType.OtherType.allCases),
        ]
        let values = types.flatMap({ $0.queryValues ?? [] })
        let distinctValues = Set(values)
        return distinctValues
    }

    var queryValues: [String]? {
        switch self {
            case .noMatter: return nil
            case .land: return ["LAND"]

            case .others(let values):
                let items = values.map({ value -> String in
                    switch value {
                        case .office: return "OFFICE"
                        case .retail: return "RETAIL"
                        case .freePurpose: return "FREE_PURPOSE"
                        case .warehouse: return "WAREHOUSE"
                        case .manufacturing: return "MANUFACTURING"
                        case .publicCatering: return "PUBLIC_CATERING"
                        case .autoRepair: return "AUTO_REPAIR"
                        case .hotel: return "HOTEL"
                        case .business: return "BUSINESS"
                        case .legalAddress: return "LEGAL_ADDRESS"
                    }
                })
                return items
        }
    }
}
