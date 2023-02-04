//
//  Created by Vladislav Kiryukhin on 9/24/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalytics

class VSAPPS5314OfferSearchCategoryClassifierTests: XCTestCase {
    typealias Input = AnalyticsSearchCategoryClassifierInput

    // New categories for house & lot

    // MARK: - Lot

    func test_MixedLot() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "MixedLot_Sell"

        let input = Input(
            action: .buy,
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
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_SecondaryLot() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryLot_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .lot,
            buildingOfferType: .typeSecondHand,
            villageOfferTypes: nil,
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_VillageLot() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "VillageLot_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .lot,
            buildingOfferType: .typeNew,
            villageOfferTypes: nil,
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    // MARK: - House

    func test_MixedHouse() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "MixedHouse_Sell"

        let input = Input(
            action: .buy,
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
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_SecondaryHouse() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "SecondaryHouse_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .house,
            buildingOfferType: .typeSecondHand,
            villageOfferTypes: nil,
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_VillageHouse_NoTypes() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let category = "VillageHouse_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .house,
            buildingOfferType: .typeNew,
            villageOfferTypes: nil,
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(category))
    }

    func test_VillageHouse_OneType() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let categoryMain = "VillageHouse_Sell"
        let categoryTownhouse = "Townhouse_VillageHouse_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .house,
            buildingOfferType: .typeNew,
            villageOfferTypes: [.townhouse],
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(categoryMain))
        XCTAssertTrue(categoryValues.contains(categoryTownhouse))
    }

    func test_VillageHouse_MultipleTypes() {
        let rulesProvider = AnalyticsMetricaSearchCategoryClassifierRulesProvider()
        let classifier = AnalyticsMetricaSearchCategoryClassifier(rulesProvider: rulesProvider)
        let categoryMain = "VillageHouse_Sell"
        let categoryTownhouse = "Townhouse_VillageHouse_Sell"
        let categoryLand = "Land_VillageHouse_Sell"

        let input = Input(
            action: .buy,
            periodLongNotShort: false,
            category: .house,
            buildingOfferType: .typeNew,
            villageOfferTypes: [.townhouse, .land],
            parkType: NSOrderedSet(),
            pondType: NSOrderedSet(),
            expectMetro: false,
            hasNonGrandmotherRenovation: false,
            hasTagsToInclude: false,
            hasTagsToExclude: false,
            isYandexRent: false
        )
        let categoryValues = classifier.categoryValues(forInput: input)

        XCTAssertTrue(categoryValues.contains(categoryMain))
        XCTAssertTrue(categoryValues.contains(categoryTownhouse))
        XCTAssertTrue(categoryValues.contains(categoryLand))
    }
}
