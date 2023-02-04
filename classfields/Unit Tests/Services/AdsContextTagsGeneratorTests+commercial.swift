//
//  AdsContextTagsGeneratorTests+commercial.swift
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
    func testBuyCommercial() {
        let filterRoot = self.makeFilterRoot(action: .buy)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "коммерческую недвижимость"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testBuyCommercialAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .buy)
        guard let filter = filterRoot.actionCategoryFilter as? YREBuyCommercialFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.commercialType.value = Traits.allBuyCommercialTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "купить",
            "коммерческую недвижимость",
            "офис",
            "торговое помещение",
            "помещение свободного назначения",
            "склад",
            "общепит",
            "гостиницу",
            "автосервис",
            "производственное помещение",
            "участок коммерческого назначения",
            "готовый бизнес"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testRentCommercial() {
        let filterRoot = self.makeFilterRoot(action: .rent)

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "коммерческую недвижимость",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    func testRentCommercialAllTypes() {
        let filterRoot = self.makeFilterRoot(action: .rent)
        guard let filter = filterRoot.actionCategoryFilter as? YRERentCommercialFilter else {
            XCTFail("Invalid `actionCategoryFilter`")
            return
        }
        filter.commercialType.value = Traits.allRentCommercialTypes

        let generator = AdsContextTagsGenerator()
        let result = generator.make(filterRoot: filterRoot)

        let expectation = [
            "снять",
            "коммерческую недвижимость",
            "офис",
            "торговое помещение",
            "помещение свободного назначения",
            "склад",
            "общепит",
            "гостиницу",
            "автосервис",
            "производственное помещение",
            "юридический адрес",
            "участок коммерческого назначения",
            "готовый бизнес",
            "в аренду"
        ]
        XCTAssertEqual(result, expectation)
    }

    // MARK: Private

    fileprivate func makeFilterRoot(action: kYREFilterAction) -> FilterRootProtocol {
        let filterRoot = YREFilterRoot()
        filterRoot.action = action
        filterRoot.category = .commercial
        return filterRoot
    }
}
