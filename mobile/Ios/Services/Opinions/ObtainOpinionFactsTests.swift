import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ObtainOpinionFactsTests: NetworkingTestCase {
    private var opinionsService: OpinionsServiceImpl!

    override func setUp() {
        super.setUp()

        opinionsService = OpinionsServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        opinionsService = nil
        super.tearDown()
    }

    func test_shouldObtainFactsForOpinion() {
        // given
        let categoryId = 15_553_892

        stub(
            requestPartName: "resolveProductFactorsByCategory",
            responseFileName: "categories_15553892_facts"
        )

        // when
        let result = opinionsService.obtainFacts(categoryId: categoryId, isV2Enabled: false).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let categoryId = 15_553_892

        stubError(requestPartName: "resolveProductFactorsByCategory", code: 500)

        // when
        let result = opinionsService.obtainFacts(categoryId: categoryId, isV2Enabled: false).expect(in: self)

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
