import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRu AutoRuServices AutoRuUserOfferStat AutoRuUserSaleCard
final class UserOfferStatTests: BaseTest {

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_noStatAfterFirstShow() {
        mocker.mock_userOffers(counters: [0: (56, 0)])

        launchWithResetting()
            .focus(on: .userOfferStatBubble, ofType: .userOfferStatBubble) { element in
                element
                    .should(.positionCounter, .match("56 в поиске"))
                    .should(.viewCounter, .be(.hidden))
            }

        relaunchAppWithoutResetting()
            .should(.userOfferStatBubble, .be(.hidden))
    }

    func test_statChangeAfterOpenUserOffers() {
        mocker.mock_userOffers(counters: [0: (10, 10)])

        let step = launchWithResetting()
            .should(.userOfferStatBubble, .exist)

        mocker.mock_userOffers(counters: [0: (100, 100)])

        step
            .base
            .toggle(to: \.offers)
            .wait(for: 3)

        mocker.mock_userOffers(counters: [0: (150, 150)])

        relaunchAppWithoutResetting()
            .focus(on: .userOfferStatBubble, ofType: .userOfferStatBubble) { element in
                element
                    .should(.positionCounter, .match("50 в поиске"))
                    .should(.viewCounter, .match("+50"))
            }
    }

    func test_statChangeAfterOpenUserOffer() {
        mocker.mock_userOffers(counters: [0: (10, 10)], normalizeIDs: true)
        mocker.mock_userOfferStats(id: "0", category: .trucks)

        let step = launchWithResetting()
            .should(.userOfferStatBubble, .exist)

        mocker.mock_userOffer(id: "0", category: .trucks, searchPosition: 100, counterAll: 100)

        step
            .base
            .toggle(to: \.offers)
            .should(provider: .userSaleListScreen, .exist)
            .focus {
                $0
                    .openOffer(offerId: "0")
                    .wait(for: 3)
            }

        mocker.mock_userOffers(counters: [0: (150, 150)], normalizeIDs: true)
        mocker.mock_userOffer(id: "0", category: .trucks, searchPosition: 150, counterAll: 150)

        relaunchAppWithoutResetting()
            .focus(on: .userOfferStatBubble, ofType: .userOfferStatBubble) { element in
                element
                    .should(.positionCounter, .match("50 в поиске"))
                    .should(.viewCounter, .match("+50"))
            }
    }

    // MARK: - Private

    private func setupServer() {
        mocker.startMock()
        mocker.server.forceLoginMode = .forceLoggedIn

        mocker
            .mock_base()
            .mock_user()
    }

    private func launchWithResetting() -> UIElementProviderHost<MainScreen_, TransportScreen> {
        return launchMain(options: .init(launchType: .default, overrideAppSettings: ["clean_user_offer_stat": true]))
    }

    private func relaunchAppWithoutResetting() -> UIElementProviderHost<MainScreen_, TransportScreen> {
        app.terminate()

        app.launchArguments = app.launchArguments.filter { $0 != "--resetDefaults" }

        XCTAssertFalse(
            app.launchArguments.contains("--resetDefaults"),
            "Нельзя сбрасывать дефолтсы перед запуском"
        )

        return launchMain(options: .init(launchType: .default, overrideAppSettings: ["clean_user_offer_stat": false]))
    }
}
