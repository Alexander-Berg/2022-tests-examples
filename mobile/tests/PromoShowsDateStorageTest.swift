//
//  PromoShowsDateStorageTest.swift
//  Filters-Unit-Tests
//
//  Created by Timur Turaev on 15.04.2022.
//

import XCTest
@testable import Filters

internal final class PromoShowsDateStorageTest: XCTestCase {
    private var storage: PromoShowsDateStorage!

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.storage = PromoShowsDateStorage()
        XCTAssertTrue(self.storage.innerStorage.isEmpty)
    }

    override func tearDownWithError() throws {
        try super.tearDownWithError()

        self.storage.clear()
    }

    func testStorage() throws {
        let referenceDate = Date(timeIntervalSinceReferenceDate: 100)
        self.storage.setPromoLastShowDate(referenceDate, for: 123, promoKind: .applyLabel)
        XCTAssertFalse(self.storage.innerStorage.isEmpty)

        XCTAssertEqual(self.storage.promoLastShowDateFor(uid: 123, promoKind: .markRead), .distantPast)
        XCTAssertEqual(self.storage.promoLastShowDateFor(uid: 345, promoKind: .applyLabel), .distantPast)

        XCTAssertEqual(self.storage.promoLastShowDateFor(uid: 123, promoKind: .applyLabel), referenceDate)
    }

    func testReadingFromOtherStorage() throws {
        let referenceDate = Date(timeIntervalSinceReferenceDate: 100)
        self.storage.setPromoLastShowDate(referenceDate, for: 123, promoKind: .applyLabel)

        XCTAssertEqual(self.storage.promoLastShowDateFor(uid: 123, promoKind: .applyLabel), referenceDate)
        XCTAssertEqual(PromoShowsDateStorage().promoLastShowDateFor(uid: 123, promoKind: .applyLabel), referenceDate)
    }
}
