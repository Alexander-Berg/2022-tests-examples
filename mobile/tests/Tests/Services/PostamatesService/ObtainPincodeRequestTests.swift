import OHHTTPStubs
import XCTest

@testable import BeruServices

class ObtainPincodeRequestTests: NetworkingTestCase {

    private var postamateService: PostamateServiceImpl!

    override func setUp() {
        super.setUp()
        postamateService = PostamateServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        postamateService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperPincodes() throws {
        // given
        let orderIds = Set<OrderId>([5_217_552, 5_217_560])

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard
                let params = body["params"] as? [[AnyHashable: Any]],
                let orderIds = params.first?["orderIds"] as? [Int]
            else {
                return false
            }

            return Set(orderIds) == Set([5_217_552, 5_217_560])
        }

        stub(
            requestPartName: "resolvePostamateShipment",
            responseFileName: "simple_obtain_pincodes",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolvePostamateShipment"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = postamateService.obtainPincodes(orderIds: orderIds).expect(in: self)

        // then
        let pincodes = try result.get()
        XCTAssertEqual(pincodes.count, 2)
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderIds = Set<OrderId>([5_217_552, 5_217_560])
        stubError(requestPartName: "resolvePostamateShipment", code: 500)

        // when
        let result = postamateService.obtainPincodes(orderIds: orderIds).expect(in: self)

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

    func test_shouldReturnError_whenReceivedFAPIError() {
        // given
        let orderIds = Set<OrderId>([5_217_552, 5_217_560])
        stub(
            requestPartName: "resolvePostamateShipment",
            responseFileName: "obtain_pincodes_with_error"
        )

        // when
        let result = postamateService.obtainPincodes(orderIds: orderIds).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderIds = Set<OrderId>([5_217_552, 5_217_560])
        stub(
            requestPartName: "resolvePostamateShipment",
            responseFileName: "obtain_pincodes_with_invalid_response"
        )

        // when
        let result = postamateService.obtainPincodes(orderIds: orderIds).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
