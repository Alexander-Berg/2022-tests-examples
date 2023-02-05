import Foundation
import XCTest
@testable import YxSwissKnife

class SequenceLinqTestCase: XCTestCase {
    func testLazyUnique() {
        let uniqueCollection = ["1", "2", "3", "1", "2", "4"].lazy.unique()
        XCTAssertEqual(uniqueCollection.toArray(), ["1", "2", "3", "4"])
    }

    func testLazyUniqueSequenceIsReusable() {
        let uniqueCollection = ["1", "2", "3", "1", "2", "4"].lazy.unique()
        let firstUse = uniqueCollection.toArray()
        let secondUse = uniqueCollection.toArray()
        XCTAssertEqual(firstUse, secondUse)
    }

    func testUnique() {
        let uniqueArray = ["1", "2", "3", "1", "2", "4"].unique()
        XCTAssertEqual(uniqueArray, ["1", "2", "3", "4"])
    }

    func testDictionarized() {
        let values = ["1", "2", "3", "1", "2"]
        let result = values.dictionarized(key: { $0 })
        XCTAssertEqual(result, ["1": "1", "2": "2", "3": "3"])
    }

    func testDictionarizedKeyValue() {
        let values = [(1, "1"), (2, "2"), (3, "3"), (1, "1")]
        let result = values.dictionarized(key: { $0.0 }, value: { $0.1 })
        XCTAssertEqual(result, [1: "1", 2: "2", 3: "3"])
    }
}
