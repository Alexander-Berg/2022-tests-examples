import MarketAPI
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

class ObtainEditPossibilitiesTests: NetworkingTestCase {

    private var orderEditService: OrderEditServiceImpl!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().apiClient
        orderEditService = OrderEditServiceImpl(
            apiClient: apiClient,
            orderEditAddressAPI: OrderEditAddressAPIImpl(apiClient: apiClient),
            notificationCenter: .default,
            rgbColors: nil
        )
    }

    override func tearDown() {
        orderEditService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperEditPossibilities() {
        // given
        let orderIds: [OrderId] = [4_434_692, 4_434_691]
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard let orderIds = params?.first?["orderIds"].array else {
                return false
            }
            return orderIds == [4_434_692, 4_434_691]
        }

        stub(
            requestPartName: "resolveOrderEditVariants",
            responseFileName: "simple_obtain_edit_possibilities",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderEditVariants"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderEditService.obtainEditPossibilities(with: orderIds).expect(in: self)

        // then
        switch result {
        case let .success(possibilities):
            XCTAssertEqual(possibilities.count, 2)
            possibilities.forEach { possibility in
                XCTAssertNotNil(possibility.deliveryService)
            }
        default:
            XCTFail("Wrong edit possibilities count result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderIds: [OrderId] = [4_434_692, 4_434_691]
        stubError(requestPartName: "resolveOrderEditVariants", code: 500)

        // when
        let result = orderEditService.obtainEditPossibilities(with: orderIds).expect(in: self)

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
        let orderIds: [OrderId] = [4_434_692, 4_434_691]

        stub(
            requestPartName: "resolveOrderEditVariants",
            responseFileName: "obtain_edit_possibilities_with_error"
        )

        // when
        let result = orderEditService.obtainEditPossibilities(with: orderIds).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderIds: [OrderId] = [4_434_692, 4_434_691]

        stub(
            requestPartName: "resolveOrderEditVariants",
            responseFileName: "obtain_edit_possibilities_with_invalid_response"
        )

        // when
        let result = orderEditService.obtainEditPossibilities(with: orderIds).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
