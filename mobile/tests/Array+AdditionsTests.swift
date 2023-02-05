import LangExtensions
import XCTest

class ArrayAdditionsTests: XCTestCase {

    // MARK: - ble_safeAppend

    func test_safeAppend_nil() {

        // given

        let array = [0]
        let nilInt: Int? = nil

        // when

        var testArray = array
        testArray.ble_safeAppend(nilInt)

        // then

        XCTAssertEqual(testArray, array)
    }

    func test_safeAppend_nonNil() {

        // given

        var array = [0]
        let int = 1
        let arrayWithInt: [Int] = [0, int]

        XCTAssertNotEqual(array, arrayWithInt)

        // when

        array.ble_safeAppend(int)

        // then

        XCTAssertEqual(array, arrayWithInt)
    }

    // MARK: - ble_mapFirst

    func test_mapFirst_success() {
        // given

        let array: [Int] = [0, 1, 2]

        // when

        let firstMapped = array.ble_mapFirst(mapFunc(threshold: 0))

        // then

        XCTAssertEqual(firstMapped, "1")
    }

    func test_mapFirst_failure() {
        // given

        let array: [Int] = [0, 1, 2]

        // when

        let firstMapped = array.ble_mapFirst(mapFunc(threshold: 2))

        // then

        XCTAssertNil(firstMapped)
    }

    func test_safeSubscriptRange_wholeArray() throws {
        // given

        let array = [9, 8, 7, 6]

        // when

        let slice = try XCTUnwrap(array[safe: 0 ... 3])

        // then

        XCTAssertEqual(slice.count, 4)
        XCTAssertEqual(slice.indices.min(), 0)
        XCTAssertEqual(slice.indices.max(), 3)
        XCTAssertEqual(slice[0], 9)
        XCTAssertEqual(slice[1], 8)
        XCTAssertEqual(slice[2], 7)
        XCTAssertEqual(slice[3], 6)
    }

    func test_safeSubscriptRange_partialArray() throws {
        // given

        let array = [16, 32, 64, 128]

        // when

        let slice = try XCTUnwrap(array[safe: 1 ... 2])

        // then

        XCTAssertEqual(slice.count, 2)
        XCTAssertEqual(slice.indices.min(), 1)
        XCTAssertEqual(slice.indices.max(), 2)
        XCTAssertEqual(slice[1], 32)
        XCTAssertEqual(slice[2], 64)
    }

    func test_safeSubscriptRange_emptyArray() throws {
        // given

        let array: [Int] = []

        // when

        let slice = array[safe: 0 ... 11]

        // then

        XCTAssertNil(slice)
    }

    func test_safeSubscriptRange_sliceOutOfUpperBound() throws {
        // given

        let array: [Int] = [1, 1, 2, 3, 5, 8, 13]

        // when

        let slice = array[safe: 6 ... 12]

        // then

        XCTAssertNil(slice)
    }

    func test_safeSubscriptRange_sliceOutOfLowerBound() throws {
        // given

        let array: [Int] = [1, 1, 2, 3, 5, 8, 13]

        // when

        let slice = array[safe: -3 ... 2]

        // then

        XCTAssertNil(slice)
    }

    // MARK: - Private

    private func mapFunc(threshold: Int) -> ((Int) -> String?) {
        {
            if $0 > threshold {
                return "\($0)"
            }
            return nil
        }
    }
}
