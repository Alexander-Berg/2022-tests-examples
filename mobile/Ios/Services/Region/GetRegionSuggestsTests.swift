import OHHTTPStubs
import XCTest

@testable import BeruServices

final class GetRegionSuggestsTests: NetworkingTestCase {

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

    func test_shouldSendProperRequest() {
        // given
        let text = "Москва"
        let pageSize = 12
        let expectedCount = 3

        stub(
            requestPartName: "api/v1",
            responseFileName: "fapi_response_suggests",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveGeoSuggest"])
        )

        // when
        let result = regionService.getRegionSuggests(text: text, count: pageSize).expect(in: self)

        // then
        guard case let .success(suggests) = result else {
            XCTFail("Should succeed")
            return
        }

        XCTAssertEqual(suggests.count, expectedCount)
    }

    func test_shouldFail_whenServerRespondsWith500() {
        // given
        let text = "Москва"
        let pageSize = 12
        stubError(requestPartName: "api/v1", code: 500)

        // when
        let result = regionService.getRegionSuggests(text: text, count: pageSize).expect(in: self)

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
