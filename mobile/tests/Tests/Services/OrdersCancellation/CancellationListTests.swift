import OHHTTPStubs
import XCTest

@testable import BeruServices

final class CancellationListTests: NetworkingTestCase {

    private var orderCancellationService: OrderCancellationServiceImpl!

    private let orderId = OrderId(6_440_362)
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
            let rgb = params?.first?["rgb"] as? String

            return orderId == OrderId(6_440_362) && rgb == "WHITE,BLUE"
        }

        let expectedData: [OrderToCancel] = [
            OrderToCancel(id: 6_440_362, itemsCount: 1, buyerCurrency: .RUB, buyerTotal: 4_800),
            OrderToCancel(id: 6_440_363, itemsCount: 1, buyerCurrency: .RUB, buyerTotal: 1)
        ]

        stub(
            requestPartName: "resolveCancellationList",
            responseFileName: "cancellation_list",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveCancellationList"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderCancellationService.obtainOrdersToCancelList(orderId: orderId).expect(in: self)

        // then
        guard case let .success(orders) = result else {
            XCTFail("Failed to cancel multiorder \(String(describing: result))")
            return
        }

        XCTAssertEqual(orders.count, expectedData.count)
        zip(orders, expectedData).forEach { responseOrder, expectedOrder in
            XCTAssertEqual(responseOrder.id, expectedOrder.id)
            XCTAssertEqual(responseOrder.itemsCount, expectedOrder.itemsCount)
            XCTAssertEqual(responseOrder.buyerCurrency, expectedOrder.buyerCurrency)
            XCTAssertEqual(responseOrder.buyerTotal, expectedOrder.buyerTotal)
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "resolveCancellationList", code: 500)

        // when
        let result = orderCancellationService.obtainOrdersToCancelList(orderId: orderId).expect(in: self)

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
