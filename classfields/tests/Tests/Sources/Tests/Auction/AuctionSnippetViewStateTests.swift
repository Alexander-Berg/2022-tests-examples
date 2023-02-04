import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuModels
import AutoRuSimpleItemList
@testable import AutoRuAuctionSnippet

final class AuctionSnippetViewStateTests: BaseUnitTest {
    func test_activeSnippet() throws {
        let items = [
            try Auto_C2b_Reception_AuctionApplication.with { model in
                model.id = 1
                model.pricePrediction = .with {
                    $0.from = 1_000_000
                    $0.to = 1_200_000
                }
                model.offer = try Self.mockOffer()

                model.humanReadableStatuses = [
                    .with {
                        $0.title = "Пункт 1"
                        $0.description_p = "Описание 1"
                        $0.isCurrent = false
                    },
                    .with {
                        $0.title = "Пункт 2"
                        $0.description_p = "Описание 2"
                        $0.isCurrent = true
                    },
                    .with {
                        $0.title = "Пункт 3"
                        $0.description_p = "Описание 3"
                        $0.isCurrent = false
                    }
                ]
            }
        ]

        let list = SimpleItemList.createIdle(items, canLoadMore: false, maxItemsCount: nil)
        let auctionSnippetState = AuctionSnippetState(applications: list, reloadCounter: 0)
        let state = AuctionSnippetViewState(auctionSnippetState)

        XCTAssertEqual(state.applications.count, 1)

        let application = state.applications[0]

        XCTAssertEqual(application.info.phone, "8\u{00a0}800\u{00a0}700-68-76")
        XCTAssertEqual(application.info.workHours, "ежедневно\nс 10:00 до 19:00")
        XCTAssertEqual(application.info.price, "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}1\u{00a0}200\u{00a0}000\u{00a0}₽")
        XCTAssertEqual(application.info.title, "BMW 3 серия 320i VI (F3x), 2012")

        guard case .active(let stages) = application.legacyStatus else {
            XCTFail("Неверный статус аукциона")
            return
        }

        XCTAssertEqual(stages.count, 3)

        XCTAssertEqual(stages[0].title, "Пункт 1")
        XCTAssertEqual(stages[0].description, "Описание 1")
        XCTAssertEqual(stages[0].index, 1)
        XCTAssertEqual(stages[0].active, true)

        XCTAssertEqual(stages[1].title, "Пункт 2")
        XCTAssertEqual(stages[1].description, "Описание 2")
        XCTAssertEqual(stages[1].index, 2)
        XCTAssertEqual(stages[1].active, true)

        XCTAssertEqual(stages[2].title, "Пункт 3")
        XCTAssertEqual(stages[2].description, "Описание 3")
        XCTAssertEqual(stages[2].index, 3)
        XCTAssertEqual(stages[2].active, false)
    }

    func test_finishedSnippet() throws {
        let items = [
            try Auto_C2b_Reception_AuctionApplication.with { model in
                model.id = 1
                model.offer = try Self.mockOffer()
                model.status = .finished
            }
        ]

        let list = SimpleItemList.createIdle(items, canLoadMore: false, maxItemsCount: nil)
        let auctionSnippetState = AuctionSnippetState(applications: list, reloadCounter: 0)
        let state = AuctionSnippetViewState(auctionSnippetState)

        XCTAssertEqual(state.applications.count, 1)

        let application = state.applications[0]

        XCTAssertEqual(application.info.title, "BMW 3 серия 320i VI (F3x), 2012")
        XCTAssertEqual(application.info.legacyStatus, .finished)
    }

    func test_rejectedSnippet() throws {
        let items = [
            try Auto_C2b_Reception_AuctionApplication.with { model in
                model.id = 1
                model.offer = try Self.mockOffer()
                model.status = .rejected
            }
        ]

        let list = SimpleItemList.createIdle(items, canLoadMore: false, maxItemsCount: nil)
        let auctionSnippetState = AuctionSnippetState(applications: list, reloadCounter: 0)
        let state = AuctionSnippetViewState(auctionSnippetState)

        XCTAssertEqual(state.applications.count, 1)

        let application = state.applications[0]

        XCTAssertEqual(application.info.title, "BMW 3 серия 320i VI (F3x), 2012")
        XCTAssertEqual(application.info.legacyStatus, .rejected)
    }

    private static func mockOffer() throws -> Auto_Api_Offer {
        let url = Bundle.current.url(forResource: "offer_CARS_1098252972-99d8c274_ok", withExtension: "json")!
        let response = try XCTUnwrap(try? Auto_Api_OfferResponse(jsonUTF8Data: Data(contentsOf: url)))
        return response.offer
    }

}
