import MarketAPI
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

class SaveOrderEditingRequestTests: NetworkingTestCase {

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

    func test_shouldReceiveProperEditDeliveryRequest() {
        // given
        let orderId = OrderId(4_434_692)
        guard let dateInterval = makeDeliveryDateIntervalFromJsonWithName("delivery_date_interval") else {
            XCTFail("Unable to create DeliveryDateInterval model")
            return
        }
        guard let timeInterval = makeDeliveryTimeIntervalFromJsonWithName("delivery_time_interval") else {
            XCTFail("Unable to create DeliveryTimeInterval model")
            return
        }
        guard let recipient = makeRecipientFromJsonWithName("recipient") else {
            XCTFail("Unable to create Recipient model")
            return
        }
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            let delivery = params?.first?["delivery"]
            let interval = delivery?["interval"]
            let recipient = params?.first?["recipient"]
            let recipientName = recipient?["recipientName"]

            return params?.first?["orderId"].intValue == 4_434_692
                && delivery?["fromDate"].string == "16-10-2019"
                && delivery?["toDate"].string == "16-10-2019"
                && delivery?["reason"].string == "USER_MOVED_DELIVERY_DATES"
                && interval?["fromTime"].string == "10:00"
                && interval?["toTime"].string == "14:00"
                && recipient?["phone"].string == "+79999999999"
                && recipientName?["firstName"].string == "Имя"
                && recipientName?["lastName"].string == "Фамилия"
        }

        stub(
            requestPartName: "saveOrderEditingRequest",
            responseFileName: "simple_save_edit_delivery_request",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "saveOrderEditingRequest"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderEditService.saveOrderEditingRequest(
            with: orderId,
            dateInterval: dateInterval,
            timeInterval: timeInterval,
            recipient: recipient,
            extendedDate: nil,
            paymentMethod: nil,
            address: nil,
            outlet: nil,
            deliveryType: nil
        ).expect(in: self)

        // then
        switch result {
        case let .success(orderEditRequest):
            XCTAssertNotNil(orderEditRequest)
        default:
            XCTFail("Wrong edit options count result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderId = OrderId(4_434_692)
        guard let dateInterval = makeDeliveryDateIntervalFromJsonWithName("delivery_date_interval") else {
            XCTFail("Unable to create DeliveryDateInterval model")
            return
        }
        guard let timeInterval = makeDeliveryTimeIntervalFromJsonWithName("delivery_time_interval") else {
            XCTFail("Unable to create DeliveryTimeInterval model")
            return
        }
        guard let recipient = makeRecipientFromJsonWithName("recipient") else {
            XCTFail("Unable to create Recipient model")
            return
        }
        stubError(requestPartName: "saveOrderEditingRequest", code: 500)

        // when
        let result = orderEditService.saveOrderEditingRequest(
            with: orderId,
            dateInterval: dateInterval,
            timeInterval: timeInterval,
            recipient: recipient,
            extendedDate: nil,
            paymentMethod: nil,
            address: nil,
            outlet: nil,
            deliveryType: nil
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
        guard let dateInterval = makeDeliveryDateIntervalFromJsonWithName("delivery_date_interval") else {
            XCTFail("Unable to create DeliveryDateInterval model")
            return
        }
        guard let timeInterval = makeDeliveryTimeIntervalFromJsonWithName("delivery_time_interval") else {
            XCTFail("Unable to create DeliveryTimeInterval model")
            return
        }
        guard let recipient = makeRecipientFromJsonWithName("recipient") else {
            XCTFail("Unable to create Recipient model")
            return
        }
        stub(
            requestPartName: "saveOrderEditingRequest",
            responseFileName: "save_edit_delivery_request_with_error"
        )

        // when
        let result = orderEditService.saveOrderEditingRequest(
            with: orderId,
            dateInterval: dateInterval,
            timeInterval: timeInterval,
            recipient: recipient,
            extendedDate: nil,
            paymentMethod: nil,
            address: nil,
            outlet: nil,
            deliveryType: nil
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
        guard let dateInterval = makeDeliveryDateIntervalFromJsonWithName("delivery_date_interval") else {
            XCTFail("Unable to create DeliveryDateInterval model")
            return
        }
        guard let timeInterval = makeDeliveryTimeIntervalFromJsonWithName("delivery_time_interval") else {
            XCTFail("Unable to create DeliveryTimeInterval model")
            return
        }
        guard let recipient = makeRecipientFromJsonWithName("recipient") else {
            XCTFail("Unable to create Recipient model")
            return
        }
        stub(
            requestPartName: "saveOrderEditingRequest",
            responseFileName: "save_edit_delivery_request_with_invalid_response"
        )

        // when
        let result = orderEditService.saveOrderEditingRequest(
            with: orderId,
            dateInterval: dateInterval,
            timeInterval: timeInterval,
            recipient: recipient,
            extendedDate: nil,
            paymentMethod: nil,
            address: nil,
            outlet: nil,
            deliveryType: nil
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldSendEditDeliveryRequestSavedNotification() {
        // given
        let orderId = OrderId(4_434_692)
        guard let dateInterval = makeDeliveryDateIntervalFromJsonWithName("delivery_date_interval") else {
            XCTFail("Unable to create DeliveryDateInterval model")
            return
        }
        guard let timeInterval = makeDeliveryTimeIntervalFromJsonWithName("delivery_time_interval") else {
            XCTFail("Unable to create DeliveryTimeInterval model")
            return
        }
        guard let recipient = makeRecipientFromJsonWithName("recipient") else {
            XCTFail("Unable to create Recipient model")
            return
        }
        stub(
            requestPartName: "saveOrderEditingRequest",
            responseFileName: "simple_save_edit_delivery_request"
        )

        // when
        let condition = expectation(
            forNotification: Notification.Name.OrderEditService.editDeliveryRequestSavedNotification,
            object: nil,
            handler: nil
        )
        orderEditService.saveOrderEditingRequest(
            with: orderId,
            dateInterval: dateInterval,
            timeInterval: timeInterval,
            recipient: recipient,
            extendedDate: nil,
            paymentMethod: nil,
            address: nil,
            outlet: nil,
            deliveryType: nil
        ).expect(in: self)

        // then
        wait(for: [condition], timeout: 1)
    }

    // MARK: - Private

    private func makeRecipientFromJsonWithName(_ name: String) -> Recipient? {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return nil }

        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)

        return jsonRepresentation.map(toFapiClass: Recipient.self) as? Recipient
    }

    private func makeDeliveryDateIntervalFromJsonWithName(_ name: String) -> DeliveryDateInterval? {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return nil }

        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)

        return jsonRepresentation.map(toFapiClass: DeliveryDateInterval.self) as? DeliveryDateInterval
    }

    private func makeDeliveryTimeIntervalFromJsonWithName(_ name: String) -> DeliveryTimeInterval? {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return nil }

        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)

        return jsonRepresentation.map(toFapiClass: DeliveryTimeInterval.self) as? DeliveryTimeInterval
    }

}
