import MarketDTO
import MarketModels
import XCTest

@testable import BeruServices

class PaymentMethodMapperTests: XCTestCase {

    // MARK: - Request

    func test_paymentMethodParamsMapping(
        input: PaymentMethodNew,
        output: PaymentMethodResult
    ) {
        // when
        let result = PaymentMethodMapper.makePaymentMethodParams(from: input)

        // then
        XCTAssertEqual(result, output)
    }

    func test_paymentMethodParamsMapping_whenCashOnDelivery() {
        test_paymentMethodParamsMapping(
            input: .cashOnDelivery,
            output: .cashOnDelivery
        )
    }

    func test_paymentMethodParamsMapping_whenCardOnDelivery() {
        test_paymentMethodParamsMapping(
            input: .cardOnDelivery,
            output: .cardOnDelivery
        )
    }

    func test_paymentMethodParamsMapping_whenCard() {
        test_paymentMethodParamsMapping(
            input: .card,
            output: .yandex
        )
    }

    func test_paymentMethodParamsMapping_whenApplePayResult() {
        test_paymentMethodParamsMapping(
            input: .applePay,
            output: .applePay
        )
    }

    func test_paymentMethodParamsMapping_whenTinkoffCreditResult() {
        test_paymentMethodParamsMapping(
            input: .tinkoffCredit,
            output: .tinkoffCredit
        )
    }

    func test_paymentMethodParamsMapping_whenTinkoffInstallmentsResult() {
        test_paymentMethodParamsMapping(
            input: .tinkoffInstallments,
            output: .tinkoffInstallments
        )
    }

    func test_paymentMethodParamsMapping_whenSBP() {
        test_paymentMethodParamsMapping(
            input: .sbp,
            output: .sbp
        )
    }

    // MARK: - Response

    func test_paymentMethodMapping(
        input: PaymentMethodResult,
        output: PaymentMethodNew
    ) {
        // when
        let result = PaymentMethodMapper.extractPaymentMethod(from: input)

        // then
        XCTAssertEqual(result, output)
    }

    func test_paymentMethodMapping_whenCashOnDeliveryResult() {
        test_paymentMethodMapping(
            input: .cashOnDelivery,
            output: .cashOnDelivery
        )
    }

    func test_paymentMethodMapping_whenCardOnDeliveryResult() {
        test_paymentMethodMapping(
            input: .cardOnDelivery,
            output: .cardOnDelivery
        )
    }

    func test_paymentMethodMapping_whenCardResult() {
        test_paymentMethodMapping(
            input: .yandex,
            output: .card
        )
    }

    func test_paymentMethodMapping_whenApplePayResult() {
        test_paymentMethodMapping(
            input: .applePay,
            output: .applePay
        )
    }

    func test_paymentMethodMapping_whenTinkoffCreditResult() {
        test_paymentMethodMapping(
            input: .tinkoffCredit,
            output: .tinkoffCredit
        )
    }

    func test_paymentMethodMapping_whenTinkoffInstallmentsResult() {
        test_paymentMethodMapping(
            input: .tinkoffInstallments,
            output: .tinkoffInstallments
        )
    }

    func test_paymentMethodMapping_whenSBPResult() {
        test_paymentMethodMapping(
            input: .sbp,
            output: .sbp
        )
    }

}
