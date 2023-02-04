//
//  Created by Vladislav Kiryukhin on 9/24/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

// https://st.yandex-team.ru/VSAPPS-5314
class HouseAndLotOfferCategoryClassifierTests: XCTestCase {
    // MARK: - Lot

    // Lot_Sell => LotInVillage_Sell, LotInVillage_Sell_Paid + SecondaryLot_Sell

    func test_Lot_PrimarySell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "LotInVillage_Sell"

        let categoryValues = classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .lot,
            houseType: .unknown,
            isInVillage: true,
            hasPaidCalls: false
        )

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_Lot_PrimarySell_Paid() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "LotInVillage_Sell",
            "LotInVillage_Sell_Paid"
        ]
        let categoriesToCheck = Set(classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .lot,
            houseType: .unknown,
            isInVillage: true,
            hasPaidCalls: true
        ))
        XCTAssertTrue(categoriesToCheck.isStrictSuperset(of: expectedCategories))
    }

    func test_Lot_SecondarySell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryLot_Sell"

        let categoryValues = classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .lot,
            houseType: .unknown,
            isInVillage: false,
            hasPaidCalls: false
        )

        XCTAssertTrue(categoryValues.contains(category))
    }

    // MARK: - House

    // House_Sell => HouseInVillage_Sell, HouseInVillage_Sell_Paid + SecondaryHouse_Sell

    func test_House_PrimarySell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "HouseInVillage_Sell"

        let categoryValues = classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .house,
            houseType: .unknown,
            isInVillage: true,
            hasPaidCalls: false
        )

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_House_PrimarySell_Paid() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let expectedCategories: Set = [
            "HouseInVillage_Sell",
            "HouseInVillage_Sell_Paid"
        ]
        let categoriesToCheck = Set(classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .house,
            houseType: .unknown,
            isInVillage: true,
            hasPaidCalls: true
        ))
        XCTAssertTrue(categoriesToCheck.isStrictSuperset(of: expectedCategories))
    }

    func test_House_SecondarySell() {
        let rulesProvider = AnalyticsMetricaAnyOfferCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaAnyOfferCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryHouse_Sell"

        let categoryValues = classifier.categoryValues(
            forOfferType: .sell,
            periodLongNotShort: false,
            offerCategory: .house,
            houseType: .unknown,
            isInVillage: false,
            hasPaidCalls: false
        )

        XCTAssertTrue(categoryValues.contains(category))
    }
}
