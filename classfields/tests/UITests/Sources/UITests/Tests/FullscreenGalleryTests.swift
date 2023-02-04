import XCTest
import AutoRuProtoModels

final class FullscreenGalleryTests: BaseTest {
    private static let offerID = "1098105416-543819ea"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openAndCloseGallery() {
        mockOffer()

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                _ = screen.step("Закрытие крестиком") { $0
                    .tap(.closeButton)
                }
            }
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                _ = screen.step("Закрытие свайпом") { $0
                    .swipe(.down)
                }
            }
            .should(provider: .galleryScreen, .be(.hidden))
    }

    func test_checkPhotoPageIndex() {
        mockOffer()

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen
                    .should(.pageIndex, .match("1/6"))
                    .base
                    .swipeToNextPhoto()
                    .should(.pageIndex, .match("2/6"))
            }
    }

    func test_addAndDeleteToFavorite() {
        mockOffer()

        let expectationAdd = api.user.favorites.category(.cars).offerId(Self.offerID).post.expect()
        let expectationRemove = api.user.favorites.category(.cars).offerId(Self.offerID).delete.expect()

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen
                    .should(.favoriteButton(isFavorite: false), .exist)
                    .tap(.favoriteButton(isFavorite: false))
                    .wait(for: [expectationAdd])
                    .should(.favoriteButton(isFavorite: true), .exist)
                    .tap(.favoriteButton(isFavorite: true))
                    .wait(for: [expectationRemove])
            }
    }

    func test_noReport_callButton() {
        mockOffer()

        let expectation = api.offer.category(.cars).offerID(Self.offerID).phones.get.expect()

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen
                    .tap(.callButton)
                    .wait(for: [expectation])
            }
    }

    func test_hasReport_call() {
        mockOffer {
            $0.tags.append("vin_offers_history")
        }

        let expectation = api.offer.category(.cars).offerID(Self.offerID).phones.get.expect()

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen
                    .tap(.callButton)
                    .wait(for: [expectation])
            }
    }

    func test_hasReport_openChat() {
        mockOffer {
            $0.additionalInfo.chatOnly = true
            $0.tags.append("vin_offers_history")
        }

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen.tap(.chatButton)
            }
            .should(provider: .chatScreen, .exist)
    }

    func test_hasReport_openReport() {
        mockOffer {
            $0.additionalInfo.chatOnly = true
            $0.tags.append("vin_offers_history")
        }

        openCard()
            .tap(.images)
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen.tap(.reportButton)
            }
            .should(provider: .carReportScreen, .exist)
    }

    func test_scrollOnCardAndOpen() {
        mockOffer()

        openCard()
            .focus(on: .images, ofType: .largeCardGalleryCell) { $0
                .swipeToNextPhoto()
                .swipeToNextPhoto()
                .tap()
            }
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen.should(.pageIndex, .match("3/6"))
            }
    }

    private func openCard() -> SaleCardScreen_ {
        launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("autoru://app/cars/used/sale/citroen/c3/\(Self.offerID)"))
        )
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)

        api.user.favorites.category(.cars).offerId(Self.offerID)
            .post
            .ok(mock: .model(.init()))

        api.user.favorites.category(.cars).offerId(Self.offerID)
            .delete
            .ok(mock: .model(.init()))

        api.offer.category(.cars).offerID(Self.offerID).phones.get.ok(mock: .model())

        mocker.startMock()
    }

    private func mockOffer(_ mutation: @escaping (inout Auto_Api_Offer) -> Void = { _ in }) {
        api.offer.category(.cars).offerID(Self.offerID)
            .get
            .ok(
                mock: .file("offer_CARS_1098230510-dd311329_ok") { response in
                    response.offer.id = Self.offerID
                    mutation(&response.offer)
                }
            )
    }
}
