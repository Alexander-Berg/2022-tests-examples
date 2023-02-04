//
//  URLComparisonTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Pavel Zhuravlev on 08.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

// swiftlint:disable force_unwrapping

final class URLComparisonTests: XCTestCase {
    func testEqualIdentity() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner/?only-content=true")!

        let result = url.yre_isEqualWithoutQueryItems(to: url)
        XCTAssertTrue(result, "URL must be equal to itself")
    }

    func testEqualDespiteQueryItems() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner/?only-content=true")!
        let anotherUrl = URL(string: "https://arenda.yandex.ru/app/owner/")!

        let result = url.yre_isEqualWithoutQueryItems(to: anotherUrl)
        XCTAssertTrue(result, "URLs must be equal despite the queryItems presence")
    }

    func testLastPathPart() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner/")!
        let anotherUrl = URL(string: "https://arenda.yandex.ru/app/owner")!

        let result = url.yre_isEqualWithoutQueryItems(to: anotherUrl)
        XCTAssertTrue(result, "URLs must be equal")
    }

    func testDifferentSchemes() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner")!
        let anotherUrl = URL(string: "yandexrealty://arenda.yandex.ru/app/owner")!

        let result = url.yre_isEqualWithoutQueryItems(to: anotherUrl)
        XCTAssertFalse(result, "URLs must not be equal")
    }

    func testDifferentHosts() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner")!
        let anotherUrl = URL(string: "https://yandex.ru/app/owner")!

        let result = url.yre_isEqualWithoutQueryItems(to: anotherUrl)
        XCTAssertFalse(result, "URLs must not be equal")
    }

    func testDifferentPaths() {
        let url = URL(string: "https://arenda.yandex.ru/app/owner")!
        let anotherUrl = URL(string: "https://arenda.yandex.ru/app")!

        let result = url.yre_isEqualWithoutQueryItems(to: anotherUrl)
        XCTAssertFalse(result, "URLs must not be equal")
    }
}
