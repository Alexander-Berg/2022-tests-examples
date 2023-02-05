import OHHTTPStubs
import XCTest

@testable import BeruServices

final class CheckDeliveryRegionTests: NetworkingTestCase {

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

    func test_shouldReturnRegion_whenRegionDeliverable() throws {
        // given
        let expectedRegionId = 28

        stub(
            requestPartName: "api/v1",
            responseFileName: "deliverable_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAvailableDelivery"])
        )

        // when
        let result = regionService.checkDelivery(regionId: expectedRegionId).expect(in: self)

        // then
        let region = try result.get()

        XCTAssertTrue(region.isDeliveryAvailable)
        XCTAssertEqual(region.region.id.intValue, expectedRegionId)
    }

    func test_thatRegionDeliveryUnavailable() throws {
        // given
        let expectedRegionId = 115

        stub(
            requestPartName: "api/v1",
            responseFileName: "undeliverable_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAvailableDelivery"])
        )

        // when
        let result = regionService.checkDelivery(regionId: expectedRegionId).expect(in: self)

        // then
        let region = try result.get()

        XCTAssertFalse(region.isDeliveryAvailable)
        XCTAssertEqual(region.region.id.intValue, expectedRegionId)
    }

    func test_shouldSendProperRequest() {
        // given
        let regionId = 43

        stub(
            requestPartName: "api/v1",
            responseFileName: "deliverable_region",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAvailableDelivery"])
        )

        // when
        let result = regionService.checkDelivery(regionId: regionId).expect(in: self)

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
        let result = regionService.checkDelivery(regionId: 34).expect(in: self)

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
