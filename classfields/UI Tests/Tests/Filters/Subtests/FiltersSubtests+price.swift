//
//  FiltersSubtests+price.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

extension FiltersSubtests {
    struct PriceConfiguration {
        let parameter: PriceParameter
        let value: PriceParameter.Value
        let expectation: String

        // FIXME: Add optimization for that method.
        // https://st.yandex-team.ru/VSAPPS-7678
//        var runKey: String {
//            return [parameter.rawValue, value.from, value.to, expectation].joined(separator: "_")
//        }
    }

    enum PriceParameter: String {
        typealias Value = FilterPricePickerSteps.Price
        typealias PriceType = FilterPricePickerSteps.PriceType
        typealias PricePeriod = FilterPricePickerSteps.PricePeriod

        case perOffer
        case perMeter
        case perAre

        case perOfferPerMonth
        case perOfferPerYear

        case perCommercialOfferPerMonth
        case perCommercialOfferPerYear

        case perMeterPerMonth
        case perArePerMonth

        case perMeterPerYear
        case perArePerYear
    }
}

extension FiltersSubtests {
    func price(configurations: [PriceConfiguration], skipPriceType: Bool = false) {
        // FIXME: Add optimization for that method.
        // https://st.yandex-team.ru/VSAPPS-7678
//        let runKey = #function
//            + configurations.map { $0.runKey }.joined(separator: "__")
//            + (skipPriceType ? "skipPriceType" : "")
//
//        guard Self.subtestsExecutionMarks[runKey] != true else {
//            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'Цена'"
//            let accessibilityIdentifier = "filters.cell.price"
//            XCTContext.runActivity(named: activityName) { _ -> Void in
//                self.filtersSteps.isCellPresented(accessibilityIdentifier: accessibilityIdentifier)
//            }
//            return
//        }
//        Self.subtestsExecutionMarks[runKey] = true

        configurations.forEach { configuration in
            var (priceType, pricePeriod) = self.priceSubparameters(parameter: configuration.parameter)
            let activityTitle = self.activityTitle(priceType: priceType, pricePeriod: pricePeriod, value: configuration.value)

            let expectation = XCTestExpectation(description: activityTitle)

            if skipPriceType {
                priceType = nil
            }

            XCTContext.runActivity(named: activityTitle) { _ in
                self.priceSteps(filterPriceType: priceType,
                                filterPricePeriod: pricePeriod,
                                value: configuration.value,
                                expectedValue: configuration.expectation,
                                completion: {
                    expectation.fulfill()
                })
            }
            let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
            XCTAssert(result)
        }
    }

    // MARK: Private

    // Parameters with both priceType and pricePeriod don't work yet.
    // See YREPriceTypePeriodAggregateFilterDisplayEntryProxy for implementation details.
    // FIXME: https://st.yandex-team.ru/VSAPPS-6964 (56)
    private func priceSubparameters(
        parameter: PriceParameter
    ) -> (PriceParameter.PriceType?, PriceParameter.PricePeriod?) {
        switch parameter {
            case .perOffer:
                return (.perOffer, nil)
            case .perMeter:
                return (.perM2, nil)
            case .perAre:
                return (.perAre, nil)

            case .perOfferPerMonth:
                return (.perOffer, .perMonth)
            case .perOfferPerYear:
                return (.perOffer, .perYear)

            case .perCommercialOfferPerMonth:
                return (.perOffer, .perMonth)
            case .perCommercialOfferPerYear:
                return (.perOffer, .perYear)

            case .perMeterPerMonth:
                return (.perM2, .perMonth)
            case .perArePerMonth:
                return (.perAre, .perMonth)

            case .perMeterPerYear:
                return (.perM2, .perYear)
            case .perArePerYear:
                return (.perAre, .perYear)
        }
    }

    private func priceSteps(
        filterPriceType: PriceParameter.PriceType?,
        filterPricePeriod: PriceParameter.PricePeriod?,
        value: PriceParameter.Value,
        expectedValue: String,
        completion: @escaping () -> Void
    ) {
        let pricePickerSteps = self.filtersSteps
            .tapOnPrice()
            .isPricePickerPresented()

        if let filterPriceType = filterPriceType {
            pricePickerSteps.change(type: filterPriceType)
        }
        if let filterPricePeriod = filterPricePeriod {
            pricePickerSteps.change(period: filterPricePeriod)
        }

        pricePickerSteps
            .enter(price: .init(from: value.from, to: value.to))

        // Stub API and set API expectations right before the picker applying
        let queryItems = self.queryItems(priceType: filterPriceType, pricePeriod: filterPricePeriod, value: value)
        self.api.setupSearchCounter(
            predicate: .queryItems(contain: queryItems),
            handler: completion
        )
        
        pricePickerSteps
            .apply()

        // We need this at least until we have screenshot-based tests
        self.filtersSteps
            .priceParameter(hasValue: expectedValue)
    }

    private func queryItems(
        priceType: PriceParameter.PriceType?,
        pricePeriod: PriceParameter.PricePeriod?,
        value: PriceParameter.Value
    ) -> Set<URLQueryItem> {
        var queryItems: Set<URLQueryItem> = [
            URLQueryItem(name: "priceMin", value: value.from),
            URLQueryItem(name: "priceMax", value: value.to),
        ]
        switch priceType {
            case .none:
                break
            case .perOffer:
                queryItems.insert(URLQueryItem(name: "priceType", value: "PER_OFFER"))
            case .perM2:
                queryItems.insert(URLQueryItem(name: "priceType", value: "PER_METER"))
            case .perAre:
                queryItems.insert(URLQueryItem(name: "priceType", value: "PER_ARE"))
        }
        switch pricePeriod {
            case .none:
                break
            case .perMonth:
                queryItems.insert(URLQueryItem(name: "pricingPeriod", value: "PER_MONTH"))
            case .perYear:
                queryItems.insert(URLQueryItem(name: "pricingPeriod", value: "PER_YEAR"))
        }
        return queryItems
    }

    private func activityTitle(
        priceType: PriceParameter.PriceType?,
        pricePeriod: PriceParameter.PricePeriod?,
        value: PriceParameter.Value
    ) -> String {
        var components = [String]()
        components.append("Проверяем пикер цены")
        if let priceType = priceType {
            components.append("\(priceType.placeholderName.localizedLowercase)")
        }
        if let pricePeriod = pricePeriod {
            components.append("\(pricePeriod.placeholderName.localizedLowercase)")
        }
        if value.from.isEmpty == false {
            components.append("от \(value.from) р")
        }
        if value.to.isEmpty == false {
            components.append("до \(value.to) р")
        }
        return components.joined(separator: ", ")
    }
}
