import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleCard AutoRuPayments AutoRuStandaloneCarHistory
final class AttachedCardPaymentTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_successfullPaymentUsingAttachedCard() {
        let requestTimeout: TimeInterval = 2
        let ticketId = "user:44730048-acf993a6c3cbb188c6a56e1bd3c63c3a-1622047281927"

        let paymentInitRequestExpectation = expectationForRequest(
            method: "POST",
            uri: "/billing/autoru/payment/init",
            requestChecker: { (req: Auto_Api_Billing_InitPaymentRequest) -> Bool in
                XCTAssertEqual(req.product.count, 1)
                XCTAssertEqual(req.product[0].name, "offers-history-reports")
                XCTAssertEqual(req.product[0].count, 1)
                return true
            }
        )

        let paymentProcessRequestExpectation = expectationForRequest(
            method: "POST",
            uri: "/billing/autoru/payment/process",
            requestChecker: { (req: Auto_Api_Billing_ProcessPaymentRequest) -> Bool in
                XCTAssertEqual(req.payByPaymentMethod.ticketID, ticketId)
                return true
            }
        )

        let paymentStatusRequestExpectation = expectationForRequest(
            method: "GET",
            uri: "/billing/autoru/payment?ticket_id=\(ticketId)"
        )

        api.offer.category(.cars).offerID("1101613244-b69e1290")
            .get
            .ok(mock: .file("PaymentHistoryAll_discount-precedence_offer"))
        mocker
            .mock_reportLayoutForOffer(bought: false)
            .mock_paymentInitWithAttachedCard()
            .mock_paymentProcess()
            .mock_paymentClosed()

        let paymentOptionsSteps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
            .wait(for: 1)
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .purchaseReportButton)
                    .tap(.purchaseReportButton)
            }

        wait(for: [paymentInitRequestExpectation], timeout: requestTimeout)

        paymentOptionsSteps
            .wait(for: 1)
            .should(provider: .paymentOptionsScreen, .exist)
            .focus { screen in
                screen
                    .tap(.purchaseButton)
            }


        wait(for: [paymentProcessRequestExpectation, paymentStatusRequestExpectation], timeout: requestTimeout)

        paymentOptionsSteps
            .focus({ screen in
                screen
                    .as(PaymentOptionsSteps<SaleCardSteps>.self)
                    .checkHasActivityHud("Готово!")
            })

    }
}
