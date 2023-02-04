import XCTest

final class SaleCardBottomViewTests: BaseTest {
    private static let offerID = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_onlyChat() {
        api.offer.category(.cars).offerID(Self.offerID)
            .get
            .ok(
                mock: .model(.init()) { response in
                    response.offer.status = .active
                    response.offer.seller.chatsEnabled = true
                    response.offer.additionalInfo.chatOnly = true
                }
            )

        openSaleCard()
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) { $0
                .validateSnapshot()
                .should(.callButton, .be(.hidden))
                .focus(on: .chatButton, ofType: .stylizedButton) { $0
                    .should(.title, .match("Написать в чат"))
                }
                .should(.chatHelpButton, .exist)
                .tap(.chatButton)
            }
            .should(provider: .loginScreen, .exist)
    }

    func test_callAndChat() {
        api.offer.category(.cars).offerID(Self.offerID)
            .get
            .ok(
                mock: .model(.init()) { response in
                    response.offer.status = .active
                    response.offer.seller.chatsEnabled = true
                    response.offer.additionalInfo.chatOnly = false
                }
            )

        openSaleCard()
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) { $0
                .validateSnapshot()
                .focus(on: .callButton, ofType: .stylizedButton) { $0
                    .should(.title, .match("Позвонить"))
                }
                .focus(on: .chatButton, ofType: .stylizedButton) { $0
                    .should(.title, .be(.hidden))
                }
                .tap(.chatButton)
            }
            .should(provider: .loginScreen, .exist)
    }

    func test_onlyCall() {
        api.offer.category(.cars).offerID(Self.offerID)
            .get
            .ok(
                mock: .model(.init()) { response in
                    response.offer.status = .active
                    response.offer.seller.chatsEnabled = false
                    response.offer.additionalInfo.chatOnly = false
                    response.offer.seller.phones = [
                        .with { phone in
                            phone.callHourStart = 0
                            phone.callHourEnd = 0
                        }
                    ]
                }
            )

        openSaleCard()
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) { $0
                .validateSnapshot()
                .should(.chatButton, .be(.hidden))
                .focus(on: .callButton, ofType: .stylizedButton) { $0
                    .should(.title, .match("Позвонить"))
                    .should(.subtitle, .match("Круглосуточно"))
                }
            }
    }

    private func openSaleCard() -> SaleCardScreen_ {
        launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(Self.offerID)"))
        )
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_offerFromHistoryLastAll()
            .startMock()
    }
}
