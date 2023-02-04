import AutoRuProtoModels
import SwiftProtobuf
import XCTest

/// @depends_on AutoRuSaleCard AutoRuOfferPrice
final class OfferPriceTests: BaseTest, KeyboardManaging {
    private static let searchURI = "POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
    private static let offerID = "1101101721-a355a648-sale-card-header"

    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    func test_price_withoutDiscount_withNDS() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .commercial
            offer.category = .trucks
            offer.priceInfo.withNds = Google_Protobuf_BoolValue(true)
        }

        self.openAsPriceHistory()
            .checkPrice(state: .normal(nds: true, discount: false, isDealer: true, greatDeal: nil))
            .checkButton(.addToFavorites)
    }

    func test_price_withDiscount_withNDS() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .commercial
            offer.category = .trucks

            offer = offer
                .addTags(.hasDiscount)
                .setDiscount(
                    tradein: 170_000,
                    insurance: 50_000,
                    credit: 100_000,
                    max: 320_000
                )

            offer.priceInfo.withNds = Google_Protobuf_BoolValue(true)
        }

        self.openAsPriceHistory()
            .checkPrice(state: .normal(nds: true, discount: true, isDealer: true, greatDeal: nil))
            .checkDiscounts()
            .checkButton(.addToFavorites)
    }

    func test_price_withoutDiscount_withNDS_private() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .trucks
            offer.priceInfo.withNds = Google_Protobuf_BoolValue(true)
        }

        self.openAsPriceHistory()
            .checkPrice(state: .normal(nds: true, discount: false, isDealer: false, greatDeal: nil))
            .checkPopup(id: "popup_only_price")
            .checkButton(.addToFavorites)
    }

    func test_priceFrom_withDiscounts_withNDS() {
        self.setupOffer { offer in

            offer.section = .new
            offer.sellerType = .commercial

            offer = offer
                .addTags(.hasDiscount)
                .setDiscount(
                    tradein: 170_000,
                    insurance: 50_000,
                    credit: 100_000,
                    max: 320_000
                )

            offer.priceInfo.withNds = Google_Protobuf_BoolValue(true)
        }

        self.openAsPriceHistory()
            .checkPrice(state: .priceFrom(nds: true, discount: true, greatDeal: nil))
            .checkDiscounts()
            .checkButton(.makeCall)
    }

    func test_priceFrom_withDiscounts_withNDS_trucks() {
        self.setupOffer { offer in
            offer.section = .new
            offer.sellerType = .commercial
            offer.category = .trucks

            offer = offer
                .addTags(.hasDiscount)
                .setDiscount(
                    tradein: 170_000,
                    insurance: 50_000,
                    credit: 100_000,
                    max: 320_000
                )

            offer.priceInfo.withNds = Google_Protobuf_BoolValue(true)
        }

        self.openAsPriceHistory()
            .checkPrice(state: .priceFrom(nds: true, discount: true, greatDeal: nil, category: .trucks))
            .checkDiscounts()
            .checkButton(.makeCall)
    }

    func test_priceHistory() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private

            offer = offer
                .addTag(.priceChange)
                .addPriceHistory(price: 7_000_000)
                .addPriceHistory(price: 1_000_000)
        }

        self.openAsPriceHistory()
            .checkPriceHistory(countOfRecords: 3)
            .checkPopup(id: "popup_with_price_history")
            .checkButton(.addToFavorites)
    }

    func test_no_priceHistory_for_new_cars() {
        self.setupOffer { offer in
            offer.section = .new
            offer.sellerType = .commercial

            offer = offer
                .addTag(.priceChange)
                .addPriceHistory(price: 7_000_000)
                .addPriceHistory(price: 1_000_000)
        }

        self.openAsPriceHistory()
            .checkPopup(id: "popup_with_no_price_history")
            .checkButton(.makeCall)
    }

    func test_greatDeal_good() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private

            offer = offer
                .addTag(.goodDeal)
                .addTag(.priceChange)
                .addPriceHistory(price: 7_000_000)
                .addPriceHistory(price: 1_000_000)
        }

        self.openAsGreatDeal()
            .checkPrice(state: .normal(nds: false, discount: false, isDealer: false, greatDeal: .good))
            .checkGreatDeal(.good, hasReport: false)
            .checkPopup(id: "popup_with_price_history_with_great_deal")
            .checkButton(.addToFavorites)
    }

    func test_greatDeal_excellent() {
        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer = offer.addTag(.greatDeal)
        }

        self.openAsGreatDeal()
            .checkPrice(state: .normal(nds: false, discount: false, isDealer: false, greatDeal: .excellent))
            .checkGreatDeal(.excellent, hasReport: false)
            .checkPopup(id: "popup_with_great_deal")
            .checkButton(.addToFavorites)
    }

    func test_greatDeal_good_withReport() {
        self.server.addHandler("GET /carfax/offer/cars/\(Self.offerID)/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "carfax_report_raw_GET_ok", userAuthorized: true)
        }
        mocker.mock_reportLayoutForOffer(bought: false)
        mocker.mock_reportLayoutForReport(bought: false)

        self.setupOffer { offer in
            offer.section = .new
            offer.sellerType = .commercial
            offer = offer
                .addTag(.greatDeal)
                .addTags(.hasDiscount)
                .setDiscount(
                    tradein: 170_000,
                    insurance: 50_000,
                    credit: 100_000,
                    max: 320_000
                )
        }

        self.openAsGreatDeal()
            .checkPrice(state: .priceFrom(nds: false, discount: false, greatDeal: .excellent))
            .checkGreatDeal(.excellent, hasReport: true)
            .checkPopup(id: "popup_with_discounts_with_great_deal")
            .checkButton(.makeCall)
    }

    func test_actionButton_addToFavorites() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            Response.okResponse(fileName: "great_deal_excellent_deal_response")
        }

        server.addHandler("GET /offer/CARS/1") { (_, _) -> Response? in
            Response.okResponse(fileName: "great_deal_excellent_deal_offer_response")
        }

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            request.method == "POST" && request.uri == "/user/favorites/cars/1"
        }

        self.mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .openCarOffer(with: "1")
            .tapOnGreatDealBadge()
            .tapOnActionButton(.addToFavorites)

        wait(for: [requestExpectation], timeout: 5)
    }

    func test_actionButton_removeFromFavorites() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            Response.okResponse(fileName: "great_deal_excellent_deal_response")
        }

        server.addHandler("GET /offer/CARS/1") { (_, _) -> Response? in
            Response.okResponse(fileName: "great_deal_excellent_deal_offer_response")
        }

        server.addHandler("POST /user/favorites/cars/1") { (request, _) -> Response? in
            Response.okResponse(fileName: "subscription_success")
        }

        let requestExpectationDeleteFavorite: XCTestExpectation = expectationForRequest { request -> Bool in
            request.method == "DELETE" && request.uri == "/user/favorites/cars/1"
        }

        let requestExpectationAddFavorite: XCTestExpectation = expectationForRequest { request -> Bool in
            request.method == "POST" && request.uri == "/user/favorites/cars/1"
        }

        let saleCardListSteps = self.mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .likeCarOffer(withId: "1")
            .checkFavoriteIconOnSnippetIsSelected(offerId: "1")

        wait(for: [requestExpectationAddFavorite], timeout: 5)

        saleCardListSteps
            .openCarOffer(with: "1")
            .tapOnGreatDealBadge()
            .tapOnActionButton(.removeFromFavorites)

        wait(for: [requestExpectationDeleteFavorite], timeout: 5)
    }

    func test_garageBanner_no_auth_hasPrice() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_postAuthLoginOrRegister()
            .mock_postUserConfirm()

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_card_listing_prices"))

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = true
        }

        let provider = openAsPriceHistory()
            .should(.garageBanner, .exist)
            .tap(.garageBanner)
            .should(provider: .loginScreen, .exist)

        mocker
            .mock_user()
            .mock_getSession()
            .setForceLoginMode(.forceLoggedIn)

        provider
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .offerPriceScreen, .exist)
            .should(.tradeInBlock, .exist)
    }

    func test_garageBanner_no_auth_hasNotPrice() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_postAuthLoginOrRegister()
            .mock_postUserConfirm()

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_10"))

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = true
        }

        let provider = openAsPriceHistory()
            .should(.garageBanner, .exist)
            .tap(.garageBanner)
            .should(provider: .loginScreen, .exist)

        mocker
            .mock_user()
            .mock_getSession()
            .setForceLoginMode(.forceLoggedIn)

        provider
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .offerPriceGarageCarPickerScreen, .exist)
            .focus { $0.tap(.pickerItem("Audi A4")) }
            .should(provider: .garageFormScreen, .exist)
    }

    func test_garageBanner_no_auth_no_garage_cars() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_postAuthLoginOrRegister()
            .mock_postUserConfirm()

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_empty"))

        api.garage.user.vehicleInfo.identifier("А123АА77")
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        api.garage.user.card.identifier.identifier("А123АА77")
            .post
            .ok(mock: .file("garage_add_car_result"))

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = true
        }

        let provider = openAsPriceHistory()
            .should(.garageBanner, .exist)
            .tap(.garageBanner)
            .should(provider: .loginScreen, .exist)

        mocker
            .mock_user()
            .mock_getSession()
            .setForceLoginMode(.forceLoggedIn)

        provider
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { $0.tap(.govNumberInputField) }
            .do {
                typeFromKeyboard("a123aa77")
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { $0
                .tap(.bottomButton(.search))
                .should(.bottomButton(.addToGarage), .exist)
                .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .offerPriceScreen, .exist)
            .should(.tradeInBlock, .exist)
    }

    func test_garageBanner_no_auth_no_exchange() {
        server.forceLoginMode = .forceLoggedOut

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = false
        }

        openAsPriceHistory()
            .should(.garageBanner, .be(.hidden))
            .should(.tradeInButton, .be(.hidden))
    }

    func test_garageBanner_exchange_without_tradeIn() {
        server.forceLoginMode = .forceLoggedIn

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_card_listing_prices"))

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .commercial
            offer.category = .cars
            offer.additionalInfo.exchange = true
            offer.additionalInfo.redemptionAvailable = false
        }

        openAsPriceHistory()
            .wait(for: 3)
            .should(provider: .offerPriceScreen, .exist)
            .focus { $0.validateSnapshot() }
            .should(.youGarageCar, .match("Ваш Hyundai Solaris"))
            .should(.tradeInButton, .be(.hidden))
    }

    func test_garageBanner_auth_no_garage_cars() {
        server.forceLoginMode = .forceLoggedIn

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = true
        }

        api.garage.user.cards
            .post
            .ok(mock: .model(.init(), mutation: { listingResponse in
                listingResponse.status = .success
                listingResponse.listing = []
            }))

        openAsPriceHistory()
            .should(.garageBanner, .exist)
            .focus { $0.validateSnapshot() }
    }

    func test_garageBanner_oneCarWithoutPrice() {
        server.forceLoginMode = .forceLoggedIn

        self.setupOffer { offer in
            offer.section = .used
            offer.sellerType = .private
            offer.category = .cars
            offer.additionalInfo.exchange = true
        }

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        openAsPriceHistory()
            .should(.garageBanner, .exist)
            .tap(.garageBanner)
            .should(provider: .garageFormScreen, .exist)
    }

    func test_garageTradeIn() {
        let phone2 = "79872222222"
        let formattedPhone1 = "+7 987 111-11-11"
        let formattedPhone2 = "+7 987 222-22-22"
        mocker.mock_user(userPhones: [formattedPhone1, formattedPhone2])

        let tradeInExpectation = api
            .offer
            .category(.cars)
            .offerID(Self.offerID)
            .tradeIn
            .post
            .expect { tradeInRequest, _ in
                guard tradeInRequest.userInfo.phoneNumber == formattedPhone2 else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_card_listing_prices"))

        api.offer.category(.cars)
            .offerID(Self.offerID)
            .tradeIn
            .post
            .ok(mock: .model(.init(), mutation: { response in
                response.status = .success
            }))

        self.setupOffer { offer in
            offer.salon.dealerID = "123"
            offer.section = .new
            offer.sellerType = .commercial
            offer.category = .cars
            offer.additionalInfo.exchange = true
            offer.additionalInfo.redemptionAvailable = true
        }

        openAsPriceHistory()
            .should(.tradeInButton, .exist)
            .should(provider: .offerPriceScreen, .exist)
            .focus { $0.validateSnapshot(snapshotId: #function + "_1") }
            .tap(.tradeInButton)
            .should(provider: .phonesListPopupScreen, .exist)
            .focus {
                $0
                    .should(.phoneRow(number: phone2), .exist)
                    .should(.addPhoneButton, .exist)
                    .validateSnapshot(snapshotId: #function + "_2")
                    .tap(.phoneRow(number: phone2))
            }
            .should(provider: .saleCardScreen, .exist)

        wait(for: [tradeInExpectation], timeout: 5)
    }

    func test_garage_selectCarForTradeIn() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_card_listing_prices"))

        self.setupOffer { offer in
            offer.salon.dealerID = "123"
            offer.section = .new
            offer.sellerType = .commercial
            offer.category = .cars
            offer.additionalInfo.exchange = true
            offer.additionalInfo.redemptionAvailable = true
        }

        openAsPriceHistory()
            .should(.tradeInBlock, .exist)
            .should(.youGarageCar, .match("Ваш Hyundai Solaris"))
            .should(.tradeInPayment, .match("Останется доплатить"))
            .tap(.youGarageCar)
            .should(provider: .offerPriceGarageCarPickerScreen, .exist)
            .focus { $0.validateSnapshot() }
            .focus { $0.tap(.pickerItem("Mercedes-Benz G-Класс")) }
            .should(.youGarageCar, .match("Ваш Mercedes-Benz G-Класс"))
            .should(.tradeInPayment, .match("У вас останется"))
            .swipe(.down)
            .should(provider: .saleCardScreen, .exist)
            .focus { card in
                card
                    .should(.priceHistoryButton, .exist)
                    .tap(.priceHistoryButton)
                    .tap(.priceHistoryButton)
            }
            .should(provider: .offerPriceScreen, .exist)
            .should(.youGarageCar, .match("Ваш Mercedes-Benz G-Класс"))
    }

    // MARK: - Private

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .startMock()

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_form_suggest_mark"))
    }

    private func setupOffer(mutation: @escaping (inout Auto_Api_Offer) -> Void) {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                mutation(&offer)
            }
        }
    }

    private func openAsGreatDeal() -> OfferPriceSteps {
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .openCarOffer(with: Self.offerID)
            .tapOnGreatDealBadge()
    }

    private func openAsPriceHistory() -> OfferPriceSteps {
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .openCarOffer(with: Self.offerID)
            .tapOnPriceHistory()
    }

    private static func listingOkResponse(mutation: (inout Auto_Api_Offer) -> Void) -> Response {
        var model: Auto_Api_OfferListingResponse = .init(mockFile: "SaleListHeaderTests_single-offer")
        model.offers[0].id = Self.offerID
        mutation(&model.offers[0])
        return Response.okResponse(message: model)
    }
}
