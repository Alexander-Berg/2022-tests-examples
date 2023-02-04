import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class ObtainModelTests: NetworkingTestCase {

    private var modelService: ModelServiceImpl!

    override func setUp() {
        super.setUp()
        modelService = ModelServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        modelService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperModel() {
        // given
        stub(
            requestPartName: "/models/\(123)",
            responseFileName: "obtain_model_response",
            testBlock: isMethodGET()
        )

        // when
        let result = modelService.obtainModel(
            modelId: 123
        ).expect(in: self)

        // then
        XCTAssertEqual(try result.get().id, 123)
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "/models/\(123)", code: 500)

        // when
        let result = modelService.obtainModel(
            modelId: 123
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

}
