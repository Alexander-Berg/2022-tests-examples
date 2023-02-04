//
//  CommercialOfferCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 3/1/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-4686
class CommercialOfferCategoryClassifierTests: XCTestCase {
    func test_Commercial() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: false,
                                                offerCategory: .commercial,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_commercial_Rent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial_Rent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent, // rent
                                                periodLongNotShort: true, // true (must not affect)
                                                offerCategory: .commercial, // commercial
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent, // rent
                                                periodLongNotShort: false, // false (must not affect)
                                                offerCategory: .commercial, // commercial
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_commercial_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial_Sell"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell, // sell
                                                periodLongNotShort: true,
                                                offerCategory: .commercial, // commercial
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_commercial_only_Rent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let longRentCategory = "LongRent"
        let dailyRentCategory = "DailyRent"
        
        // We must not generate `LongRent` or `DailyRent` for commercial, because there is no means to do this.
        // When we have means to detect if commercial is long rent or daily rent - reconsider.
        
        let longPeriodCategories = classifier.categoryValues(forOfferType: .rent, // rent
                                                             periodLongNotShort: true, // true
                                                             offerCategory: .commercial, // commercial
                                                             houseType: .unknown,
                                                             isInVillage: false)
        
        XCTAssertFalse(longPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(longPeriodCategories.contains(dailyRentCategory))
        
        
        let shortPeriodCategories = classifier.categoryValues(forOfferType: .rent, // rent
                                                              periodLongNotShort: false, // false
                                                              offerCategory: .commercial, // commercial
                                                              houseType: .unknown,
                                                              isInVillage: false)
        
        XCTAssertFalse(shortPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(shortPeriodCategories.contains(dailyRentCategory))
    }
}
