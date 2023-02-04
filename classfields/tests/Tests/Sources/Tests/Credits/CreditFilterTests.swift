//
//  CreditFilterTests.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 08.09.2021.
//

import XCTest
@testable import AutoRuCredit
import AutoRuUtils

final class CreditFilterTests: BaseUnitTest {
    func testDoneTap() {
        let module = emptyModule()
        let dismissExpectation = XCTestExpectation()
        _ = module.dismiss.subscribe(onNext: ({ dismissExpectation.fulfill() }))
        module.store.send(.doneTap)

        wait(for: [dismissExpectation], timeout: 1)
    }

    func testOpenCredit() {
        let module = emptyModule()
        module.store.send(.openCreditTap)
        XCTAssert(module.store.state.requestOpenCredit)
    }

    func testResetTap() {
        let module = SharkCreditPriceFilterModule(maxTerm: 60,
                                                  minTerm: 1,
                                                  rate: 0.099,
                                                  maxAmount: 5_000_000,
                                                  minAmount: 100_000,
                                                  priceFrom: 2_000_000,
                                                  priceTo: 4_000_000,
                                                  term: 60,
                                                  initialPayment: 0,
                                                  paymentFrom: 40_000,
                                                  paymentTo: 87_000,
                                                  enableCreditOnStart: false)!

        module.store.send(.resetTap)
        XCTAssert(module.store.state.priceTo == nil)
        XCTAssert(module.store.state.priceFrom == nil)
        XCTAssert(module.store.state.creditParam == nil)
    }

    func testChangeInitialPayment() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changeInitialPayment(1_000_000))

        XCTAssert(module.store.state.priceTo == 6_000_000)
        XCTAssert(module.store.state.priceFrom == 1_000_000)
        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)

    }

    func testChangeInitialPaymentOverLimits() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changeInitialPayment(100_000_000))

        XCTAssert(module.store.state.priceTo == 15_000_000)
        XCTAssert(module.store.state.priceFrom == 10_000_000)
        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)

    }

    func testChangeTerm() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changeTerm(36))

        XCTAssertEqual(module.store.state.priceTo, 3_294_000)
        XCTAssertEqual(module.store.state.priceFrom, 0)
        XCTAssertEqual(module.store.state.creditParam?.maxPayment, 163_000)

        XCTAssertEqual(module.store.state.creditParam?.paymentFrom, 0)
        XCTAssertEqual(module.store.state.creditParam?.paymentTo, 107_000)
    }

    func testChangeTermWithNoZeroPriceFrom() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changePriceFrom(1_000_000))
        module.store.send(.changeTerm(36))

        XCTAssertEqual(module.store.state.priceTo, 3_294_000)
        XCTAssertEqual(module.store.state.priceFrom, 678_000)
        XCTAssertEqual(module.store.state.creditParam?.maxPayment, 163_000)

        XCTAssertEqual(module.store.state.creditParam?.paymentFrom, 22_000)
        XCTAssertEqual(module.store.state.creditParam?.paymentTo, 107_000)
    }

    func testChangePayment() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changePayment((20_000, 50_000)))

        XCTAssert(module.store.state.priceTo == 2_340_000)
        XCTAssert(module.store.state.priceFrom == 936_000)
    }

    func testWithCreditTap_emptyPrice() {
        let module = emptyModule()
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 5_000_000)
        XCTAssert(module.store.state.priceFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
    }

    func testWithCreditTap_emptyPriceTo_normalPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceFrom(500_000))
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 5_000_000)
        XCTAssert(module.store.state.priceFrom == 500_000)

        XCTAssert(module.store.state.creditParam?.paymentFrom == 11_000)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
    }

    func testWithCreditTap_emptyPriceTo_overLimitPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceFrom(10_000_000))
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 15_000_000)
        XCTAssert(module.store.state.priceFrom == 10_000_000)

        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
        XCTAssert(module.store.state.creditParam?.initialPayment == 10_000_000)
    }

    func testWithCreditTap_normalPriceTo_emptyPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceTo(500_000))
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 500_000)
        XCTAssert(module.store.state.priceFrom == 0)

        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 11_000)
    }

    func testWithCreditTap_overLimitPriceTo_emptyPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceTo(10_000_000))
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 10_000_000)
        XCTAssert(module.store.state.priceFrom == 0)

        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
        XCTAssert(module.store.state.creditParam?.initialPayment == 5_000_000)
    }

    func testWithCreditTap_overLimitPriceTo_overLimitPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceTo(10_000_000))
        module.store.send(.changePriceFrom(7_000_000))
        module.store.send(.withCreditTap)
        XCTAssert(module.store.state.priceTo == 10_000_000)
        XCTAssert(module.store.state.priceFrom == 7_000_000)

        XCTAssert(module.store.state.creditParam?.paymentFrom == 43_000)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
        XCTAssert(module.store.state.creditParam?.initialPayment == 5_000_000)
    }

    func testWithCreditTap_withCreditOn() {
        let module = SharkCreditPriceFilterModule(maxTerm: 60,
                                                  minTerm: 1,
                                                  rate: 0.099,
                                                  maxAmount: 5_000_000,
                                                  minAmount: 100_000,
                                                  priceFrom: 2_000_000,
                                                  priceTo: 4_000_000,
                                                  term: 60,
                                                  initialPayment: 0,
                                                  paymentFrom: 40_000,
                                                  paymentTo: 87_000,
                                                  enableCreditOnStart: false)!
        module.store.send(.withCreditTap)

        XCTAssert(module.store.state.priceTo == 4_000_000)
        XCTAssert(module.store.state.priceFrom == 2_000_000)
        XCTAssert(module.store.state.creditParam == nil)
    }

    func testWithCreditTap_changePriceFrom_noCredit() {
        let module = emptyModule()
        module.store.send(.changePriceFrom(7_000_000))
        XCTAssert(module.store.state.priceFrom == 7_000_000)
        XCTAssert(module.store.state.creditParam == nil)
    }

    func testWithCreditTap_changePriceFrom_noCredit_morePriceTo() {
        let module = emptyModule()
        module.store.send(.changePriceTo(4_000_000))
        module.store.send(.changePriceFrom(7_000_000))

        XCTAssert(module.store.state.priceFrom == 4_000_000)
        XCTAssert(module.store.state.creditParam == nil)
    }

    func testWithCreditTap_changePriceFrom_noCredit_overLimits() {
        let module = emptyModule()
        module.store.send(.changePriceFrom(25_000_000))

        XCTAssert(module.store.state.priceFrom == 15_000_000)
        XCTAssert(module.store.state.creditParam == nil)
    }

    func testWithCreditTap_changePriceFrom_credit() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changePriceFrom(5_000_000))

        XCTAssert(module.store.state.creditParam?.paymentFrom == 107_000)
    }

    func testWithCreditTap_changePriceTo_noCredit() {
        let module = emptyModule()

        module.store.send(.changePriceTo(500_000))

        XCTAssert(module.store.state.priceTo == 500_000)
    }

    func testWithCreditTap_changePriceTo_overLimit() {
        let module = emptyModule()

        module.store.send(.changePriceTo(50_000_000))

        XCTAssert(module.store.state.priceTo == 15_000_000)
    }

    func testWithCreditTap_changePriceTo_lessPriceFrom() {
        let module = emptyModule()
        module.store.send(.changePriceFrom(50_000_000))

        module.store.send(.changePriceTo(10_000_000))

        XCTAssert(module.store.state.priceTo == 10_000_000)
        XCTAssert(module.store.state.priceFrom == 10_000_000)
    }

    func testWithCreditTap_changePriceTo_credit() {
        let module = emptyModule()
        module.store.send(.withCreditTap)
        module.store.send(.changePriceTo(10_000_000))

        XCTAssert(module.store.state.priceTo == 10_000_000)
        XCTAssert(module.store.state.creditParam?.initialPayment == 5_000_000)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
    }

    func testWithCreditTap_enableCreditOnStart() {
        let module = SharkCreditPriceFilterModule(maxTerm: 60,
                                                  minTerm: 1,
                                                  rate: 0.099,
                                                  maxAmount: 5_000_000,
                                                  minAmount: 100_000,
                                                  priceFrom: nil,
                                                  priceTo: nil,
                                                  term: nil,
                                                  initialPayment: nil,
                                                  paymentFrom: nil,
                                                  paymentTo: nil,
                                                  enableCreditOnStart: true)!

        XCTAssert(module.store.state.priceTo == 5_000_000)
        XCTAssert(module.store.state.priceFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentFrom == 0)
        XCTAssert(module.store.state.creditParam?.paymentTo == 107_000)
    }

    func emptyModule() -> SharkCreditPriceFilterModule {
        return SharkCreditPriceFilterModule(maxTerm: 60,
                                            minTerm: 1,
                                            rate: 0.099,
                                            maxAmount: 5_000_000,
                                            minAmount: 100_000,
                                            priceFrom: nil,
                                            priceTo: nil,
                                            term: nil,
                                            initialPayment: nil,
                                            paymentFrom: nil,
                                            paymentTo: nil,
                                            enableCreditOnStart: false)!
    }
}
