import LangExtensions
import XCTest

final class NSDecimalNumberAdditionsTests: XCTestCase {

    func test_shouldProperlyCompareNumbers_whenOnBiggerThanAnother() {
        // given
        let first = NSDecimalNumber(value: 25)
        let second = NSDecimalNumber(value: 27.0)

        // when
        let shouldBeFalse = first > second
        let shouldBeTrue = first < second

        // then
        XCTAssertTrue(shouldBeTrue)
        XCTAssertFalse(shouldBeFalse)
    }

    func test_shouldProperlyCompareNumbers_whenNumbersAreSame() {
        // given
        let first = NSDecimalNumber(value: 27.33)
        let second = NSDecimalNumber(value: 27.33)

        // when
        let shouldBeFalse_1 = first > second
        let shouldBeFalse_2 = first < second
        let shouldBeTrue = first == second

        // then
        XCTAssertFalse(shouldBeFalse_1)
        XCTAssertFalse(shouldBeFalse_2)
        XCTAssertTrue(shouldBeTrue)
    }

    // MARK: - ble_abs tests

    func test_shouldReturnPositiveNumber_whenInitialIsNegative() {
        // given
        let initial = NSDecimalNumber(value: -105)

        // when
        let abs = initial.ble_abs

        // then
        XCTAssertEqual(abs, NSDecimalNumber(value: 105))
    }

    func test_shouldReturnPositiveNumber_whenInitialIsPositive() {
        // given
        let initial = NSDecimalNumber(value: 1)

        // when
        let abs = initial.ble_abs

        // then
        XCTAssertEqual(abs, NSDecimalNumber(value: 1))
    }

    func test_shouldReturnZero_whenInitialIsZero() {
        // given
        let initial = NSDecimalNumber(value: 0)

        // when
        let abs = initial.ble_abs

        // then
        XCTAssertEqual(abs, NSDecimalNumber(value: 0))
    }

}
