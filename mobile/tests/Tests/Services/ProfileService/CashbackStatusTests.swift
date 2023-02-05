import OHHTTPStubs
import XCTest

@testable import BeruServices

class CashbackStatusTests: NetworkingTestCase {

    var profileService: ProfileServiceImpl!

    override func setUp() {
        super.setUp()

        let dependencyProvider = DependencyProvider()

        profileService = ProfileServiceImpl(apiClient: dependencyProvider.apiClient)
    }

    override func tearDown() {
        profileService = nil
        super.tearDown()
    }

    func test_shouldReceiveBalance() throws {
        // given
        stub(
            requestPartName: "resolveUserPlusBalance",
            responseFileName: "obtain_cashback_balance",
            testBlock: isMethodPOST()
        )

        // when
        let result = profileService.obtainCashbackStatus(at: nil).expect(in: self)

        // then
        let status = try result.get()
        XCTAssertEqual(status.balance, 2_000)
        XCTAssertTrue(status.isPurchased)
    }

}
