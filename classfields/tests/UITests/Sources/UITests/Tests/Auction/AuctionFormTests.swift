import XCTest
import AutoRuProtoModels

final class AuctionFormTests: BaseTest {
    private static let draftID = "1640897598478615376-2c0470c2"
    private static let applicationID: UInt64 = 100

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_auctionCanNotApply() {
        api.user.draft.category(.cars).offerId(Self.draftID).c2bApplicationInfo
            .get
            .ok(
                mock: .model(.init()) { model in
                    model.canApply = false
                }
            )

        openForm()
            .scroll(to: .activationButton, maxSwipes: 20)
            .tap(.activationButton)
            .should(provider: .vasTrapScreen, .exist)
    }

    func test_auctionSkip() {
        openForm()
            .scroll(to: .activationButton, maxSwipes: 20)
            .tap(.activationButton)
            .should(provider: .auctionWelcomeScreen, .exist)
            .focus { $0.tap(.skipButton) }
            .should(provider: .vasTrapScreen, .exist)
    }

    func test_auctionApply() {
        let claimRequestExpectation = api.c2bAuction.application.category(.cars).draftId(Self.draftID)
            .post(parameters: .wildcard)
            .expect(checker: nil)

        let applicationsRequestExpectation = api.c2bAuction.application.list
            .get(parameters: .wildcard)
            .expect()

        openForm()
            .do {
                api.c2bAuction.application.list
                    .get(parameters: .wildcard)
                    .ok(
                        mock: .model(.init()) { model in
                            model.applications = [
                                .with { application in
                                    application.id = Self.applicationID
                                }
                            ]
                        }
                    )
            }
            .scroll(to: .activationButton, maxSwipes: 20)
            .tap(.activationButton)
            .should(provider: .auctionWelcomeScreen, .exist)
            .focus { $0.tap(.nextButton) }
            .should(provider: .auctionClaimScreen, .exist)
            .focus { $0.tap(.claimButton) }
            .wait(for: [claimRequestExpectation])
            .should(provider: .auctionSuccessClaimScreen, .exist)
            .focus { $0.tap(.showClaimsButton) }
            .wait(for: [applicationsRequestExpectation])
            .should(provider: .userSaleListScreen, .exist)
            .focus { screen in
                screen.should(.auctionSnippetInfo(id: "\(Self.applicationID)"), .exist)
            }
    }

    private func openForm() -> OfferEditScreen_ {
        launch(
            on: .offerEditScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/add"),
                overrideAppSettings: ["c2bWizardAuctionPosition": "before_price"]
            )
        )
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_userOffers(counters: [:])
            .mock_userDraft(id: Self.draftID)
            .mock_userOffer(id: Self.draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferProductProlongable(offerId: Self.draftID, product: "all_sale_activate")
            .mock_referenceCatalogCarsAllOptions()
            .mock_wizardReferenceCatalogCars()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_putDraftCars(id: Self.draftID)
            .mock_postDraftCarsPublish(id: Self.draftID)
            .mock_activateUserOfferFrom(category: "cars", id: Self.draftID)
            .startMock()

        api.user.draft.category(.cars).offerId(Self.draftID).c2bApplicationInfo
            .get
            .ok(
                mock: .model(.init()) { model in
                    model.canApply = true
                    model.priceRange = .with {
                        $0.from = 1_000_000
                        $0.to = 2_000_000
                    }
                }
            )

        api.c2bAuction.application.category(.cars).draftId(Self.draftID)
            .post(parameters: .wildcard)
            .ok(mock: .model(.init()))

        api.c2bAuction.application.mobileTexts
            .get
            .ok(
                mock: .model(.init()) { texts in
                    texts.mobileTexts.forms.buyback = .with {
                        $0.title = "Продайте машину за"
                        $0.description_p = "Описание"
                    }

                    texts.mobileTexts.forms.checkupClaim = .with {
                        $0.place = "20 км от МКАД"
                        $0.time = "10:00 - 10:00"
                        $0.description_p = "Описание"
                    }

                    texts.mobileTexts.forms.successClaim = .with {
                        $0.title = "Заявка отправлена"
                    }
                }
            )
    }
}
