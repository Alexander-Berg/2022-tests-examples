import LangExtensions
import XCTest

final class DataHexStringTests: XCTestCase {

    func test_returnsEmptyString_whenDataIsEmpty() {
        let data = Data()
        let emptyString = ""

        let dataHexString = data.ble_hexString

        XCTAssertEqual(dataHexString, emptyString)
    }

    func test_returnsCorrectHexString() {
        let givenData = Data([
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
            0xA0, 0xB1, 0xC2, 0xD3, 0xE4, 0xF5,
            0xFF, 0x00
        ])
        let givenDataString = "000102030405a0b1c2d3e4f5ff00"

        let dataString = givenData.ble_hexString

        XCTAssertEqual(dataString, givenDataString)
    }
}
