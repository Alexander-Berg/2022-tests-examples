import OHHTTPStubs
import XCTest

@testable import BeruServices

class FreeDeliveryStatusTests: NetworkingTestCase {

    var service: FreeDeliveryInfoServiceImpl!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient
        apiClient.token = token

        service = FreeDeliveryInfoServiceImpl(
            apiClient: APIClient(apiClient: apiClient)
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldReceiveStatus() throws {
        // given
        stub(
            requestPartName: "resolveFreeDeliveryPromo",
            responseFileName: "obtain_delivery_status",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.getFreeDeliveryStatus(isAuthenticationRequired: true).expect(in: self)

        // then
        let status = try result.get()
        XCTAssertEqual(status.promoType, .plus)
        XCTAssertEqual(status.priceFrom?.value, 2_999)
        XCTAssertTrue(status.isPlusPromoAvailable)
        XCTAssertEqual(status.plusPriceFrom?.value, 699)
    }

}

// MARK: - Test data

private extension FreeDeliveryStatusTests {
    private var token: String {
        "123124123123123"
    }
}
