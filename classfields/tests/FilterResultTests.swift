//
//  Created by Alexey Aleshkov on 23.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import Hayloft

/// https://en.wikipedia.org/wiki/Three-valued_logic

final class FilterResultTests: XCTestCase {
    func testLogicNOT() {
        let cases = [
            (FilterResult.deny, FilterResult.accept),
            (FilterResult.neutral, FilterResult.neutral),
            (FilterResult.accept, FilterResult.deny),
        ]
        cases.forEach({ pair in
            XCTAssertEqual(!pair.0, pair.1)
        })
    }

    func testLogicAND() {
        let cases = [
            ((FilterResult.deny, FilterResult.deny), FilterResult.deny),
            ((FilterResult.deny, FilterResult.neutral), FilterResult.deny),
            ((FilterResult.deny, FilterResult.accept), FilterResult.deny),

            ((FilterResult.neutral, FilterResult.deny), FilterResult.deny),
            ((FilterResult.neutral, FilterResult.neutral), FilterResult.neutral),
            ((FilterResult.neutral, FilterResult.accept), FilterResult.neutral),

            ((FilterResult.accept, FilterResult.deny), FilterResult.deny),
            ((FilterResult.accept, FilterResult.neutral), FilterResult.neutral),
            ((FilterResult.accept, FilterResult.accept), FilterResult.accept),
        ]
        cases.forEach({ pair in
            XCTAssertEqual(pair.0.0 && pair.0.1, pair.1)
        })
    }

    func testLogicOR() {
        let cases = [
            ((FilterResult.deny, FilterResult.deny), FilterResult.deny),
            ((FilterResult.deny, FilterResult.neutral), FilterResult.neutral),
            ((FilterResult.deny, FilterResult.accept), FilterResult.accept),

            ((FilterResult.neutral, FilterResult.deny), FilterResult.neutral),
            ((FilterResult.neutral, FilterResult.neutral), FilterResult.neutral),
            ((FilterResult.neutral, FilterResult.accept), FilterResult.accept),

            ((FilterResult.accept, FilterResult.deny), FilterResult.accept),
            ((FilterResult.accept, FilterResult.neutral), FilterResult.accept),
            ((FilterResult.accept, FilterResult.accept), FilterResult.accept),
        ]
        cases.forEach({ pair in
            XCTAssertEqual(pair.0.0 || pair.0.1, pair.1)
        })
    }
}
