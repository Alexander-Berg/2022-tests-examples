import BeruMapping
import MarketModels
import XCTest
@testable import BeruServices

class SecretSaleTests: XCTestCase {

    func testFAPISecretSaleMapping() {
        // given, when
        guard let secretSale = makeSecretSaleFromJsonWithName("fapi_secret_sale", useFAPIMapping: true) else {
            XCTFail("Unable to create SecretSale model")
            return
        }

        // then
        XCTAssertNotNil(secretSale.id)
        XCTAssertNotNil(secretSale.startDate)
        XCTAssertNotNil(secretSale.endDate)
    }

    func testCAPISecretSaleMapping() {
        // given, when
        guard let secretSale = makeSecretSaleFromJsonWithName("capi_secret_sale", useFAPIMapping: false) else {
            XCTFail("Unable to create SecretSale model")
            return
        }

        // then
        XCTAssertNotNil(secretSale.id)
        XCTAssertNotNil(secretSale.startDate)
        XCTAssertNotNil(secretSale.endDate)
    }

    // MARK: - Helpers

    private func makeSecretSaleFromJsonWithName(_ name: String, useFAPIMapping: Bool) -> SecretSale? {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return nil }

        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)

        if useFAPIMapping {
            return SecretSale.model(withFAPIJSON: jsonRepresentation)
        }

        return SecretSale.model(withJSON: jsonRepresentation)
    }
}
