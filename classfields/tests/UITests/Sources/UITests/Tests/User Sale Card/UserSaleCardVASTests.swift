import XCTest
import AutoRuProtoModels

final class UserSaleCardVASTests: BaseTest {
    private let audiID = "1103930878-40839e2d"
    private let apolloID = "3948131-63e693fc"

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_expDisabled() {
        openUserSaleList(expEnabled: false)
            .scroll(to: .vas(id: audiID, name: "VIP"))
            .should(.vasHeader, .be(.hidden))
            .should(.expandButton, .be(.hidden))
    }

    func test_expEnabled() {
        openUserSaleList()
            .focus(on: .vasHeader, ofType: .userSaleListOfferVASHeader) { header in
                header
                    .should(.title, .match("Найдите покупателя быстрее"))
                    .should(.text, .match("Готовьте ключи и документы: эти пакеты опций увеличивают число просмотров и ускоряют продажу"))
            }
            .scroll(to: .vas(id: audiID, name: "VIP"))
            .focus(on: .vas(id: audiID, name: "VIP"), ofType: .userSaleListOfferVAS) { snippet in
                snippet
                    .should(.title, .match("До ×60 просмотров"))
                    .should(.text, .match("Все опции на 60 дней"))
            }
            .scroll(to: .vas(id: audiID, name: "Турбо-продажа"))
            .focus(on: .vas(id: audiID, name: "Турбо-продажа"), ofType: .userSaleListOfferVAS) { snippet in
                snippet
                    .should(.title, .match("До ×20 просмотров"))
                    .should(.text, .match("3 сильных опции на 3 дня"))
            }
            .scroll(to: .vas(id: audiID, name: "Поднятие в поиске"))
            .focus(on: .vas(id: audiID, name: "Поднятие в поиске"), ofType: .userSaleListOfferVAS) { snippet in
                snippet
                    .should(.title, .match("До ×2 просмотров"))
                    .should(.text, .match("На один день"))
            }
            .scroll(to: .expandButton)
            .tap(.expandButton)
            .scroll(to: .offer(id: apolloID))
            .tap(.offerMainButton)
            .scroll(to: .vas(id: apolloID, name: "Экспресс-продажа"), ofType: .userSaleListOfferVAS)
            .focus(on: .vas(id: apolloID, name: "Экспресс-продажа"), ofType: .userSaleListOfferVAS) { snippet in
                snippet
                    .should(.title, .match("До ×15 просмотров"))
                    .should(.text, .match("2 опции на 6 дней"))
            }

    }

    private func openUserSaleList(expEnabled: Bool = true) -> UserSaleListScreen {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/my"),
            overrideAppSettings: ["isVASExperimentEnabled": expEnabled]
        )

        return launch(on: .userSaleListScreen, options: options) {
            $0.should(provider: .userSaleListScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_userOffersVAS()

        mocker.startMock()
    }
}
