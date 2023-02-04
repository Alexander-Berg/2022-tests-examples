import XCTest
import AutoRuProtoModels
import AutoRuUtils
@testable import AutoRuSafeDeal

final class SafeDealConfirmationStateTests: BaseUnitTest {

    func test_noNewSafeDealRequests() {
        Step("Проверяем генерацию стейта, когда нет новых безопасных сделок")

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [
                makeDeal(id: dealIds.id1, sellerStep: .sellerAwaitingAccept),
                makeDeal(id: dealIds.id2, sellerStep: .sellerAwaitingAccept)
            ]
            $0.offers = [offer]
        }

        let tuple = SafeDealConfirmationStateGenerator.getState(
            by: response,
            viewedWaitingDeals: [dealIds.id1, dealIds.id2]
        )

        XCTAssert(tuple.state == nil, "Стейт должен быть равен nil")
    }

    func test_singleSafeDealRequest() {
        Step("Проверяем генерацию стейта с одним новым запросом на БС, у пользователя есть имя")

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [makeDeal(id: dealIds.id1)]
            $0.offers = [offer]
        }

        let tuple = SafeDealConfirmationStateGenerator.getState(by: response, viewedWaitingDeals: [])

        guard let state = tuple.state else {
            XCTAssert(false, "Стейт не должен быть равен nil")
            return
        }

        guard case let .singleRequest(offerInfo, buyerName, _, _, _) = state else {
            XCTAssert(false, "Стейт не является '.singleRequest'")
            return
        }

        XCTAssertEqual(buyerName, "Тим Кук")
        XCTAssertEqual(offerInfo.title, "Honda Accord, 2007")
    }

    func test_singleSafeDealRequestNoUsername() {
        Step("Проверяем генерацию стейта с одним новым запросом на БС, у пользователя нет имени")

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [makeDeal(id: dealIds.id1, sellerName: "")]
            $0.offers = [offer]
        }

        let tuple = SafeDealConfirmationStateGenerator.getState(by: response, viewedWaitingDeals: [])

        guard let state = tuple.state else {
            XCTAssert(false, "Стейт не должен быть равен nil")
            return
        }

        guard case let .singleRequest(_, buyerName, _, _, _) = state else {
            XCTAssert(false, "Стейт не является '.singleRequest'")
            return
        }

        XCTAssert(buyerName.isEmpty)
    }

    func test_multipleSafeDealRequests() {
        Step("Проверяем генерацию стейта c несколкими запросами на БС по одному авто")

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [makeDeal(id: dealIds.id1), makeDeal(id: dealIds.id2)]
            $0.offers = [offer]
        }
        let tuple = SafeDealConfirmationStateGenerator.getState(by: response, viewedWaitingDeals: [])

        guard let state = tuple.state else {
            XCTAssert(false, "Стейт не должен быть равен nil")
            return
        }

        guard case let .multipleRequests(offerInfo, dealsRequestsCount) = state else {
            XCTAssert(false, "Стейт не является '.multipleRequests'")
            return
        }

        XCTAssertEqual(dealsRequestsCount, "2 новых запроса")
        XCTAssertEqual(offerInfo.title, "Honda Accord, 2007")
    }

    func test_multipleOffersAndDeals() {
        Step("Проверяем генерацию стейта c несколькими запросами на БС по нескольким авто")

        let offerId = "2112781260-80ca11ab"
        var offer2 = offer
        offer2 = .with { $0.id = offerId }

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [makeDeal(id: dealIds.id1), makeDeal(id: dealIds.id2, offerId: offerId)]
            $0.offers = [offer, offer2]
        }
        
        let tuple = SafeDealConfirmationStateGenerator.getState(by: response, viewedWaitingDeals: [], filterByAcceptingDeal: false)

        guard let state = tuple.state else {
            XCTAssert(false, "Стейт не должен быть равен nil")
            return
        }

        guard case let .multipleOffersAndDeals(dealsRequestsCount) = state else {
            XCTAssert(false, "Стейт не является '.multipleOffersAndDeals'")
            return
        }

        XCTAssertEqual(dealsRequestsCount, "2 новых запроса")
    }

    func test_overExistingSafeDealRequests() {
        Step("Проверяем генерацию стейта c одним или несколькими запросами на БС поверх существующей активной сделки")

        let response: Auto_Api_SafeDeal_RichDealListResponse = .with {
            $0.deals = [makeDeal(id: dealIds.id1, sellerStep: .sellerAwaitingAccept)]
            $0.offers = [offer]
        }

        let tuple = SafeDealConfirmationStateGenerator.getState(by: response, viewedWaitingDeals: [], filterByAcceptingDeal: false)

        guard let state = tuple.state else {
            XCTAssert(false, "Стейт не должен быть равен nil")
            return
        }

        guard case let .overExistingRequests(offerInfo, buyerName, _, _, _, dealsRequestsCount) = state else {
            XCTAssert(false, "Стейт не является '.overExistingRequests'")
            return
        }

        XCTAssertEqual(dealsRequestsCount, "1 новый запрос")
        XCTAssertEqual(offerInfo.title, "Honda Accord, 2007")
        XCTAssertEqual(buyerName, "Тим Кук")
    }

    private let dealIds: (id1: String, id2: String) = (
        "fd0e806f-cb31-464a-957c-9cd94c7c345e",
        "ca7b5518-6917-4fb4-bbe0-5b667fc22a26"
    )

    private let offer: Auto_Api_Offer = .with {
        $0.id = "1092781260-70ca00fc"
        $0.category = .cars
        $0.documents = .with { documents in
            documents.year = 2007
        }
        $0.carInfo = .with { carInfo in
            carInfo.modelInfo.name = "Accord"
            carInfo.markInfo.name = "Honda"
        }
    }

    private func makeDeal(
        id: String,
        offerId: String = "1092781260-70ca00fc",
        sellerName: String = "Тим Кук",
        sellerStep: Vertis_SafeDeal_SellerStep = .sellerAcceptingDeal
    ) -> Vertis_SafeDeal_DealView {
        let deal: Vertis_SafeDeal_DealView = .with {
            $0.id = id
            $0.state = .inProgress
            $0.step = .dealCreated
            $0.sellerStep = sellerStep
            $0.buyerStep = .buyerAwaitingAccept

            $0.subject = .with { subject in
                subject.autoru = .with { autoru in
                    autoru.offer = .with { offer in
                        offer.id = offerId
                        offer.category = .cars
                    }
                    autoru.vin = "JHMCL96807C212204"
                    autoru.mark = "Honda"
                    autoru.model = "Accord"
                }
            }
            $0.party = .with { party in
                party.seller = .with { seller in
                    seller.buyerInfo = .with {
                        $0.userName = sellerName
                    }
                }
            }
        }
        return deal
    }
}
