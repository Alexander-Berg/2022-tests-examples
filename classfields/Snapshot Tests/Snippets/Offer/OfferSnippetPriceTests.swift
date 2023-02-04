//
//  OfferSnippetPriceTrendTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 02.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
import YREModel
import YREModelObjc
import YREModelHelpers
import YRESnippets

// MARK: - Price

extension OfferSnippetTests {
    func testPricePerOffer() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perOffer,
            period: .wholeLife
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerDay() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perOffer,
            period: .perDay
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerMonth() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perOffer,
            period: .perMonth
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerYear() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perOffer,
            period: .perYear
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerM2() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perSquareMeter,
            period: .wholeLife
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerAre() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perAre,
            period: .wholeLife
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerM2PerMonth() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perSquareMeter,
            period: .perMonth
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testPricePerM2PerYear() {
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: 3_900_000),
            unit: .perSquareMeter,
            period: .perYear
        )
        let viewModel = Self.makeViewModelToCheckPrice(price)

        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    // MARK: Private

    private static func makeViewModelToCheckPrice(
        _ price: YREPrice
    ) -> OfferSnippetViewModel? {
        let priceInfo = Self.makeOfferPriceInfo(
            price: price,
            previousPrice: nil,
            trend: .unchanged
        )
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: .moscow),
            price: price,
            offerPriceInfo: priceInfo
        )
        return Self.makeViewModel(with: snippet)
    }
}

// MARK: - Price Trend

extension OfferSnippetTests {
    func testUnchangedPriceTrend() {
        let viewModel = Self.makeViewModelToCheckPriceTrend(
            priceValue: 3_900_000,
            previousPriceValue: 3_900_000
        )
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testIncreasedPriceTrend() {
        let viewModel = Self.makeViewModelToCheckPriceTrend(
            priceValue: 3_900_000,
            previousPriceValue: 2_800_000
        )
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testDecreasedPriceTrend() {
        let viewModel = Self.makeViewModelToCheckPriceTrend(
            priceValue: 3_900_000,
            previousPriceValue: 4_100_000
        )
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    // MARK: Private

    private static func makeViewModelToCheckPriceTrend(
        priceValue: Int,
        previousPriceValue: Int
    ) -> OfferSnippetViewModel? {
        let price = Self.makePrice(priceValue)

        let trend: PriceTrend
        let previousPrice: YREPrice?
        if priceValue > previousPriceValue {
            trend = .increased
            previousPrice = Self.makePrice(previousPriceValue)
        }
        else if priceValue < previousPriceValue {
            trend = .decreased
            previousPrice = Self.makePrice(previousPriceValue)
        }
        else {
            trend = .unchanged
            previousPrice = nil
        }

        let priceInfo = Self.makeOfferPriceInfo(
            price: price,
            previousPrice: previousPrice,
            trend: trend
        )
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: .moscow),
            price: price,
            offerPriceInfo: priceInfo
        )
        return Self.makeViewModel(with: snippet)
    }

    private static func makePrice(_ value: Int) -> YREPrice {
        return YREPrice(
            currency: .RUB,
            value: NSNumber(value: value),
            unit: .perOffer,
            period: .wholeLife
        )
    }

    private static func makeOfferPriceInfo(
        price: YREPrice,
        previousPrice: YREPrice? = nil,
        trend: PriceTrend
    ) -> YREOfferPriceInfo {
        return YREOfferPriceInfo(
            price: price,
            pricePerUnit: nil,
            wholePrice: nil,
            trend: trend,
            previousPrice: previousPrice,
            hasPriceHistory: nil
        )!
    }
}
