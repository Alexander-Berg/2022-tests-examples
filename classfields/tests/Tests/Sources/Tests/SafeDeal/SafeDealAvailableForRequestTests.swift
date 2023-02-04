import XCTest
import AutoRuProtoModels
@testable import AutoRuProtobuf

final class SafeDealAvailableForRequestTests: BaseUnitTest {
    func test_checkAvailableForSafeDeal_noDeals() {
        Step("Проверяем, что БС доступна, когда есть тег и не было сделок")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = false
            $0.tags = ["allowed_for_safe_deal"]
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = false
                info.deals = []
            }
        }

        XCTAssertTrue(offer.isAvailableForSafeDealRequest)
    }

    func test_checkAvailableForSafeDeal_noCompletedDeals() {
        Step("Проверяем, что БС доступна, когда есть тег и не было полных сделок (но есть отмененные)")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = false
            $0.tags = ["allowed_for_safe_deal"]
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = false
                info.deals = [
                    .with { deal in
                        deal.step = .dealDeclined
                    }
                ]
            }
        }

        XCTAssertTrue(offer.isAvailableForSafeDealRequest)
    }

    func test_checkNoAvailableForSafeDeal_noTag() {
        Step("Проверяем, что БС недоступна, когда нет тега")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = false
            $0.tags = []
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = false
                info.deals = [
                    .with { deal in
                        deal.step = .dealDeclined
                    }
                ]
            }
        }

        XCTAssertFalse(offer.isAvailableForSafeDealRequest)
    }

    func test_checkNoAvailableForSafeDeal_completedDeals() {
        Step("Проверяем, что БС недоступна, когда есть завершенные БС")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = false
            $0.tags = ["allowed_for_safe_deal"]
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = true
                info.deals = [
                    .with { deal in
                        deal.step = .dealCompleted
                    }
                ]
            }
        }

        XCTAssertFalse(offer.isAvailableForSafeDealRequest)
    }

    func test_checkNoAvailableForSafeDeal_activeDeals() {
        Step("Проверяем, что БС недоступна, когда есть принятые БС")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = false
            $0.tags = ["allowed_for_safe_deal"]
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = false
                info.deals = [
                    .with { deal in
                        deal.step = .dealInviteAccepted
                    }
                ]
            }
        }

        XCTAssertFalse(offer.isAvailableForSafeDealRequest)
    }

    func test_checkNoAvailableForSafeDeal_owner() {
        Step("Проверяем, что БС недоступна, когда мы продавец")

        let offer: Auto_Api_Offer = .with {
            $0.additionalInfo.isOwner = true
            $0.tags = ["allowed_for_safe_deal"]
            $0.safeDealInfo = .with { info in
                info.hasCompletedDeals_p = false
                info.deals = [
                    .with { deal in
                        deal.step = .dealInviteAccepted
                    }
                ]
            }
        }

        XCTAssertFalse(offer.isAvailableForSafeDealRequest)
    }
}
