import XCTest
import AutoRuProtoModels

final class SafeDealStatusBannerTests: BaseTest {
    private static let dealOneID = "01675384-a256-4f61-92b6-db9d6e4c9fbd"
    private static let dealTwoID = "be52cd13-dfa0-46b2-bfd9-709e8707d7a8"
    private static let offerOneID = "265829954274310832-e92c7e2b"
    private static let offerTwoID = "265829954274310832-ff22bbcc"

    private static let deal: Vertis_SafeDeal_DealView = {
        Vertis_SafeDeal_DealView.with {
            $0.id = SafeDealStatusBannerTests.dealOneID
            $0.step = .dealInviteAccepted
            $0.sellerStep = .sellerIntroducingPassportDetails
            $0.subject.autoru.offer.id = SafeDealStatusBannerTests.offerOneID
            $0.buyerID = "buyer:123"
            $0.participantType = .buyer
        }
    }()

    private static let deal2: Vertis_SafeDeal_DealView = {
        Vertis_SafeDeal_DealView.with {
            $0.id = SafeDealStatusBannerTests.dealTwoID
            $0.step = .dealInviteAccepted
            $0.buyerStep = .buyerIntroducingPassportDetails
            $0.subject.autoru.offer.id = SafeDealStatusBannerTests.offerTwoID
            $0.sellerID = "seller:123"
            $0.participantType = .seller
        }
    }()

    private static let offer: Auto_Api_Offer = {
        var offer = Auto_Api_OfferResponse(mockFile: "offer_CARS_1098230510-dd311329_ok").offer
        offer.id = SafeDealStatusBannerTests.offerOneID
        return offer
    }()

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_singleActiveOffer() {
        Step("Эксп включен. Одна активная БС")

        let dealResponse: Auto_Api_SafeDeal_RichDealListResponse = .with { model in
            model.deals = [Self.deal]
            model.offers = [Self.offer]
        }

        checkBannerExistThenShowSafeDealList(dealResponse)
    }

    func test_multipleActiveOffers() {
        Step("Эксп включен. Несколько активных БС")

        let dealResponse: Auto_Api_SafeDeal_RichDealListResponse = .with { model in
            model.deals = [Self.deal, Self.deal2]
            var offer = Self.offer
            offer.id = Self.offerTwoID
            model.offers = [Self.offer, offer]
        }

        checkBannerExistThenShowSafeDealList(dealResponse)
    }

    func test_bannerHidden() {
        Step("Эксп выключен. Одна активная БС")

        let dealResponse: Auto_Api_SafeDeal_RichDealListResponse = .with { model in
            model.deals = [Self.deal]
            model.offers = [Self.offer]
        }

        api.safeDeal.deal.list
            .get(parameters: .wildcard)
            .ok(mock: .model(dealResponse))

        launchMain()
            .toggle(to: \.transport)
            .should(.safeDealStatusCell, .be(.hidden))
    }

    func test_noActiveDeals() {
        Step("Эксп включен. Нет активных БС")

        var deal = Self.deal
        deal.step = .dealCreated
        deal.sellerStep = .sellerAwaitingAccept

        let dealResponse: Auto_Api_SafeDeal_RichDealListResponse = .with { model in
            model.deals = [deal]
            model.offers = [Self.offer]
        }

        api.safeDeal.deal.list
            .get(parameters: .wildcard)
            .ok(mock: .model(dealResponse))

        launchMain()
            .toggle(to: \.transport)
            .should(.safeDealStatusCell, .be(.hidden))
    }

    private func checkBannerExistThenShowSafeDealList(_ dealResponse: Auto_Api_SafeDeal_RichDealListResponse) {
        api.safeDeal.deal.list
            .get(parameters: .wildcard)
            .ok(mock: .model(dealResponse))

        launchMain(options: .init(overrideAppSettings: ["safeDealStatusBannerEnabled": true]))
            .toggle(to: \.transport)
            .focus(on: .safeDealStatusCell) { cell in
                cell.tap()
            }
            .should(provider: .safeDealListScreen, .exist)
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()

        mocker.startMock()
    }
}
