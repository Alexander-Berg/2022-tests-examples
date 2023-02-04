import XCTest
import AutoRuProtoModels

final class AuctionSmallClaimTests: BaseTest {
    private static let offerID = "1640897598478615376-2c0470c2"

    private static var options: AppLaunchOptions {
        .init(overrideAppSettings: ["c2bWizardAuctionPosition": "before_price"])
    }

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_smallClaim_submit() {
        launchMain(options: Self.options) { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
        .focus(on: .auctionSmallClaim(offerID: Self.offerID), ofType: .auctionSmallClaimCell) { $0
            .tap(.submit)
        }
        .should(provider: .auctionWelcomeScreen, .exist)
    }

    func test_smallClaim_openPromo() {
        launchMain(options: Self.options) { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
        .focus(on: .auctionSmallClaim(offerID: Self.offerID), ofType: .auctionSmallClaimCell) { $0
            .should(.priceRange, .match("1 000 000 – 2 000 000 ₽"))
            .tap(.more)
        }
        .should(provider: .webViewPicker, .exist)
    }

    func test_smallClaim_close() {
        launchMain(options: Self.options) { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
        }
        .focus(on: .auctionSmallClaim(offerID: Self.offerID), ofType: .auctionSmallClaimCell) { $0
            .tap(.close)
        }
        .should(.auctionSmallClaim(offerID: Self.offerID), .be(.hidden))
    }

    func test_smallClaim_saleCard() {
        launchMain(options: Self.options) { screen in
            screen
                .toggle(to: \.offers)
                .should(provider: .userSaleListScreen, .exist)
                .tap(.offer(id: Self.offerID))
                .should(provider: .userSaleCardScreen, .exist)
        }
        .should(.auctionSmallClaim, .exist)
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .startMock()

        api.user.offers.category(.cars).offerID(Self.offerID).edit
            .post
            .ok(
                mock: .model(.init()) { response in
                    response.offerID = Self.offerID
                    response.offer.category = .cars
                }
            )

        api.user.offers.category(.cars).offerID(Self.offerID).c2bCanApply
            .get
            .ok(
                mock: .model(.init()) { response in
                    response.canApply = true
                    response.priceRange = .with {
                        $0.from = 1000
                        $0.to = 5000
                    }
                }
            )

        api.user.offers.category(._unknown("all"))
            .get(parameters: .wildcard)
            .ok(
                mock: .file("GET user_offers_all") { response in
                    response.offers[0].id = Self.offerID
                    response.offers[0].category = .cars
                    response.offers[0].state.c2BAuctionInfo.priceRange = .with {
                        $0.from = 1_000_000
                        $0.to = 2_000_000
                    }
                }
            )

        api.user.offers.category(.cars).offerID(Self.offerID)
            .get
            .ok(
                mock: .file("GET user_offers_CARS_1101389279-dccb254c") { response in
                    response.offer.id = Self.offerID
                    response.offer.category = .cars
                    response.offer.humanReasonsBan = []
                    response.offer.status = .active
                    response.offer.state.c2BAuctionInfo.priceRange = .with {
                        $0.from = 1_000_000
                        $0.to = 2_000_000
                    }
                }
            )

        api.user.offers.category(.cars).offerID(Self.offerID).stats
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()))

        api.c2bAuction.application.mobileTexts
            .get
            .ok(mock: .model(.init()))

        api.c2bAuction.application.category(.cars).offerId(Self.offerID).offer
            .post(parameters: .wildcard)
            .ok(mock: .model(.init()))
    }
}
