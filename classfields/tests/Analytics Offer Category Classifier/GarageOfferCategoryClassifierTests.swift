//
//  GarageOfferCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 20/03/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-4709
class GarageOfferCategoryClassifierTests: XCTestCase {
    func test_Garage() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: false,
                                                offerCategory: .garage,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_garage_Rent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage_Rent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .garage,
                                                houseType: .unknown,
                                                isInVillage: false,
                                                hasPaidCalls: false).contains(category))
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: false,
                                                offerCategory: .garage,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_garage_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage_Sell"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: true,
                                                offerCategory: .garage,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_garage_only_Rent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let longRentCategory = "LongRent"
        let dailyRentCategory = "DailyRent"
        
        // We must not generate `LongRent` or `DailyRent` for garage, because there is no means to do this.
        // When we have means to detect if garage is long rent or daily rent - reconsider.
        
        let longPeriodCategories = classifier.categoryValues(forOfferType: .rent,
                                                             periodLongNotShort: true,
                                                             offerCategory: .garage,
                                                             houseType: .unknown,
                                                             isInVillage: false)
        
        XCTAssertFalse(longPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(longPeriodCategories.contains(dailyRentCategory))
        
        
        let shortPeriodCategories = classifier.categoryValues(forOfferType: .rent,
                                                              periodLongNotShort: false,
                                                              offerCategory: .garage,
                                                              houseType: .unknown,
                                                              isInVillage: false)
        
        XCTAssertFalse(shortPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(shortPeriodCategories.contains(dailyRentCategory))
    }
}
