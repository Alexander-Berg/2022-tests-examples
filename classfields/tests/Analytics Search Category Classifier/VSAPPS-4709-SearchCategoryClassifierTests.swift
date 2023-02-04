//
//  VSAPPS-4709-SearchCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 05/03/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// swiftlint:disable:next type_name
class VSAPPS_4709_SearchCategoryClassifierTests: XCTestCase {
    
    typealias Input = AnalyticsSearchCategoryClassifierInput
    
    func test_Garage() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .garage,
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
    
    func test_garage_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage_Rent"
        
        let longRentInput = Input(action: .rent,
                                  periodLongNotShort: true, // true (must not affect)
                                  category: .garage,
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
        
        let shortRentInput = Input(action: .rent,
                                   periodLongNotShort: false, // false (must not affect)
                                   category: .garage,
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
    
    func test_garage_Buy() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Garage_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: true,
                          category: .garage,
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
    
    func test_garage_only_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let longRentCategory = "LongRent"
        let dailyRentCategory = "DailyRent"
        
        // We must not generate `LongRent` or `DailyRent` for garage, because there is no means to do this.
        // When we have means to detect if garage is long rent or daily rent - reconsider.
        
        let longRentInput = Input(action: .rent,
                                  periodLongNotShort: true, // true (must not affect)
                                  category: .garage,
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
        
        let shortRentInput = Input(action: .rent,
                                   periodLongNotShort: false, // false (must not affect)
                                   category: .garage,
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
