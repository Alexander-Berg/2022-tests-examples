import XCTest
@testable import BeruMapping

final class YBMJSONRepresentationCodingKeyTests: XCTestCase {

    private let _testDict: [String: Any] = [
        CodingKeys.intStr.rawValue: "1543",
        CodingKeys.int.rawValue: 1_544,
        CodingKeys.string.rawValue: "1545",
        CodingKeys.cgFloatStr.rawValue: "1.546",
        CodingKeys.cgFloat.rawValue: 1.547,
        CodingKeys.boolStr.rawValue: "true",
        CodingKeys.bool.rawValue: true,
        CodingKeys.url.rawValue: "https://ya.ru",
        CodingKeys.deeplink.rawValue: "beru://morda",
        CodingKeys.object.rawValue: [
            TestObject.CodingKeys.int.rawValue: 1_548,
            TestObject.CodingKeys.str.rawValue: "1549",
            TestObject.CodingKeys.cgFloat.rawValue: 1.550,
            TestObject.CodingKeys.bool.rawValue: true
        ] as [String: Any],
        CodingKeys.array.rawValue: [
            [TestObject.CodingKeys.int.rawValue: 1_551],
            [TestObject.CodingKeys.int.rawValue: 1_552],
            [TestObject.CodingKeys.int.rawValue: "1553"]
        ],
        CodingKeys.arrayWrongType.rawValue: [
            1_554, "1555"
        ]
    ]

    private var testJson: YBMJSONRepresentation!

    override func setUpWithError() throws {
        try super.setUpWithError()

        testJson = YBMJSONRepresentation(targetObject: _testDict)
        XCTAssertNotNil(testJson)
    }

    override func tearDownWithError() throws {
        testJson = nil
        try super.tearDownWithError()
    }

    func testCodingKeysMapping() throws {
        let int1: Int? = testJson[CodingKeys.intStr]
        XCTAssertEqual(int1, 1_543)

        let int2: Int? = testJson[CodingKeys.int]
        XCTAssertEqual(int2, 1_544)

        let string: String? = testJson[CodingKeys.string]
        XCTAssertEqual(string, "1545")

        let cgFloat1: CGFloat? = testJson[CodingKeys.cgFloatStr]
        XCTAssertEqual(cgFloat1, 1.546)

        let cgFloat2: CGFloat? = testJson[CodingKeys.cgFloat]
        XCTAssertEqual(cgFloat2, 1.547)

        let bool1: Bool = try XCTUnwrap(testJson[CodingKeys.boolStr])
        XCTAssertTrue(bool1)

        let bool2: Bool = try XCTUnwrap(testJson[CodingKeys.bool])
        XCTAssertTrue(bool2)

        let url: URL? = testJson[CodingKeys.url]
        let thenUrl = try XCTUnwrap(URL(string: "https://ya.ru"))
        XCTAssertEqual(url, thenUrl)

        let deeplink: URL? = testJson[CodingKeys.deeplink]
        let thenDeeplink = try XCTUnwrap(URL(string: "beru://morda"))
        XCTAssertEqual(deeplink, thenDeeplink)

        let object: TestObject = try XCTUnwrap(testJson[CodingKeys.object])
        XCTAssertEqual(object.valueInt, 1_548)
        XCTAssertEqual(object.valueStr, "1549")
        XCTAssertEqual(object.valueCgFloat, 1.550)
        XCTAssertTrue(try XCTUnwrap(object.valueBool))

        let array: [TestObject] = try XCTUnwrap(testJson[CodingKeys.array])
        let arrayCount = 3
        if array.count == arrayCount {
            XCTAssertEqual(array[0].valueInt, 1_551)
            XCTAssertEqual(array[1].valueInt, 1_552)
            XCTAssertEqual(array[2].valueInt, 1_553)
        } else {
            XCTFail("\(array.count) != \(arrayCount)")
        }

        let arraySingleObject: [TestObject] = try XCTUnwrap(testJson[CodingKeys.object])
        XCTAssertEqual(arraySingleObject.count, 1)
        if let object = arraySingleObject.first {
            XCTAssertEqual(object.valueInt, 1_548)
            XCTAssertEqual(object.valueStr, "1549")
            XCTAssertEqual(object.valueCgFloat, 1.550)
            XCTAssertTrue(try XCTUnwrap(object.valueBool))
        } else {
            XCTFail("Single object array coding failed")
        }

        let arrayWrongType: [TestObject] = try XCTUnwrap(testJson[CodingKeys.arrayWrongType])
        XCTAssertEqual(arrayWrongType.count, 0)

        let optionalInt: Int? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalInt)

        let optionalString: String? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalString)

        let optionalCgFloat: CGFloat? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalCgFloat)

        // swiftlint:disable:next discouraged_optional_boolean
        let optionalBool: Bool? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalBool)

        let optionalObject: TestObject? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalObject)

        // swiftlint:disable:next discouraged_optional_collection
        let optionalObjectArray: [TestObject]? = testJson[CodingKeys.absentKey]
        XCTAssertNil(optionalObjectArray)
    }

    func test_codingKeysMappingFails_whenSubscriptedNonDictionary() throws {
        let stringNSObject = testJson.object(forKey: CodingKeys.string.rawValue)
        // **WARNING! Unsafe arithmetics.**
        // Здесь требуется для воспроизведение поведения на границе между Objc и Swift
        // Подобное поведение возможно встретить, когда пытаешься создать Swift модель из Objc модели таким образом:
        //     _link = [YBMCMSLink modelWithJSON:json[@"link"]];
        // Что приводит к крашу в рантайме, если по ключу "link" лежит не объект, а строка
        let stringJson = unsafeBitCast(stringNSObject, to: YBMJSONRepresentation.self)

        let shouldNotCrash: String? = stringJson[CodingKeys.string]
        XCTAssertNil(shouldNotCrash)
    }
}

extension YBMJSONRepresentationCodingKeyTests {
    enum CodingKeys: String, CodingKey {
        case intStr
        case int
        case string
        case cgFloatStr
        case cgFloat
        case boolStr
        case bool
        case url
        case deeplink
        case object
        case array
        case arrayWrongType
        case absentKey
    }

    final class TestObject: NSObject, YBMJSONInstantiable {

        let valueInt: Int?
        let valueStr: String?
        let valueCgFloat: CGFloat?
        // swiftlint:disable:next discouraged_optional_boolean
        let valueBool: Bool?

        init?(json: YBMJSONRepresentation) {
            valueInt = json[CodingKeys.int]
            valueStr = json[CodingKeys.str]
            valueCgFloat = json[CodingKeys.cgFloat]
            valueBool = json[CodingKeys.bool]
            super.init()
        }

        static func model(withJSON json: YBMJSONRepresentation) -> Self? {
            Self(json: json)
        }

        enum CodingKeys: String, CodingKey {
            case int
            case str
            case cgFloat
            case bool
        }
    }
}
