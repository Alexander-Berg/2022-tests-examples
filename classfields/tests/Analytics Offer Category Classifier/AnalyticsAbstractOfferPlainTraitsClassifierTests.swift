//
//  AnalyticsAnyOfferPlainTraitsClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 10/4/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// swiftlint:disable identifier_name

class AnalyticsAnyOfferPlainTraitsClassifierTests: XCTestCase {
    func testSiteCategories() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        // payed site
        let payedSiteParsingResult = classifier.parsingResultForSite(hasPaidCalls: true, isNewSite: nil)
        XCTAssertEqual(payedSiteParsingResult.category, .sell)
        XCTAssertEqual(payedSiteParsingResult.type.plain, .residentialComplex)
        XCTAssertEqual(payedSiteParsingResult.context.hasPaidCalls, true)
        
        // not payed site
        let notPayedSiteParsingResult = classifier.parsingResultForSite(hasPaidCalls: false, isNewSite: nil)
        XCTAssertEqual(notPayedSiteParsingResult.category, .sell)
        XCTAssertEqual(notPayedSiteParsingResult.type.plain, .residentialComplex)
        XCTAssertEqual(notPayedSiteParsingResult.context.hasPaidCalls, false)
    }
    
    func testOfferPlainCategories() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell apartment
        let sellApartmentParsingResult = classifier.parsingResult(forOfferType: .sell,
                                                                  periodLongNotShort: false,
                                                                  offerCategory: .apartment,
                                                                  houseType: .unknown,
                                                                  isInVillage: false)
        XCTAssertEqual(sellApartmentParsingResult.category, .sell)
        
        //
        // long rent apartment
        let longRentApartmentParsingResult = classifier.parsingResult(forOfferType: .rent,
                                                                      periodLongNotShort: true,
                                                                      offerCategory: .apartment,
                                                                      houseType: .unknown,
                                                                      isInVillage: false)
        XCTAssertEqual(longRentApartmentParsingResult.category, .rent(.long))
        
        //
        // daily rent apartment
        let dailyRentApartmentParsingResult = classifier.parsingResult(forOfferType: .rent,
                                                                       periodLongNotShort: false,
                                                                       offerCategory: .apartment,
                                                                       houseType: .unknown,
                                                                       isInVillage: false)
        XCTAssertEqual(dailyRentApartmentParsingResult.category, .rent(.daily))
        
        //
        // unknown offer type apartment (should lead to empty categories)
        let unknownOfferTypeApartmentParsingResult = classifier.parsingResult(forOfferType: .unknown,
                                                                              periodLongNotShort: false,
                                                                              offerCategory: .apartment,
                                                                              houseType: .unknown,
                                                                              isInVillage: false)
        XCTAssertEqual(unknownOfferTypeApartmentParsingResult.category, .unknown)
    }
    
    func testOfferPlainOfferTypes_Sell_Apartment() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell secondary sell apartment
        let sellSecondaryApartmentParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .apartment, // apartment
                                     houseType: .unknown,
                                     isInVillage: false) // secondary sell
        if case .apartment(.secondary) = sellSecondaryApartmentParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }

        //
        // sell apartment in newbuilding
        let sellApartmentInNewbuildingParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .apartment, // apartment
                                     houseType: .unknown,
                                     isInVillage: true,
                                     offerSaleType: .new(isPrimarySale: false)) // offer in newbuilding
        if case .apartment(.newSale) = sellApartmentInNewbuildingParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }
    }
    
    func testOfferPlainOfferTypes_Sell_Room() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell room
        let sellRoomParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .room, // room
                                     houseType: .unknown,
                                     isInVillage: false)
        XCTAssertEqual(sellRoomParsingResult.type.plain, .room)
    }
    
    func testOfferPlainOfferTypes_Sell_House() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell duplex house
        let sellDuplexHouseParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .house, // house
                                     houseType: .duplex,
                                     isInVillage: false)
        if case .house(.secondary, _, _) = sellDuplexHouseParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }

        //
        // sell part of house
        let sellPartHouseParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .house, // house
                                     houseType: .part,
                                     isInVillage: false)
        if case .house(.secondary, _, _) = sellPartHouseParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }

        //
        // sell townhouse
        let sellTownHouseParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .house, // house
                                     houseType: .townHouse,
                                     isInVillage: false)
        if case .house(.secondary, _, .townhouse) = sellTownHouseParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }

        //
        // sell house with unknown type
        let sellUnknownHouseParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .house, // house
                                     houseType: .unknown,
                                     isInVillage: false)
        if case .house(.secondary, _, _) = sellUnknownHouseParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }

        //
        // sell whole house
        let sellWholeHouseParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .house, // house
                                     houseType: .whole,
                                     isInVillage: false)
        if case .house(.secondary, _, _) = sellWholeHouseParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }
    }
    
    func testOfferPlainOfferTypes_Sell_Lot() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell lot
        let sellLotParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .lot, // lot
                                     houseType: .unknown,
                                     isInVillage: false)
        if case .landPlot(.secondary, _) = sellLotParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }
    }
    
    func testOfferPlainOfferTypes_Rent_Lot() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // impossible category: long rent lot in newbuilding
        let impossibleLongRentLotInNewbuildingParsingResult =
            classifier.parsingResult(forOfferType: .rent, // rent
                                     periodLongNotShort: true, // long
                                     offerCategory: .lot, // lot
                                     houseType: .unknown,
                                     isInVillage: true)
        XCTAssertEqual(impossibleLongRentLotInNewbuildingParsingResult.category, .rent(.long))

        if case .landPlot(_, .cottageVillage) = impossibleLongRentLotInNewbuildingParsingResult.type {
            XCTAssert(true)
        }
        else {
            XCTAssert(false)
        }
    }
    
    func testPayedOfferContext() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // payed offer
        let payedParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .apartment,
                                     houseType: .unknown,
                                     isInVillage: true,
                                     hasPaidCalls: true) // hasPaidCalls (true means it is payed)
        XCTAssertEqual(payedParsingResult.context.hasPaidCalls, true)
        
        //
        // not payed offer
        let notPayedParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .apartment,
                                     houseType: .unknown,
                                     isInVillage: true) // hasPaidCalls (false means it is not payed)
        XCTAssertEqual(notPayedParsingResult.context.hasPaidCalls, false)
        
        //
        // not very possible situation, because currently only offers inside sites have stat params and could be payed
        //
        // not payed offer
        let notVeryPossiblePayedParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .apartment,
                                     houseType: .unknown,
                                     isInVillage: false, // not offer inside site
                                     hasPaidCalls: true) // hasPaidCalls (true means it is payed)
        XCTAssertEqual(notVeryPossiblePayedParsingResult.context.hasPaidCalls, true)
    }
    
    func testOfferPlainOfferTypes_Sell_Commercial() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()
        
        //
        // sell lot
        let sellCommercialParsingResult =
            classifier.parsingResult(forOfferType: .sell,
                                     periodLongNotShort: false,
                                     offerCategory: .commercial, // commercial
                                     houseType: .unknown,
                                     isInVillage: false)
        XCTAssertEqual(sellCommercialParsingResult.type.plain, .commercial)
    }

    func testVillageCategories() {
        let classifier = AnalyticsAnyOfferPlainTraitsClassifier()

        // payed village
        let payedSiteParsingResult = classifier.parsingResultForVillage(hasPaidCalls: true)
        XCTAssertEqual(payedSiteParsingResult.category, .sell)
        XCTAssertEqual(payedSiteParsingResult.type.plain, .cottageVillage)
        XCTAssertEqual(payedSiteParsingResult.context.hasPaidCalls, true)

        // not payed village
        let notPayedSiteParsingResult = classifier.parsingResultForVillage(hasPaidCalls: false)
        XCTAssertEqual(notPayedSiteParsingResult.category, .sell)
        XCTAssertEqual(notPayedSiteParsingResult.type.plain, .cottageVillage)
        XCTAssertEqual(notPayedSiteParsingResult.context.hasPaidCalls, false)
    }
}
