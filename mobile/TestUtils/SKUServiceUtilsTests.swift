import XCTest
@testable import BeruServices

final class SKUServiceUtilsTests: XCTestCase {

    var collectionJson: YBMJSONRepresentation!

    override func setUp() {
        super.setUp()
        guard let rawJson = dataFromBundle(resourceName: "recent_products"),
              let jsonAny = try? JSONSerialization.jsonObject(with: rawJson, options: []),
              let json = jsonAny as? [AnyHashable: Any],
              let jsonCollections = json["collections"] as? [AnyHashable: Any] else {
            XCTFail("Json file is not initialzied")
            return
        }
        let response = YBMJSONRepresentation(rootJson: jsonCollections, targetObject: jsonCollections)

        collectionJson = response
    }

    override func tearDown() {
        collectionJson = nil
        super.tearDown()
    }

    func testThatSKUServiceUtilsReturnsProductsCorrectly() {

        // given
        let key = "purchasedGood"
        let expectedTotalCount = 22

        let expectedSkuSCount = 17
        let expectedProductsCount = 5
        let expectedOfferCount = 0

        // when
        let result = SKUServiceUtils.makeProducts(from: collectionJson, keyOfitemsToExtract: key)

        let counted = result.reduce(into: (0, 0, 0)) { tupleResult, product in
            switch product.productId {
            case .sku:
                tupleResult.0 += 1
            case .model:
                tupleResult.1 += 1
            case .offer:
                tupleResult.2 += 1
            }
        }

        // then
        XCTAssertEqual(expectedTotalCount, result.count)
        XCTAssertEqual(expectedSkuSCount, counted.0)
        XCTAssertEqual(expectedProductsCount, counted.1)
        XCTAssertEqual(expectedOfferCount, counted.2)

    }
}
