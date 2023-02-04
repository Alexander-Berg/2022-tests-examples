//
//  AnalyticsSearchPlainTraitsClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 10/6/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// swiftlint:disable identifier_name

public class AnalyticsSearchPlainTraitsClassifierTests: XCTestCase {
    public func testSearchPlainCategories() {
        let classifier = AnalyticsSearchPlainTraitsClassifier()
        
        //
        // sell apartment
        let sellApartmentParsingResult = classifier.parsingResult(forFilterAction: .buy, // buy (sell)
                                                                  periodLongNotShort: false,
                                                                  category: .apartment, // apartment
                                                                  buildingOfferType: nil,
                                                                  villageOfferTypes: nil,
                                                                  parkType: NSOrderedSet(),
                                                                  pondType: NSOrderedSet(),
                                                                  expectMetro: false,
                                                                  hasNonGrandmotherRenovation: false,
                                                                  hasTagsToInclude: false,
                                                                  hasTagsToExclude: false,
                                                                  isYandexRent: false)
        XCTAssertEqual(sellApartmentParsingResult.categories, [.sell])
        
        //
        // long rent apartment
        let longRentApartmentParsingResult = classifier.parsingResult(forFilterAction: .rent, // rent
                                                                      periodLongNotShort: true, // long
                                                                      category: .apartment, // apartment
                                                                      buildingOfferType: nil,
                                                                      villageOfferTypes: nil,
                                                                      parkType: NSOrderedSet(),
                                                                      pondType: NSOrderedSet(),
                                                                      expectMetro: false,
                                                                      hasNonGrandmotherRenovation: false,
                                                                      hasTagsToInclude: false,
                                                                      hasTagsToExclude: false,
                                                                      isYandexRent: false)
        XCTAssertEqual(longRentApartmentParsingResult.categories, [.rent, .longRent, .longRentRealty])

        //
        // long rent apartment - Arenda
        let longRentArendaApartmentParsingResult = classifier.parsingResult(forFilterAction: .rent, // rent
                                                                            periodLongNotShort: true, // long
                                                                            category: .apartment, // apartment
                                                                            buildingOfferType: nil,
                                                                            villageOfferTypes: nil,
                                                                            parkType: NSOrderedSet(),
                                                                            pondType: NSOrderedSet(),
                                                                            expectMetro: false,
                                                                            hasNonGrandmotherRenovation: false,
                                                                            hasTagsToInclude: false,
                                                                            hasTagsToExclude: false,
                                                                            isYandexRent: true)
        XCTAssertEqual(longRentArendaApartmentParsingResult.categories, [.rent, .longRent, .longRentArenda])
        
        //
        // daily rent apartment
        let dailyRentApartmentParsingResult = classifier.parsingResult(forFilterAction: .rent, // rent
                                                                       periodLongNotShort: false, // short
                                                                       category: .apartment, // apartment
                                                                       buildingOfferType: nil,
                                                                       villageOfferTypes: nil,
                                                                       parkType: NSOrderedSet(),
                                                                       pondType: NSOrderedSet(),
                                                                       expectMetro: false,
                                                                       hasNonGrandmotherRenovation: false,
                                                                       hasTagsToInclude: false,
                                                                       hasTagsToExclude: false,
                                                                       isYandexRent: false)
        XCTAssertEqual(dailyRentApartmentParsingResult.categories, [.rent, .dailyRent])
    }

    // swiftlint:disable:next function_body_length
    func testSearchPlainOfferTypes() {
        let classifier = AnalyticsSearchPlainTraitsClassifier()
        
        //
        // buy secondary sell apartment
        let buySecondaryApartmentParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .apartment, // apartment
                                     buildingOfferType: .typeSecondHand, // secondary sell
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buySecondaryApartmentParsingResult.types, [.flat, .secondaryFlat])

        //
        // buy site
        let buySiteParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .apartment, // apartment
                                     buildingOfferType: .typeNew, // newbuilding
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buySiteParsingResult.types, [.flat, .newbuilding])
        
        //
        // buy mixed
        let buyMixedParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .apartment, // apartment
                                     buildingOfferType: nil, // mixed
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buyMixedParsingResult.types, [.flat, .mixedFlat])
        
        //
        // buy room
        let buyRoomParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .room, // room
                                     buildingOfferType: nil,
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buyRoomParsingResult.types, [.room])
        
        //
        // buy mixed house
        let buyHouseParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .house, // house
                                     buildingOfferType: nil,
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buyHouseParsingResult.types, [.house, .mixedHouse])
        
        //
        // buy mixed lot
        let buyLotParsingResult =
            classifier.parsingResult(forFilterAction: .buy, // buy
                                     periodLongNotShort: false,
                                     category: .lot, // lot
                                     buildingOfferType: nil,
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(buyLotParsingResult.types, [.lot, .mixedLot])
        
        //
        // impossible category: long rent lot in newbuilding
        let impossibleLongRentLotInNewbuildingParsingResult =
            classifier.parsingResult(forFilterAction: .rent, // rent
                                     periodLongNotShort: true, // long
                                     category: .lot, // lot
                                     buildingOfferType: .typeNew, // newbuilding
                                     villageOfferTypes: nil,
                                     parkType: NSOrderedSet(),
                                     pondType: NSOrderedSet(),
                                     expectMetro: false,
                                     hasNonGrandmotherRenovation: false,
                                     hasTagsToInclude: false,
                                     hasTagsToExclude: false,
                                     isYandexRent: false)
        XCTAssertEqual(impossibleLongRentLotInNewbuildingParsingResult.categories, [.rent, .longRent, .longRentRealty])
        XCTAssertEqual(impossibleLongRentLotInNewbuildingParsingResult.types, [.lot, .villageLot])
    }
}
