import XCTest
import AutoRuProtoModels

/// @depends_on AutoRuSafeDeal
final class SafeDealConfirmationPopupTests: BaseTest {
    private static let dealID = "4eebee84-0919-4ace-8869-d899c455d885"
    private static let offerID = "1098252972-99d8c274"

    private static let deal: Vertis_SafeDeal_DealView = {
        Vertis_SafeDeal_DealView.with {
            $0.id = SafeDealConfirmationPopupTests.dealID
            $0.sellingPriceRub = .init(1_200_000)
            $0.step = .dealCreated
            $0.sellerStep = .sellerAcceptingDeal
            $0.subject.autoru.offer.id = SafeDealConfirmationPopupTests.offerID
            $0.buyerID = "buyer:123"
        }
    }()

    private static let offer: Auto_Api_Offer = {
        var offer = Auto_Api_OfferResponse(mockFile: "offer_CARS_1098230510-dd311329_ok").offer
        offer.id = SafeDealConfirmationPopupTests.offerID
        return offer
    }()

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_singleRequestConfirmation_approve() {
        mockForSingleRequest()

        let approveExpectation = api.safeDeal.deal.update
            .dealId(Self.dealID)
            .post(parameters: .wildcard)
            .expect { req, _ in
                .okIf(req.bySeller.newDealApprove.approve)
            }

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup.tap(.approve)
            }
            .should(provider: .safeDealSellerConfirmationPopup, .be(.hidden))
            .wait(for: [approveExpectation])
    }

    func test_singleRequestConfirmation_reject() {
        mockForSingleRequest()

        let rejectExpectation = api.safeDeal.deal.update
            .dealId(Self.dealID)
            .post(parameters: .wildcard)
            .expect { req, _ in
                .okIf(req.bySeller.cancelDealWithReason.cancelReason == .sellerAnotherReason)
            }

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup.tap(.reject)
            }
            .should(provider: .safeDealRequestCancelPopup, .exist)
            .focus { popup in
                popup.tap(.reason("Другое"))
            }
            .should(provider: .textReasonPopup, .exist)
            .focus { popup in
                popup
                    .tap(.textView)
                    .type("Другая причина")
                    .tap(.confirm)
            }
            .should(provider: .safeDealRequestCancelPopup, .be(.hidden))
            .wait(for: [rejectExpectation])
            .should(provider: .safeDealSellerConfirmationPopup, .be(.hidden))
    }

    func test_singleRequestConfirmation_secondaryActions() {
        mockForSingleRequest()

        let chatCreationExpectation = api.chat.room
            .post
            .expect { request, _ in
                .okIf(
                    request.subject.offer.id == Self.offerID &&
                    request.subject.offer.category == "cars" &&
                    request.users == ["buyer:123"]
                )
            }

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("Покупатель предлагает 1 200 000 ₽"))
                    .should(.contact, .exist)
                    .should(.reject, .exist)
                    .should(.approve, .exist)
                    .should(.agreement, .exist)

                popup
                    .step("Проверяем открытие соглашения") { popup in
                        popup
                            .tap(.agreement)
                            .should(provider: .webViewPicker, .exist)
                            .focus { $0.closeBySwipe() }
                    }
                    .step("Проверяем открытие промо") { popup in
                        popup
                            .tap(.description)
                            .should(provider: .webViewPicker, .exist)
                            .focus { $0.closeBySwipe() }
                    }
                    .step("Открываем чат") { popup in
                        popup
                            .tap(.contact)
                            .should(provider: .chatScreen, .exist)
                            .wait(for: [chatCreationExpectation])
                    }
            }
    }

    func test_cancelRequestConfirmation_contact() {
        mockForOverExistingRequest()

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("Другой покупатель предлагает 1 200 000 ₽"))
                    .tap(.contact)
            }
            .should(provider: .chatScreen, .exist)
    }

    func test_cancelRequestConfirmation_dealsList() {
        mockForOverExistingRequest()

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("Другой покупатель предлагает 1 200 000 ₽"))
                    .tap(.list)
            }
            .should(provider: .safeDealListScreen, .exist)
    }

    func test_multipleRequestsSingleOffer() {
        mockForMultipleRequestsSingleOffer()

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("2 новых запроса на Безопасную сделку"))
                    .step("Проверяем открытие промо") { popup in
                        popup
                            .tap(.description)
                            .should(provider: .webViewPicker, .exist)
                            .focus { $0.closeBySwipe() }
                    }
                    .tap(.list)
            }
            .should(provider: .safeDealListScreen, .exist)
    }

    func test_multipleRequestsMultipleOffers() {
        mockForMultipleRequestsMultitpleOffers()

        launchAndOpenSaleCard()
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("2 новых запроса на Безопасную сделку"))
                    .step("Проверяем открытие промо") { popup in
                        popup
                            .tap(.description)
                            .should(provider: .webViewPicker, .exist)
                            .focus { $0.closeBySwipe() }
                    }
                    .tap(.list)
            }
            .should(provider: .safeDealListScreen, .exist)
    }

    func test_popupVisibleOnlyOncePerDeal() {
        let dealResponse: Auto_Api_SafeDeal_RichDealListResponse = .with { model in
            model.deals = [Self.deal]
            model.offers = [Self.offer]
        }

        var newDeal = Self.deal
        newDeal.id = "\(newDeal.id)_"
        newDeal.sellingPriceRub = .init(2_200_000)

        api.safeDeal.deal.list
            .get(parameters: .wildcard)
            .ok(mock: .model(dealResponse))

        api.search.cars.post(parameters: .wildcard)
            .ok(mock: .file("history_last_all_credit_ok"))
        launchAndOpenSaleCard()
            .log("Проверяем попап БС для первой сделки (единственная)")
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("Покупатель предлагает 1 200 000 ₽"))
                    .tap(.close)
            }
            .should(provider: .safeDealSellerConfirmationPopup, .be(.hidden))
            .should(provider: .navBar, .exist)
            .focus { navBar in
                navBar.tap(.back)
            }
            .log("Проверяем попап БС для второй сделки (теперь их две)")
            .do {
                api.safeDeal.deal.list
                    .get(parameters: .wildcard)
                    .ok(
                        mock: .model(dealResponse) { model in
                            model.deals.append(newDeal)
                        }
                    )
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.offerCell(.custom(Self.offerID)))
            }
            .should(provider: .safeDealSellerConfirmationPopup, .exist)
            .focus { popup in
                popup
                    .should(.title, .match("Покупатель предлагает 2 200 000 ₽"))
            }
    }

    // MARK: - Private

    private func mockForSingleRequest() {
        Step("Мокаем для одной сделки по одному офферу") {
            api.safeDeal.deal.list
                .get(parameters: .wildcard)
                .ok(
                    mock: .model(.init()) { model in
                        model.deals = [Self.deal]
                        model.offers = [Self.offer]
                    }
                )
        }
    }

    private func mockForOverExistingRequest() {
        Step("Мокаем для одной сделки поверх другой сделки") {
            var oldDeal = Self.deal
            oldDeal.sellerStep = .sellerApprovingDeal

            var newDeal = Self.deal
            newDeal.id = "\(newDeal.id)_"

            api.safeDeal.deal.list
                .get(parameters: .wildcard)
                .ok(
                    mock: .model(.init()) { model in
                        model.deals = [oldDeal, newDeal]
                        model.offers = [Self.offer]
                    }
                )
        }
    }

    private func mockForMultipleRequestsSingleOffer() {
        Step("Мокаем для нескольких сделок") {
            let oldDeal = Self.deal

            var newDeal = Self.deal
            newDeal.id = "\(newDeal.id)_"

            api.safeDeal.deal.list
                .get(parameters: .wildcard)
                .ok(
                    mock: .model(.init()) { model in
                        model.deals = [oldDeal, newDeal]
                        model.offers = [Self.offer]
                    }
                )
        }
    }

    private func mockForMultipleRequestsMultitpleOffers() {
        Step("Мокаем для нескольких сделок и нескольких офферов") {
            let oneDeal = Self.deal
            let oneOffer = Self.offer

            var twoOffer = Self.offer
            twoOffer.id = "\(Self.offerID)_"

            var twoDeal = Self.deal
            twoDeal.id = "\(twoDeal.id)_"
            twoDeal.subject.autoru.offer.id = twoOffer.id

            api.safeDeal.deal.list
                .get(parameters: .wildcard)
                .ok(
                    mock: .model(.init()) { model in
                        model.deals = [oneDeal, twoDeal]
                        model.offers = [oneOffer, twoOffer]
                    }
                )
        }
    }

    private func launchAndOpenSaleCard() -> SaleCardScreen_ {
        launch(on: .transportScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(Self.offerID)"), userDefaults: [:])) { screen in
            screen
                .should(provider: .saleCardScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_offerCars(id: Self.offerID)
            .mock_safeDealCreate(offerID: Self.offerID)
            .mock_safeDealCancel(dealID: Self.dealID, offerID: Self.offerID)

        mocker.startMock()
    }
}
