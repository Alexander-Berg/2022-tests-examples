import XCTest
import AutoRuProtoModels

final class AuctionUserSaleListTests: BaseTest {
    private static let draftID = "1640897598478615376-2c0470c2"
    private static let applicationID: UInt64 = 100

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_approveDealerPreOffer() {
        // Мок нового флоу
        api.c2bAuction.application.list
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { model in
                    model.applications = [
                        .with { application in
                            application.id = Self.applicationID
                            application.statusFinishAt = .init(date: Date(timeIntervalSinceReferenceDate: 100))
                            application.buyOutAlg = .withPreOffers
                            application.status = .confirmPreOffers

                            application.propositions = [
                                .with {
                                    $0.dealerName = "Dealer 1"
                                    $0.value = 1_000_000
                                },
                                .with {
                                    $0.dealerName = "Dealer 2"
                                    $0.value = 2_000_000
                                }
                            ]
                        }
                    ]
                }
            )

        let submitExpectation = api.c2bAuction.application.applicationId(Int(Self.applicationID)).acceptPreOffers
            .post
            .expect()

        let auctionsReloadExpectation = api.c2bAuction.application.list.get(parameters: .wildcard).expect()

        openUserSaleList()
            .focus(on: .auctionSnippetActions(id: "\(Self.applicationID)"), ofType: .auctionSnippetActionsCell) { cell in
                cell.tap(.showPreOffers)
            }
            .should(provider: .auctionPreOffersScreen, .exist)
            .focus { screen in
                screen
                    .should(.preOffer(dealerName: "Dealer 1"), .exist)
                    .should(.preOffer(dealerName: "Dealer 2"), .exist)
                    .focus(on: .nextButton, ofType: .stylizedButton) { button in
                        button.should(.subtitle, .match("Примите решение до 1 января, 03:01"))
                    }
                    .tap(.nextButton)
            }
            .should(provider: .auctionInspectionConfirmationScreen, .exist)
            .focus { screen in
                screen
                    .step("Проверяем показ промки \"Как работает выкуп?\"") {
                        screen
                            .tap(.howItWorksLink)
                            .should(provider: .webViewPicker, .exist)
                            .focus { picker in
                                picker.wait(for: 1).closeBySwipe()
                            }
                    }
                    .tap(.nextButton)
            }
            .wait(for: [submitExpectation])
            .should(provider: .auctionWaitManagerCallScreen, .exist)
            .focus { screen in
                screen
                    .step("Проверяем показ промки про подготовку к осмотру") {
                        screen
                            .tap(.howToPrepareLink)
                            .should(provider: .webViewPicker, .exist)
                            .focus { picker in
                                picker.wait(for: 1).closeBySwipe()
                            }
                    }
                    .tap(.closeButton)
            }
            .should(provider: .auctionWaitManagerCallScreen, .be(.hidden))
            .wait(for: [auctionsReloadExpectation])
    }

    func test_cancelApplication() {
        api.c2bAuction.application.applicationId(Int(Self.applicationID)).close
            .post(parameters: [.dropOfferDraft(true)])
            .ok(mock: .file("success"))

        openUserSaleList()
            .focus(on: .auctionSnippetActions(id: "\(Self.applicationID)"), ofType: .auctionSnippetActionsCell) { $0
                .tap(.cancelButton)
            }
            .step("Проверяем, что по тапу на кнопку появился алерт и его можно закрыть") { $0
                .should(provider: .systemAlert, .exist)
                .focus { alert in
                    alert.tap(.button("Остаться"))
                }
            }
            .focus(on: .auctionSnippetActions(id: "\(Self.applicationID)"), ofType: .auctionSnippetActionsCell) { $0
                .tap(.cancelButton)
            }
            .step("Отменяем заявку через алерт") { alert -> UserSaleListScreen in
                let cancelExpectation = expectationForRequest(
                    method: "POST",
                    uri: "/c2b-auction/application/\(Int(Self.applicationID))/close?drop_offer_draft=true"
                )

                api.c2bAuction.application.list
                    .get(parameters: .wildcard)
                    .ok(mock: .model(.init()) { response in
                        response.applications = []
                    })

                let reloadExpectation = api.c2bAuction.application.list
                    .get(parameters: .wildcard)
                    .expect()

                return alert
                    .should(provider: .systemAlert, .exist)
                    .focus { alert in
                        alert.tap(.button("Отменить"))
                    }
                    .wait(for: [cancelExpectation, reloadExpectation])
                    .should(provider: .userSaleListScreen, .exist).base
            }
            .should(.auctionSnippetInfo(id: "\(Self.applicationID)"), .be(.hidden))
    }

    private func openUserSaleList() -> UserSaleListScreen {
        launchMain { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_userOffers(counters: [:])
            .startMock()

        api.c2bAuction.application.applicationId(Int(Self.applicationID)).close
            .post(parameters: .parameters([]))
            .ok(mock: .model(.init()))

        api.c2bAuction.application.applicationId(Int(Self.applicationID)).acceptPreOffers
            .post
            .ok(mock: .model(.init()))

        api.c2bAuction.application.list
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { response in
                    response.applications = [
                        .with { model in
                            model.id = Self.applicationID
                            model.pricePrediction = .with {
                                $0.from = 1_000_000
                                $0.to = 1_200_000
                            }
                            model.offer = Auto_Api_OfferResponse(mockFile: "offer_CARS_1098252972-99d8c274_ok").offer
                            model.status = .waitingInspection

                            model.humanReadableStatuses = [
                                .with {
                                    $0.title = "Статус 1"
                                    $0.isCurrent = true
                                }
                            ]
                        }
                    ]
                }
            )
    }
}
