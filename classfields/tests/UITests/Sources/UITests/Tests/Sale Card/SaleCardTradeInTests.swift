import XCTest
import AutoRuProtoModels

final class SaleCardTradeInTests: BaseTest {
    private let offerId = "1115148869-33ed8311"
    
    override func setUp() {
        super.setUp()
        setupServer()
    }

    override func tearDown() {
        super.tearDown()
        mocker.stopMock()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "offer_1115148869-33ed8311_trade_in_ok")
            }
            .mock_requestTradeIn(id: offerId)
            .startMock()
    }

    func test_requestTradeIn() {
        let phone = "+7 987 564-32-12"
        let requestPostWasCalled =
        api.offer.category(.cars).offerID(offerId).tradeIn.post.expect { request, _ in
            let isCorrectBody: Bool = {
                request.userInfo.phoneNumber == phone &&
                request.userOfferInfo.offerID == "1514694829-6bd89bdd" &&
                request.clientOfferInfo.offerID == "1115148869-33ed8311"
            }()

            return isCorrectBody ? .ok : .fail(reason: "Ошибка отправки параметров в запросе на tradeIn")
        }

        mocker
            .mock_userOffersCarsOne()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .tradeInButton)
                    .tap(.tradeInButtonInfo)
            }
            .should(provider: .modalPicker, .exist)
            .focus { picker in
                picker.tap(.item("Обменять"))
            }
            .should(provider: .phonesListPopupScreen, .exist)
            .focus { popup in
                popup.tap(.phoneRow(number: phone))
            }
            .wait(for: [requestPostWasCalled])
    }

    func test_requestTradeInChoosingUserOffer() {
        let phone = "+7 987 564-32-12"
        let requestPostWasCalled =
        api.offer.category(.cars).offerID(offerId).tradeIn.post.expect { request, _ in
            let isCorrectBody: Bool = {
                request.userInfo.phoneNumber == phone &&
                request.userOfferInfo.offerID == "1514694831-bdc720af" &&
                request.clientOfferInfo.offerID == "1115148869-33ed8311"
            }()

            return isCorrectBody ? .ok : .fail(reason: "Ошибка отправки параметров в запросе на tradeIn")
        }

        mocker
            .mock_userOffersCarsCouple()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus({ screen in
                screen
                    .scroll(to: .tradeInButton)
                    .tap(.tradeInButton)
            })
            .should(provider: .phonesListPopupScreen, .exist)
            .focus { popup in
                popup.tap(.phoneRow(number: phone))
            }
            .should(provider: .tradeInOfferPickerScreen, .exist)
            .focus { picker in
                picker.tap(.tradeInUserOffer)
            }
            .wait(for: [requestPostWasCalled])
    }
}
