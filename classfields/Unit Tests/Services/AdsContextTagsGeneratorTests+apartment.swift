//
//  AdsContextTagsGeneratorTests+apartment.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 09.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREServiceLayer
@testable import YRELegacyFiltersCore
import YREFiltersModel

extension AdsContextTagsGeneratorTests {
    func testBuyApartment() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        
        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "квартиру"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyApartmentInNewBuilding() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.objectType.value = Traits.newbuilding

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "квартиру",
            "в новостройке"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyApartmentWithRoomsCount() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.roomsCount.value = Traits.allRooms

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "однокомнатную",
            "двухкомнатную",
            "трехкомнатную",
            "четырехкомнатную",
            "квартиру",
            "студию"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyApartmentInNewBuildingWithRoomsCount() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.objectType.value = Traits.newbuilding
        filter.roomsCount.value = Traits.allRooms

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "однокомнатную",
            "двухкомнатную",
            "трехкомнатную",
            "четырехкомнатную",
            "квартиру",
            "студию",
            "в новостройке"
        ]
        XCTAssertEqual(result, expectation)
    }
}

extension AdsContextTagsGeneratorTests {
    func testLongRentApartmentWithRoomsCount() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.roomsCount.value = Traits.allRooms
        filter.rentTime.value = Traits.RentTime.long

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "однокомнатную",
            "двухкомнатную",
            "трехкомнатную",
            "четырехкомнатную",
            "квартиру",
            "студию",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testShortRentApartmentWithRoomsCount() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.roomsCount.value = Traits.allRooms
        filter.rentTime.value = Traits.RentTime.short

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "однокомнатную",
            "двухкомнатную",
            "трехкомнатную",
            "четырехкомнатную",
            "квартиру",
            "студию",
            "посуточно"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testLongRentApartment() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.long

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "квартиру",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testShortRentApartment() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentApartamentFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.rentTime.value = Traits.RentTime.short

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "квартиру",
            "посуточно"
        ]
        XCTAssertEqual(result, expectation)
    }
}

// MARK: - Private

extension AdsContextTagsGeneratorTests {
    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .apartment
        return filterRoot
    }
}
