//
//  AnalyticsSearchCategoryClassifierSpecialParametersTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 5/16/18.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREFiltersModel
@testable import YREAnalytics

//
// https://st.yandex-team.ru/VSAPPS-3335
// https://st.yandex-team.ru/VSAPPS-4783

// swiftlint:disable:next type_name
class AnalyticsSearchCategoryClassifierSpecialParametersTests: XCTestCase {
    
    let parkTypeParkCategory = "ParkTypePark"
    let pondTypeRiverCategory = "PondTypeRiver"
    let expectMetroCategory = "ExpectMetro"
    
    let hasNonGrandmotherRenovation = "NonGrandmotherRenovation"
    let hasTagsToInclude = "TagsInclude"
    let hasTagsToExclude = "TagsExclude"
    
    typealias Input = AnalyticsSearchCategoryClassifierInput
    
    func test_HasPark() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(array: [FilterParkType.park.rawValue]),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.parkTypeParkCategory))
    }
    
    func test_HasPond() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(array: [FilterPondType.river.rawValue]),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.pondTypeRiverCategory))
    }
    
    func test_ExpectMetro() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: true,  // expectMetro
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.expectMetroCategory))
    }
    
    func test_NonGrandmotherRenovation() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: true,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasNonGrandmotherRenovation))
    }
    
    func test_TagsToInclude() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: true,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasTagsToInclude))
    }
    
    func test_TagsToExclude() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: true,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasTagsToExclude))
    }
    
    func test_All_at_once() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(array: [FilterParkType.park.rawValue]),
                          pondType: NSOrderedSet(array: [FilterPondType.river.rawValue]),
                          expectMetro: true,
                          hasNonGrandmotherRenovation: true,
                          hasTagsToInclude: true,
                          hasTagsToExclude: true,
                          isYandexRent: false)
        
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.parkTypeParkCategory))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.pondTypeRiverCategory))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.expectMetroCategory))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasNonGrandmotherRenovation))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasTagsToInclude))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(self.hasTagsToExclude))
    }
    
    func test_None_at_once() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,  // NO expectMetro
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: false)
        
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.parkTypeParkCategory))
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.pondTypeRiverCategory))
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.expectMetroCategory))
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.hasNonGrandmotherRenovation))
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.hasTagsToInclude))
        XCTAssertFalse(classifier.categoryValues(forInput: input).contains(self.hasTagsToExclude))
    }
}
