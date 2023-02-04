//
//  Created by Alexey Aleshkov on 23.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import Hayloft

class CompoundFilterTestCase: XCTestCase {
    static func makeEvent() -> Event {
        return Event(
            timestamp: .init(),
            level: .debug,
            message: "",
            context: .init(),
            file: "",
            function: "",
            line: 1,
            loggerIdentifier: "ru.yandex"
        )
    }

    func testNOT() {
        let cases = [
            (FilterResult.deny, FilterResult.accept),
            (FilterResult.neutral, FilterResult.neutral),
            (FilterResult.accept, FilterResult.deny),
        ]

        let event = Self.makeEvent()
        cases.forEach({ pair in
            XCTAssertEqual(
                CompoundFilter.not(ClosureFilter({ _ in pair.0 })).filter(event),
                pair.1
            )
        })
    }

    func testPairAND() {
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

        let event = Self.makeEvent()
        cases.forEach({ pair in
            XCTAssertEqual(
                CompoundFilter.and(ClosureFilter({ _ in pair.0.0 }), ClosureFilter({ _ in pair.0.1 })).filter(event),
                pair.1
            )
        })
    }

    func testPairOR() {
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

        let event = Self.makeEvent()
        cases.forEach({ pair in
            XCTAssertEqual(
                CompoundFilter.or(ClosureFilter({ _ in pair.0.0 }), ClosureFilter({ _ in pair.0.1 })).filter(event),
                pair.1
            )
        })
    }

    func testArrayAND() {
        let cases = [
            ([FilterResult.deny, FilterResult.deny, FilterResult.deny], FilterResult.deny),
            ([FilterResult.deny, FilterResult.deny, FilterResult.neutral], FilterResult.deny),
            ([FilterResult.deny, FilterResult.deny, FilterResult.accept], FilterResult.deny),

            ([FilterResult.deny, FilterResult.neutral, FilterResult.deny], FilterResult.deny),
            ([FilterResult.deny, FilterResult.neutral, FilterResult.neutral], FilterResult.deny),
            ([FilterResult.deny, FilterResult.neutral, FilterResult.accept], FilterResult.deny),

            ([FilterResult.deny, FilterResult.accept, FilterResult.deny], FilterResult.deny),
            ([FilterResult.deny, FilterResult.accept, FilterResult.neutral], FilterResult.deny),
            ([FilterResult.deny, FilterResult.accept, FilterResult.accept], FilterResult.deny),

            ([FilterResult.neutral, FilterResult.deny, FilterResult.deny], FilterResult.deny),
            ([FilterResult.neutral, FilterResult.deny, FilterResult.neutral], FilterResult.deny),
            ([FilterResult.neutral, FilterResult.deny, FilterResult.accept], FilterResult.deny),

            ([FilterResult.neutral, FilterResult.neutral, FilterResult.deny], FilterResult.deny),
            ([FilterResult.neutral, FilterResult.neutral, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.neutral, FilterResult.accept], FilterResult.neutral),

            ([FilterResult.neutral, FilterResult.accept, FilterResult.deny], FilterResult.deny),
            ([FilterResult.neutral, FilterResult.accept, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.accept, FilterResult.accept], FilterResult.neutral),

            ([FilterResult.accept, FilterResult.deny, FilterResult.deny], FilterResult.deny),
            ([FilterResult.accept, FilterResult.deny, FilterResult.neutral], FilterResult.deny),
            ([FilterResult.accept, FilterResult.deny, FilterResult.accept], FilterResult.deny),

            ([FilterResult.accept, FilterResult.neutral, FilterResult.deny], FilterResult.deny),
            ([FilterResult.accept, FilterResult.neutral, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.accept, FilterResult.neutral, FilterResult.accept], FilterResult.neutral),

            ([FilterResult.accept, FilterResult.accept, FilterResult.deny], FilterResult.deny),
            ([FilterResult.accept, FilterResult.accept, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.accept, FilterResult.accept, FilterResult.accept], FilterResult.accept),
        ]

        let event = Self.makeEvent()
        cases.forEach({ pair in
            XCTAssertEqual(
                CompoundFilter.and(pair.0.map({ result in ClosureFilter({ _ in result }) })).filter(event),
                pair.1
            )
        })
    }

    func testArrayOR() {
        let cases = [
            ([FilterResult.deny, FilterResult.deny, FilterResult.deny], FilterResult.deny),
            ([FilterResult.deny, FilterResult.deny, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.deny, FilterResult.deny, FilterResult.accept], FilterResult.accept),

            ([FilterResult.deny, FilterResult.neutral, FilterResult.deny], FilterResult.neutral),
            ([FilterResult.deny, FilterResult.neutral, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.deny, FilterResult.neutral, FilterResult.accept], FilterResult.accept),

            ([FilterResult.deny, FilterResult.accept, FilterResult.deny], FilterResult.accept),
            ([FilterResult.deny, FilterResult.accept, FilterResult.neutral], FilterResult.accept),
            ([FilterResult.deny, FilterResult.accept, FilterResult.accept], FilterResult.accept),

            ([FilterResult.neutral, FilterResult.deny, FilterResult.deny], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.deny, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.deny, FilterResult.accept], FilterResult.accept),

            ([FilterResult.neutral, FilterResult.neutral, FilterResult.deny], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.neutral, FilterResult.neutral], FilterResult.neutral),
            ([FilterResult.neutral, FilterResult.neutral, FilterResult.accept], FilterResult.accept),

            ([FilterResult.neutral, FilterResult.accept, FilterResult.deny], FilterResult.accept),
            ([FilterResult.neutral, FilterResult.accept, FilterResult.neutral], FilterResult.accept),
            ([FilterResult.neutral, FilterResult.accept, FilterResult.accept], FilterResult.accept),

            ([FilterResult.accept, FilterResult.deny, FilterResult.deny], FilterResult.accept),
            ([FilterResult.accept, FilterResult.deny, FilterResult.neutral], FilterResult.accept),
            ([FilterResult.accept, FilterResult.deny, FilterResult.accept], FilterResult.accept),

            ([FilterResult.accept, FilterResult.neutral, FilterResult.deny], FilterResult.accept),
            ([FilterResult.accept, FilterResult.neutral, FilterResult.neutral], FilterResult.accept),
            ([FilterResult.accept, FilterResult.neutral, FilterResult.accept], FilterResult.accept),

            ([FilterResult.accept, FilterResult.accept, FilterResult.deny], FilterResult.accept),
            ([FilterResult.accept, FilterResult.accept, FilterResult.neutral], FilterResult.accept),
            ([FilterResult.accept, FilterResult.accept, FilterResult.accept], FilterResult.accept),
        ]

        let event = Self.makeEvent()
        cases.forEach({ pair in
            XCTAssertEqual(
                CompoundFilter.or(pair.0.map({ result in ClosureFilter({ _ in result }) })).filter(event),
                pair.1
            )
        })
    }
}
