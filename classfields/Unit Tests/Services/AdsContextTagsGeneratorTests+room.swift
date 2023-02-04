//
//  AdsContextTagsGeneratorTests+room.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 14.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREServiceLayer
@testable import YRELegacyFiltersCore
import YREFiltersModel

extension AdsContextTagsGeneratorTests {
    func testBuyRoom() {
        let filterRoot = self.makeFilterRoot(action: .buy)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "комнату"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testLongRentRoom() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentRoomFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.long

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "комнату",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testShortRentRoom() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentRoomFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.short

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "комнату",
            "посуточно"
        ]
        XCTAssertEqual(result, expectation)
    }

    // MARK: Private

    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .room
        return filterRoot
    }
}
