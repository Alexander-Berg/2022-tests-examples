import XCTest
@testable import MarketCashback

// swiftlint:disable line_length force_unwrapping
final class CartCashbackTextTests: XCTestCase {

    let cashbackAmountOptions = [
        100: "100 баллов",
        1_234: "1 234 балла",
        11_121: "11 121 балл"
    ]

    let cashbackAmountOptionsWithoutText = [
        100: "100",
        1_234: "1 234",
        11_121: "11 121"
    ]

    func testMirForCart() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "С картой «Мир» — ещё  \(cashbackAmountString)\nОплатите заказ картой «Мир» онлайн и получите ещё 10% баллами",
                (try XCTUnwrap(
                    CartCashbackTextFactory
                        .makePaymentSystemCashbackTextForCart(cashbackAmount, paymentSystem: .mir, percentage: 10)
                ))
                .accessibilityLabel
            )
        }
    }

    func testMastercardForCart() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "С Mastercard — ещё  \(cashbackAmountString)\nОплатите заказ картой Mastercard® онлайн и получите ещё 10% баллами",
                (try XCTUnwrap(CartCashbackTextFactory.makePaymentSystemCashbackTextForCart(
                    cashbackAmount,
                    paymentSystem: .mastercard,
                    percentage: 10
                )))
                .accessibilityLabel
            )
        }
    }

    func testPlusSubscription() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Купите дешевле на \(cashbackAmountOptionsWithoutText[cashbackAmount]!) ₽\nПодключите Плюс, чтобы списать за заказ  \(cashbackAmountString)",
                (try XCTUnwrap(CartCashbackTextFactory.makePlusSubscriptionForCart(
                    isSubscriptionActive: false,
                    spendAmount: cashbackAmount
                ))).accessibilityLabel
            )

            XCTAssertEqual(
                "От  \(cashbackAmountString)\nполучите за покупку, если\nподключите Плюс сейчас",
                (try XCTUnwrap(CartCashbackTextFactory.makePlusSubscriptionForCart(
                    isSubscriptionActive: false,
                    emitAmount: cashbackAmount,
                    isPlusForNoLogIn: true
                ))).accessibilityLabel
            )

            XCTAssertEqual(
                "Купите дешевле на \(cashbackAmountOptionsWithoutText[cashbackAmount]!) ₽\nСпишите  \(cashbackAmountString) при оформлении",
                (try XCTUnwrap(CartCashbackTextFactory.makePlusSubscriptionForCart(
                    isSubscriptionActive: true,
                    spendAmount: cashbackAmount
                ))).accessibilityLabel
            )

            XCTAssertEqual(
                "Купите дешевле на \(cashbackAmountOptionsWithoutText[cashbackAmount]!) ₽\nСпишите  \(cashbackAmountString) при оформлении или получите  \(cashbackAmountString) за покупку",
                (try XCTUnwrap(CartCashbackTextFactory.makePlusSubscriptionForCart(
                    isSubscriptionActive: true,
                    spendAmount: cashbackAmount,
                    emitAmount: cashbackAmount
                ))).accessibilityLabel
            )
        }
    }

    func testMakeGrowingCashbackNotPlusCartString() {
        // when
        let text = CartCashbackTextFactory.makeGrowingCashbackCartNotPlusString(100, minCartAmount: 1_500)

        // then
        let expectedResult = " 100 баллов за заказ от 1 500 ₽,\nесли подключите Яндекс Плюс"
        XCTAssertEqual(text.accessibilityLabel, expectedResult)
    }

    func testMakeGrowingCashbackCartString() {
        // when
        let text = CartCashbackTextFactory.makeGrowingCashbackCartString(100, priceLeftAmount: 150)

        // then
        let expectedResult = "Ещё   100 баллов за заказ,\nесли добавите товаров ещё на 150 ₽"
        XCTAssertEqual(text.accessibilityLabel, expectedResult)
    }

    func testMakeGrowingCashbackCartCompletedString() {
        // when
        let text = CartCashbackTextFactory.makeGrowingCashbackCartCompletedString(100)

        // then
        XCTAssertEqual(text.accessibilityLabel, "Ещё   100 баллов за заказ\nБаллы придут вместе с заказом")
    }

    func testMakeYandexCardForCart() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Со Счётом в Яндексе — ещё  \(cashbackAmountString)\nОплатите заказ со Счёта в Яндексе онлайн и получите ещё 10% баллами",
                (try XCTUnwrap(CartCashbackTextFactory.makeYandexCardForCart(cashbackAmount, percentage: 10)))
                    .accessibilityLabel
            )
        }
    }
}
