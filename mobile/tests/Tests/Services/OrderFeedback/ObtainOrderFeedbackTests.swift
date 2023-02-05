import BeruLegacyNetworking
import BeruMapping
import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class ObtainOrderFeedbackTests: NetworkingTestCase {

    private var orderFeedbackService: OrderFeedbackServiceImpl!

    override func setUp() {
        super.setUp()
        let apiClient = DependencyProvider().legacyAPIClient
        orderFeedbackService = OrderFeedbackServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            notificationCenter: NotificationCenter.default
        )
    }

    func test_shouldReceiveProperFeedback() {
        // given
        let orderId: OrderId = 5_746_488
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard let orderId = params?.first?["orderId"] else {
                return false
            }
            return orderId == 5_746_488
        }

        stub(
            requestPartName: "resolveOrderFeedback",
            responseFileName: "simple_obtain_order_feedback",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderFeedback"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderFeedbackService.obtainOrderFeedback(for: orderId).expect(in: self)

        // then
        switch result {
        case let .success(orderFeedback):
            XCTAssertNotNil(orderFeedback)
        default:
            XCTFail("Failed to obtain feedback \(String(describing: result))")
        }
    }

    func test_shouldObtainOrderGrades() {
        // given
        let targetOrderId: OrderId = 5_746_488
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard let orderId = params?.first?["orderIds"].array?.first?.int else {
                return false
            }
            return orderId == 5_746_488
        }

        stub(
            requestPartName: "resolveOrderGradesByIds",
            responseFileName: "obtain_order_grades",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderGradesByIds"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderFeedbackService.orderGrades(for: [targetOrderId]).expect(in: self)

        // then
        switch result {
        case let .success(orderGrades):
            XCTAssertEqual(orderGrades.count, 1)
        default:
            XCTFail("Wrong order grades count result \(String(describing: result))")
        }
    }

    func test_shouldObtainAllOrderGrades() {
        // given
        stub(
            requestPartName: "resolveOrderGrades",
            responseFileName: "obtain_order_all_grades",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderGrades"])
        )

        // when
        let result = orderFeedbackService.orderGrades().expect(in: self)

        // then
        switch result {
        case let .success(orderGrades):
            XCTAssertEqual(orderGrades.count, 1)
        default:
            XCTFail("Wrong order grades count result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        let orderId: OrderId = 5_746_488
        stub(
            requestPartName: "resolveOrderFeedback",
            responseFileName: "obtain_order_feedback_invalid_response"
        )

        // when
        let result = orderFeedbackService.obtainOrderFeedback(for: orderId).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReceiveProperScenario_whenSendFeedback() {
        // given
        let orderFeedbacks = makeOrderFeedbacksFromJsonWithName("simple_obtain_order_feedback")

        guard
            let orderFeedback = orderFeedbacks.first
        else {
            XCTFail("Unable to create OrderFeedback model")
            return
        }

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            print(json)

            guard
                let feedback = json["params"].array?.first?["feedback"],
                let question = feedback["answers"].array?.first
            else {
                return false
            }
            return feedback["id"].int == 5_746_488
                && question["questionId"].int == 5_678
        }

        stub(
            requestPartName: "setOrderFeedback",
            responseFileName: "send_order_feedback",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "setOrderFeedback"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = orderFeedbackService.sendOrderFeedback(orderFeedback).expect(in: self)

        // then
        switch result {
        case let .success(scenario):
            XCTAssertNotNil(scenario)
        default:
            XCTFail("Failed to obtain scenario \(String(describing: result))")
        }
    }

    // MARK: - Private

    private func makeOrderFeedbacksFromJsonWithName(_ name: String) -> [OrderFeedback] {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return [] }
        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)
        let jsonResponseCollection = YBMFAPIResponseCollections.model(withJSON: jsonRepresentation)
        return jsonResponseCollection?.models(
            ofClass: OrderFeedback.self,
            atKeypath: "orderFeedback"
        ) as? [OrderFeedback] ?? []
    }
}
