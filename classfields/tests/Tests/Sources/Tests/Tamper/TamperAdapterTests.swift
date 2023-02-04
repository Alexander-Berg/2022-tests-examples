@testable import AutoRuNetwork
import AutoRuUtils
import AutoRuNetworkUtils
import XCTest

// https://st.yandex-team.ru/VSAPPS-6519
final class TamperAdapterTests: BaseUnitTest {
    func test_queryParameters_singleValue() {
        // Склеиваем в одну строку параметры и значения key1=value1key2=value2
        let queryParameters: QueryParameters = [
            "key1": .value("value1"),
            "key2": .value("value2")
        ]

        XCTAssertEqual(queryParameters.makeTamperKeyValueString(), "key1=value1key2=value2")
    }

    func test_queryParameters_array() {
        // При наличии массива параметры сортируются и склеиваются следующим образом: key1=value1key1=value2key1=value3
        let queryParameters: QueryParameters = [
            "key1": .array(["value3", "value1", "value2"])
        ]

        XCTAssertEqual(queryParameters.makeTamperKeyValueString(), "key1=value1key1=value2key1=value3")
    }

    func test_queryParameters_empty() {
        // В этом случае склеивается без значения keyWithEmptyValue=key1=value1key2=value2
        let queryParameters: QueryParameters = [
            "key1": .empty,
            "key2": .empty
        ]

        XCTAssertEqual(queryParameters.makeTamperKeyValueString(), "")
    }

    func test_queryParameters_complex() {
        let queryParameters: QueryParameters = [
            "a": .empty,
            "abb": .value("a"),
            "aba": .value("b"),
            "aa": .array(["a", "c", "b"]),
            "ad": .empty
        ]

        XCTAssertEqual(queryParameters.makeTamperKeyValueString(), "aa=aaa=baa=caba=babb=a")
    }

    func test_queryParameters_uppercased() {
        let queryParameters: QueryParameters = [
            "aA": .value("a"),
            "aa": .value("a"),
            "AA": .value("a")
        ]

        XCTAssertEqual(queryParameters.makeTamperKeyValueString(), "AA=aaA=aaa=a")
    }

    func test_headerValue_realtySample() {
        let params: QueryParameters = [
            "category": .value("HOUSE"),
            "currency": .value("RUR"),
            "id": .value("9105050433078320640"),
            "objectType": .value("OFFER"),
            "priceMax": .value("11000000"),
            "priceType": .value("PER_OFFER"),
            "showOnMobile": .value("YES"),
            "type": .value("SELL")
        ]

        let uuid = "a308ae2137ac4bdf90fa9e3c39174257"
        let salt = "@bM${rmFnR%XRtjcK@P"

        let decodedString = params.makeTamperKeyValueString()
        XCTAssertEqual(
            decodedString,
            "category=HOUSEcurrency=RURid=9105050433078320640objectType=OFFERpriceMax=11000000priceType=PER_OFFERshowOnMobile=YEStype=SELL"
        )

        let value = TamperAdapter.generate(for: decodedString, uuid: uuid, salt: salt, bodySize: 100)
        XCTAssertEqual(value, "5e43f0bf595d0581ce8d98744c367666")
    }

    func test_headerValue_dummy() {
        // Добавление tamper параметра к остальным запросам.
        // 1. Генерируем GUID к каждому запросу
        // 2. Считаем MD5 от полученного GUID

        let value = TamperAdapter.generateDummy()

        XCTAssert(value.isValidMD5)
    }
}

extension String {
    var isValidMD5: Bool {
        return self.count == 32 && self.allSatisfy({ "1234567890qwertyuiopasdfghjklzxcvbnm".contains($0) })
    }
}
