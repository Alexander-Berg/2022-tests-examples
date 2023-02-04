import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuUserSaleList
final class UserSaleListTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_garagePromoBanner() {
        mocker
            .mock_userOffersWithGaragePromo()
            .mock_garageListing()
            .mock_garageCard("16227978-01dc7292")

        openUserSaleList()
            .log("Проверяем наличие баннера гаража и тапаем на него")
            .scroll(to: .garagePromoBanner)
            .tap(.garagePromoBanner)
            .log("Проверяем, что открылась карточка гаража")
            .should(provider: .garageCardScreen, .exist)
            .log("Проверяем, что баннер гаража больше не показывается")

        MainSteps(context: self).openTab(.offers)
            .as(UserSaleListScreen.self)
            .should(provider: .userSaleListScreen, .exist)
            .should(.garagePromoBanner, .be(.hidden))
    }

    // MARK: - Private
    private func openUserSaleList() -> UserSaleListScreen {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/my"),
            overrideAppSettings: [:]
        )
        return launch(on: .userSaleListScreen, options: options)
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()

        mocker.startMock()
    }
}
