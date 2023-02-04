//
//  FiltersSubtests+singleSelection.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 7/13/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter
import YREFormatters
import YREWeb

extension FiltersSubtests {
    enum SingleSelectParameter {
        struct ValueItem: Equatable {
            let readableName: String
            let queryItemValue: String

            static let emptyItem = ValueItem(readableName: "Неважно", queryItemValue: "")
        }

        enum CommercialBuildingType: CaseIterable {
            case noMatter
            case businessCenter
            case warehouse
            case shoppingCenter
            case detachedBuilding
            case residentalBuilding
        }

        case minCeilingHeight
        case livingApartmentType
        case balconyType
        case bathroomType
        case lastFloor
        case cityRenovation
        case furniture
        case hasFurniture
        case dealStatusCommercialRent
        case commercialBuildingType([CommercialBuildingType])
        case commercialPlanType
        case entranceType
        case deliveryDate
        case dealStatusBuyApartament
    }

    func singleSelectParameterCancellation(_ parameter: SingleSelectParameter) {
        // Test Close button for picker
        let picker = self.filtersSteps
            .openSingleSelectionPicker(for: parameter)
        picker
            .isPickerPresented(with: parameter.readableName)
            .tapOnCloseButton()
            .isPickerClosed()
    }

    func singleSelectParameter(_ parameter: SingleSelectParameter) {
        let runKey = #function + "_" + parameter.accessibilityIdentifier
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки '\(parameter.readableName)'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: parameter.accessibilityIdentifier)
            }
            return
        }

        Self.subtestsExecutionMarks[runKey] = true

        // TODO: @coreshock: Test picker cancellation once somewhere outside of this method.
        self.singleSelectParameterCancellation(parameter)

        var prevItem: SingleSelectParameter.ValueItem = .emptyItem

        // Test picker values changing
        parameter.items.reversed().forEach { item in
            let expectation = XCTestExpectation(description: "Проверка формирования запроса для выбранного значения '\(item.readableName)' в пикере '\(parameter.readableName)' главного фильтра.")
            
            // Expected value in filter after selection
            let expectedValueInFilter = item.readableName

            let predicate: Predicate<HttpRequest>

            if item == .emptyItem {
                predicate = .notContains(queryKey: parameter.queryItemKey)
            }
            else {
                let queryItem = URLQueryItem(name: parameter.queryItemKey, value: item.queryItemValue)

                let isPrevItemNotEmpty = prevItem != .emptyItem
                let notContaints: Set<URLQueryItem> = isPrevItemNotEmpty
                    ? [URLQueryItem(name: parameter.queryItemKey, value: prevItem.queryItemValue)]
                    : []
                predicate = .queryItems(contain: [queryItem], notContain: notContaints)
            }

            let pickerStep = self.filtersSteps
                .openSingleSelectionPicker(for: parameter)

            let responseHandler: () -> Void = {
                expectation.fulfill()
            }

            // Stub for parameter value expection
            self.api.setupSearchCounter(
                predicate: predicate,
                handler: responseHandler
            )

            pickerStep
                .isPickerPresented(with: parameter.readableName)
                .hasOneSelectedRow(item.readableName)
                .tapOnRow(item.readableName)

            let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
            XCTAssert(result)
            
            pickerStep.isPickerClosed()
            self.filtersSteps.singleSelectParameter(with: parameter.accessibilityIdentifier,
                                                    hasValue: expectedValueInFilter)
            prevItem = item
        }
    }
}

extension FiltersSubtests.SingleSelectParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .minCeilingHeight:         id = "minCeilingHeight"
            case .livingApartmentType:      id = "livingApartmentType"
            case .balconyType:              id = "balconyType"
            case .bathroomType:             id = "bathroomType"
            case .lastFloor:                id = "lastFloor"
            case .cityRenovation:           id = "cityRenovation"
            case .furniture:                id = "furniture"
            case .hasFurniture:             id = "hasFurniture"
            case .commercialBuildingType:   id = "commercialBuildingType"
            case .commercialPlanType:       id = "commercialPlanType"
            case .entranceType:             id = "entranceType"
            case .deliveryDate:             id = "deliveryDate"

            case .dealStatusCommercialRent, .dealStatusBuyApartament:  
                id = "dealStatus"
        }
        return "filters.cell." + id
    }

    var queryItemKey: String {
        switch self {
            case .minCeilingHeight:         return "ceilingHeightMin"
            case .livingApartmentType:      return "apartments"
            case .balconyType:              return "balcony"
            case .bathroomType:             return "bathroomUnit"
            case .lastFloor:                return "lastFloor"
            case .cityRenovation:           return "expectDemolition"
            case .furniture:                return "hasFurniture"
            case .hasFurniture:             return "hasFurniture"
            case .commercialBuildingType:   return "commercialBuildingType"
            case .commercialPlanType:       return "commercialPlanType"
            case .entranceType:             return "entranceType"
            case .deliveryDate:             return "deliveryDate"

            case .dealStatusCommercialRent, .dealStatusBuyApartament:  
                return "dealStatus"
        }
    }

    var readableName: String {
        switch self {
            case .minCeilingHeight:         return "Высота потолков"
            case .livingApartmentType:      return "Апартаменты"
            case .balconyType:              return "Балкон"
            case .bathroomType:             return "Санузел"
            case .lastFloor:                return "Последний этаж"
            case .cityRenovation:           return "Программа реновации"
            case .furniture:                return "Наличие мебели"
            case .hasFurniture:             return "Наличие мебели"
            case .commercialBuildingType:   return "Тип здания"
            case .commercialPlanType:       return "Планировка"
            case .entranceType:             return "Вход в помещение"
            case .deliveryDate:             return "Срок сдачи"
            
            case .dealStatusCommercialRent, .dealStatusBuyApartament:  
                return "Тип сделки"
        }
    }

    var items: [ValueItem] {
        switch self {
            case .minCeilingHeight:         return Self.itemsForMinCeilingHeight
            case .livingApartmentType:      return Self.itemsForLivingApartmentType
            case .balconyType:              return Self.itemsForBalconyType
            case .bathroomType:             return Self.itemsForBathroomType
            case .lastFloor:                return Self.itemsForLastFloor
            case .cityRenovation:           return Self.itemsForCityRenovation
            case .furniture:                return Self.itemsForFurniture
            case .hasFurniture:             return Self.itemsForFurniture
            case .dealStatusCommercialRent: return Self.itemsDealStatusCommercialRent
            case .entranceType:             return Self.itemsEntranceType
            case .commercialPlanType:       return Self.itemsCommercialPlanType
            case .deliveryDate:             return Self.itemsDeliveryDate
            case .dealStatusBuyApartament:  return Self.itemsDealStatusBuyApartament

            case .commercialBuildingType(let types):
                return types.map({ $0.item })
        }
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForLivingApartmentType: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Только апартаменты", queryItemValue: "YES"),
            .init(readableName: "Без апартаментов", queryItemValue: "NO"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForMinCeilingHeight: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "От 2.5 м", queryItemValue: "2.5"),
            .init(readableName: "От 2.7 м", queryItemValue: "2.7"),
            .init(readableName: "От 3 м", queryItemValue: "3"),
            .init(readableName: "От 4 м", queryItemValue: "4"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForBalconyType: [ValueItem] {
        // NOTE: @arkadysmirnov. There isn't any sense to test all 25 floors, that's why i test only second and 25th floors.
        return [
            .emptyItem,
            .init(readableName: "Есть балкон", queryItemValue: "BALCONY"),
            .init(readableName: "Есть лоджия", queryItemValue: "LOGGIA"),
            .init(readableName: "Любой балкон", queryItemValue: "ANY"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForBathroomType: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Совмещённый", queryItemValue: "MATCHED"),
            .init(readableName: "Раздельный", queryItemValue: "SEPARATED"),
            .init(readableName: "Два и более", queryItemValue: "TWO_AND_MORE"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForLastFloor: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Только последний этаж", queryItemValue: "YES"),
            .init(readableName: "Кроме последнего этажа", queryItemValue: "NO"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForCityRenovation: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Показать только дома под снос", queryItemValue: "YES"),
            .init(readableName: "Не показывать дома под снос", queryItemValue: "NO"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsForFurniture: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Есть мебель", queryItemValue: "YES"),
            .init(readableName: "Нет мебели", queryItemValue: "NO"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsDealStatusCommercialRent: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Прямая аренда", queryItemValue: "DIRECT_RENT"),
            .init(readableName: "Субаренда", queryItemValue: "SUBRENT"),
            .init(readableName: "Продажа права аренды", queryItemValue: "SALE_OF_LEASE_RIGHTS"),
        ]
    }

    static var itemsDealStatusBuyApartament: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Обмен", queryItemValue: "COUNTERSALE"),
            .init(readableName: "Переуступка", queryItemValue: "REASSIGNMENT"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter.CommercialBuildingType {
    var item: FiltersSubtests.SingleSelectParameter.ValueItem {
        switch self {
            case .noMatter: return .emptyItem
            case .businessCenter: return .init(readableName: "Бизнес-центр", queryItemValue: "BUSINESS_CENTER")
            case .warehouse: return .init(readableName: "Складской комплекс", queryItemValue: "WAREHOUSE")
            case .shoppingCenter: return .init(readableName: "Торговый центр", queryItemValue: "SHOPPING_CENTER")
            case .detachedBuilding: return .init(readableName: "Отдельно стоящее здание", queryItemValue: "DETACHED_BUILDING")
            case .residentalBuilding: return .init(readableName: "Встроенное помещение, жилой дом", queryItemValue: "RESIDENTIAL_BUILDING")
        }
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsEntranceType: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Отдельный", queryItemValue: "SEPARATE"),
            .init(readableName: "Общий", queryItemValue: "COMMON"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsCommercialPlanType: [ValueItem] {
        return [
            .emptyItem,
            .init(readableName: "Open space", queryItemValue: "OPEN_SPACE"),
            .init(readableName: "Кабинетная", queryItemValue: "CABINET"),
            .init(readableName: "Коридорная", queryItemValue: "CORRIDOR"),
        ]
    }
}

extension FiltersSubtests.SingleSelectParameter {
    static var itemsDeliveryDate: [ValueItem] {
        let validOptions = DeliveryDateOptionsGenerator.validOptions(forDate: Date())
        let optionsToDisplayedStrings = DeliveryDateOptionsGenerator.optionsToDisplayedStrings(forValidOptions: validOptions)
        return validOptions.compactMap { option in
            guard
                let readableName = optionsToDisplayedStrings[option],
                let queryItemValue = DeliveryDateOptionsSerializer.serializedValue(for: option),
                queryItemValue.isEmpty == false
            else { return nil }
            return .init(readableName: readableName, queryItemValue: queryItemValue)
        }
    }
}
