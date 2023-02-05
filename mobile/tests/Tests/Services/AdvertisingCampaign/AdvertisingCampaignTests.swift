import OHHTTPStubs
import XCTest

@testable import BeruServices

class AdvertisingCampaignTests: NetworkingTestCase {

    var service: AdvertisingCampaignServiceImpl!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient
        apiClient.token = token

        service = AdvertisingCampaignServiceImpl(
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
            requestPartName: "checkUserWelcomeCashbackOrderEmitAvailable",
            responseFileName: "check_user_status",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.checkUserStatus().expect(in: self)

        // then
        let status = try result.get()
        XCTAssertEqual(status.cashbackAmount, 500)
        XCTAssertTrue(status.isPurchased)
        XCTAssertEqual(status.priceFrom, 3_500)
    }

}

// MARK: - Test data

private extension AdvertisingCampaignTests {
    private var token: String {
        "123124123123123"
    }
}
