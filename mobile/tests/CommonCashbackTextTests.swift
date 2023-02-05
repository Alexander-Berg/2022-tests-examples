import XCTest
@testable import MarketCashback

final class CommonCashbackTextTests: XCTestCase {

    func testSnippetCashback() {
        // when
        let regularCashback = CommonCashbackTextFactory.makeSnippetCashback(100)
        let cashbackFrom = CommonCashbackTextFactory.makeSnippetCashback(100, withFromPrefix: true)

        // then
        XCTAssertEqual(" 100", regularCashback.attributedString.accessibilityLabel)
        XCTAssertEqual("от  100", cashbackFrom.attributedString.accessibilityLabel)
    }

    func testRedesignedSnippetCashback() {
        // when
        let regularCashback = CommonCashbackTextFactory.makeRedesignedCartItemCashback(100)
        let cashbackFrom = CommonCashbackTextFactory.makeRedesignedCartItemCashback(100, withFromPrefix: true)

        // then
        XCTAssertEqual(" 100", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  100", cashbackFrom.accessibilityLabel)
    }

    func testCartItemCashback() {
        // when
        let regularCashback = CommonCashbackTextFactory
            .makeCartItemCashback(
                100,
                withFromPrefix: false,
                isExtra: false
            )
        let cashbackFrom = CommonCashbackTextFactory
            .makeCartItemCashback(
                101,
                withFromPrefix: true,
                isExtra: false
            )

        // then
        XCTAssertEqual(" 100 баллов", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  101 балла", cashbackFrom.accessibilityLabel)
    }

    func testSummaryCashback() {
        // when
        let regularCashback = CommonCashbackTextFactory
            .makeCashbackInSummary(100, withFromPrefix: false)
        let cashbackFrom = CommonCashbackTextFactory
            .makeCashbackInSummary(101, withFromPrefix: true)

        // then
        XCTAssertEqual(" 100", regularCashback.attributedString.accessibilityLabel)
        XCTAssertEqual("от  101", cashbackFrom.attributedString.accessibilityLabel)
    }

    func testCashbackOnPlus() {
        // when
        let regularCashback = CommonCashbackTextFactory.makeCashbackOnPlus(100)

        // then
        XCTAssertEqual(" 100 баллов на Плюс", regularCashback.accessibilityLabel)
    }

    func testCashbackPercent() throws {
        // when
        let percentageString = try XCTUnwrap(CommonCashbackTextFactory.makeCashbackPercentString(10))

        // then
        XCTAssertEqual("￼﻿￼﻿ 10%", percentageString.attributedString.string)
    }

    func testCashbackForPresent() {
        // when
        let regularCashback = CommonCashbackTextFactory.makeCashbackForPresentString(100, font: .bodyRegularStandardM)

        // then
        XCTAssertEqual(" 100 баллов в подарок", regularCashback.attributedString.accessibilityLabel)
    }

}
