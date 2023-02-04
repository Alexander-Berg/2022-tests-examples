import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class RequestCPAOffersTests: NetworkingTestCase {

    private var modelService: ModelServiceImpl!

    override func setUp() {
        super.setUp()
        modelService = ModelServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        modelService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperCPAOffers() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let productId = params?.first?["productIds"].array?.first?.int
            else {
                return false
            }
            return productId == 123
        }

        stub(
            requestPartName: "resolveCpaOffers",
            responseFileName: "cpa_offers_response",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveCpaOffers"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = modelService.requestCPAOffers(
            modelIds: [123],
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        XCTAssertEqual(try result.get().count, 1)
    }

    func test_shouldReturnEmtpyArray_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "resolveCpaOffers", code: 500)

        // when
        let result = modelService.requestCPAOffers(
            modelIds: [123],
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        XCTAssertEqual(try result.get().count, 0)
    }

    func test_shouldRecover_whenReceivedFAPIError() {
        // given
        stub(
            requestPartName: "resolveCpaOffers",
            responseFileName: "cpa_offers_with_error"
        )

        // when
        let result = modelService.requestCPAOffers(
            modelIds: [123],
            isPreorderEnabled: true
        ).expect(in: self)

        // then
        XCTAssertEqual(try result.get().count, 0)
    }
}
