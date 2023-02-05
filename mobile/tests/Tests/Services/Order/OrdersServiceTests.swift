import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class OrdersServiceTests: NetworkingTestCase {

    private var ordersService: OrdersServiceImpl!

    override func setUp() {
        super.setUp()
        ordersService = OrdersServiceImpl(apiClient: DependencyProvider().apiClient, rgbColors: "WHITE,BLUE")
    }

    override func tearDown() {
        ordersService = nil
        super.tearDown()
    }

    // MARK: - obtainOrders

    func test_obtainOrders_shouldReceiveProperOrders() {
        // given
        let page = 1
        let pageSize = 20
        let status = Order.Status.delivery
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let page = params?.first?["page"].int,
                let pageSize = params?.first?["pageSize"].int,
                let isArchived = params?.first?["archived"].bool,
                let sort = params?.first?["sort"].string,
                let isDigitalEnabled = params?.first?["digitalEnabled"].bool,
                let rgb = params?.first?["rgb"].string,
                let status = params?.first?["status"].string,
                let withChangeRequest = params?.first?["withChangeRequest"].bool,
                let withCashbackEmitInfo = params?.first?["withCashbackEmitInfo"].bool
            else {
                return false
            }
            return page == 1
                && pageSize == 20
                && isArchived == false
                && sort == "BY_IMPORTANCE"
                && isDigitalEnabled == true
                && rgb == "WHITE,BLUE"
                && status == "DELIVERY"
                && withChangeRequest == true
                && withCashbackEmitInfo == true
        }

        stub(
            requestPartName: "resolveUserOrdersFull",
            responseFileName: "simple_obtain_orders",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveUserOrdersFull"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = ordersService.obtainOrders(
            page: page,
            pageSize: pageSize,
            isArchived: false,
            sortByImportance: true,
            status: status
        ).expect(in: self)

        // then
        switch result {
        case let .success(ordersResult):
            XCTAssertEqual(ordersResult.orders.count, 2)
        default:
            XCTFail("Wrong order result \(String(describing: result))")
        }
    }

    func test_obtainOrders_shouldReturnError_whenStatusCodeIs500() {
        // given
        let page = 1
        let pageSize = 20
        stubError(requestPartName: "resolveUserOrdersFull", code: 500)

        // when
        let result = ordersService.obtainOrders(
            page: page,
            pageSize: pageSize,
            isArchived: false
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

    func test_obtainOrders_shouldReturnError_whenReceivedFAPIError() {
        // given
        let page = 1
        let pageSize = 20
        stub(
            requestPartName: "resolveUserOrdersFull",
            responseFileName: "obtain_orders_with_error"
        )

        // when
        let result = ordersService.obtainOrders(
            page: page,
            pageSize: pageSize,
            isArchived: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_obtainOrders_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let page = 1
        let pageSize = 20
        stub(
            requestPartName: "resolveUserOrdersFull",
            responseFileName: "obtain_orders_with_invalid_response"
        )

        // when
        let result = ordersService.obtainOrders(
            page: page,
            pageSize: pageSize,
            isArchived: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    // MARK: - obtainOrder

    func test_obtainOrder_shouldReceiveProperOrder() {
        // given
        let orderId = OrderId(5_957_631)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let orderId = params?.first?["orderId"].intValue,
                let withChangeRequest = params?.first?["withChangeRequest"].bool,
                let rgb = params?.first?["rgb"].string
            else {
                return false
            }
            return orderId == 5_957_631
                && withChangeRequest == true
                && rgb == "WHITE,BLUE"

        }

        stub(
            requestPartName: "resolveUserOrderByIdFull",
            responseFileName: "simple_obtain_order",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveUserOrderByIdFull"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = ordersService.obtainOrder(with: orderId, isArchived: false).expect(in: self)

        // then
        switch result {
        case let .success(order):
            XCTAssertNotNil(order)
        default:
            XCTFail("Wrong order result \(String(describing: result))")
        }
    }

    func test_obtainOrder_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderId = OrderId(5_957_631)

        stubError(requestPartName: "resolveUserOrderByIdFull", code: 500)

        // when
        let result = ordersService.obtainOrder(with: orderId, isArchived: false).expect(in: self)

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

    func test_obtainOrder_shouldReturnError_whenReceivedFAPIError() {
        // given
        let orderId = OrderId(5_957_631)

        stub(
            requestPartName: "resolveUserOrderByIdFull",
            responseFileName: "obtain_order_with_error"
        )

        // when
        let result = ordersService.obtainOrder(with: orderId, isArchived: false).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_obtainOrder_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderId = OrderId(5_957_631)

        stub(
            requestPartName: "resolveUserOrderByIdFull",
            responseFileName: "obtain_order_with_invalid_response"
        )

        // when
        let result = ordersService.obtainOrder(with: orderId, isArchived: false).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
