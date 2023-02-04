import XCTest
import Snapshots

/// @depends_on AutoRuGarageCard
final class PublicGarageCardTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .startMock()
    }

    func test_publicCardNotAuthorizedTransitionToLogin() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        launchAndOpenGarageCard()
            .log("Проверяем, что открылась публичная карточка с кнопкой \"Добавить автомобиль в свой гараж\"")
            .should(.titleLabel, .match("BMW 5 серии"))
            .tap(.goToOwnGarageButton("Добавьте свой автомобиль в гараж"))
            .log("Проверяем, что открылась форма авторизации")
            .should(provider: .loginScreen, .exist)
        }

    func test_publicCardEmptyListingTransitionToAddNew() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .mock_garageListingEmpty()
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        launchAndOpenGarageCard()
            .log("Проверяем, что открылась публичная карточка с кнопкой \"Добавить автомобиль в свой гараж\"")
            .should(.titleLabel, .match("BMW 5 серии"))
            .tap(.goToOwnGarageButton("Добавьте свой автомобиль в гараж"))
            .wait(for: 1)
            .log("Проверяем, что открылась форма добавления авто в гараж")
            .should(provider: .garageAddCarScreen, .exist)
        }

    func test_publicCardWithListingTransitionToGarage() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .mock_garageListing()
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        launchAndOpenGarageCard()
            .log("Проверяем, что открылась публичная карточка с кнопкой \"Перейти в свой гараж\"")
            .should(.titleLabel, .match("BMW 5 серии"))
            .tap(.goToOwnGarageButton("Перейти в свой гараж"))
            .wait(for: 1)
            .log("Проверяем, что открылся гараж со списком авто")
            .should(provider: .garageCardScreen, .exist)
    }

    func test_publicCardWithListingTransitionToCard() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .mock_garageListingWithOneCar()
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        launchAndOpenGarageCard()
            .log("Проверяем, что открылась публичная карточка с кнопкой \"Перейти в свой гараж\"")
            .should(.titleLabel, .match("BMW 5 серии"))
            .tap(.goToOwnGarageButton("Перейти в свой гараж"))
            .wait(for: 1)
            .log("Проверяем, что открылась карточка гаража со своим авто")
            .should(provider: .garageCardScreen, .exist)
    }

    func test_publicCardDealerLoggedNoGoToGarageButton() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_dealer()
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        launchAndOpenGarageCard()
            .log("Проверяем, что в публичной карточке у дилера нет кнопки \"Перейти в свой гараж\"")
            .should(.goToOwnGarageButton("Перейти в свой гараж"), .be(.hidden))
    }

    func test_publicCardAppearance() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .mock_garageListingWithOneCar()
            .mock_publicGarageCard("1955418404")
            .mock_garageReviews()

        let garageCard = launchAndOpenGarageCard()
        checkGarageHeaderCells(garageCard)
        checkGarageCells(garageCard)
    }

    func test_publicCardOnSale() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_garageReviews()

        api.garage.card.cardId("1955418404")
            .get
            .ok(mock: .file("garage_public_card_1955418404") { response in
                response.card.offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.offer.category(.cars).offerID("123")
            .get
            .ok(mock: .file("offer_CARS_1092222570-3203c7f6_ok"))

        launchAndOpenGarageCard()
            .should(.onSaleLabel, .exist)
            .should(.offerHeader, .exist)
            .tap(.offerHeader)
            .should(provider: .saleCardScreen, .exist)
    }

    func test_publicCardOnSale_fromLabel() {
        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_garageReviews()

        api.garage.card.cardId("1955418404")
            .get
            .ok(mock: .file("garage_public_card_1955418404") { response in
                response.card.offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.offer.category(.cars).offerID("123")
            .get
            .ok(mock: .file("offer_CARS_1092222570-3203c7f6_ok"))

        launchAndOpenGarageCard()
            .should(.onSaleLabel, .exist)
            .should(.offerHeader, .exist)
            .tap(.onSaleLabel)
            .should(provider: .saleCardScreen, .exist)
    }

    // MARK: - Checks

    private func checkGarageHeaderCells(_ garageCard: GarageCardSteps) {
        garageCard
            .should(.priceHeader, .exist).focus { $0.validateSnapshot(snapshotId: "price_header") }
            .should(.sellButton, .be(.hidden))
            .should(.paramsHeader, .exist).focus { $0.validateSnapshot(snapshotId: "params_header") }
            .should(.changeParamsButton, .be(.hidden))
            .should(.addVinHeader, .be(.hidden))
            .should(.promoHeader, .be(.hidden))
            .should(.provenOwnerHeader, .be(.hidden))
            .should(.calculatePriceHeader, .be(.hidden))
            .should(.writeReviewHeader, .be(.hidden))
            .should(.reportHeader, .be(.hidden))
            .tap(.paramsHeader)
            .wait(for: 1)
            .should(provider: .genericModalPopup, .exist)
            .focus { popup in
                popup
                    .validateSnapshot(snapshotId: "params_bottomsheet")
                    .tap(.dismissButton)
            }
    }

    private func checkGarageCells(_ garageCard: GarageCardSteps) {
        let performChecks: () -> Void = {
            garageCard
                .should(.provenOwnerCell(.anyStatus), .be(.hidden))
                .should(.reportPreviewCell, .be(.hidden))
                .should(.addVinCell, .be(.hidden))
        }

        // we need to look through the whole scroll view
        for _ in 1...5 {
            performChecks()
            garageCard.swipe()
        }
    }

    private func launchAndOpenGarageCard() -> GarageCardScreen_ {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/garage/share/1955418404"),
            overrideAppSettings: [:]
        )

        return launch(on: .garageCardScreen, options: options)
    }
}
