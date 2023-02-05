import XCTest

final class DictionaryAdditionsTests: XCTestCase {

    func test_withoutNilValues() {
        // given
        let values = [TestStruct(id: 1), TestStruct(id: 2), TestStruct(id: 3)]

        // when
        let result = Dictionary(uniqueValues: values, by: \.id)

        // then
        XCTAssertEqual(result.values.count, 3)
    }

    func test_withNilValues() {
        // given
        let values = [TestStruct(id: 1), TestStruct(id: nil), TestStruct(id: 3)]

        // when
        let result = Dictionary(uniqueValues: values, by: \.id)

        // then
        XCTAssertEqual(result.values.count, 2)
    }

    private struct TestStruct {
        let id: Int?
    }

}
