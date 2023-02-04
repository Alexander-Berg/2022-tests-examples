//
//  CommonOfferCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 10/6/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-2589
class CommonOfferCategoryClassifierTests: XCTestCase {
    func test_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Sell"

        XCTAssertTrue(classifier.categoryValuesForSite(hasPaidCalls: false, isNewSite: nil).contains(category))
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: false,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_Rent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Rent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: false,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_LongRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "LongRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_Flat_LongRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_LongRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_Flat_DailyRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Flat_DailyRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: false,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_House_LongRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_LongRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .house,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_House_DailyRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "House_DailyRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: false,
                                                offerCategory: .house,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_Room_LongRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_LongRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: true,
                                                offerCategory: .room,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_Room_DailyRent() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_DailyRent"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .rent,
                                                periodLongNotShort: false,
                                                offerCategory: .room,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_SecondaryFlat_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryFlat_Sell"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: false,
                                                offerCategory: .apartment,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
    
    func test_SaleNewFlat_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "Sell",
            "NewFlatSale"
        ]
        let categoriesToCheck = Set(
            classifier.categoryValues(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: true,
                offerSaleType: .new(isPrimarySale: false)
            )
        )
        XCTAssertEqual(categoriesToCheck, expectedCategories)
    }

    func test_PrimarySaleNewFlat_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "Sell",
            "NewFlatSale",
            "NewFlatSale_Primary"
        ]
        let categoriesToCheck = Set(
            classifier.categoryValues(
                forOfferType: .sell,
                periodLongNotShort: false,
                offerCategory: .apartment,
                houseType: .unknown,
                isInVillage: true,
                offerSaleType: .new(isPrimarySale: true)
            )
        )
        XCTAssertEqual(categoriesToCheck, expectedCategories)
    }

    func test_ZhkNewbuildingForSite_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "ZhkNewbuilding_Sell"
        XCTAssertTrue(classifier.categoryValuesForSite(hasPaidCalls: true, isNewSite: nil).contains(category))
    }

    func test_Room_Sell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "Room_Sell"
        
        XCTAssertTrue(classifier.categoryValues(forOfferType: .sell,
                                                periodLongNotShort: false,
                                                offerCategory: .room,
                                                houseType: .unknown,
                                                isInVillage: false).contains(category))
    }
}
