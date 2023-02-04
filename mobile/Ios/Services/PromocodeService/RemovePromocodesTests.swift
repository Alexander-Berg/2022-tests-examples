import OHHTTPStubs
import XCTest

@testable import BeruServices

class RemovePromocodesTests: NetworkingTestCase {
    private var service: PromocodeServiceImpl!

    override func setUp() {
        super.setUp()

        service = PromocodeServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldRemovePromocodes() {
        // given
        let promocodes = ["test1", "test2"]

        stub(
            requestPartName: "api/v1",
            responseFileName: "removePromocodes",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "removePromocodes"])
        )

        // when
        let result = service.remove(promocodes: promocodes).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let promocodes = ["test1", "test2"]

        stubError(
            requestPartName: "api/v1",
            code: 500,
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "removePromocodes"])
        )

        // when
        let result = service.remove(promocodes: promocodes).expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, 500)
    }
}
