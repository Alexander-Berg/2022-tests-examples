import LangExtensions
import XCTest

final class OptionalAdditionsTests: XCTestCase {

    // MARK: - ble_asBool tests

    func test_shouldUseFallback_whenStringIsNotBool() {
        // given
        let target: String? = "someString"
        let fallbackTrue = true
        let fallbackFalse = false

        // when
        let resultOne = target.ble_asBool(fallback: fallbackTrue)
        let resultTwo = target.ble_asBool(fallback: fallbackFalse)

        // then
        XCTAssertEqual(resultOne, fallbackTrue)
        XCTAssertEqual(resultTwo, fallbackFalse)
    }

    func test_shouldUseFallback_whenStringIsNil() {
        // given
        let target: String? = nil
        let fallbackTrue = true
        let fallbackFalse = false

        // when
        let resultOne = target.ble_asBool(fallback: fallbackTrue)
        let resultTwo = target.ble_asBool(fallback: fallbackFalse)

        // then
        XCTAssertEqual(resultOne, fallbackTrue)
        XCTAssertEqual(resultTwo, fallbackFalse)
    }

}
