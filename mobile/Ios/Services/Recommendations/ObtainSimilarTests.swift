import BeruServices
import MarketModels
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

final class ObtainSimilarTests: NetworkingTestCase {

    private var service: RecommendationsService!

    override func setUp() {
        super.setUp()
        service = RecommendationsServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldSendProperRequestParameters() throws {
        // given
        let skuId = "100131945205"
        let modelId = 14_112_311
        let hid = 91_491
        let cartSnapshot = [["one": "two"]]

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array?.first
            guard
                let parsedSkuId = params?["skuId"].string,
                let parsedModelId = params?["modelId"].int,
                let parsedHid = params?["hid"].int,
                let parsedSnapshot = params?["cartSnapshot"].arrayObject as? [[String: String]]
            else { return false }

            return parsedSkuId == skuId &&
                parsedModelId == modelId &&
                parsedHid == hid &&
                parsedSnapshot == cartSnapshot
        }

        stub(
            requestPartName: "resolveSimilar",
            responseFileName: "similars",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveSimilar"]) &&
                verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = service.obtainSimilarProducts(
            skuId: skuId,
            modelId: modelId,
            hid: hid,
            cartSnapshot: cartSnapshot,
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        let searchResult = try XCTUnwrap(result.get())
        XCTAssertEqual(searchResult.primeSearchResult?.models.count, 14)
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let skuId = "100131945205"
        let modelId = 14_112_311
        let hid = 91_491
        let cartSnapshot = [["one": "two"]]

        stubError(requestPartName: "resolveSimilar", code: 500)

        // when
        let result = service.obtainSimilarProducts(
            skuId: skuId,
            modelId: modelId,
            hid: hid,
            cartSnapshot: cartSnapshot,
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { _ in }
    }

    func test_shouldReturnError_whenReceivedFAPIError() {
        // given
        let skuId = "100131945205"
        let modelId = 14_112_311
        let hid = 91_491
        let cartSnapshot = [["one": "two"]]

        stub(
            requestPartName: "resolveSimilar",
            responseFileName: "fapi_error"
        )

        // when
        let result = service.obtainSimilarProducts(
            skuId: skuId,
            modelId: modelId,
            hid: hid,
            cartSnapshot: cartSnapshot,
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { _ in }
    }

}
