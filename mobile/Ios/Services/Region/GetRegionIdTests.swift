import OHHTTPStubs
import XCTest

@testable import BeruServices

final class GetRegionIdTests: NetworkingTestCase {

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

    func test_shouldReturnRegionId_IfServerDetectedRegion() {
        // given
        let expectedRegionId = 117_053
        let latitude = 55.450_7
        let longitude = 37.365_6

        stub(
            requestPartName: "api/v1",
            responseFileName: "get_region_id_by_lat_lon",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveUserAddressAndRegionByGpsCoordinate"])
        )

        // when
        let result = regionService.getRegionId(
            latitude: latitude,
            longitude: longitude
        ).expect(in: self)

        // then
        guard case let .success(regionId) = result else {
            XCTFail("Region id request failed")
            return
        }

        XCTAssertEqual(expectedRegionId, regionId)
    }

    func test_shouldFail_whenSuggestRequestFailed() {
        // given
        let latitude = 55.450_7
        let longitude = 37.365_6

        stubError(requestPartName: "api/v1", code: 500)

        // when
        let result = regionService.getRegionId(
            latitude: latitude,
            longitude: longitude
        ).expect(in: self)

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

    func test_shouldFail_whenRegionNotExists() {
        // given
        let latitude = 55.450_7
        let longitude = 37.365_6

        stub(
            requestPartName: "v1/region",
            responseFileName: "region_not_found"
        )

        // when
        let result = regionService.getRegionId(
            latitude: latitude,
            longitude: longitude
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }

}
