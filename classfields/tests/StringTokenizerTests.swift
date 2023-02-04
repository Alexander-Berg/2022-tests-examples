//
//  StringTokenizerTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Alexey Salangin on 21.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YRECoreUtils
import XCTest

final class StringTokenizerTests: XCTestCase {
    func testTokenizeByWords() {
        let sentence = "Все 🤬, а я — капитан-лейтенант д’Артаньян"
        let expectedWordsWithRanges = [
            ("Все", NSRange(location: 0, length: 3)),
            ("🤬", NSRange(location: 4, length: 2)),
            ("а", NSRange(location: 8, length: 1)),
            ("я", NSRange(location: 10, length: 1)),
            ("капитан-лейтенант", NSRange(location: 14, length: 17)),
            ("д’Артаньян", NSRange(location: 32, length: 10)),
        ]

        let result = sentence.yre_tokenizeByWords()
        zip(result, expectedWordsWithRanges).forEach {
            XCTAssertEqual($0.0, $1.0)
            XCTAssertEqual($0.1, $1.1)
        }
    }

    func testTokenizeByWordsWithRepeatedWords() {
        let sentence = """
        Соображения высшего порядка, а также социально-экономическое развитие обеспечивает широкому кругу \
        специалистов участие в формировании существующих финансовых и административных условий.
        """
        let expectedWordsWithRanges = [
            ("Соображения", NSRange(location: 0, length: 11)),
            ("высшего", NSRange(location: 12, length: 7)),
            ("порядка", NSRange(location: 20, length: 7)),
            ("а", NSRange(location: 29, length: 1)),
            ("также", NSRange(location: 31, length: 5)),
            ("социально-экономическое", NSRange(location: 37, length: 23)),
            ("развитие", NSRange(location: 61, length: 8)),
            ("обеспечивает", NSRange(location: 70, length: 12)),
            ("широкому", NSRange(location: 83, length: 8)),
            ("кругу", NSRange(location: 92, length: 5)),
            ("специалистов", NSRange(location: 98, length: 12)),
            ("участие", NSRange(location: 111, length: 7)),
            ("в", NSRange(location: 119, length: 1)),
            ("формировании", NSRange(location: 121, length: 12)),
            ("существующих", NSRange(location: 134, length: 12)),
            ("финансовых", NSRange(location: 147, length: 10)),
            ("и", NSRange(location: 158, length: 1)),
            ("административных", NSRange(location: 160, length: 16)),
            ("условий", NSRange(location: 177, length: 7)),
        ]

        let result = sentence.yre_tokenizeByWords()
        zip(result, expectedWordsWithRanges).forEach {
            XCTAssertEqual($0.0, $1.0)
            XCTAssertEqual($0.1, $1.1)
        }
    }
}
