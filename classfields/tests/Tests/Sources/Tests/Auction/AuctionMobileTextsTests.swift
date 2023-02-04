import XCTest
import AutoRuProtoModels
@testable import AutoRuAuctionClaim

final class AuctionMobileTextsTests: BaseUnitTest {
    private static let texts: Auto_C2b_Common_MobileTexts = {
        Auto_C2b_Common_MobileTexts.with { model in
            model.forms.buyback = .with {
                $0.title = "Продайте машину за"
                $0.description_p = "Описание"
                $0.items = [
                    .with { item in
                        item.title = "Заголовок"
                        item.subtitle = "Описание айтема"
                    }
                ]
            }

            model.forms.checkupClaim = .with {
                $0.place = "20 км от МКАД"
                $0.time = "10:00 - 10:00"
                $0.description_p = "Описание 1"
            }

            model.forms.successClaim = .with {
                $0.title = "Заявка отправлена"
                $0.description_p = "Описание 2"
                $0.items = [
                    .with { item in
                        item.title = "Заголовок 1"
                    }
                ]
            }
        }
    }()

    private static let info: Auto_Api_C2BApplicationInfoResponse = {
        .with { model in
            model.canApply = true
            model.priceRange = .with {
                $0.from = 1_000_000
                $0.to = 1_500_000
            }
        }
    }()

    func test_buybackScreenModel() {
        let model = AuctionBuybackModel(from: Self.info, texts: Self.texts)

        XCTAssertEqual(
            model.priceRange,
            "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}1\u{00a0}500\u{00a0}000\u{00a0}₽"
        )

        XCTAssertEqual(model.title, "Продайте машину за")
        XCTAssertEqual(model.description, "Описание")

        XCTAssertEqual(model.items.count, 1)
        XCTAssertEqual(model.items[0].title, "Заголовок")
        XCTAssertEqual(model.items[0].subtitle, "Описание айтема")
    }

    func test_claimScreenModel() {
        let model = AuctionClaimModel(from: Self.info, phone: "8-800-555-35-35", texts: Self.texts)

        XCTAssertEqual(model.description, "Описание 1\n\n10:00 - 10:00")
        XCTAssertEqual(model.place, "20 км от МКАД")
        XCTAssertEqual(model.phone, "8-800-555-35-35")
    }

    func test_successScreenModel() {
        let model = ClaimSuccessModel(from: Self.texts)

        XCTAssertEqual(model.title, "Заявка отправлена")
        XCTAssertEqual(model.description, "Описание 2")

        XCTAssertEqual(model.items.count, 1)
        XCTAssertEqual(model.items[0], "Заголовок 1")
    }
}
