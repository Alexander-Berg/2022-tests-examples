import XCTest
import AutoRuProtoModels

final class AuctionWizardTests: BaseTest, WizardTraversable {
    private static let draftID = "1640897598478615376-2c0470c2"
    private static let applicationID: UInt64 = 100

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_auctionCanApplyButSkip() {
        traverseWizardToAuction()
            .log("После проверенного собственника показываем аукцион")
            .should(provider: .auctionWelcomeScreen, .exist)
            .focus { $0.tap(.skipButton) }
            .should(provider: .wizardPricePicker, .exist)
    }

    func test_auctionCanNotApply() {
        api.user.draft.category(.cars).offerId(Self.draftID).c2bApplicationInfo
            .get
            .ok(
                mock: .model(.init()) { model in
                    model.canApply = false
                }
            )

        traverseWizardToAuction()
            .log("После проверенного собственника не показываем аукцион, сразу цену")
            .should(provider: .wizardPricePicker, .exist)
    }

    func test_auctionOpenWebPromo() {
        traverseWizardToAuction()
            .should(provider: .auctionWelcomeScreen, .exist)
            .log("Проверяем, что по тапу на ссылку 'как это работает?' открываем вебвью")
            .focus { $0.tap(.howItWorksLink) }
            .should(provider: .webViewPicker, .exist)
            .focus { picker in
                picker.step("Проверяем открытую ссылку") { picker in
                    XCTAssert(
                        picker.currentURL.starts(with: "https://m.auto.ru/c2b-auction/applications/\(Self.draftID)"),
                        "Открыли неверную ссылку"
                    )
                }
            }
    }

    func test_auctionCanApplyButDidMobileTextsFail() {
        api.c2bAuction.application.mobileTexts.get.error(status: ._500, mock: .model(Auto_Api_ErrorResponse()))

        traverseWizardToAuction()
            .should(provider: .wizardPricePicker, .exist)
    }

    func test_auctionClaimAgreement() {
        traverseWizardToAuction()
            .should(provider: .auctionWelcomeScreen, .exist)
            .focus { $0.tap(.nextButton) }
            .step("Снимаем чекбокс, тапаем на кнопку и проверяем, что дальше не ушли никуда") { $0
                .should(provider: .auctionClaimScreen, .exist)
                .focus { screen in
                    screen
                        .log("Снимаем чекбокс, тапаем на кнопку и проверяем, что дальше не ушли никуда")
                        .tap(.agreementCheckmark)
                        .tap(.claimButton)
                }
                .should(provider: .auctionClaimScreen, .exist)
            }
            .step("Тапаем в текст и открываем вебвью") { $0
                .should(provider: .auctionClaimScreen, .exist)
                .focus { screen in
                    screen.tap(.agreementText)
                }
                .should(provider: .webViewPicker, .exist)
                .focus { picker in
                    picker.step("Проверяем открытую ссылку") { picker in
                        XCTAssert(
                            picker.currentURL.starts(with: "https://yandex.ru/legal/autoru_bot_tender"),
                            "Открыли неверную ссылку"
                        )
                    }
                }
            }
    }

    func test_auctionApply() {
        let claimRequestExpectation = api.c2bAuction.application.category(.cars).draftId(Self.draftID)
            .post(parameters: .wildcard)
            .expect { request, _ in
                request.buyOutAlg != .withPreOffers ? .ok : .fail(reason: "Неверный параметр")
            }

        let applicationsRequestExpectation = api.c2bAuction.application.list
            .get(parameters: .wildcard)
            .expect()

        traverseWizardToAuction()
            .do {
                mocker.mock_userOffers(counters: [:])

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

    func test_auctionApply_withPreOffers() {
        let claimRequestExpectation = api.c2bAuction.application.category(.cars).draftId(Self.draftID)
            .post(parameters: .wildcard)
            .expect { request, _ in
                request.buyOutAlg == .withPreOffers ? .ok : .fail(reason: "Неверный параметр")
            }

        let requestCreatedApplication = api.c2bAuction.application.applicationId(Int(Self.applicationID))
            .get
            .expect()

        let applicationsRequestExpectation = api.c2bAuction.application.list
            .get(parameters: .wildcard)
            .expect()

        traverseWizardToAuction(usePreOffersFlow: true)
            .do {
                mocker.mock_userOffers(counters: [:])

                api.c2bAuction.application.list
                    .get(parameters: .wildcard)
                    .ok(
                        mock: .model(.init()) { model in
                            model.applications = [
                                .with { application in
                                    application.id = Self.applicationID
                                    application.statusFinishAt = .init(date: Date(timeIntervalSinceReferenceDate: 100))
                                    application.buyOutAlg = .withPreOffers
                                    application.status = .new
                                }
                            ]
                        }
                    )
            }
            .should(provider: .auctionWelcomeScreen, .exist)
            .shouldEventBeReported("Выкуп. Черновик", with: ["Источник": ["Визард": ["draft_id": Self.draftID]]])
            .focus { screen in
                screen
                    .step("Проверяем ссылку Как работает выкуп") { $0
                        .tap(.howItWorksLink)
                        .should(provider: .webViewPicker, .exist)
                        .wait(for: 1)
                        .focus { $0.closeBySwipe() }
                    }
                    .step("Проверяем ссылку на правила выкупа") { $0
                        .tap(.agreementText)
                        .should(provider: .webViewPicker, .exist)
                        .wait(for: 1)
                        .focus { $0.closeBySwipe() }
                    }
                    .tap(.nextButton)
            }
            .should(provider: .auctionBuybackPreviewScreen, .exist)
            .wait(for: [claimRequestExpectation, requestCreatedApplication])
            .focus { screen in
                screen.tap(.nextButton)
            }
            .should(provider: .userSaleListScreen, .exist)
            .wait(for: [applicationsRequestExpectation])
            .focus { screen in
                screen.should(.auctionSnippetInfo(id: "\(Self.applicationID)"), .exist)
            }
    }


    private func traverseWizardToAuction(usePreOffersFlow: Bool = false) -> WizardScreen_ {
        launch(
            on: .mainScreen,
            options: .init(
                overrideAppSettings: [
                    "c2bWizardAuctionPosition": "before_price",
                    "c2bAuctionWizardNewFlow": usePreOffersFlow
                ]
            )
        ) { screen in
            screen
                .step("Переключаемся на таб с ЛК") { screen.toggle(to: .offersAttentions) }
                .step("Открываем пикер категорий") { $0
                    .should(provider: .userSaleListScreen, .exist)
                    .focus { screen in
                        screen.tap(.placeFreeLabel)
                    }
                }
                .step("Открываем визард для авто") { $0
                    .should(provider: .categoryPicker, .exist)
                    .focus { screen in
                        screen.tap(.auto)
                    }
                }
                .should(provider: .wizardScreen, .exist)
                .focus { wizard in
                    wizard
                        .step("Проходим визард до проверенного собственника") {
                            traverseWizard(for: Self.draftID, from: wizard, to: .provenOwner)
                        }
                        .tap(.skipButton)
                }
                .should(provider: .wizardScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .startMock()

        setupMocksForWizard(draftID: Self.draftID)

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
            .ok(
                mock: .model(.init()) { model in
                    model.applicationID = Int64(Self.applicationID)
                }
            )

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
