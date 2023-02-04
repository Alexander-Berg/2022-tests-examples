//
//  VSAPPS-4686-SearchCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 3/1/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

class VSAPPS4686SearchCategoryClassifierTests: XCTestCase {
    
    typealias Input = AnalyticsSearchCategoryClassifierInput
    
    func test_Commercial() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .commercial,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(category))
    }
    
    func test_commercial_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial_Rent"
        
        
        let longRentInput = Input(action: .rent,  // rent
                                  periodLongNotShort: true, // true (must not affect)
                                  category: .commercial, // commercial
                                  buildingOfferType: nil,
                                  villageOfferTypes: nil,
                                  parkType: NSOrderedSet(),
                                  pondType: NSOrderedSet(),
                                  expectMetro: false,
                                  hasNonGrandmotherRenovation: false,
                                  hasTagsToInclude: false,
                                  hasTagsToExclude: false,
                                  isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: longRentInput).contains(category))
        
        let shortRentInput = Input(action: .rent, // rent
                                   periodLongNotShort: false, // false (must not affect)
                                   category: .commercial, // commercial
                                   buildingOfferType: nil,
                                   villageOfferTypes: nil,
                                   parkType: NSOrderedSet(),
                                   pondType: NSOrderedSet(),
                                   expectMetro: false,
                                   hasNonGrandmotherRenovation: false,
                                   hasTagsToInclude: false,
                                   hasTagsToExclude: false,
                                   isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: shortRentInput).contains(category))
    }
    
    func test_commercial_Buy() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Commercial_Sell"
        
        let input = Input(action: .buy, // buy
                          periodLongNotShort: true,
                          category: .commercial, // commercial
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(category))
    }
    
    func test_commercial_only_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let longRentCategory = "LongRent"
        let dailyRentCategory = "DailyRent"
        
        // We must not generate `LongRent` or `DailyRent` for commercial, because there is no means to do this.
        // When we have means to detect if commercial is long rent or daily rent - reconsider.
        
        let longRentInput = Input(action: .rent,  // rent
                                  periodLongNotShort: true, // true (must not affect)
                                  category: .commercial, // commercial
                                  buildingOfferType: nil,
                                  villageOfferTypes: nil,
                                  parkType: NSOrderedSet(),
                                  pondType: NSOrderedSet(),
                                  expectMetro: false,
                                  hasNonGrandmotherRenovation: false,
                                  hasTagsToInclude: false,
                                  hasTagsToExclude: false,
                                  isYandexRent: false)
        let longPeriodCategories = classifier.categoryValues(forInput: longRentInput)
        
        XCTAssertFalse(longPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(longPeriodCategories.contains(dailyRentCategory))
        
        let shortRentInput = Input(action: .rent, // rent
                                   periodLongNotShort: false, // false (must not affect)
                                   category: .commercial, // commercial
                                   buildingOfferType: nil,
                                   villageOfferTypes: nil,
                                   parkType: NSOrderedSet(),
                                   pondType: NSOrderedSet(),
                                   expectMetro: false,
                                   hasNonGrandmotherRenovation: false,
                                   hasTagsToInclude: false,
                                   hasTagsToExclude: false,
                                   isYandexRent: false)
        let shortPeriodCategories = classifier.categoryValues(forInput: shortRentInput)
        
        XCTAssertFalse(shortPeriodCategories.contains(longRentCategory))
        XCTAssertFalse(shortPeriodCategories.contains(dailyRentCategory))
    }
}
