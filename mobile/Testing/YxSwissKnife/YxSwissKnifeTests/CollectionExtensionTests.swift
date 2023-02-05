import Foundation
import XCTest
@testable import YxSwissKnife

final class CollectionExtensionTests: XCTestCase {

    func testThatSafeSubscriptReturnsElementIfIndexExists() {
        let collection = Array(repeating: 1, count: 3)
        let index = collection.index(after: collection.startIndex)

        XCTAssert(collection[safe: index] != nil)
    }

    func testThatSafeSubscriptReturnsNilIfIndexDoesNotExist() {
        let collection = [Int]()
        let index = collection.endIndex

        XCTAssert(collection[safe: index] == nil)
    }

    func testThatGetRandomReturnsNilIfCollectionIsEmpty() {
        let collection = [Int]()
        XCTAssertNil(collection.anyElement)
    }

    func testThatGetRandomReturnsExistingElementIfCollectionIsNotEmpty() {
        let collection = [1, 2, 3]
        guard let randomElement = collection.anyElement else {
            XCTFail("Can't fetch any element")
            return
        }
        XCTAssertTrue(collection.contains(randomElement))
    }

}
