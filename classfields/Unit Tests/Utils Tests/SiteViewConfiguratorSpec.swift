//
//  Created by Alexey Aleshkov on 30/11/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YRESnippets
@testable import YandexRealty
import YREModel
import YREModelObjc

final class SiteViewConfiguratorSpec: XCTestCase {
    let five: NSNumber = 5
    let fortytwo: NSNumber = 42

    // Should get min filter stats price
    func testMinPrice() {
        let price5 = YREPrice(currency: .RUB, value: five, unit: .unknown, period: .unknown)
        let price42 = YREPrice(currency: .RUB, value: fortytwo, unit: .unknown, period: .unknown)

        //
        // if one of them is nil, return second one
        let minPriceNil5 = SiteSnippetViewModelGenerator.minPriceByValue(priceA: nil, priceB: price5)
        XCTAssertNotNil(minPriceNil5)
        XCTAssertTrue(minPriceNil5?.value?.isEqual(to: five) == true)

        let minPrice5Nil = SiteSnippetViewModelGenerator.minPriceByValue(priceA: price5, priceB: nil)
        XCTAssertNotNil(minPrice5Nil)
        XCTAssertTrue(minPrice5Nil?.value?.isEqual(to: five) == true)

        // two nils must result in nil
        let minPriceNilNil = SiteSnippetViewModelGenerator.minPriceByValue(priceA: nil, priceB: nil)
        XCTAssertNil(minPriceNilNil)


        // Prices should compere well
        let minPrice5_42 = SiteSnippetViewModelGenerator.minPriceByValue(priceA: price5, priceB: price42)
        XCTAssertNotNil(minPrice5_42)
        XCTAssertTrue(minPrice5_42?.value?.isEqual(to: five) == true)

        let minPrice42_5 = SiteSnippetViewModelGenerator.minPriceByValue(priceA: price42, priceB: price5)
        XCTAssertNotNil(minPrice42_5)
        XCTAssertTrue(minPrice42_5?.value?.isEqual(to: five) == true)

        // Also it must ignore unit, currency and period, but it's hard to compare those.
    }

    // Should get min filter stats area
    func testMinArea() {
        let area5 = YREArea(unit: .m2, value: five.doubleValue)
        let area42 = YREArea(unit: .m2, value: fortytwo.doubleValue)

        //
        // if one of them is nil, return second one
        let minAreaNil5 = SiteSnippetViewModelGenerator.minAreaByValue(areaA: nil, areaB: area5)
        XCTAssertNotNil(minAreaNil5)
        XCTAssertTrue(minAreaNil5?.value == five.doubleValue)

        let minArea5Nil = SiteSnippetViewModelGenerator.minAreaByValue(areaA: area5, areaB: nil)
        XCTAssertNotNil(minArea5Nil)
        XCTAssertTrue(minArea5Nil?.value == five.doubleValue)

        // two nils must result in nil
        let minAreaNilNil = SiteSnippetViewModelGenerator.minAreaByValue(areaA: nil, areaB: nil)
        XCTAssertNil(minAreaNilNil)


        // Prices should compere well
        let minArea5_42 = SiteSnippetViewModelGenerator.minAreaByValue(areaA: area5, areaB: area42)
        XCTAssertNotNil(minArea5_42)
        XCTAssertTrue(minArea5_42?.value == five.doubleValue)

        let minArea42_5 = SiteSnippetViewModelGenerator.minAreaByValue(areaA: area42, areaB: area5)
        XCTAssertNotNil(minArea42_5)
        XCTAssertTrue(minArea42_5?.value == five.doubleValue)

        // Also it must ignore unit, but it's hard to compare those.
    }

    // Primary no totalOffers, resale absent
    func testPrimaryNoTotalOffersResaleAbsent() {
        let price5 = YREPrice(currency: .RUB, value: five, unit: .unknown, period: .unknown)
        let area5 = YREArea(unit: .m2, value: five.doubleValue)

        // If total offers is nil and resale filter stats is absent it must be combined as nil
        let primaryFilterStats = YREFilterStats(totalOffers: nil, minPrice: price5, minArea: area5)
        let statsCombined = SiteSnippetViewModelGenerator.combined(primaryFilterStats: primaryFilterStats,
                                                                   resaleFilterStats: nil)
        XCTAssertNotNil(statsCombined)
        XCTAssertNil(statsCombined?.totalOffers)
    }

    // Primary no totalOffers, resale no total offers
    func testPrimaryNoTotalOffersResaleNoTotalOffers() {
        let price5 = YREPrice(currency: .RUB, value: five, unit: .unknown, period: .unknown)
        let area5 = YREArea(unit: .m2, value: five.doubleValue)

        // If total offers is nil and resale filter stats total offers is nil it must be combined as nil
        let primaryFilterStats = YREFilterStats(totalOffers: nil, minPrice: price5, minArea: area5)
        let resaleFilterStats = YREFilterStats(totalOffers: nil, minPrice: price5, minArea: area5)
        let statsCombined = SiteSnippetViewModelGenerator.combined(primaryFilterStats: primaryFilterStats,
                                                                   resaleFilterStats: resaleFilterStats)
        XCTAssertNotNil(statsCombined)
        XCTAssertNil(statsCombined?.totalOffers)
    }

    // Primary absent, resale five total offers
    func testPrimaryAbsentResaleFiveTotalOffers() {
        let price5 = YREPrice(currency: .RUB, value: five, unit: .unknown, period: .unknown)
        let area5 = YREArea(unit: .m2, value: five.doubleValue)

        // If primary filter stats is absent and resale filter stats has 5 offers, combined must have total offers from primary
        let resaleFilterStats = YREFilterStats(totalOffers: five, minPrice: price5, minArea: area5)
        let statsCombined = SiteSnippetViewModelGenerator.combined(primaryFilterStats: nil,
                                                                   resaleFilterStats: resaleFilterStats)
        XCTAssertNotNil(statsCombined)
        XCTAssertTrue(statsCombined?.totalOffers?.isEqual(to: five) == true)
    }

    // Normal compare, everything present
    func testNormalCompareEverythingPresent() {
        let price5 = YREPrice(currency: .RUB, value: five, unit: .unknown, period: .unknown)
        let price42 = YREPrice(currency: .RUB, value: fortytwo, unit: .unknown, period: .unknown)
        let area5 = YREArea(unit: .m2, value: five.doubleValue)
        let area42 = YREArea(unit: .m2, value: fortytwo.doubleValue)

        let primaryFilterStats = YREFilterStats(totalOffers: fortytwo, minPrice: price5, minArea: area42)
        let resaleFilterStats = YREFilterStats(totalOffers: five, minPrice: price42, minArea: area5)
        let statsCombined = SiteSnippetViewModelGenerator.combined(primaryFilterStats: primaryFilterStats,
                                                                   resaleFilterStats: resaleFilterStats)

        XCTAssertNotNil(statsCombined)

        let sum = (five.intValue + fortytwo.intValue) as NSNumber
        XCTAssertTrue(statsCombined?.totalOffers?.isEqual(to: sum) == true)

        XCTAssertNotNil(statsCombined?.minPrice)
        XCTAssertTrue(statsCombined?.minPrice?.value?.isEqual(to: five) == true)
        XCTAssertNotNil(statsCombined?.minArea)
        XCTAssertTrue(statsCombined?.minArea?.value == five.doubleValue)
    }
}
