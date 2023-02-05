import OHHTTPStubs
import XCTest

@testable import BeruServices

class PlusTrialServiceTests: NetworkingTestCase {

    var service: PlusTrialServiceImpl!

    override func setUp() {
        super.setUp()

        service = PlusTrialServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldReceiveActivationSuccess() throws {
        // given
        stub(
            requestPartName: "activateYandexPlusTrial",
            responseFileName: "activate_plus_success",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.activatePlusTrial().expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReceiveActivationFailure() throws {
        // given
        stub(
            requestPartName: "activateYandexPlusTrial",
            responseFileName: "activate_plus_failure",
            testBlock: isMethodPOST()
        )

        // when
        let result = service.activatePlusTrial().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }

}
