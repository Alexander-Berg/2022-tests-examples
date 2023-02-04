import XCTest

final class SaleCardPublicProfileTests: BaseTest {
    private let offerId = "1115882408-9a416a6b"
    private static let encryptedUserID = "SxKXJ-yVbqA-_neifxQC_A"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_saleCardPublicProfileInfoSnippet() {
        launch(on: .transportScreen)
            .tap(.searchHistory(title: "LADA (ВАЗ) 1111 Ока"))
            .should(provider: .saleListScreen, .exist)
            .focus { saleList in
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .title), ofType: .offerSnippet) { snippet in
                    snippet.tap()
                }
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { saleCard in
                saleCard
                    .scrollTo("public_profile_info")
                    .should(.publicProfileInfo, .exist)
                    .should(.publicProfileSales, .exist)
                    .focus { $0.tap() }
            }
            .should(provider: .publicProfileScreen, .exist)
            .wait(for: 2)
    }

    func test_openPublicProfileFromDeepLink() {
        launch(
            on: .transportScreen,
            options: .init(launchType: .deeplink("https://auto.ru/reseller/\(Self.encryptedUserID)/all"))
        ) { screen in
            screen
                .should(provider: .publicProfileScreen, .exist)
        }
        .wait(for: 1)
        .should(provider: .navBar, .exist)
        .focus { navBar in
            navBar.tap(.back)
        }
        .should(provider: .transportScreen, .exist)
    }

    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_searchHistory(state: .used)
            .mock_publicProfileUser(encryptedUserID: Self.encryptedUserID)
            .mock_publicProfileList(encryptedUserID: Self.encryptedUserID)
            .mock_searchCars {
                var response = Responses.Search.Cars.success(for: .global)
                response.offers[0] = response.offers[0]
                    .mutate { offer in
                        offer.additionalInfo.otherOffersShowInfo = .with {
                            $0.encryptedUserID = Self.encryptedUserID
                        }
                    }
                return response
            }
        mocker.startMock()
    }
}
