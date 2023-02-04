//
//  Created by Dmitry Barillo on 06.06.2022.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREAnalyticsGateway

final class AdjustURLNormalizerTests: XCTestCase {
    func testAdjustURLWithRealtyRedirectPath() throws {
        let url = try XCTUnwrap(URL(string: "https://bzfk.adj.st/realty.yandex.ru/moskva/kupit/kvartira/?newFlat=YES"))
        let normalizedURL = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url, withScheme: nil))
        let expectedURL = try XCTUnwrap(URL(string: "https://realty.yandex.ru/moskva/kupit/kvartira/?newFlat=YES"))
        XCTAssertEqual(normalizedURL, expectedURL)
    }

    func testAdjustURLWithoutRealtyRedirectPath() throws {
        let url = try XCTUnwrap(URL(string: "https://bzfk.adj.st/moskva/kupit/kvartira/?newFlat=YES"))
        let normalizedURL = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url, withScheme: nil))
        let expectedURL = try XCTUnwrap(URL(string: "https://realty.yandex.ru/moskva/kupit/kvartira/?newFlat=YES"))
        XCTAssertEqual(normalizedURL, expectedURL)
    }

    func testRealtyURL() throws {
        let url = try XCTUnwrap(URL(string: "https://realty.yandex.ru/moskva/kupit/kvartira/?newFlat=YES"))
        let normalizedURL = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url, withScheme: nil))
        let expectedURL = try XCTUnwrap(URL(string: "https://realty.yandex.ru/moskva/kupit/kvartira/?newFlat=YES"))
        XCTAssertEqual(normalizedURL, expectedURL)
    }

    func testNonRealtyURL() throws {
        let url1 = try XCTUnwrap(URL(string: "https://realhost.ru/newbuilding/23105"))
        let url2 = try XCTUnwrap(URL(string: "https://newbuilding/23105"))

        let normalizedURL1 = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url1, withScheme: nil))
        let normalizedURL2 = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url2, withScheme: nil))

        let expectedURL1 = try XCTUnwrap(URL(string: "https://realty.yandex.ru/newbuilding/23105"))
        let expectedURL2 = try XCTUnwrap(URL(string: "https://realty.yandex.ru/newbuilding/23105"))

        XCTAssertEqual(normalizedURL1, expectedURL1)
        XCTAssertEqual(normalizedURL2, expectedURL2)
    }

    func testURLWithCustomScheme() throws {
        let url = try XCTUnwrap(URL(string: "https://newbuilding/23105"))
        let normalizedURL = try XCTUnwrap(self.normalizer.normalizedDeepLinkURL(url, withScheme: "realty.ru"))
        let expectedURL = try XCTUnwrap(URL(string: "https://realty.yandex.ru/newbuilding/23105"))
        XCTAssertEqual(normalizedURL, expectedURL)
    }

    private let normalizer = AdjustURLNormalizer.self
}
