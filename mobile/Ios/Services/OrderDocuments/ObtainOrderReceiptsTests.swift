import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

class ObtainOrderReceiptsTests: NetworkingTestCase {

    private var orderDocumentsService: OrderDocumentsServiceImpl!

    override func setUp() {
        super.setUp()
        orderDocumentsService = OrderDocumentsServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        orderDocumentsService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperOrderReceipts() {
        // given
        let orderId = OrderId(5_957_631)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard let orderId = params?.first?["orderId"].intValue else {
                return false
            }
            return orderId == 5_957_631
        }

        stub(
            requestPartName: "resolveOrderReceiptsByOrderId",
            responseFileName: "simple_obtain_order_receipts",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderReceiptsByOrderId"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderDocumentsService.obtainReceipts(with: orderId, isArchived: false).expect(in: self)

        // then
        switch result {
        case let .success(receipts):
            XCTAssertEqual(receipts.count, 2)
        default:
            XCTFail("Wrong order result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderId = OrderId(5_957_631)

        stubError(requestPartName: "resolveOrderReceiptsByOrderId", code: 500)

        // when
        let result = orderDocumentsService.obtainReceipts(with: orderId, isArchived: false).expect(in: self)

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
        let orderId = OrderId(5_957_631)

        stub(
            requestPartName: "resolveOrderReceiptsByOrderId",
            responseFileName: "obtain_order_receipts_with_error"
        )

        // when
        let result = orderDocumentsService.obtainReceipts(with: orderId, isArchived: false).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.unknown() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderId = OrderId(5_957_631)

        stub(
            requestPartName: "resolveOrderReceiptsByOrderId",
            responseFileName: "obtain_order_receipts_with_invalid_response"
        )

        // when
        let result = orderDocumentsService.obtainReceipts(with: orderId, isArchived: false).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }
}
