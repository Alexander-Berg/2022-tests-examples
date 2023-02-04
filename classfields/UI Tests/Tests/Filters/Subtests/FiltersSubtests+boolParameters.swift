//
//  FiltersSubtests+boolParameters.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 07.07.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREFiltersModel
import XCTest

extension FiltersSubtests {
    enum BoolParameter: String {
        /// Кроме первого этажа
        case exceptFirstFloor
        /// Холодильник
        case fridge
        /// Посудомоечная машина
        case dishwasher
        /// Кондиционер
        case aircondition
        /// Телевизор
        case tv // swiftlint:disable:this identifier_name
        /// Стиральная машина
        case washingMachine
        /// Можно с животными
        case animal
        /// Можно с детьми
        case kids
        /// Холодильник
        case hasFridge
        /// Телевизор
        case hasTV
        /// Стиральная машина
        case hasWashingMachine
        /// Можно с животными
        case animalAllowed
        /// Можно с детьми
        case kidsAllowed

        /// Рядом построят станцию метро
        case expectMetro
        /// Рядом есть ж/д станция
        case hasRailwayStation

        /// Только с фото
        case photoRequired
        /// Только от собственников
        case offerPropertyType
        /// Без комиссии агенту
        case noFee
        /// Есть видео
        case hasVideo
        /// Можно посмотреть онлайн
        case supportsOnlineView
        /// Проверенно в Росреестре
        case withExcerptsOnly

        /// Скидки
        case hasSpecialProposal
        /// Ипотека
        case hasSiteMortgage
        /// Рассрочка
        case hasInstallment
        /// 214 ФЗ
        case fz214
        /// Материнский капитал
        case hasSiteMaternityFunds
        /// Военная ипотека
        case hasMilitarySiteMortgage

        /// ЖК с закрытыми продажами
        case showOutdated

        /// Электричество
        case hasElectricitySupply
        /// Газ
        case hasGasSupply
        /// Водопровод
        case hasWaterSupply
        /// Канализация
        case hasSewerageSupply
        /// Отопление
        case hasHeatingSupply
        /// Охрана
        case hasSecurity

        /// КУ включены
        case hasUtilitiesIncluded
        /// Электроэнергия включена
        case hasElectricityIncluded
        /// Клининг включен
        case hasCleaningIncluded

        /// Доступ 24/7
        case hasTwentyFourSeven
        /// Парковка
        case hasParking

        /// Вентиляция
        case hasVentilation
    }

    func boolParameter(_ parameter: BoolParameter) {
        let runKey = #function + "_" + parameter.accessibilityIdentifier
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки '\(parameter.readableName)'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isCellPresented(accessibilityIdentifier: parameter.accessibilityIdentifier)
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true
        
        let activityTitle = "Проверяем формирование запроса при переключении параметра '\(parameter.readableName)'."
        let expectation = XCTestExpectation(description: activityTitle)

        XCTContext.runActivity(named: activityTitle) { _ in
            self.api.setupSearchCounter(
                predicate: .contains(queryItem: URLQueryItem(name: parameter.queryItemKey, value: parameter.queryItemValue)),
                handler: { expectation.fulfill() }
            )
            self.filtersSteps.tapOnBoolParameterCell(accessibilityIdentifier: parameter.accessibilityIdentifier)
        }

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }
}

extension FiltersSubtests.BoolParameter {
    var accessibilityIdentifier: String {
        return "filters.cell." + self.rawValue
    }

    var queryItemKey: String {
        switch self {
            case .exceptFirstFloor: return "floorExceptFirst"

            case .fridge: return "hasRefrigerator"
            case .dishwasher: return "hasDishwasher"
            case .aircondition: return "hasAircondition"
            case .tv: return "hasTelevision"
            case .washingMachine: return "hasWashingMachine"
            case .animal: return "withPets"
            case .kids: return "withChildren"
            case .hasFridge: return "hasRefrigerator"
            case .hasTV: return "hasTelevision"
            case .hasWashingMachine: return "hasWashingMachine"
            case .animalAllowed: return "withPets"
            case .kidsAllowed: return "withChildren"

            case .expectMetro: return "expectMetro"
            case .hasRailwayStation: return "hasRailwayStation"

            case .photoRequired: return "hasPhoto"
            case .offerPropertyType: return "agents"
            case .noFee: return "hasAgentFee"
            case .hasVideo: return "videoUrl"
            case .supportsOnlineView: return "onlineShow"
            case .withExcerptsOnly: return "withExcerptsOnly"

            case .hasSpecialProposal: return "hasSpecialProposal"
            case .hasSiteMortgage: return "hasSiteMortgage"
            case .hasInstallment: return "hasInstallment"
            case .fz214: return "dealType"
            case .hasSiteMaternityFunds: return "hasSiteMaternityFunds"
            case .hasMilitarySiteMortgage: return "hasMilitarySiteMortgage"

            case .showOutdated: return "showOutdated"

            case .hasElectricitySupply: return "hasElectricitySupply"
            case .hasGasSupply: return "hasGasSupply"
            case .hasWaterSupply: return "hasWaterSupply"
            case .hasSewerageSupply: return "hasSewerageSupply"
            case .hasHeatingSupply: return "hasHeatingSupply"
            case .hasSecurity: return "hasSecurity"
            case .hasUtilitiesIncluded: return "hasUtilitiesIncluded"
            case .hasElectricityIncluded: return "hasElectricityIncluded"
            case .hasCleaningIncluded: return "hasCleaningIncluded"

            case .hasTwentyFourSeven: return "hasTwentyFourSeven"
            case .hasParking: return "hasParking"

            case .hasVentilation: return "hasVentilation"
        }
    }

    var queryItemValue: String {
        switch self {
            case .exceptFirstFloor,
                 .fridge,
                 .dishwasher,
                 .aircondition,
                 .tv,
                 .washingMachine,
                 .animal,
                 .kids,
                 .expectMetro,
                 .hasRailwayStation,
                 .photoRequired,
                 .hasVideo,
                 .supportsOnlineView,
                 .withExcerptsOnly,
                 .hasSpecialProposal,
                 .hasSiteMortgage,
                 .hasInstallment,
                 .hasSiteMaternityFunds,
                 .hasMilitarySiteMortgage,
                 .showOutdated,
                 .hasElectricitySupply,
                 .hasGasSupply,
                 .hasWaterSupply,
                 .hasSewerageSupply,
                 .hasHeatingSupply,
                 .hasSecurity,
                 .hasFridge,
                 .hasTV,
                 .hasWashingMachine,
                 .animalAllowed,
                 .kidsAllowed,
                 .hasUtilitiesIncluded,
                 .hasElectricityIncluded,
                 .hasCleaningIncluded,
                 .hasTwentyFourSeven,
                 .hasParking,
                 .hasVentilation:
                return "YES"

            case .offerPropertyType,
                 .noFee:
                return "NO"

            case .fz214:
                return "FZ_214"
        }
    }

    var readableName: String {
        switch self {
            case .exceptFirstFloor: return "Кроме первого этажа"

            case .fridge: return "Холодильник"
            case .dishwasher: return "Посудомоечная машина"
            case .aircondition: return "Кондиционер"
            case .tv: return "Телевизор"
            case .washingMachine: return "Стиральная машина"
            case .animal: return "Можно с животными"
            case .kids: return "Можно с детьми"
            case .hasFridge: return "Холодильник"
            case .hasTV: return "Телевизор"
            case .hasWashingMachine: return "Стиральная машина"
            case .animalAllowed: return "Можно с животными"
            case .kidsAllowed: return "Можно с детьми"

            case .expectMetro: return "Рядом построят станцию метро"
            case .hasRailwayStation: return "Рядом есть ж/д станция"

            case .photoRequired: return "Только с фото"
            case .offerPropertyType: return "Только от собственников"
            case .noFee: return "Без комиссии агенту"
            case .hasVideo: return "Есть видео"
            case .supportsOnlineView: return "Можно посмотреть онлайн"
            case .withExcerptsOnly: return "Проверенно в Росреестре"

            case .hasSpecialProposal: return "Скидки"
            case .hasSiteMortgage: return "Ипотека"
            case .hasInstallment: return "Рассрочка"
            case .fz214: return "214 ФЗ"
            case .hasSiteMaternityFunds: return "Материнский капитал"
            case .hasMilitarySiteMortgage: return "Военная ипотека"

            case .showOutdated: return "ЖК с закрытыми продажами"

            case .hasElectricitySupply: return "Электричество"
            case .hasGasSupply: return "Газ"
            case .hasWaterSupply: return "Водопровод"
            case .hasSewerageSupply: return "Канализация"
            case .hasHeatingSupply: return "Отопление"

            case .hasSecurity: return "Охрана"

            case .hasUtilitiesIncluded: return "КУ включены"
            case .hasElectricityIncluded: return "Электроэнергия включена"
            case .hasCleaningIncluded: return "Клининг включен"

            case .hasTwentyFourSeven: return "Доступ 24/7"
            case .hasParking: return "Паркинг"

            case .hasVentilation: return "Вентиляция"
        }
    }
}
