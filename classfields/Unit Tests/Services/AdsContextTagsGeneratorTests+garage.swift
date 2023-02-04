//
//  AdsContextTagsGeneratorTests+garage.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 18.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREServiceLayer
@testable import YRELegacyFiltersCore
import YREFiltersModel

extension AdsContextTagsGeneratorTests {
    func testBuyGarage() {
        let filterRoot = self.makeFilterRoot(action: .buy)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "гараж"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyGarageAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyGarageFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.garageType.value = Traits.allGarageTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "бокс",
            "гараж",
            "машиноместо"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testRentGarage() {
        let filterRoot = self.makeFilterRoot(action: .rent)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "гараж",
            // Don't use it here
            // "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testRentGarageAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentGarageFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.garageType.value = Traits.allGarageTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "бокс",
            "гараж",
            "машиноместо",
            // Don't use it here
            // "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    // MARK: Private

    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .garage
        return filterRoot
    }
}
