import OHHTTPStubs
import XCTest

@testable import BeruServices

final class GetClosestDeliveryRegionTests: NetworkingTestCase {

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

    func test_nearestRegionsCountReturnsProperly() throws {
        // given
        let expectedCount = 10
        let regionId = 115

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_regions",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAvailableDelivery"])
        )

        // when
        let result = regionService.getClosestDeliveryRegions(regionId: regionId).expect(in: self)

        // then
        let closest = try result.get()

        XCTAssertEqual(closest.region.id.intValue, regionId)
        XCTAssertEqual(closest.nearestRegions.count, expectedCount)
    }

    func test_nearestRegionsCountEqualZero() throws {
        // given
        let expectedCount = 0
        let regionId = 28

        stub(
            requestPartName: "api/v1",
            responseFileName: "empty_regions",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAvailableDelivery"])
        )

        // when
        let result = regionService.getClosestDeliveryRegions(regionId: regionId).expect(in: self)

        // then
        let closest = try result.get()

        XCTAssertEqual(closest.nearestRegions.count, expectedCount)
    }

    func test_shouldFail_whenServerRespondsWith500() {
        // given
        stubError(
            requestPartName: "api/v1",
            code: 500
        )

        // when
        let result = regionService.getClosestDeliveryRegions(regionId: 34).expect(in: self)

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
