//
//  Created by Fedor Solovev on 11.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import Typograf

final class TypografTests: XCTestCase {
    func testEmpty() {
        XCTAssertEqual(Typograf.insertNBSPs(""), "")
    }

    func testWithoutNbsp() {
        XCTAssertEqual(Typograf.insertNBSPs("Строка, содержащая только обычные пробелы"),
                       "Строка, содержащая только обычные пробелы")
    }

    func testSpaceIsLastSymbol() {
        XCTAssertEqual(Typograf.insertNBSPs("Расставим все точки перед и "),
                       "Расставим все точки перед и")
    }

    func testPrefix() {
        XCTAssertEqual(Typograf.insertNBSPs("Совет да любовь"),
                       "Совет\(SpecialSymbol.nbsp)да любовь")
    }

    func testPrefixAfterSpace() {
        XCTAssertEqual(Typograf.insertNBSPs("Совет и да любовь"),
                       "Совет и\(SpecialSymbol.nbsp)да любовь")
    }

    func testPostfix() {
        XCTAssertEqual(Typograf.insertNBSPs("Меч и магия"),
                       "Меч и\(SpecialSymbol.nbsp)магия")
    }

    func testPostfixAfterSpace() {
        XCTAssertEqual(Typograf.insertNBSPs("Меч и и магия"),
                       "Меч и\(SpecialSymbol.nbsp)и\(SpecialSymbol.nbsp)магия")
    }

    func testNumber() {
        XCTAssertEqual(Typograf.insertNBSPs("Целочисленное 12345 проверка, вещественное 12345.12345 проверка"),
                       "Целочисленное 12345\(SpecialSymbol.nbsp)проверка, вещественное 12345.12345\(SpecialSymbol.nbsp)проверка")
    }

    func testWithExistingNBSP() {
        XCTAssertEqual(Typograf.insertNBSPs("Специальный\(SpecialSymbol.nbsp)пробел"),
                       "Специальный\(SpecialSymbol.nbsp)пробел")
    }
}
