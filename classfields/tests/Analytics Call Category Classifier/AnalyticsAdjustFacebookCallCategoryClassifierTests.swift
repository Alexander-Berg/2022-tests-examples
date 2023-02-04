//
//  AnalyticsAdjustFacebookCallCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 03/09/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

class AnalyticsAdjustFacebookCallCategoryClassifierTests: XCTestCase {
    func testRentFlatLong() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        let category = AnalyticsAdjustFacebookCallCategory.rentSecondary
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func testRentHouseShort() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        
        let categories = classifier.categoryValues(forOfferType: .rent,
                                                   periodLongNotShort: false,
                                                   offerCategory: .house,
                                                   houseType: .unknown,
                                                   isInVillage: false)
        
        XCTAssertTrue(categories.contains(AnalyticsAdjustFacebookCallCategory.house))
        XCTAssertTrue(categories.contains(AnalyticsAdjustFacebookCallCategory.rentSecondary))
    }
    
    func testBuyNewSaleFlat() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        let category = AnalyticsAdjustFacebookCallCategory.buyNewbuilding
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: true,
                                                offerSaleType: .new(isPrimarySale: false)).contains(category))
    }

    func testBuyPrimarySaleFlat() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        let category = AnalyticsAdjustFacebookCallCategory.buyNewbuilding

        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: true,
                                                offerSaleType: .new(isPrimarySale: true)).contains(category))
    }
    
    func testBuySecondaryFlat() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        let category = AnalyticsAdjustFacebookCallCategory.buySecondary
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func testBuyHouse() {
        let rulesProvider = AnalyticsAdjustFacebookCallCategoryClassifierRulesProvider()
        let classifier = AnalyticsAdjustFacebookCallCategoryClassifier(rulesProvider: rulesProvider)
        
        let categories = classifier.categoryValues(forOfferType: .sell,
                                                   periodLongNotShort: true,
                                                   offerCategory: .house,
                                                   houseType: .unknown,
                                                   isInVillage: false)
        
        XCTAssertTrue(categories.contains(AnalyticsAdjustFacebookCallCategory.house))
        XCTAssertTrue(categories.contains(AnalyticsAdjustFacebookCallCategory.buySecondary))
    }
}
