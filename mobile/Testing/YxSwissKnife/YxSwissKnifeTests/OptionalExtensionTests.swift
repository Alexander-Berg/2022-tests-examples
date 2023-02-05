import Foundation
import XCTest
@testable import YxSwissKnife

class OptionalExtensionTests: XCTestCase {
    func testThatIsNilOrEmptyIsTrueWhenCollectionIsNil() {
        let collection: [Int]? = nil
        XCTAssertTrue(collection.isNilOrEmpty)
    }

    func testThatIsNilOrEmptyIsTrueWhenCollectionIsEmpty() {
        let collection: [Int]? = []
        XCTAssertTrue(collection.isNilOrEmpty)
    }

    func testThatIsNilOrEmptyIsFalseWhenCollectionNeitherNilNorEmpty() {
        let collection: [Int]? = [1, 2, 3]
        XCTAssertFalse(collection.isNilOrEmpty)
    }
}
