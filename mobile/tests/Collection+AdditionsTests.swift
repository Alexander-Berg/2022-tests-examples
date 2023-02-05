import XCTest

final class CollectionAdditionsTests: XCTestCase {

    func test_shouldReturnProperValues_whenSafeSubscript() {
        // given
        let baseZero = 0
        let baseOne = 1
        let baseTwo = 2
        let array = [baseZero, baseOne, baseTwo]

        // when
        let valueMinusOne = array[ble_safe: -1]
        let valueZero = array[ble_safe: 0]
        let valueOne = array[ble_safe: 1]
        let valueTwo = array[ble_safe: 2]
        let valueThree = array[ble_safe: 3]

        // then
        XCTAssertNil(valueMinusOne)
        XCTAssertEqual(valueZero, baseZero)
        XCTAssertEqual(valueOne, baseOne)
        XCTAssertEqual(valueTwo, baseTwo)
        XCTAssertNil(valueThree)
    }

    func test_reduce_shouldStopByPredicate() {
        // given
        let array = Array(1 ... 10)

        // when
        let result = array.ble_reduce(0, nextPartialResult: +, while: { _, element in element <= 5 })

        // then
        XCTAssertEqual(result, 15)
    }

    func test_reduceInto_shouldStopByPredicate() {
        // given
        let array = Array(1 ... 10)

        // when
        let result = array.ble_reduce(
            into: [],
            updateAccumulatingResult: { acc, element in
                acc.append(element)
            },
            while: { acc, _ in
                acc.count < 5
            }
        )

        // then
        XCTAssertEqual(result.count, 5)
    }

}
