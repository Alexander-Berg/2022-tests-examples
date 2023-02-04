//
//  FiltersTests+factory.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 07.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import enum Typograf.SpecialSymbol

extension FiltersTests {
    final class Factory { }
}

// MARK: - Price

extension FiltersTests.Factory {
    static func makeSingleCasePrice() -> FiltersSubtests.Subtest.PriceType {
        return .singleCase(numberRangeConfiguration: Self.makeNumberRange(.price))
    }

    static func makePrice(_ parameter: FiltersSubtests.PriceParameter,
                          from: String = "100",
                          to: String = "200") -> FiltersSubtests.PriceConfiguration {
        let nbsp = SpecialSymbol.nbsp
        switch parameter {
            case .perOffer:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽")
            case .perMeter:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за м²")
            case .perAre:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за сотку")

            case .perOfferPerMonth:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за всё\(nbsp)в месяц")
            case .perOfferPerYear:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за всё\(nbsp)в год")

            case .perCommercialOfferPerMonth:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)в месяц")
            case .perCommercialOfferPerYear:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)в год")

            case .perMeterPerMonth:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за м²\(nbsp)в месяц")
            case .perArePerMonth:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за сотку\(nbsp)в месяц")

            case .perMeterPerYear:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за м²\(nbsp)в год")
            case .perArePerYear:
                return .init(parameter: parameter,
                             value: .init(from: from, to: to),
                             expectation: "от \(from) до \(to)\(nbsp)₽\(nbsp)за сотку\(nbsp)в год")
        }
    }
}

// MARK: - Number Range

extension FiltersTests.Factory {
    static func makeNumberRange(
        _ parameter: FiltersSubtests.NumberRangeParameter
    ) -> FiltersSubtests.NumberRangeParameter.Configuration {
        switch parameter {
            case .totalArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.totalArea")
            case .kitchenArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.kitchenArea")
            case .livingSpace:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.livingSpace")
            case .houseArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.houseArea")
            case .villageHouseArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.villageHouseArea")
            case .commercialArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.commercialArea")
            case .commercialLotArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.commercialLotArea")
            case .lotArea:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.lotArea")
            case .lotAreaWithSeparator:
                return .init(parameter: parameter,
                             value: .init(from: "40", to: "80"),
                             expectationScreenshotID: "filters.numberRange.lotAreaWithSeparator")
            case .floor:
                return .init(parameter: parameter,
                             value: .init(from: "7", to: "10"),
                             expectationScreenshotID: "filters.numberRange.floor")
            case .builtYear:
                return .init(parameter: parameter,
                             value: .init(from: "1950", to: "2020"),
                             expectationScreenshotID: "filters.numberRange.builtYear")
            case .price:
                return .init(parameter: parameter,
                             value: .init(from: "100", to: "200"),
                             expectationScreenshotID: "filters.numberRange.price")
            case .houseFloors:
                return .init(parameter: parameter,
                             value: .init(from: "1", to: "10"),
                             expectationScreenshotID: "filters.numberRange.houseFloors")
        }
    }
}

// MARK: - Time To Metro

extension FiltersTests.Factory {
    static let metroDistanceOptions: Set<FiltersSubtests.TimeToMetroOption> = [.option_5, .option_60]
}
