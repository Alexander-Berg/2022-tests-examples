import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class CreateCommentTests: NetworkingTestCase {
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

    func test_shouldCreateComment() {
        // given
        let params = makeCommentCreationParams()

        let bodyCheckBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let jsonParams = json["params"].array?.first?.dictionary

            return jsonParams?["parentCommentId"]?.string == params.commentId
                && jsonParams?["gradeId"]?.int == Int(params.opinionId!) // swiftlint:disable:this force_unwrapping
                && jsonParams?["rootCommentId"]?.string == "root-9-0-99647775"
                && jsonParams?["text"]?.string == "Artem K., "
        }

        stub(
            requestPartName: "addModelReviewComment",
            responseFileName: "addModelReviewComment",
            testBlock: isMethodPOST() && verifyJsonBody(bodyCheckBlock)
        )

        // when
        let result = opinionsService.makeComment(params: params).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let params = makeCommentCreationParams()

        stubError(requestPartName: "api/v1", code: 500)

        // when
        let result = opinionsService.makeComment(params: params).expect(in: self)

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

    private func makeCommentCreationParams() -> CommentCreationParams {
        CommentCreationParams(
            comment: "Artem K., ",
            opinionId: "99647775",
            commentId: "child-0-102682608"
        )
    }
}
