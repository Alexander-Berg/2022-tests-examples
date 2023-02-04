import XCTest
import AutoRuProtoModels
@testable import AutoRuModels
import Foundation

final class SaleCardSafeDealBuyerStatusTests: BaseUnitTest {
    func test_checkCancelled_fromDealView() {
        Step("Проверяем статус, который будет получен из отмененной DealView")

        var deal = Vertis_SafeDeal_DealView()

        deal.cancelledBy = .buyer

        deal.step = .dealCancelling
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), nil)

        deal.step = .dealCancelled
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), nil)

        deal.step = .dealDeclined
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), nil)

        deal.cancelledBy = .seller

        deal.step = .dealCancelling
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), .rejected)

        deal.step = .dealCancelled
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), .rejected)

        deal.step = .dealDeclined
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), .rejected)
    }

    func test_checkAwaitingAndAccepted_fromDealView() {
        Step("Проверяем статус, который будет получен из DealView")

        var deal = Vertis_SafeDeal_DealView()

        let steps: [Vertis_SafeDeal_DealStep: SafeDealBuyerRequestStatus?] = [
            .dealCreated: .awaiting,
            .dealConfirmed: nil,
            .dealCompleted: nil,
            .dealCompleting: nil,
            .dealInviteAccepted: .accepted
        ]

        for (step, res) in steps {
            deal.step = step
            XCTAssertEqual(SafeDealBuyerRequestStatus.from(dealView: deal), res)
        }
    }

    func test_checkNewestDealStatus_fromSafeDealInfo() {
        Step("Проверяем, что учитывается самая свежая сделка в подходящем статусе из SafeDealInfo")

        var info = Auto_Api_SafeDealInfo()
        info.deals = [
            .with {
                $0.created = .init(date: Date().addingTimeInterval(-10))
                $0.step = .dealCreated
            },
            .with {
                $0.created = .init(date: Date().addingTimeInterval(-5))
                $0.step = .dealInviteAccepted
            },
            .with {
                $0.created = .init(date: Date().addingTimeInterval(-100))
                $0.step = .dealCreated
            }
        ]

        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), .accepted)
    }

    func test_checkCancelled_fromSafeDealInfo() {
        Step("Проверяем статус, который будет получен из отмененной SafeDealInfo")

        var info = Auto_Api_SafeDealInfo()
        info.deals = [.init()]

        info.deals[0].cancelledBy = .buyer

        info.deals[0].step = .dealCancelling
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), nil)

        info.deals[0].step = .dealCancelled
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), nil)

        info.deals[0].step = .dealDeclined
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), nil)

        info.deals[0].cancelledBy = .seller

        info.deals[0].step = .dealCancelling
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), .rejected)

        info.deals[0].step = .dealCancelled
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), .rejected)

        info.deals[0].step = .dealDeclined
        XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), .rejected)
    }

    func test_checkAwaitingAndAccepted_fromSafeDealInfo() {
        Step("Проверяем статус, который будет получен из SafeDealInfo")

        var info = Auto_Api_SafeDealInfo()
        info.deals = [.init()]

        let steps: [Vertis_SafeDeal_DealStep: SafeDealBuyerRequestStatus?] = [
            .dealCreated: .awaiting,
            .dealConfirmed: nil,
            .dealCompleted: nil,
            .dealCompleting: nil,
            .dealInviteAccepted: .accepted
        ]

        for (step, res) in steps {
            info.deals[0].step = step
            XCTAssertEqual(SafeDealBuyerRequestStatus.from(safeDealInfo: info), res)
        }
    }
}
