import XCTest
import Foundation

final class RateAppTests: BaseTest {
    private let offerId = "1098252972-99d8c274"

    func testNegativeRateNotLogined() {
        mocker
            .mock_base()
            .mock_offerFromHistoryLastAll()
            .mock_feedbackSend()
            .startMock()

        let requestPostWasCalled = api.feedback.send.post.expect { request, _ in
            let isCorrectBody: Bool = {
                request.email.contains("pedro@666.test") &&
                request.message.contains("test")
            }()

            return isCorrectBody ? .ok : .fail(reason: "Ошибка отправки данных")
        }

        launchWithOptions()
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Нет"))
            }
        // Далее появляется аналогичный алерт, но с другим тайтлом
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Да"))
            }
            .should(provider: .feedbackScreen, .exist)
            .focus { screen in
                screen
                    .tap(.emailField)
                    .type("pedro@666.test")
                    .tap(.messageField)
                    .type("test")
                    .tap(.submitButton)
            }
            .should(provider: .saleCardScreen, .exist)
            .wait(for: [requestPostWasCalled])
    }

    func testNegativeRateLogined() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .mock_offerFromHistoryLastAll()
            .mock_getChatRoom()
            .startMock()

        launchWithOptions()
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Нет"))
            }
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Да"))
            }
            .should(provider: .chatScreen, .exist)
    }

    func testSecondRateAlertShown() {
        mocker
            .mock_base()
            .mock_offerFromHistoryLastAll()
            .startMock()

        api.search.cars.post(parameters: .wildcard)
            .ok(mock: .file("history_last_all_credit_ok"))

        launchWithOptions()
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Нет"))
            }
            .should(provider: .systemAlert, .exist)
            .focus { screen in
                screen
                    .tap(.button("Нет"))
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .tapOnBackButton()
            }
            .should(provider: .saleListScreen, .exist)
            .focus({ screen in
                screen.tap(.offerCell(.custom(offerId)))
            })
            .should(provider: .systemAlert, .exist)

    }

    private func launchWithOptions() -> TransportScreen {
        let options = AppLaunchOptions(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"), overrideAppSettings: ["showRateAppAfterCardsCount": 1,
                                                                                   "showSecondRateAppAfterCardsCount": 2,
                                                                                   "ru.auto.RateAppService.blockShowingRateDialog": false])

        return launch(on: .transportScreen, options: options)
    }
}
