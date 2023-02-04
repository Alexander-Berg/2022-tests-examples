//
//  Created by Alexey Aleshkov on 14/08/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import Swifter

extension FiltersSubtests {
    enum MultiParameter {
        struct ValueItem: Equatable {
            let readableName: String
            let queryItemValue: String
        }

        case infrastructure
        case taxationForm
        case lotType
    }

    func multiParameter(_ parameter: MultiParameter) {
        let runKey = #function + "_" + parameter.queryItemKey
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейк для мультипараметра '\(parameter.readableName)'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                parameter.items.forEach { item in
                    XCTContext.runActivity(named: "Проверяем наличие ячейки '\(item.readableName)'") { _ -> Void in
                        self.filtersSteps.isCellPresented(containing: item.readableName)
                    }
                }
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        let selectItemsTitle = "Проверяем формирование запроса при установке значений мультипараметра '\(parameter.readableName)'."
        XCTContext.runActivity(named: selectItemsTitle, block: { _ in
            let selectItemsExpectation = XCTestExpectation(description: selectItemsTitle)

            self.tapItems(
                items: parameter.items,
                parameter: parameter,
                expectation: selectItemsExpectation,
                title: { "Проверяем формирование запроса при установке значения '\($0)'." },
                predicate: { .contains(queryItem: $0) }
            )

            let result = XCTWaiter.yreWait(for: [selectItemsExpectation], timeout: Constants.timeout)
            XCTAssert(result)
        })

        let deselectItemsTitle = "Проверяем формирование запроса при удалении значений мультипараметра '\(parameter.readableName)'."
        XCTContext.runActivity(named: deselectItemsTitle, block: { _ in
            let deselectItemsExpectation = XCTestExpectation(description: deselectItemsTitle)

            self.tapItems(
                items: parameter.items,
                parameter: parameter,
                expectation: deselectItemsExpectation,
                title: { "Проверяем формирование запроса при удалении значения '\($0)'." },
                predicate: { .notContains(queryItem: $0) }
            )

            let result = XCTWaiter.yreWait(for: [deselectItemsExpectation], timeout: Constants.timeout)
            XCTAssert(result)
        })
    }

    private func tapItems(
        items: [MultiParameter.ValueItem],
        parameter: MultiParameter,
        expectation: XCTestExpectation,
        title: @escaping (String) -> String,
        predicate: @escaping (URLQueryItem) -> Predicate<HttpRequest>
    ) {
        items.forEach({ item in
            let selectItemTitle = title(item.readableName)
            XCTContext.runActivity(named: selectItemTitle, block: { _ in
                let selectItemExpectation = XCTestExpectation(description: selectItemTitle)

                self.api.setupSearchCounter(
                    predicate: predicate(URLQueryItem(name: parameter.queryItemKey, value: item.queryItemValue)),
                    handler: { selectItemExpectation.fulfill() }
                )
                self.filtersSteps.tapOnBoolParameterCell(containing: item.readableName)

                let result = XCTWaiter.yreWait(for: [selectItemExpectation], timeout: Constants.timeout)
                XCTAssert(result)
            })
        })
        expectation.fulfill()
    }
}

extension FiltersSubtests.MultiParameter {
    var queryItemKey: String {
        switch self {
            case .infrastructure:
                return "infrastructureType"
            case .taxationForm:
                return "taxationForm"
            case .lotType:
                return "lotType"
        }
    }

    var readableName: String {
        switch self {
            case .infrastructure:
                return "Инфраструктура"
            case .taxationForm:
                return "Условия сделки"
            case .lotType:
                return "Тип участка"
        }
    }

    var items: [ValueItem] {
        switch self {
            case .infrastructure:
                return self.infrastructureItems
            case .taxationForm:
                return self.taxationFormItems
            case .lotType:
                return self.lotTypeItems
        }
    }

    private var infrastructureItems: [ValueItem] {
        return [
            ValueItem(readableName: "Рядом есть водоём", queryItemValue: "POND"),
            ValueItem(readableName: "Рядом есть парк", queryItemValue: "PARK"),
        ]
    }

    private var taxationFormItems: [ValueItem] {
        return [
            ValueItem(readableName: "НДС", queryItemValue: "NDS"),
            ValueItem(readableName: "УСН", queryItemValue: "USN"),
        ]
    }

    private var lotTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "ИЖС", queryItemValue: "IGS"),
            ValueItem(readableName: "СНТ", queryItemValue: "GARDEN"),
        ]
    }
}
