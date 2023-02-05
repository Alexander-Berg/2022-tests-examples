import LangExtensions
import XCTest

class NSDataHexStringTests: XCTestCase {

    func test_returnsEmptyString_whenNSDataIsEmpty() {
        let data = NSData()
        let emptyString = ""

        let dataHexString = data.ble_hexString

        XCTAssertEqual(dataHexString, emptyString)
    }

    func test_returnsCorrectHexString() {
        let givenData = NSData(data: Data([
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
            0xA0, 0xB1, 0xC2, 0xD3, 0xE4, 0xF5,
            0xFF, 0x00
        ]))
        let givenDataString = "000102030405a0b1c2d3e4f5ff00"

        let dataString = givenData.ble_hexString

        XCTAssertEqual(dataString, givenDataString)
    }

    func test_returnsCorrectHexStringForToken() {
        let givenData = NSData(data: Data([
            0x90, 0x19, 0x23, 0x84, 0xFF, 0x2E, 0x09, 0x7E,
            0x0D, 0x56, 0x90, 0x84, 0x5B, 0xCB, 0x25, 0x16,
            0x66, 0x9B, 0xD7, 0xDE, 0x9E, 0xA7, 0x53, 0xF6,
            0x20, 0x70, 0xDD, 0xEC, 0xA7, 0x49, 0xF6, 0x07
        ]))
        let givenDataString = "90192384ff2e097e0d5690845bcb2516669bd7de9ea753f62070ddeca749f607"

        let dataString = givenData.ble_hexString

        XCTAssertEqual(dataString, givenDataString)
    }
}
