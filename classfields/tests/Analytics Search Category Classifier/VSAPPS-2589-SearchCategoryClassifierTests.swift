//
//  VSAPPS-2589-SearchCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 10/6/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// swiftlint:disable:next type_body_length
class VSAPPS2589SearchCategoryClassifierTests: XCTestCase {
    
    typealias Input = AnalyticsSearchCategoryClassifierInput
    
    func test_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Sell"
        
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
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(category))
    }
    
    func test_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Rent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .apartment,
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
    
    func test_LongRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "LongRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: true,
                          category: .apartment,
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
    
    func test_DailyRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "DailyRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .apartment,
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
    
    func test_Flat_LongRent_Realty() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_LongRent"
        let subcategory = "Flat_LongRent_Realty"
        
        let input = Input(action: .rent,
                          periodLongNotShort: true,
                          category: .apartment,
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
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(subcategory))
    }

    func test_Flat_LongRent_Arenda() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_LongRent"
        let subcategory = "Flat_LongRent_Arenda"

        let input = Input(action: .rent,
                          periodLongNotShort: true,
                          category: .apartment,
                          buildingOfferType: nil,
                          villageOfferTypes: nil,
                          parkType: NSOrderedSet(),
                          pondType: NSOrderedSet(),
                          expectMetro: false,
                          hasNonGrandmotherRenovation: false,
                          hasTagsToInclude: false,
                          hasTagsToExclude: false,
                          isYandexRent: true)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(category))
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(subcategory))
    }
    
    func test_Flat_DailyRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_DailyRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .apartment,
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
    
    func test_Flat_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_Rent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .apartment,
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
    
    func test_House_LongRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_LongRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: true,
                          category: .house,
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
    
    func test_House_DailyRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_DailyRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .house,
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
    
    func test_House_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_Rent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .house,
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
    
    func test_Room_LongRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_LongRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: true,
                          category: .room,
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
    
    func test_Room_DailyRent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_DailyRent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .room,
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
    
    func test_Room_Rent() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_Rent"
        
        let input = Input(action: .rent,
                          periodLongNotShort: false,
                          category: .room,
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
    
    func test_MixedFlat_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "MixedFlat_Sell"
        
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
                          hasTagsToExclude: false,
                          isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: input).contains(category))
    }
    
    func test_SecondaryFlat_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryFlat_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: .typeSecondHand,
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
    
    func test_Newbuilding_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Newbuilding_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: .typeNew,
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
    
    func test_Flat_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: .typeNew,
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
    
    func test_Flat() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .apartment,
                          buildingOfferType: .typeNew,
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
    
    func test_House_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .house,
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
    
    func test_House() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .house,
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
    
    func test_Room_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .room,
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
    
    func test_Room() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .room,
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
    
    func test_Lot_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Lot_Sell"
        
        let input = Input(action: .buy,
                          periodLongNotShort: false,
                          category: .lot,
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
    
    func test_Flat_Sell__Room_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_Sell, Room_Sell"
        
        // flat sell
        let flatSellInput = Input(action: .buy,
                                  periodLongNotShort: false,
                                  category: .apartment,
                                  buildingOfferType: nil,
                                  villageOfferTypes: nil,
                                  parkType: NSOrderedSet(),
                                  pondType: NSOrderedSet(),
                                  expectMetro: false,
                                  hasNonGrandmotherRenovation: false,
                                  hasTagsToInclude: false,
                                  hasTagsToExclude: false,
                                  isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: flatSellInput).contains(category))
        
        // room sell
        let roomSellInput = Input(action: .buy,
                                  periodLongNotShort: false,
                                  category: .room,
                                  buildingOfferType: nil,
                                  villageOfferTypes: nil,
                                  parkType: NSOrderedSet(),
                                  pondType: NSOrderedSet(),
                                  expectMetro: false,
                                  hasNonGrandmotherRenovation: false,
                                  hasTagsToInclude: false,
                                  hasTagsToExclude: false,
                                  isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: roomSellInput).contains(category))
    }
    
    func test_Lot_Sell__House_Sell() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Lot_Sell, House_Sell"
        
        // lot sell
        let lotSellInput = Input(action: .buy,
                                 periodLongNotShort: false,
                                 category: .lot,
                                 buildingOfferType: nil,
                                 villageOfferTypes: nil,
                                 parkType: NSOrderedSet(),
                                 pondType: NSOrderedSet(),
                                 expectMetro: false,
                                 hasNonGrandmotherRenovation: false,
                                 hasTagsToInclude: false,
                                 hasTagsToExclude: false,
                                 isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: lotSellInput).contains(category))
        
        // house sell
        let houseSellInput = Input(action: .buy,
                                   periodLongNotShort: false,
                                   category: .house,
                                   buildingOfferType: nil,
                                   villageOfferTypes: nil,
                                   parkType: NSOrderedSet(),
                                   pondType: NSOrderedSet(),
                                   expectMetro: false,
                                   hasNonGrandmotherRenovation: false,
                                   hasTagsToInclude: false,
                                   hasTagsToExclude: false,
                                   isYandexRent: false)
        XCTAssertTrue(classifier.categoryValues(forInput: houseSellInput).contains(category))
    }
}
