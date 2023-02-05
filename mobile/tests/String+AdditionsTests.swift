import LangExtensions
import XCTest

final class StringAdditionsTests: XCTestCase {

    // MARK: - ble_ranges tests

    private let testSubstring = "aaa"

    func testRangesWithEmptyString() {
        // given
        let input = ""

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertTrue(output.isEmpty)
    }

    func testRangesWithStringThatDoesntContainsSubstring() {
        // given
        let input = "bbbccc"

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertTrue(output.isEmpty)
    }

    func testRangesWithStringThatEqualsSubstring() {
        // given
        let input = "aaa"
        let expectedOutput = [input.startIndex ..< input.endIndex]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    func testRangesWithStringThatContainsSubstringAtStart() {
        // given
        let input = "aaabb"
        let expectedOutput = [input.startIndex ..< input.index(input.startIndex, offsetBy: 3)]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    func testRangesWithStringThatContainsSubstringAtMiddle() {
        // given
        let input = "baaab"
        let expectedOutput = [input.index(input.startIndex, offsetBy: 1) ..< input.index(input.startIndex, offsetBy: 4)]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    func testRangesWithStringThatContainsSubstringAtEnd() {
        // given
        let input = "bbaaa"
        let expectedOutput = [input.index(input.startIndex, offsetBy: 2) ..< input.endIndex]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    func testRangesWithStringThatContainsOverlappingOccurencesOfSubstring() {
        // given
        let input = "aaaaa"
        let expectedOutput = [input.startIndex ..< input.index(input.startIndex, offsetBy: 3)]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    func testRangesWithStringThatContainsNonOverlappingOccurencesOfSubstring() {
        // given
        let input = "aaaaaa"
        let expectedOutput = [
            input.startIndex ..< input.index(input.startIndex, offsetBy: 3),
            input.index(input.startIndex, offsetBy: 3) ..< input.endIndex
        ]

        // when
        let output = input.ble_ranges(of: testSubstring)

        // then
        XCTAssertEqual(output, expectedOutput)
    }

    // MARK: - ble_boolValue tests

    func test_shouldParseTrue_whenStringIsTrue() {
        // given
        let expectedLowercased = true
        let expectedUppercased = true
        let expectedMixed = true

        let targetLowercased = "true"
        let targetUppercased = "TRUE"
        let targetMixed = "tRUe"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue
        let resultMixed = targetMixed.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
        XCTAssertEqual(resultMixed, expectedMixed)
    }

    func test_shouldParseTrue_whenStringIsYes() {
        // given
        let expectedLowercased = true
        let expectedUppercased = true
        let expectedMixed = true

        let targetLowercased = "yes"
        let targetUppercased = "YES"
        let targetMixed = "YeS"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue
        let resultMixed = targetMixed.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
        XCTAssertEqual(resultMixed, expectedMixed)
    }

    func test_shouldParseTrue_whenStringIs1() {
        // given
        let expectedResult = true

        let target = "1"

        // when
        let result = target.ble_boolValue

        // then
        XCTAssertEqual(result, expectedResult)
    }

    func test_shouldParseTrue_whenStringIsY() {
        // given
        let expectedLowercased = true
        let expectedUppercased = true

        let targetLowercased = "y"
        let targetUppercased = "Y"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
    }

    func test_shouldParseTrue_whenStringIsT() {
        // given
        let expectedLowercased = true
        let expectedUppercased = true

        let targetLowercased = "t"
        let targetUppercased = "T"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
    }

    func test_shouldParseFalse_whenStringIsFalse() {
        // given
        let expectedLowercased = false
        let expectedUppercased = false
        let expectedMixed = false

        let targetLowercased = "false"
        let targetUppercased = "FALSE"
        let targetMixed = "faLsE"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue
        let resultMixed = targetMixed.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
        XCTAssertEqual(resultMixed, expectedMixed)
    }

    func test_shouldParseTrue_whenStringIsNo() {
        // given
        let expectedLowercased = false
        let expectedUppercased = false
        let expectedMixed = false

        let targetLowercased = "no"
        let targetUppercased = "NO"
        let targetMixed = "No"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue
        let resultMixed = targetMixed.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
        XCTAssertEqual(resultMixed, expectedMixed)
    }

    func test_shouldParseFalse_whenStringIs0() {
        // given
        let expectedResult = false

        let target = "0"

        // when
        let result = target.ble_boolValue

        // then
        XCTAssertEqual(result, expectedResult)
    }

    func test_shouldParseFalse_whenStringIsN() {
        // given
        let expectedLowercased = false
        let expectedUppercased = false

        let targetLowercased = "n"
        let targetUppercased = "N"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
    }

    func test_shouldParseFalse_whenStringIsF() {
        // given
        let expectedLowercased = false
        let expectedUppercased = false

        let targetLowercased = "f"
        let targetUppercased = "F"

        // when
        let resultLowercased = targetLowercased.ble_boolValue
        let resultUppercased = targetUppercased.ble_boolValue

        // then
        XCTAssertEqual(resultLowercased, expectedLowercased)
        XCTAssertEqual(resultUppercased, expectedUppercased)
    }

}
