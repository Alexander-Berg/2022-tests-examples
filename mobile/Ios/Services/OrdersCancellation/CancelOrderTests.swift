import OHHTTPStubs
import XCTest

@testable import BeruServices

final class CancelOrderTests: NetworkingTestCase {

    private var orderCancellationService: OrderCancellationServiceImpl!

    private let orderId = OrderId(6_430_531)
    private let substatus = "CUSTOM"
    private let notes = "notes"
    private let rgbColor = "WHITE,BLUE"

    override func setUp() {
        super.setUp()

        orderCancellationService = OrderCancellationServiceImpl(
            apiClient: DependencyProvider().apiClient,
            rgbColors: rgbColor
        )
    }

    override func tearDown() {
        orderCancellationService = nil
        super.tearDown()
    }

    func test_shouldSendProperRequest() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]

            let orderId = params?.first?["orderId"] as? OrderId
            let substatus = params?.first?["substatus"] as? String
            let notes = params?.first?["notes"] as? String
            let rgb = params?.first?["rgb"] as? String

            return orderId == OrderId(6_430_531)
                && substatus == "CUSTOM"
                && notes == "notes"
                && rgb == "WHITE,BLUE"
        }

        stub(
            requestPartName: "saveCancellationRequest",
            responseFileName: "cancel_order",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "saveCancellationRequest"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        switch result {
        case let .success(order):
            XCTAssertNotNil(order)
        default:
            XCTFail("Failed to cancel order \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "saveCancellationRequest", code: 500)

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
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

    func test_shouldReturnProperError_whenReceivedAlreadyRequestedError() {
        // given
        stub(
            requestPartName: "saveCancellationRequest",
            responseFileName: "cancel_order_already_requested"
        )

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        guard
            case let .failure(error as OrderCancellationError) = result,
            error == OrderCancellationError.cancellationAlreadyRequested
        else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnProperError_whenReceivedCannotBeCancelledSeparatelyError() {
        // given
        stub(
            requestPartName: "saveCancellationRequest",
            responseFileName: "cancel_order_cannot_be_cancelled_separately"
        )

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        guard
            case let .failure(error as OrderCancellationError) = result,
            error == OrderCancellationError.orderCannotBeCancelledSeparately
        else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnProperError_whenReceivedStatusNotAllowedForCancellationError() {
        // given
        stub(
            requestPartName: "saveCancellationRequest",
            responseFileName: "cancel_order_status_not_allowed_for_cancellation"
        )

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        guard
            case let .failure(error as OrderCancellationError) = result,
            error == OrderCancellationError.statusNotAllowedForCancellation
        else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnProperError_whenReceivedOrderNotFoundError() {
        // given
        stub(
            requestPartName: "saveCancellationRequest",
            responseFileName: "cancel_order_order_not_found"
        )

        // when
        let result = orderCancellationService.cancelOrder(
            orderId: orderId,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        guard
            case let .failure(error as OrderCancellationError) = result,
            error == OrderCancellationError.orderNotFound
        else {
            XCTFail("Incorrect error class")
            return
        }
    }

}
