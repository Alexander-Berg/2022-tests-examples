import OHHTTPStubs
import XCTest

@testable import BeruServices

class PostcodeServiceTests: NetworkingTestCase {

    var postcodeService: PostcodeServiceImpl!

    override func setUp() {
        super.setUp()

        postcodeService = PostcodeServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        postcodeService = nil

        super.tearDown()
    }

    func test_shouldReturnIndex_whenRequestSucceeded() throws {
        // given
        let expectedIndex = "117418"

        let address = "улица Пушкина"
        let expectedQueryParams = [
            "type": "postal_codes",
            "exp_flags": "postalcodes_snippets",
            "text": address
        ]
        stub(
            requestPartName: "search/wizardsjson",
            responseFileName: "searchwizard_success",
            testBlock: isMethodGET() && containsQueryParams(expectedQueryParams)
        )

        // when
        let result = postcodeService.getPostcode(for: address).expect(in: self)

        // then
        let actualIndex = try result.get()
        XCTAssertEqual(expectedIndex, actualIndex)
    }

    func test_shouldReturnNil_whenSeachResultIsEmpty() throws {
        // given
        stub(
            requestPartName: "search/wizardsjson",
            responseFileName: "searchwizard_empty"
        )

        // when
        let result = postcodeService.getPostcode(for: "123").expect(in: self)

        // then
        XCTAssertNil(try result.get())
    }

    func test_shouldReturnError_whenRequestFailed() throws {
        // given
        stubNetworkError(
            requestPartName: "search/wizardsjson",
            with: .notConnectedToInternet
        )

        // when
        let result = postcodeService.getPostcode(for: "123").expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }
}
