import Foundation

import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ObtainOpinionsTests: NetworkingTestCase {
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

    func test_shouldObtainOpinions() {
        // given
        let params = makeOpinionsRequestParams()

        stub(
            requestPartName: "api/v1",
            responseFileName: "resolveProductReviews",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveProductReviews,resolveProductReviewsFactorsSummary"])
        )

        // when
        let result = opinionsService.obtainOpinions(params: params).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let params = makeOpinionsRequestParams()

        stubError(
            requestPartName: "api/v1",
            code: 500,
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveProductReviews,resolveProductReviewsFactorsSummary"])
        )

        // when
        let result = opinionsService.obtainOpinions(params: params).expect(in: self)

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

    private func makeOpinionsRequestParams() -> YMTModelOpinionsRequestParams {
        let parameters = YMTModelOpinionsRequestParams()
        parameters.modelId = 1_711_138_831
        parameters.pageSize = 30
        parameters.page = 1
        parameters.maxComments = 10_000
        parameters.sort = .byGrade
        parameters.grade = .best
        parameters.how = .asc
        return parameters
    }
}
