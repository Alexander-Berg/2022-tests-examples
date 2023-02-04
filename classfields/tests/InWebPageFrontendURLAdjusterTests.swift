//
//  InWebPageFrontendURLAdjusterTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Alexey Salangin on 9/25/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class InWebPageFrontendURLAdjusterTests: XCTestCase {
    func testHTTPURLAdjustment() {
        let convertedHttpURL = self.urlAdjuster.adjustURLToLoad(self.basicHttpURL, options: [.onlyContent])
        XCTAssertEqual(convertedHttpURL, self.basicHttpExpectedURL)
    }

    func testHTTPSURLAdjustment() {
        let convertedHttpsURL = self.urlAdjuster.adjustURLToLoad(self.basicHttpsURL, options: [.onlyContent])
        XCTAssertEqual(convertedHttpsURL, self.basicHttpsExpectedURL)
    }

    func testNonHTTPURLAdjustment() {
        let convertedNonHttpURL = self.urlAdjuster.adjustURLToLoad(self.basicNonHttpURL, options: [.onlyContent])
        XCTAssertEqual(convertedNonHttpURL, self.basicNonHttpURL)
    }

    func testHTTPURLAdjustmentWithQueryItems() {
        let convertedHttpURLWithParameter = self.urlAdjuster.adjustURLToLoad(
            self.basicHttpURLWithParameterURL,
            options: [.onlyContent]
        )
        XCTAssertEqual(convertedHttpURLWithParameter, self.basicHttpURLWithParameterExpectedURL)
    }

    func testHTTPURLAdjustmentWithOnlyContentQyeryItem() {
        let convertedHttpURLWithParameter = self.urlAdjuster.adjustURLToLoad(
            self.basicHttpURLWithContentOnlyParameterURL,
            options: [.onlyContent]
        )
        XCTAssertEqual(convertedHttpURLWithParameter, self.basicHttpExpectedURL)
    }

    func testInfiniteLoop() {
        let convertedHttpURL = self.urlAdjuster.adjustURLToLoad(self.basicHttpURL, options: [.onlyContent])
        XCTAssertEqual(convertedHttpURL, self.basicHttpExpectedURL)

        let secondlyConvertedHttpHttpURL = self.urlAdjuster.adjustURLToLoad(convertedHttpURL, options: [.onlyContent])
        XCTAssertEqual(secondlyConvertedHttpHttpURL, self.basicHttpExpectedURL)
    }

    func testNonRealtyURLAdjustment() {
        let convertedHttpURL = self.urlAdjuster.adjustURLToLoad(self.basicHttpNonRealtyURL, options: [.onlyContent])
        XCTAssertEqual(convertedHttpURL, self.basicHttpNonRealtyURL)
    }

    func testAdjustmentWithoutSelectedOption() {
        let convertedHttpURL = self.urlAdjuster.adjustURLToLoad(self.basicHttpURL, options: [])
        XCTAssertEqual(convertedHttpURL, self.basicHttpURL)
        XCTAssertNotEqual(convertedHttpURL, self.basicHttpExpectedURL)
    }

    func testAdjustmentWithoutEnabledOption() {
        let urlAdjuster = InWebPageFrontendURLAdjuster(enabledOptions: [])

        let convertedHttpURL = urlAdjuster.adjustURLToLoad(self.basicHttpURL, options: [.onlyContent])
        XCTAssertEqual(convertedHttpURL, self.basicHttpURL)
        XCTAssertNotEqual(convertedHttpURL, self.basicHttpExpectedURL)
    }

    private let basicHttpURL = URL(string: "http://realty.yandex.ru/")
    private let basicName = "only-content"
    private let basicNewValue = "true"
    private let basicHttpExpectedURL = URL(string: "http://realty.yandex.ru/?only-content=true")
    private let basicHttpsURL = URL(string: "https://realty.yandex.ru/")
    private let basicHttpsExpectedURL = URL(string: "https://realty.yandex.ru/?only-content=true")
    private let basicNonHttpURL = URL(string: "tel://123456789")
    private let basicHttpURLWithParameterURL = URL(string: "http://realty.yandex.ru/?some=true")
    private let basicHttpURLWithParameterExpectedURL = URL(string: "http://realty.yandex.ru/?some=true&only-content=true")
    private let basicHttpURLWithContentOnlyParameterURL = URL(string: "http://realty.yandex.ru/?only-content=true")
    private let basicHttpNonRealtyURL = URL(string: "http://yandex.ru/")

    private let urlAdjuster = InWebPageFrontendURLAdjuster(enabledOptions: [.onlyContent, .darkMode])
}
