import XCTest
@testable import MarketCashback

final class CheckoutCashbackTextTests: XCTestCase {

    let cashbackAmountOptions = [
        100: "100 баллов",
        1_234: "1 234 балла",
        11_121: "11 121 балл"
    ]

    func testMastercardHeaderForCheckout() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Оплатите заказ картой  Mastercard® онлайн и получите дополнительно  \(cashbackAmountString)",
                try XCTUnwrap(
                    CheckoutCashbackTextFactory
                        .makePaymentSystemHeaderForCheckout(cashbackAmount, paymentSystem: .mastercard)
                )
                .accessibilityLabel
            )
        }
    }

    func testMirHeaderForCheckout() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Оплатите заказ онлайн картой  и получите дополнительно  \(cashbackAmountString)",
                try XCTUnwrap(
                    CheckoutCashbackTextFactory
                        .makePaymentSystemHeaderForCheckout(cashbackAmount, paymentSystem: .mir)
                )
                .accessibilityLabel
            )
        }
    }

    func testYandexCardHeaderForCheckout() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Оплатите заказ со Счёта в Яндексе  онлайн и получите дополнительно  \(cashbackAmountString)",
                try XCTUnwrap(
                    CheckoutCashbackTextFactory
                        .makeYandexCardHeaderForCheckout(cashbackAmount: cashbackAmount, hasYandexCard: false)
                )
                .accessibilityLabel
            )
        }
    }

    func testCashbackForOutlet() throws {
        // when
        let cashbackStringLong = try XCTUnwrap(
            CheckoutCashbackTextFactory
                .makeCashbackForOutlet(100, shortVersion: false)
        )
        let cashbackStringShort = try XCTUnwrap(
            CheckoutCashbackTextFactory
                .makeCashbackForOutlet(100, shortVersion: true)
        )

        // then
        XCTAssertEqual(" 100 баллов в подарок", cashbackStringLong.attributedString.accessibilityLabel)
        XCTAssertEqual(" 100", cashbackStringShort.attributedString.accessibilityLabel)
    }

    func testDeliveryDisclaimer() {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "За самовывоз —  \(cashbackAmountString)",
                CheckoutCashbackTextFactory.makeDeliveryDisclaimer(cashbackAmount).attributedString.accessibilityLabel
            )
        }
    }

    func testAdvertisingCampaignIncompleteString() {
        // when
        let cashbackString = CheckoutCashbackTextFactory.makeAdvertisingCampaignIncompleteString(
            priceLeftAmount: 1_000,
            cashbackAmount: 500
        )

        // then
        XCTAssertEqual(
            "Ещё ￼﻿￼﻿ 500 баллов за 1й заказ\nесли добавите товаров ещё на 1 000 ₽",
            cashbackString.accessibilityLabel
        )
    }

    func testAdvertisingCampaignCompleteString() {
        // when
        let cashbackString = CheckoutCashbackTextFactory.makeAdvertisingCampaignCompleteString(cashbackAmount: 500)

        // then
        XCTAssertEqual(
            "Ещё ￼﻿￼﻿ 500 баллов за 1й заказ\nБаллы придут вместе с заказом",
            cashbackString.accessibilityLabel
        )
    }

    func testNotInPlusDisclaimer() throws {
        for (cashbackAmount, cashbackAmountString) in cashbackAmountOptions {
            XCTAssertEqual(
                "Получите от  \(cashbackAmountString) за покупку,\nесли подключите Плюс сейчас",
                (try XCTUnwrap(
                    CheckoutCashbackTextFactory.makeNotInPlusDisclaimer(cashbackAmount)?.attributedString
                )).accessibilityLabel
            )
        }
    }

}
