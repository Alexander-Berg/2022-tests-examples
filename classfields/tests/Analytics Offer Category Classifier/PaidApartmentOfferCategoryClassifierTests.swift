//
//  PaidApartmentOfferCategoryClassifierTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 10/6/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-3385
class PaidApartmentOfferCategoryClassifierTests: XCTestCase {
    func test_SaleNewFlat_Paid() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "Sell",
            "NewFlatSale",
            "NewFlatSale_Paid"
        ]
        let categoriesToCheck = Set(classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .apartment,
            houseType: .unknown,
            isInVillage: true,
            offerSaleType: .new(isPrimarySale: false),
            hasPaidCalls: true
        ))
        XCTAssertEqual(categoriesToCheck, expectedCategories)
    }

    func test_PrimarySaleNewFlat_Paid() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "Sell",
            "NewFlatSale",
            "NewFlatSale_Paid",
            "NewFlatSale_Primary",
            "NewFlatSale_Primary_Paid"
        ]
        let categoriesToCheck = Set(classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .apartment,
            houseType: .unknown,
            isInVillage: true,
            offerSaleType: .new(isPrimarySale: true),
            hasPaidCalls: true
        ))
        XCTAssertEqual(categoriesToCheck, expectedCategories)
    }
    
    func test_ZhkNewbuilding_Sell_Paid() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "Sell",
            "ZhkNewbuilding_Sell",
            "ZhkNewbuilding_Sell_Paid"
        ]
        let categoriesToCheck = Set(classifier.categoryValuesForSite(
            hasPaidCalls: true,
            isNewSite: nil
        ))
        XCTAssertEqual(categoriesToCheck, expectedCategories)
    }
}
