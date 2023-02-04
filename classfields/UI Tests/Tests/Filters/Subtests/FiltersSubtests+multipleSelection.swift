//
//  FilterSubtests+multipleSelection.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 16.07.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

extension FiltersSubtests {
    enum MultipleSelectParameter: String {
        struct ValueItem: Equatable {
            let readableName: String
            let queryItemValue: String

            static let emptyItem = ValueItem(readableName: "Неважно", queryItemValue: "")
        }

        case buyRenovation
        case rentRenovation
        case commercialRenovation
        case parkingType
        case decoration
        case buildingClass
        case buildingType
        case villageClass
        case wallsType
        case houseType
        case landType
        case roomsCount
        case officeClass
        case buildingConstructionType
        case buildingEpochType
        case parkType
        case pondType
    }

    func multipleSelectParameter(_ parameter: MultipleSelectParameter) {
        switch parameter {
            case .officeClass:
                self.showOfficeClassCell()
            default:
                // do nothing
                break
        }

        let runKey = #function + "_" + parameter.runKey
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки '\(parameter.readableName)'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: parameter.accessibilityIdentifier)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        let picker = self.filtersSteps.openMultipleSelectionPicker(for: parameter)

        // Test Close button for picker
        picker
            .isPickerPresented(with: parameter.readableName)
            .tapOnCloseButton()
            .isPickerClosed()

        let items = parameter.items
        let queryItems = items.map { URLQueryItem(name: parameter.queryItemKey, value: $0.queryItemValue) }
        let allItemsSelectedReadableName = parameter.allItemsSelectedReadableName

        let allItemsSelectedPredicate = Predicate<HttpRequest>.queryItems(
            contain: Set(queryItems),
            notContainKeys: [],
            notContain: []
        )
        let allItemsDeselectedPredicate = Predicate<HttpRequest>.queryItems(
            contain: [],
            notContain: Set(queryItems)
        )

        let allItemsSelectedExpectation = XCTestExpectation(description: "Проверка формирования запроса при выборе всех значений в пикере '\(parameter.readableName)' главного фильтра.")
        self.tapPickerItems(
            items: items,
            parameter: parameter,
            expectation: allItemsSelectedExpectation,
            predicate: allItemsSelectedPredicate,
            parameterFinalValue: allItemsSelectedReadableName
        )

        let allItemsDeselectedExpectation = XCTestExpectation(description: "Проверка формирования запроса при отключении всех значений в пикере '\(parameter.readableName)' главного фильтра.")
        self.tapPickerItems(
            items: items,
            parameter: parameter,
            expectation: allItemsDeselectedExpectation,
            predicate: allItemsDeselectedPredicate,
            parameterFinalValue: MultipleSelectParameter.ValueItem.emptyItem.readableName
        )
    }

    private func tapPickerItems(
        items: [MultipleSelectParameter.ValueItem],
        parameter: MultipleSelectParameter,
        expectation: XCTestExpectation,
        predicate: Predicate<HttpRequest>,
        parameterFinalValue: String
    ) {
        let picker = self.filtersSteps.openMultipleSelectionPicker(for: parameter)

        self.api.setupSearchCounter(
            predicate: predicate,
            handler: {
                expectation.fulfill()
            }
        )

        parameter.items.forEach { item in
            picker.tapOnRow(item.readableName)
        }
        picker.tapOnApplyButton()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
        self.filtersSteps.multipleSelectParameter(with: parameter.accessibilityIdentifier,
                                                  hasValue: parameterFinalValue)
    }

    private func showOfficeClassCell() {
        self.api.unregisterAllHandlers()
        let singleSelectParam = SingleSelectParameter.commercialBuildingType([.businessCenter])
        self.filtersSteps
            .openSingleSelectionPicker(for: singleSelectParam)
            .isPickerPresented(with: SingleSelectParameter.CommercialBuildingType.businessCenter.item.readableName)
            .hasOneSelectedRow(SingleSelectParameter.CommercialBuildingType.businessCenter.item.readableName)
            .tapOnRow(SingleSelectParameter.CommercialBuildingType.businessCenter.item.readableName)
            .isPickerClosed()
    }
}

extension FiltersSubtests.MultipleSelectParameter {
    var runKey: String {
        self.rawValue
    }

    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .buyRenovation,
                 .rentRenovation,
                 .commercialRenovation:
                id = "renovation"
            case .parkingType:
                id = "parkingType"
            case .decoration:
                id = "decoration"
            case .buildingClass:
                id = "buildingClass"
            case .buildingType:
                id = "buildingType"
            case .villageClass:
                id = "villageClass"
            case .wallsType:
                id = "wallsType"
            case .houseType:
                id = "houseType"
            case .landType:
                id = "landType"
            case .roomsCount:
                id = "roomsCount"
            case .officeClass:
                id = "officeClass"
            case .buildingConstructionType, .buildingEpochType:
                // @coreshock: Two parameters behind one cell
                id = "buildingConstructionType"
            case .parkType:
                id = "parkType"
            case .pondType:
                id = "pondType"
        }
        return "filters.cell." + id
    }

    var queryItemKey: String {
        switch self {
            case .buyRenovation,
                 .rentRenovation,
                 .commercialRenovation:
                return "renovation"
            case .parkingType:
                return "parkingType"
            case .decoration:
                return "decoration"
            case .buildingClass:
                return "buildingClass"
            case .buildingType:
                return "buildingType"
            case .villageClass:
                return "villageClass"
            case .wallsType:
                return "wallsType"
            case .houseType:
                return "houseType"
            case .landType:
                return "landType"
            case .roomsCount:
                return "roomsTotal"
            case .officeClass:
                return "officeClass"
            case .buildingConstructionType:
                return "buildingType"
            case .buildingEpochType:
                return "buildingEpoch"
            case .parkType:
                return "parkType"
            case .pondType:
                return "pondType"
        }
    }

    var readableName: String {
        switch self {
            case .buyRenovation,
                 .rentRenovation,
                 .commercialRenovation:
                return "Ремонт"
            case .parkingType:
                return "Парковка"
            case .decoration:
                return "Отделка"
            case .buildingClass:
                return "Класс жилья"
            case .buildingType:
                return "Материал дома"
            case .villageClass:
                return "Класс посёлка"
            case .wallsType:
                return "Материал стен"
            case .houseType:
                return "Тип дома"
            case .landType:
                return "Тип земли"
            case .roomsCount:
                return "Комнат в квартире"
            case .officeClass:
                return "Класс бизнес-центра"
            case .buildingConstructionType, .buildingEpochType:
                // @coreshock: Two parameters behind one cell
                return "Тип дома"
            case .parkType:
                return "Тип парка"
            case .pondType:
                return "Тип водоёма"
        }
    }

    var items: [ValueItem] {
        switch self {
            case .buyRenovation:
                return self.buyRenovationItems
            case .rentRenovation:
                return self.rentRenovationItems
            case .commercialRenovation:
                return self.commercialRenovationItems
            case .parkingType:
                return self.parkingTypeItems
            case .decoration:
                return self.decorationItems
            case .buildingClass:
                return self.buildingClassItems
            case .buildingType:
                return self.buildingTypeItems
            case .villageClass:
                return self.villageClassItems
            case .wallsType:
                return self.wallsTypeItems
            case .houseType:
                return self.houseTypeItems
            case .landType:
                return self.landTypeItems
            case .roomsCount:
                return self.roomsCountItems
            case .officeClass:
                return self.officeClassItems
            case .buildingConstructionType:
                return self.buildingConstructionTypeItems
            case .buildingEpochType:
                return self.buildingEpochTypeItems
            case .parkType:
                return self.parkTypeItems
            case .pondType:
                return self.pondTypeItems
        }
    }

    var allItemsSelectedReadableName: String {
        switch self {
            case .roomsCount:
                return "2–6, 7+"

            // it is ok use `default` here because we have only one exception from rule
            default:
                return self.items.map(\.readableName).joined(separator: ", ")
        }
    }

    private var buyRenovationItems: [ValueItem] {
        return [
            ValueItem(readableName: "Косметический", queryItemValue: "COSMETIC_DONE"),
            ValueItem(readableName: "Евро", queryItemValue: "EURO"),
            ValueItem(readableName: "Дизайнерский", queryItemValue: "DESIGNER_RENOVATION"),
            ValueItem(readableName: "Требуется ремонт", queryItemValue: "NEEDS_RENOVATION"),
        ]
    }

    private var rentRenovationItems: [ValueItem] {
        return [
            ValueItem(readableName: "Косметический", queryItemValue: "COSMETIC_DONE"),
            ValueItem(readableName: "Евро", queryItemValue: "EURO"),
            ValueItem(readableName: "Дизайнерский", queryItemValue: "DESIGNER_RENOVATION"),
            ValueItem(readableName: "Современный", queryItemValue: "NON_GRANDMOTHER"),
            ValueItem(readableName: "Требуется ремонт", queryItemValue: "NEEDS_RENOVATION"),
        ]
    }

    private var commercialRenovationItems: [ValueItem] {
        return [
            ValueItem(readableName: "Требуется ремонт", queryItemValue: "NEEDS_RENOVATION"),
            ValueItem(readableName: "Косметический", queryItemValue: "COSMETIC_DONE"),
            ValueItem(readableName: "Дизайнерский", queryItemValue: "DESIGNER_RENOVATION"),
        ]
    }

    private var parkingTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Закрытая", queryItemValue: "CLOSED"),
            ValueItem(readableName: "Подземная", queryItemValue: "UNDERGROUND"),
            ValueItem(readableName: "Открытая", queryItemValue: "OPEN"),
        ]
    }

    private var decorationItems: [ValueItem] {
        return [
            ValueItem(readableName: "Черновая", queryItemValue: "ROUGH"),
            ValueItem(readableName: "Чистовая", queryItemValue: "CLEAN"),
            ValueItem(readableName: "Под ключ", queryItemValue: "TURNKEY"),
        ]
    }

    private var buildingClassItems: [ValueItem] {
        return [
            ValueItem(readableName: "Эконом", queryItemValue: "ECONOM"),
            ValueItem(readableName: "Комфорт", queryItemValue: "COMFORT"),
            ValueItem(readableName: "Комфорт+", queryItemValue: "COMFORT_PLUS"),
            ValueItem(readableName: "Бизнес", queryItemValue: "BUSINESS"),
            ValueItem(readableName: "Элитный", queryItemValue: "ELITE"),
        ]
    }

    private var buildingTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Кирпичный", queryItemValue: "BRICK"),
            ValueItem(readableName: "Монолитный", queryItemValue: "MONOLIT"),
            ValueItem(readableName: "Панельный", queryItemValue: "PANEL"),
            ValueItem(readableName: "Кирпично-монолитный", queryItemValue: "MONOLIT_BRICK"),
            ValueItem(readableName: "Блочный", queryItemValue: "BLOCK"),
            ValueItem(readableName: "Деревянный", queryItemValue: "WOOD"),
            ValueItem(readableName: "Железобетонный", queryItemValue: "FERROCONCRETE"),
        ]
    }

    private var villageClassItems: [ValueItem] {
        return [
            ValueItem(readableName: "Эконом", queryItemValue: "ECONOMY"),
            ValueItem(readableName: "Комфорт", queryItemValue: "COMFORT"),
            ValueItem(readableName: "Комфорт+", queryItemValue: "COMFORT_PLUS"),
            ValueItem(readableName: "Бизнес", queryItemValue: "BUSINESS"),
            ValueItem(readableName: "Элитный", queryItemValue: "ELITE"),
        ]
    }

    private var wallsTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Дерево", queryItemValue: "WOOD"),
            ValueItem(readableName: "Каркасно-щитовые", queryItemValue: "FRAME"),
            ValueItem(readableName: "Кирпич", queryItemValue: "BRICK"),
            ValueItem(readableName: "Фахверк", queryItemValue: "TIMBER_FRAMING"),
            ValueItem(readableName: "Бетон", queryItemValue: "CONCRETE"),
        ]
    }

    private var houseTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Таунхаус", queryItemValue: "TOWNHOUSE"),
            ValueItem(readableName: "Дуплекс", queryItemValue: "DUPLEX"),
            ValueItem(readableName: "Часть дома", queryItemValue: "PARTHOUSE"),
            ValueItem(readableName: "Отдельный дом", queryItemValue: "HOUSE"),
        ]
    }

    private var landTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "ДНП", queryItemValue: "DNP"),
            ValueItem(readableName: "ИЖС", queryItemValue: "IZHS"),
            ValueItem(readableName: "ЛПХ", queryItemValue: "LPH"),
            ValueItem(readableName: "МЖС", queryItemValue: "MZHS"),
            ValueItem(readableName: "СНТ", queryItemValue: "SNT"),
        ]
    }

    private var roomsCountItems: [ValueItem] {
        return [
            ValueItem(readableName: "2", queryItemValue: "2"),
            ValueItem(readableName: "3", queryItemValue: "3"),
            ValueItem(readableName: "4", queryItemValue: "4"),
            ValueItem(readableName: "5", queryItemValue: "5"),
            ValueItem(readableName: "6", queryItemValue: "6"),
            ValueItem(readableName: "7+", queryItemValue: "PLUS_7"),
        ]
    }

    private var officeClassItems: [ValueItem] {
        return [
            ValueItem(readableName: "A", queryItemValue: "A"),
            ValueItem(readableName: "A+", queryItemValue: "A_PLUS"),
            ValueItem(readableName: "B", queryItemValue: "B"),
            ValueItem(readableName: "B+", queryItemValue: "B_PLUS"),
            ValueItem(readableName: "C", queryItemValue: "C"),
            ValueItem(readableName: "C+", queryItemValue: "C_PLUS"),
        ]
    }

    private var buildingConstructionTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Кирпичный", queryItemValue: "BRICK"),
            ValueItem(readableName: "Монолитный", queryItemValue: "MONOLIT"),
            ValueItem(readableName: "Панельный", queryItemValue: "PANEL"),
            ValueItem(readableName: "Кирпично-монолитный", queryItemValue: "MONOLIT_BRICK"),
            ValueItem(readableName: "Блочный", queryItemValue: "BLOCK"),
        ]
    }

    private var buildingEpochTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Хрущёвка", queryItemValue: "KHRUSHCHEV"),
            ValueItem(readableName: "Сталинка", queryItemValue: "STALIN"),
            ValueItem(readableName: "Брежневка", queryItemValue: "BREZHNEV"),
        ]
    }

    private var parkTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Лес", queryItemValue: "FOREST"),
            ValueItem(readableName: "Сад", queryItemValue: "GARDEN"),
        ]
    }

    private var pondTypeItems: [ValueItem] {
        return [
            ValueItem(readableName: "Море", queryItemValue: "SEA"),
            ValueItem(readableName: "Залив", queryItemValue: "BAY"),
            ValueItem(readableName: "Пролив", queryItemValue: "STRAIT"),
            ValueItem(readableName: "Озеро", queryItemValue: "LAKE"),
            ValueItem(readableName: "Пруд", queryItemValue: "POND"),
        ]
    }
}
