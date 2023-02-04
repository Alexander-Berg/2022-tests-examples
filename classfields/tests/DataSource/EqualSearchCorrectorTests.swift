//
//  EqualSearchCorrectorTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 13.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
@testable import YREServiceLayer

final class EqualSearchCorrectorTests: XCTestCase {
    func testWhenResponseInfoIsNextPage() {
        // given
        let firstResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "first", logQueryText: "logQueryText", sort: "sort"),
            pager: .init(page: 0, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let secondResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "second", logQueryText: "logQueryText", sort: "sort"),
            pager: .init(page: 1, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let corrector = EqualSearchCorrector(currentResponseInfo: firstResponseInfo)

        // when
        let actualResponseInfo = corrector.correct(secondResponseInfo)

        // then
        let expectedResponeInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "first", logQueryText: "logQueryText", sort: "sort"),
            pager: .init(page: 1, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        XCTAssertEqual(actualResponseInfo, expectedResponeInfo, "responseInfos aren't equal")
    }

    func testWhenResponseInfoIsAnotherSearch() {
        // given
        let firstResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "first", logQueryText: "logQueryText", sort: "sort"),
            pager: .init(page: 0, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let secondResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "second", logQueryText: "logQueryText", sort: "sort"),
            pager: .init(page: 0, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let corrector = EqualSearchCorrector(currentResponseInfo: firstResponseInfo)

        // when
        let actualResponseInfo = corrector.correct(secondResponseInfo)

        // then
        let expectedResponeInfo = secondResponseInfo
        XCTAssertEqual(actualResponseInfo, expectedResponeInfo, "responseInfos aren't equal")
    }

    func testWhenResponseInfoHasPageLessThanPrevious() {
        // given
        let firstResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "", logQueryText: "", sort: ""),
            pager: .init(page: 10, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let secondResponseInfo = ResponseInfo(
            searchQuery: .init(logQueryId: "", logQueryText: "", sort: ""),
            pager: .init(page: 0, pageSize: 10, totalItems: 100, totalPages: 10)
        )
        let corrector = EqualSearchCorrector(currentResponseInfo: firstResponseInfo)

        // when
        let actualResponseInfo = corrector.correct(secondResponseInfo)

        // then
        let expectedResponeInfo = secondResponseInfo
        XCTAssertEqual(actualResponseInfo, expectedResponeInfo, "responseInfos aren't equal")
    }
}
