import MarketProtocols
import OHHTTPStubs
import SnapshotTesting
import SwiftyJSON
import XCTest
@testable import BeruServices

class PersonalDealsTests: NetworkingTestCase {

    private var personalDealsService: PersonalDealsService!

    override func setUp() {
        super.setUp()

        personalDealsService = PersonalDealsServiceImpl(
            apiClient: DependencyProvider().apiClient,
            cartSnapshot: []
        )
    }

    override func tearDown() {
        personalDealsService = nil
        super.tearDown()
    }

    func test_shouldReceivePersonalDeals() {
        // given
        stub(
            requestPartName: "resolveDeals",
            responseFileName: "personalDeals",
            testBlock: isMethodPOST()
        )

        // when
        let result = personalDealsService?.obtainPersonalDeals().expect(in: self)
        // then
        XCTAssertNoThrow(try result?.get())
    }
}
