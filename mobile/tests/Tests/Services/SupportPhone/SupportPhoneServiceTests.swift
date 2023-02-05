import BeruCore
import OHHTTPStubs
import XCTest
@testable import BeruServices

class SupportPhoneServiceTests: NetworkingTestCase {

    private var supportPhoneService: SupportPhoneServiceImpl!
    private var storage: Storage!

    override func setUp() {
        super.setUp()
        storage = DependencyProvider().storage
        storage.removeValue(for: "CACHED_SUPPORT_PHONE")
        supportPhoneService = SupportPhoneServiceImpl(
            apiClient: DependencyProvider().apiClient,
            storage: storage
        )
    }

    override func tearDown() {
        supportPhoneService = nil
        super.tearDown()
    }

    func test_shouldReceiveSupportPhone() {
        // given
        stub(
            requestPartName: "resolveSupportPhone",
            responseFileName: "simple_obtain_support_phone",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveSupportPhone"])
        )

        // when
        let result = supportPhoneService.obtainSupportPhone().expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "resolveSupportPhone", code: 500)

        // when
        let result = supportPhoneService.obtainSupportPhone().expect(in: self)

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

    func test_shouldReturnError_whenReceivedFAPIError() {
        // given
        stub(
            requestPartName: "resolveSupportPhone",
            responseFileName: "obtain_support_phone_with_error"
        )

        // when
        let result = supportPhoneService.obtainSupportPhone().expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveSupportPhone",
            responseFileName: "obtain_support_phone_with_invalid_response"
        )

        // when
        let result = supportPhoneService.obtainSupportPhone().expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
