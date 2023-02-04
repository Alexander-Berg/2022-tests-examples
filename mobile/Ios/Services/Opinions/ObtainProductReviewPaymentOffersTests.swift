import Foundation

import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ObtainProductReviewPaymentOffersTests: NetworkingTestCase {
    private var opinionsService: OpinionsServiceImpl?

    override func setUp() {
        super.setUp()

        opinionsService = OpinionsServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        opinionsService = nil
        super.tearDown()
    }

    func test_shouldObtainProductReviewPaymentOffersTests() {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "ProductReviewPaymentOffers"
        )
        // when
        let response = opinionsService?
            .obtainProductReviewPaymentOffers(with: [12_345]).expect(in: self)

        // then
        let result = try? XCTUnwrap(response?.get())
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.amount, 555)
    }
}
