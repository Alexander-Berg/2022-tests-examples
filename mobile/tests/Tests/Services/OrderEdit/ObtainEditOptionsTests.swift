import MarketAPI
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

class ObtainEditOptionsTests: NetworkingTestCase {

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

    func test_shouldReceiveProperEditOptions() {
        // given
        let orderId = OrderId(4_434_692)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            return params?.first?["orderId"].intValue == 4_434_692
        }

        stub(
            requestPartName: "resolveOrderEditingOptions",
            responseFileName: "simple_obtain_edit_options",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderEditingOptions"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderEditService.obtainEditingOptions(
            with: orderId,
            editingOptions: [.deliveryDate],
            showCredits: false,
            showInstallments: false
        ).expect(in: self)

        // then
        switch result {
        case let .success(orderEditingOptions):
            XCTAssertEqual(orderEditingOptions.deliveryDateIntervals.count, 4)
            orderEditingOptions.deliveryDateIntervals.forEach { dateInterval in
                XCTAssertEqual(dateInterval.timeIntervals.count, 3)
            }
        default:
            XCTFail("Wrong edit options count result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderId = OrderId(4_434_692)
        stubError(requestPartName: "resolveOrderEditingOptions", code: 500)

        // when
        let result = orderEditService.obtainEditingOptions(
            with: orderId,
            editingOptions: [.deliveryDate],
            showCredits: false,
            showInstallments: false
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

    func test_shouldReturnError_whenReceivedFAPIError() {
        // given
        let orderId = OrderId(4_434_692)

        stub(
            requestPartName: "resolveOrderEditingOptions",
            responseFileName: "obtain_edit_options_with_error"
        )

        // when
        let result = orderEditService.obtainEditingOptions(
            with: orderId,
            editingOptions: [.deliveryDate],
            showCredits: false,
            showInstallments: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderId = OrderId(4_434_692)

        stub(
            requestPartName: "resolveOrderEditingOptions",
            responseFileName: "obtain_edit_options_with_invalid_response"
        )

        // when
        let result = orderEditService.obtainEditingOptions(
            with: orderId,
            editingOptions: [.deliveryDate],
            showCredits: false,
            showInstallments: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
