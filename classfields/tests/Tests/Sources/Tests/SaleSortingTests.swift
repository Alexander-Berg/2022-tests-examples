//
//  SaleSortingTests.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 08.05.2020.
//

import AutoRuAppConfig
import AutoRuJSON
@testable import AutoRuModels
import XCTest

class SaleSortingTests: BaseUnitTest {
    func testAvailableSortingsCarsNew() {
        let sorting = SaleSorting.availableSortings(forCategory: .cars, forNew: true)
        XCTAssertTrue(sorting.0 == .relevance)
        XCTAssertTrue(sorting.1 == [.relevance, .priceLow, .priceHigh, .name])
    }

    override func tearDown() {
        super.tearDown()
        AppSettings.sharedInstance.experimentProvider.update(expValues: [:])
    }

    func testAvailableSortingsCarsOld() {
        let sorting = SaleSorting.availableSortings(forCategory: .cars, forNew: false)
        XCTAssertTrue(sorting.0 == .relevance)
        XCTAssertTrue(sorting.1 == [
            .relevance,
            .creationNewer,
            .priceLow,
            .priceHigh,
            .dateHigh,
            .dateLow,
            .run,
            .name,
            .uniqueness,
            .priceRating,
            .provenOwnerFirst
        ])
    }

    // MARK: -

    private func setupExpFlags(flags: JSON) {
        AppSettings.sharedInstance.experimentProvider.update(expValues: flags)
    }
}
