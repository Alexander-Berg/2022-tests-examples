import XCTest

class AlgorithmTests: XCTestCase {

    // MARK: - Binary search
    //
    private func sortedRandomArray(ofSize size: Int) -> [Int] {
        return (1...size).map { _ in Int.random(in: 0...100500) }.sorted()
    }

    func testBinarySearchEmptySequence() {
        XCTAssertEqual([].binarySearch(predicate: { $0 < 100500 }), 0)
    }

    func testBinarySearchFirstElement() {
        for lastElem in 1...5 {
            let arr = Array(1...lastElem)
            XCTAssertEqual(arr.binarySearch(predicate: { $0 < 1 }), 0)
        }

        let randomArray = sortedRandomArray(ofSize: 2000)

        XCTAssertEqual(randomArray.binarySearch(predicate: { $0 < randomArray.first! }), 0)
    }

    func testBinarySearchMidElement() {
        XCTAssertEqual([1].binarySearch(predicate: { $0 < 1 }), 0)
        XCTAssertEqual([1, 2, 3].binarySearch(predicate: { $0 < 2 }), 1)
        XCTAssertEqual([1, 2, 3, 4, 5].binarySearch(predicate: { $0 < 3 }), 2)
    }

    func testBinarySearchElementInArray() {
        XCTAssertEqual([1, 2].binarySearch(predicate: { $0 < 2 }), 1)
        XCTAssertEqual(Array(0...8).binarySearch(predicate: { $0 < 7 }), 7)
        XCTAssertEqual([2, 3, 4, 5, 6].binarySearch(predicate: { $0 < 5 }), 3)
        XCTAssertEqual([2, 4, 6, 8].binarySearch(predicate: { $0 < 6 }), 2)
        XCTAssertEqual([3, 4, 16, 23].binarySearch(predicate: { $0 < 16 }), 2)
    }

    func testBinarySearchLastElement() {
        for lastElem in 1...5 {
            let arr = Array(1...lastElem)
            XCTAssertEqual(arr.binarySearch(predicate: { $0 < lastElem }), lastElem - 1)
        }

        let randomArray = sortedRandomArray(ofSize: 2000)

        XCTAssertEqual(
            randomArray.binarySearch(predicate: { $0 < randomArray.last! }),
            randomArray.count - 1
        )
    }

}
