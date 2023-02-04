//
//  AdsContextTagsGeneratorTests+lot.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 17.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREServiceLayer
@testable import YRELegacyFiltersCore
import YREFiltersModel

extension AdsContextTagsGeneratorTests {
    func testBuyLot() {
        let filterRoot = self.makeFilterRoot(action: .buy)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "участок"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyLotInVillage() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyLotFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.objectType.value = Traits.village

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "участок",
            "в коттеджном поселке"
        ]
        XCTAssertEqual(result, expectation)
    }

    // MARK: Private

    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .lot
        return filterRoot
    }
}
