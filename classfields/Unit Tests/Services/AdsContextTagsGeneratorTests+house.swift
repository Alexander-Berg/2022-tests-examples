//
//  AdsContextTagsGeneratorTests+house.swift
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
    func testBuyHouse() {
        let filterRoot = self.makeFilterRoot(action: .buy)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "дом"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyHouseInVillage() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.objectType.value = Traits.village

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "дом",
            "в коттеджном поселке"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyHouseAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.houseType.value = Traits.allHouseTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "таунхаус",
            "дуплекс",
            "часть дома",
            "дом"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyHouseAllTypesInVillage() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.villageOfferType.value = Traits.allVillageOfferTypes
        filter.objectType.value = Traits.village

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "дом",
            "таунхаус",
            "участок",
            "в коттеджном поселке"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testLongRentHouse() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.long

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "дом",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testShortRentHouse() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.short

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "дом",
            "посуточно"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testRentHouseAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentHouseFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.long
        filter.houseType.value = Traits.allHouseTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "таунхаус",
            "дуплекс",
            "часть дома",
            "дом",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    // MARK: Private

    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .house
        return filterRoot
    }
}
