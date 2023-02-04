//
//  AnalyticsAdjustCriteoCallCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 03/09/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-3932
class AnalyticsAdjustCriteoCallCategoryClassifierTests: XCTestCase {
    func testRentHouse() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.rentHouse

        //
        // Rent duplex house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .duplex,
                isInVillage: false
            ) == category
        )

        //
        // Rent a part of house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .part,
                isInVillage: false
            ) == category
        )

        //
        // Rent whole house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .whole,
                isInVillage: false
            ) == category
        )

        //
        // Rent unknown house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testRentTownHouse() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.rentTownHouse

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .townHouse,
                isInVillage: false
            ) == category
        )
    }

    func testRentRoom() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.rentRoom

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .room,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testRentFlatShort() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.rentFlatShort

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testRentFlatLong() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.rentFlatLong

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: true,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testRentCommercial() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let categoryToCheck = AnalyticsAdjustCriteoCallCategory.rentCommercial

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .commercial,
                houseType: .unknown,
                isInVillage: true
            ) == categoryToCheck
        )
    }

    func testRentGarage() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let categoryToCheck = AnalyticsAdjustCriteoCallCategory.rentGarage

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .rent,
                periodLongNotShort: false,
                offerCategory: .garage,
                houseType: .unknown,
                isInVillage: true
            ) == categoryToCheck
        )
    }

    func testSellLot() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellLot

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .lot,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testSellHouse() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellHouse

        //
        // Sell duplex house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .duplex,
                isInVillage: false
            ) == category
        )

        //
        // Sell a part of house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .part,
                isInVillage: false
            ) == category
        )

        //
        // Sell unknown house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )

        //
        // Sell whole house
        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .whole,
                isInVillage: false
            ) == category
        )
    }

    func testSellTownHouse() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellTownHouse

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .house,
                houseType: .townHouse,
                isInVillage: false
            ) == category
        )
    }

    func testSellRoom() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellRoom

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .room,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testSellSecondaryFlat() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellSecondaryFlat

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: false
            ) == category
        )
    }

    func testSellNewFlat() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellFlatInNewbuilding

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: true,
                offerSaleType: .new(isPrimarySale: false)
            ) == category
        )
    }

    func testPrimarySaleNewFlat() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellFlatInNewbuilding

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: true,
                offerSaleType: .new(isPrimarySale: true)
            ) == category
        )
    }

    func testSellZhkForSite() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellZhk

        XCTAssertTrue(
            classifier.categoryValueForSite(hasPaidCalls: false, isNewSite: true) == category
        )
    }

    func testSellSecondaryFlatForSite() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let category = AnalyticsAdjustCriteoCallCategory.sellSecondaryFlat

        XCTAssertTrue(
            classifier.categoryValueForSite(hasPaidCalls: false, isNewSite: false) == category
        )
    }

    func testSellCommercial() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let categoryToCheck = AnalyticsAdjustCriteoCallCategory.sellCommercial

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .commercial,
                houseType: .unknown,
                isInVillage: true
            ) == categoryToCheck
        )
    }

    func testSellGarage() {
        let classifier = AnalyticsAdjustCriteoCallCategoryClassifier()
        let categoryToCheck = AnalyticsAdjustCriteoCallCategory.sellGarage

        XCTAssertTrue(
            classifier.categoryValue(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .garage,
                houseType: .unknown,
                isInVillage: true
            ) == categoryToCheck
        )
    }
}
