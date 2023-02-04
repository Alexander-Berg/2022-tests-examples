import BeruLegacyNetworking
import BeruMapping
import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class PaymentServiceTests: NetworkingTestCase {

    private var paymentService: PaymentServiceImpl!

    private let orderIds = [5_746_488, 2_941_657]
    private let rgbColors = "WHITE"
    private let cardId = "card-xa8a12bbe0bcb944fecdd0bde"
    private let paymentId = "123456"
    private let forceTrustSync = true

    override func setUp() {
        super.setUp()
        paymentService = PaymentServiceImpl(
            apiClient: DependencyProvider().apiClient,
            rgbColors: rgbColors
        )
    }

    func test_shouldObtainCreatedOrderPayment_whenParametersAndResponseValid() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)

            guard
                let params = json["params"].array?.first,
                let orderIds = params["orderIds"].array,
                let rgbColors = params["rgb"].string,
                let cardId = params["cardId"].string
            else { return false }

            return orderIds.compactMap { $0.string } == self.orderIds.map { String($0) }
                && rgbColors == self.rgbColors
                && cardId == self.cardId
        }

        stub(
            requestPartName: "resolveOrderPaymentByOrderIds",
            responseFileName: "create_payment_by_order_ids_results",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderPaymentByOrderIds"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = paymentService.pay(
            with: orderIds,
            cardId: cardId,
            bnplPlanConstructor: nil
        ).expect(in: self)

        // then
        switch result {
        case let .success(payment):
            XCTAssertEqual(payment.id, paymentId)
        default:
            XCTFail("Wrong order payment id \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenObtainCreatedOrderPaymentAndReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveOrderPaymentByOrderIds",
            responseFileName: "create_payment_by_order_ids_wrong_format"
        )

        // when
        let result = paymentService.pay(
            with: orderIds,
            cardId: cardId,
            bnplPlanConstructor: nil
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenObtainCreatedOrderPaymentAndErrorReceived() throws {
        // given
        stub(
            requestPartName: "resolveOrderPaymentByOrderIds",
            responseFileName: "create_payment_by_order_ids_error"
        )

        // when
        let result = paymentService.pay(
            with: orderIds,
            cardId: cardId,
            bnplPlanConstructor: nil
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }

    func test_shouldObtainOrderPayment_whenParametersAndResponseValid() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)

            guard
                let params = json["params"].array?.first,
                let paymentId = params["paymentId"].string,
                let forceTrustSync = params["forceTrustSync"].bool,
                let rgbColors = params["rgb"].string
            else { return false }

            return paymentId == self.paymentId
                && forceTrustSync == self.forceTrustSync
                && rgbColors == self.rgbColors
        }

        stub(
            requestPartName: "resolveOrderPaymentById",
            responseFileName: "obtain_payment_by_id_results",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderPaymentById"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = paymentService.paymentInfo(withId: paymentId).expect(in: self)

        // then
        switch result {
        case let .success(payment):
            XCTAssertEqual(payment.id, paymentId)
        default:
            XCTFail("Wrong order payment id \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenObtainOrderPaymentAndReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveOrderPaymentById",
            responseFileName: "obtain_payment_by_id_wrong_format"
        )

        // when
        let result = paymentService.paymentInfo(withId: paymentId).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenObtainOrderPaymentAndErrorReceived() throws {
        // given
        stub(
            requestPartName: "resolveOrderPaymentById",
            responseFileName: "obtain_payment_by_id_error"
        )

        // when
        let result = paymentService.paymentInfo(withId: paymentId).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }
}
