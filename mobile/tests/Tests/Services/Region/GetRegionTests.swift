import MarketModels
import OHHTTPStubs
import XCTest
@testable import BeruServices

final class GetRegionTests: NetworkingTestCase {

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

    func test_shouldReturnRegion_IfServerReturnRegion() throws {
        // given
        let regionId = 43

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveRegionById"])
        )

        // when
        let result = regionService.getRegion(regionId: regionId).expect(in: self)

        // then
        let region = try XCTUnwrap(result.get())
        XCTAssertEqual(region.id.intValue, regionId)
    }

    func test_shouldSendProperRequest() {
        // given
        let regionId = 43

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveRegionById"])
        )

        // when
        let result = regionService.getRegion(regionId: regionId).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldFail_whenServerRespondsWith500() {
        // given
        stubError(
            requestPartName: "api/v1",
            code: 500
        )

        // when
        let result = regionService.getRegion(regionId: 51).expect(in: self)

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

    func test_shouldUpdateSelectedRegion_whenRegionRequested() {
        // given
        let dummySettings = DummyRegionSettings()
        dummySettings.selectedRegion = YMTRegion(id: 213, name: "Moscow", type: .city)
        let regionId = 43

        stub(
            requestPartName: "v1/georegion",
            responseFileName: "simple_region"
        )

        // when
        regionService.getRegion(regionId: regionId).expect(in: self)

        // then
        XCTAssertNoThrow(dummySettings.selectedRegion?.id.intValue == regionId)
    }

    func test_shouldFail_whenRegionNotExists() {
        // given
        stub(
            requestPartName: "v1/georegion",
            responseFileName: "region_not_found"
        )

        // when
        let result = regionService.getRegion(regionId: 212).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }
}
