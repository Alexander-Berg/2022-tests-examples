import MarketModels
import XCTest
@testable import MarketCashback

final class SKUCashbackTextTests: XCTestCase {

    let mastercardPromo = AggregateOfferPromo(
        id: "ecsdcsdcscsd",
        type: .cashbackByPaymentSystem,
        value: "123",
        semanticId: nil,
        promoKeys: [],
        tags: [.paymentSystem],
        info: [.init(paymentSystem: .mastercard)],
        priority: 0
    )

    let mirPromo = AggregateOfferPromo(
        id: "ecsdcsdcscsd",
        type: .cashbackByPaymentSystem,
        value: "123",
        semanticId: nil,
        promoKeys: [],
        tags: [.paymentSystem],
        info: [.init(paymentSystem: .mir)],
        priority: 0
    )

    let yandexCardPromo = AggregateOfferPromo(
        id: "adgdsfdsfsf",
        type: .cashbackYandexCard,
        value: "999",
        semanticId: nil,
        promoKeys: [],
        tags: [.extraCashback],
        info: [],
        priority: 0
    )

    func testSkuMastercardCashback() throws {
        // when
        let regularCashback = try XCTUnwrap(SKUCashbackTextFactory.makeSkuPaymentSystemCashback(
            additionalPromo: mastercardPromo,
            isWithFromPrefix: false
        ))
        let cashbackFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuPaymentSystemCashback(
            additionalPromo: mastercardPromo,
            isWithFromPrefix: true
        ))

        // then
        XCTAssertEqual(" 123 с Mastercard", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  123 с Mastercard", cashbackFrom.accessibilityLabel)
    }

    func testSkuMirCashback() throws {
        // when
        let regularCashback = try XCTUnwrap(SKUCashbackTextFactory.makeSkuPaymentSystemCashback(
            additionalPromo: mirPromo,
            isWithFromPrefix: false
        ))
        let cashbackFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuPaymentSystemCashback(
            additionalPromo: mirPromo,
            isWithFromPrefix: true
        ))

        // then
        XCTAssertEqual(" 123 с картой «Мир»", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  123 с картой «Мир»", cashbackFrom.accessibilityLabel)
    }

    func testSkuYandexCardCashback() throws {
        // when
        let regularCashback = try XCTUnwrap(SKUCashbackTextFactory.makeSkuYandexCardCashback(
            additionalPromo: yandexCardPromo,
            isWithFromPrefix: false
        ))
        let cashbackFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuYandexCardCashback(
            additionalPromo: yandexCardPromo,
            isWithFromPrefix: true
        ))

        // then
        XCTAssertEqual(" 999 со Счётом в Яндексе", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  999 со Счётом в Яндексе", cashbackFrom.accessibilityLabel)
    }

    func testSkuCashback() throws {
        // when
        let regularCashback = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: nil,
            isWithFromPrefix: false,
            isExtra: false
        ))
        let cashbackFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: nil,
            isWithFromPrefix: true,
            isExtra: false
        ))
        let cashbackExtra = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: nil,
            isWithFromPrefix: false,
            isExtra: true
        ))
        let cashbackExtraFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: nil,
            isWithFromPrefix: true,
            isExtra: true
        ))
        let regularCashbackWithMasterCard = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: mastercardPromo,
            isWithFromPrefix: false,
            isExtra: false
        ))
        let regularCashbackWithMasterCardFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: mastercardPromo,
            isWithFromPrefix: true,
            isExtra: false
        ))
        let regularCashbackWithMir = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: mirPromo,
            isWithFromPrefix: false,
            isExtra: false
        ))
        let regularCashbackWithMirFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: mirPromo,
            isWithFromPrefix: true,
            isExtra: false
        ))
        let regularCashbackForNonAuthenticated = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: nil,
            isWithFromPrefix: true,
            isExtra: false,
            isPlusForAll: true
        ))
        let regularCashbackWithYandexCard = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: yandexCardPromo,
            isWithFromPrefix: false,
            isExtra: false
        ))
        let regularCashbackWithYandexCardFrom = try XCTUnwrap(SKUCashbackTextFactory.makeSkuCashback(
            100,
            additionalPromo: yandexCardPromo,
            isWithFromPrefix: true,
            isExtra: false
        ))

        // then
        XCTAssertEqual(" 100 баллов на Плюс", regularCashback.accessibilityLabel)
        XCTAssertEqual("от  100 баллов на Плюс", cashbackFrom.accessibilityLabel)
        XCTAssertEqual(" 100 баллов — повышенный кешбэк", cashbackExtra.accessibilityLabel)
        XCTAssertEqual("от  100 баллов — повышенный кешбэк", cashbackExtraFrom.accessibilityLabel)
        XCTAssertEqual(" 100 и ещё  123 с Mastercard", regularCashbackWithMasterCard.accessibilityLabel)
        XCTAssertEqual("от  100 и ещё  123 с Mastercard", regularCashbackWithMasterCardFrom.accessibilityLabel)
        XCTAssertEqual(" 100 и ещё  123 с картой «Мир»", regularCashbackWithMir.accessibilityLabel)
        XCTAssertEqual("от  100 и ещё  123 с картой «Мир»", regularCashbackWithMirFrom.accessibilityLabel)
        XCTAssertEqual("от  100 баллов, если подключить Плюс", regularCashbackForNonAuthenticated.accessibilityLabel)
        XCTAssertEqual(" 100 и ещё  999 со Счётом в Яндексе", regularCashbackWithYandexCard.accessibilityLabel)
        XCTAssertEqual("от  100 и ещё  999 со Счётом в Яндексе", regularCashbackWithYandexCardFrom.accessibilityLabel)
    }
}
