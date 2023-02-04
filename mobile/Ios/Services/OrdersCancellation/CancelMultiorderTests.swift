import OHHTTPStubs
import XCTest

@testable import BeruServices

final class CancelMultiorderTests: NetworkingTestCase {

    private var orderCancellationService: OrderCancellationServiceImpl!

    private let mainOrderId = OrderId(6_440_449)
    private let ordersToCancelIds = [OrderId(6_440_449), OrderId(6_440_450)]
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

            let mainOrderId = params?.first?["mainOrderId"] as? OrderId
            let ordersToCancelIds = params?.first?["ordersToCancelIds"] as? [OrderId]
            let substatus = params?.first?["substatus"] as? String
            let notes = params?.first?["notes"] as? String
            let rgb = params?.first?["rgb"] as? String

            return mainOrderId == OrderId(6_440_449)
                && ordersToCancelIds == [OrderId(6_440_449), OrderId(6_440_450)]
                && substatus == "CUSTOM"
                && notes == "notes"
                && rgb == "WHITE,BLUE"
        }

        stub(
            requestPartName: "saveCancellationMultiorderRequest",
            responseFileName: "cancel_multiorder",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "saveCancellationMultiorderRequest"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderCancellationService.cancelMultiorder(
            mainOrderId: mainOrderId,
            ordersToCancelIds: ordersToCancelIds,
            substatus: substatus,
            notes: notes
        ).expect(in: self)

        // then
        switch result {
        case let .success(orders):
            XCTAssertEqual(orders.count, 2)
        default:
            XCTFail("Failed to cancel multiorder \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "saveCancellationMultiorderRequest", code: 500)

        // when
        let result = orderCancellationService.cancelMultiorder(
            mainOrderId: mainOrderId,
            ordersToCancelIds: ordersToCancelIds,
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
}
