// swiftlint:disable opening_brace

import Foundation
import XCTest
import YxSwissKnife

final class CastingTests: XCTestCase {

    func testGoodCastsWithDefaultValues() {
        let dict: [String: Any] = [
            "int": 1,
            "double": 2.0,
            "string": "Hello, World",
            "array": [1, 2, 3],
            "dict": [1: "One", 2: "Two"]
        ]

        let int1: Int = cast(dict["int"]!, default: 9000)
        XCTAssertTrue(int1 == 1)

        let int2: Int? = cast(dict["int"]!, default: 9000)
        XCTAssertTrue(int2 == 1)

        let double1: Double = cast(dict["double"]!, default: 9000.0)
        XCTAssertTrue(abs(double1 - 2.0) < 1e-9)

        let double2: Double? = cast(dict["double"]!, default: 9000.0)
        XCTAssertTrue(abs(double2! - 2.0) < 1e-9)

        let string1: String = cast(dict["string"]!, default: "defaultValue")
        XCTAssertTrue(string1 == "Hello, World")

        let string2: String? = cast(dict["string"]!, default: "defaultValue")
        XCTAssertTrue(string2 == "Hello, World")

        let array1: [Int] = cast(dict["array"]!, default: [1, 2])
        XCTAssertTrue(array1 == [1, 2, 3])

        let array2: [Int]? = cast(dict["array"]!, default: [1, 2])
        XCTAssertTrue(array2 == [1, 2, 3])

        let array3: [Int?] = cast(dict["array"]!, default: [1, 2])
        XCTAssertTrue(array3 == [1, 2, 3])

        let array4: [Int?]? = cast(dict["array"]!, default: [1, 2])
        XCTAssertTrue(array4 == [1, 2, 3])

        let dict1: [Int: String] = cast(dict["dict"], default: [1: "One"])
        XCTAssertTrue(dict1 == [1: "One", 2: "Two"])

        let dict2: [Int: String]? = cast(dict["dict"]!, default: [1: "One"])
        XCTAssertTrue(dict2 == [1: "One", 2: "Two"])

        let dict3: [Int: String?] = cast(dict["dict"]!, default: [1: "One"])
        XCTAssertTrue(dict3 == [1: "One", 2: "Two"])

        let dict4: [Int: String?]? = cast(dict["dict"]!, default: [1: "One"])
        XCTAssertTrue(dict4 == [1: "One", 2: "Two"])
    }

    func testGoodCasts() {
        let dict: [String: Any] = [
            "int": 1,
            "double": 2.0,
            "string": "Hello, World",
            "array": [1, 2, 3],
            "dict": [1: "One", 2: "Two"]
        ]

        do {
            let _: Int = try cast(dict["int"]!)
            let _: Int? = try cast(dict["int"]!)
            let _: Double = try cast(dict["double"]!)
            let _: Double? = try cast(dict["double"]!)
            let _: String = try cast(dict["string"]!)
            let _: String? = try cast(dict["string"]!)
            let _: [Int] = try cast(dict["array"]!)
            let _: [Int]? = try cast(dict["array"]!)
            let _: [Int?] = try cast(dict["array"]!)
            let _: [Int?]? = try cast(dict["array"]!)
            let _: [Int: String] = try cast(dict["dict"]!)
            let _: [Int: String]? = try cast(dict["dict"]!)
            let _: [Int: String?] = try cast(dict["dict"]!)
            let _: [Int: String?]? = try cast(dict["dict"]!)
        } catch {
            XCTFail("casting failed")
        }
    }

    func testBadCasts() {
        let dict: [String: Any] = [
            "int": "str",
            "double": "sdf",
            "string": 1,
            "array": -1,
            "dict": [100500]
        ]

        let casts = [
            { let _: Int = try cast(dict["int"]!) },
            { let _: Int? = try cast(dict["int"]!) },
            { let _: Double = try cast(dict["double"]!) },
            { let _: Double? = try cast(dict["double"]!) },
            { let _: String = try cast(dict["string"]!) },
            { let _: String? = try cast(dict["string"]!) },
            { let _: [Int] = try cast(dict["array"]!) },
            { let _: [Int]? = try cast(dict["array"]!) },
            { let _: [Int?] = try cast(dict["array"]!) },
            { let _: [Int?]? = try cast(dict["array"]!) },
            { let _: [Int: String] = try cast(dict["dict"]!) },
            { let _: [Int: String]? = try cast(dict["dict"]!) },
            { let _: [Int: String?] = try cast(dict["dict"]!) },
            { let _: [Int: String?]? = try cast(dict["dict"]!) }
        ]

        for cast in casts {
            do {
                try cast()
                XCTFail("Casting failed")
            } catch {
            }
        }
    }

    func testBadCastsWithDefaultValues() {
        let dict: [String: Any] = [
            "int": "str",
            "double": "sdf",
            "string": 1,
            "array": -1,
            "dict": [100500]
        ]

        let casts = [
            {
                let int: Int = cast(dict["int"]!, default: 9000)
                XCTAssertTrue(int == 9000, "Cast \"str\" to Int unexpectedly succeeded")
            },
            {
                let int: Int? = cast(dict["int"]!, default: 9000)
                XCTAssertTrue(int == 9000, "Cast \"str\" to Int? unexpectedly succeeded")
            },
            {
                let double: Double = cast(dict["double"]!, default: 9000.0)
                XCTAssertTrue(abs(double - 9000.0) < 1e-9, "Cast \"sdf\" to Double unexpectedly succeeded")
            },
            {
                let double: Double? = cast(dict["double"]!, default: 9000.0)
                XCTAssertTrue(abs(double! - 9000.0) < 1e-9, "Cast \"sdf\" to Double unexpectedly succeeded")
            },
            {
                let string: String = cast(dict["string"]!, default: "defaultValue")
                XCTAssertTrue(string == "defaultValue", "Cast 1 to String unexpectedly succeeded")
            },
            {
                let string: String? = cast(dict["string"]!, default: "defaultValue")
                XCTAssertTrue(string == "defaultValue", "Cast 1 to String? unexpectedly succeeded")
            },
            {
                let array: [Int] = cast(dict["array"]!, default: [1, 2])
                XCTAssertTrue(array == [1, 2], "Cast -1 to [Int] unexpectedly succeeded")
            },
            {
                let array: [Int]? = cast(dict["array"]!, default: [1, 2])
                XCTAssertTrue(array == [1, 2], "Cast -1 to [Int]? unexpectedly succeeded")
            },
            {
                let array: [Int?] = cast(dict["array"]!, default: [1, 2])
                XCTAssertTrue(array == [1, 2], "Cast -1 to [Int?] unexpectedly succeeded")
            },
            {
                let array: [Int?]? = cast(dict["array"]!, default: [1, 2])
                XCTAssertTrue(array == [1, 2], "Cast -1 to [Int?]? unexpectedly succeeded")
            },
            {
                let dict: [Int: String] = cast(dict["dict"]!, default: [1: "One"])
                XCTAssertTrue(dict == [1: "One"], "Cast [100500] to [Int: String] unexpectedly succeeded")
            },
            {
                let dict: [Int: String]? = cast(dict["dict"]!, default: [1: "One"])
                XCTAssertTrue(dict == [1: "One"], "Cast [100500] to [Int: String] unexpectedly succeeded")
            },
            {
                let dict: [Int: String?] = cast(dict["dict"]!, default: [1: "One"])
                XCTAssertTrue(dict == [1: "One"], "Cast [100500] to [Int: String] unexpectedly succeeded")
            },
            {
                let dict: [Int: String?]? = cast(dict["dict"]!, default: [1: "One"])
                XCTAssertTrue(dict == [1: "One"], "Cast [100500] to [Int: String] unexpectedly succeeded")
            }
        ]

        for cast in casts {
            cast()
        }
    }
}
