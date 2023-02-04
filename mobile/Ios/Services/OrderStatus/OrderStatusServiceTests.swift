import BeruLegacyNetworking
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class OrderStatusServiceTests: NetworkingTestCase {

    // MARK: - Properties

    private var orderStatusService: OrderStatusService!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        orderStatusService = OrderStatusServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        super.tearDown()
        orderStatusService = nil
    }

    // MARK: - Set user received

    func test_setUserReceived_shouldSendProperRequest() throws {
        // given
        let orderId = OrderId(12)
        let received = true
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let paramsDict = params?.first,
                let parsedReceived = paramsDict["userReceived"].bool
            else { return false }

            let parsedId = paramsDict["orderId"].intValue
            return parsedId == orderId
                && parsedReceived == received
        }
        stub(
            requestPartName: "resolveSetDeliveryFeedback",
            responseFileName: "set_delivery_feedback",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveSetDeliveryFeedback"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderStatusService.setUserReceived(orderId: orderId, userReceived: received).expect(in: self)

        // then
        XCTAssertNotNil(try result.get())
    }

}
