import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuGarageForm AutoRuGarageCard
final class GarageTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    // BMW, добавляем вручную
    static let garageCard1: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")
    // УАЗ, ищем по вину
    static let garageCard2: (vin: String, id: String) = ("XTT316300F1027573", "514745881")

    private lazy var mainSteps = MainSteps(context: self)
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override func setUp() {
        super.setUp()

        setupServer()
        launch()
    }

    func test_openGarageAsUnauthorized() {
        self.server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_cards_empty", userAuthorized: false)
        }
        self.server.forceLoginMode = .forceLoggedOut

        let step = openGarage()
            .checkPromoBanner()
            .tapOnPromoBanner()
            .tapOnAddCarButton()
            .as(LoginSteps.self)
            .shouldSeeLoginScreen()
        self.server.forceLoginMode = .forceLoggedIn

        step
            .enterPhone()
            .enterCode("12345678")
            .should(provider: .garageAddCarScreen, .exist)
    }

    func test_landingContentItems() {
        self.server.addHandler("POST /garage/user/cards") { _, _ in
            Response.okResponse(fileName: "garage_cards_empty", userAuthorized: false)
        }

        api.garage.user.promos
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()) {
                let promo = Auto_Api_Vin_Garage_PartnerPromo.with { promo in
                    promo.type = .superPromo
                    promo.title = "Тестовое промо из ручки"
                    promo.description_p = "Описание тестового промо"
                    promo.backgroundColor = "#EDFBFE"
                    promo.url = "testURL"
                }
                $0.partnerPromos = [promo]
            })

        self.server.forceLoginMode = .preservingResponseState

        self.openGarage()
            .checkPromoBanner()
            .checkBlock(title: "Поставьте автомобиль в Гараж")
            .checkBlock(title: "Тестовое промо из ручки")
            .checkBlock(title: "Отчёт ПроАвто в подарок!")
            .checkBlock(title: "Оценка автомобиля")
            .checkBlock(title: "Полная история вашего автомобиля")
            .checkBlock(title: "Отзывные кампании")
            .checkBlock(title: "Храните страховой полис в Гараже Авто.ру")
    }

    func test_openLandingPromo() {
        server.forceLoginMode = .forceLoggedOut

        mocker.mock_garagePromos(fileNames: ["garage_landing"])

        self.openGarage()
            .checkPromoBanner()
            .tapOnPromoBanner()
            .should(provider: .garageLanding, .exist)
            .focus { $0.tap(.promoItem) }
            .should(provider: .garagePromoPopup, .exist)
    }

    // MARK: - Private

    private func openGarage() -> GarageSteps {
        return self.mainSteps.openTab(.garage).as(GarageSteps.self)
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /auth/login-or-register") { (_, _) -> Response? in
            Response.okResponse(fileName: "login_or_register", userAuthorized: false)
        }

        server.addHandler("POST /user/confirm") { (_, _) -> Response? in
            Response.okResponse(fileName: "user_confirm", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_cards", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/vehicle_info/\(Self.garageCard1.vin)") { (_, _) -> Response? in
            Response.responseWith(code: "404", fileName: "garage_search_not_found", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/vehicle_info/\(Self.garageCard2.vin)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_search_vin", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/card/identifier/\(Self.garageCard2.vin)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_create_with_vin", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/rating *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/\(Self.garageCard1.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard1.id)", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/\(Self.garageCard2.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard2.id)", userAuthorized: true)
        }

        server.addHandler("PUT /garage/user/card/\(Self.garageCard1.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_form_create", userAuthorized: true)
        }

        mocker
            .mock_base()
            .mock_user()
            .startMock()
    }
}
