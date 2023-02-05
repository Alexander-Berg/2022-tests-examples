import OHHTTPStubs
import XCTest

@testable import BeruServices

class ReferralServiceTests: NetworkingTestCase {

    var service: ReferralServiceImpl!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient
        apiClient.token = "123124123123123"

        service = ReferralServiceImpl(
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
            requestPartName: "resolveReferralProgramStatus",
            responseFileName: "obtain_referral_status",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.obtainStatus().expect(in: self)

        // then
        let status = try result.get()
        XCTAssertTrue(status.isPurchased)
        XCTAssertEqual(status.refererReward, 300)
        XCTAssertEqual(status.promocodePercent, 10)
        XCTAssertEqual(status.maxRefererReward, 4_000)
    }

    func test_shouldReceivePromocode() throws {
        // given
        stub(
            requestPartName: "resolveReferralPromocode",
            responseFileName: "obtain_referral_promocode",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.obtainPromocode().expect(in: self)

        // then
        let promocode = try result.get()
        XCTAssertTrue(promocode.isAvailable)
        XCTAssertNotNil(promocode.details)
        XCTAssertEqual(promocode.details?.promoCode, "SOME PROMO")
        XCTAssertNotNil(promocode.details?.expireDate)
        XCTAssertEqual(promocode.details?.minPromoCodeOrderCost.value, 3_000)
        XCTAssertEqual(promocode.details?.minPromoCodeOrderCost.currency, YMTPriceCurrency.RUB)
        XCTAssertEqual(promocode.details?.alreadyGot, 900)
        XCTAssertEqual(promocode.details?.friendsOrdered, 5)
        XCTAssertEqual(promocode.details?.expectedCashback, 900)
        XCTAssertEqual(promocode.details?.refererLink, "https://ya.ru")
        XCTAssertEqual(promocode.details?.refererReward, 300)
        XCTAssertEqual(promocode.details?.promoCodeDiscount, ReferralDiscount.percentage(10))
    }

}
