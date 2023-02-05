import Foundation
import XCTest
@testable import YxSwissKnife

class StringExtensionTests: XCTestCase {
    func testSurroundedByDifferentStrings() {
        let str = "_middle_"
        XCTAssert(str.surrounded(by: ("left", "right")) == "left_middle_right")
    }

    func testSurroundedBySameString() {
        let str = "_middle_"
        XCTAssert(str.surrounded(by: "string") == "string_middle_string")
    }

    func testCapitalizingFirstCharacterForEmptyString() {
        let str = ""
        XCTAssert(str.capitalizingFirstCharacter() == "")
    }

    func testCapitalizingFirstCharacterForSingleCharacterString() {
        let first = "A"
        let second = "a"
        XCTAssert(first.capitalizingFirstCharacter() == "A")
        XCTAssert(second.capitalizingFirstCharacter() == "A")
    }

    func testCapitalizingFirstCharacterForLongString() {
        let str = "thIS is A test String"
        XCTAssert(str.capitalizingFirstCharacter() == "ThIS is A test String")
    }
}
