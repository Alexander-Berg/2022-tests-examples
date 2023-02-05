import LangExtensions
import XCTest

final class BinaryIntegerAdditionsTests: XCTestCase {

    func testClampedGreaterThanUpperBound() {
        // given
        let value = 40
        let range = 20 ... 30

        // when
        let result = value.clamped(to: range)

        // then
        XCTAssertEqual(result, range.upperBound)
    }

    func testClampedEqualsToUpperBound() {
        // given
        let value = 30
        let range = 20 ... 30

        // when
        let result = value.clamped(to: range)

        // then
        XCTAssertEqual(result, range.upperBound)
    }

    func testClampedLowerThanLowerBound() {
        // given
        let value = 10
        let range = 20 ... 30

        // when
        let result = value.clamped(to: range)

        // then
        XCTAssertEqual(result, range.lowerBound)
    }

    func testClampedEqualsToLowerBound() {
        // given
        let value = 20
        let range = 20 ... 30

        // when
        let result = value.clamped(to: range)

        // then
        XCTAssertEqual(result, range.lowerBound)
    }

    func testClampedInsideBounds() {
        // given
        let value = 25
        let range = 20 ... 30

        // when
        let result = value.clamped(to: range)

        // then
        XCTAssertEqual(result, value)
    }

}
