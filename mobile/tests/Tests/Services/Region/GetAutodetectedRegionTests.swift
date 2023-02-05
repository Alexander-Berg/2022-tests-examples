import OHHTTPStubs
import XCTest

@testable import BeruServices

final class GetAutodetectedRegionTests: NetworkingTestCase {

    // MARK: - Properties

    var regionService: RegionServiceImpl!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )
    }

    override func tearDown() {
        regionService = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_shouldReturnRegion_IfServerDetectedRegion() throws {
        // given
        let expectedRegionId = 213
        let expectedName = "Москва"

        stub(
            requestPartName: "api/v1",
            responseFileName: "current_user_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveCurrentUserRegion"])
        )

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveRegionById"])
        )

        // when
        let result = regionService.getAutodetectedRegion().expect(in: self)

        // then
        let region = try XCTUnwrap(result.get())
        XCTAssertEqual(region.id.intValue, expectedRegionId)
        XCTAssertEqual(region.name, expectedName)
    }

    func test_shouldFail_whenGetRegionRequestFailed() {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "current_user_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveCurrentUserRegion"])
        )
        stubError(requestPartName: "api/v1", code: 500)

        // when
        let result = regionService.getAutodetectedRegion().expect(in: self)

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

    func test_shouldFail_whenSAutodetectedRegionFailed() {
        // given
        stubError(requestPartName: "api/v1", code: 500)
        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveRegionById"])
        )

        // when
        let result = regionService.getAutodetectedRegion().expect(in: self)

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
