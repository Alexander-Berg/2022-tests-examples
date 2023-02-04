import XCTest
import AutoRuProtoModels
import SwiftProtobuf

final class SaleCardAdvantagesTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)
    private static let offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // Электромобили
    func test_saleCardElectrocarsBanner() {
        mocker.mock_offerCars(id: Self.offerId) { res in
            res.offer.carInfo.techParam.engineType = "ELECTRO"
        }

        launchOnSaleCardAndFocusAdvantages()
            .tap(.electrocarsAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist).focus {
                $0.tap(.electrocarsButton)
            }
            .log("Проверяем открытие главной электромобилей")
            .should(provider: .electroCarsMainScreen, .exist)
    }

    // Онлайн показ
    func test_shouldOnlineViewTagAndOpenChat() {
        mockAdvantageTag(["online_view_available"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.onlineViewAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus {
                $0.log("Проверяем внешний вид шаблонного боттомшита")
                $0.validateSnapshot(snapshotId: "advantage_popUp")
                $0.tap(.chatButton)
            }
            .log("Проверяем открытие чата через преимущество Онлайн Показ")
            .should(provider: .chatScreen, .exist)
    }

    func test_shouldOnlineViewTagAndMakeCall() {
        mockAdvantageTag(["online_view_available"])

        api.offer.category(.cars).offerID(Self.offerId).phones
            .get
            .ok(mock: .file("best_offers_phones"))

        launchOnSaleCardAndFocusAdvantages()
            .tap(.onlineViewAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.callButton) }
            .log("Проверяем звонок через преимущество Онлайн Показ")
            .should(provider: .callOptionPicker, .exist)
    }

    // Продает собственник
    func test_shouldProvenOwnerTagNonAuthUser() {
        mockAdvantageTag(["proven_owner"])

        mocker.setForceLoginMode(.forceLoggedOut)

        launchOnSaleCardAndFocusAdvantages()
            .tap(.provenOwnerAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.provenOwnerButton) }
            .log("Проверка собственника при незалогине, должен открыться экран авторизации")
            .should(provider: .loginScreen, .exist)
    }

    func test_shouldProvenOwnerTagAuthUserNoActiveOffer() {
        mockAdvantageTag(["proven_owner"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.provenOwnerAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.provenOwnerButton) }
            .log("Проверка собственника без активных объявлений, должен открыться чат")
            .should(provider: .chatScreen, .exist)
    }

    func test_shouldProvenOwnerTagAuthUserWithActiveOffer() {
        mockAdvantageTag(["proven_owner"])

        api.user.offers.category(.cars)
            .get(parameters: [.page(1), .pageSize(1), .sort("cr_date-desc"), .status(["active"])])
            .ok(mock: .file("user_offers_cars_with_active_offer"))

        launchOnSaleCardAndFocusAdvantages()
            .tap(.provenOwnerAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.provenOwnerButton) }
            .do {
                mainSteps.handleSystemAlertIfNeeded()
            }
            .log("Проверка собственника с активным оффером, должна открыться камера для документов")
            .should(provider: .photoSetScreen, .exist)
    }

    // ДТП
    func test_shouldNoAccidentsTag() {
        mockAdvantageTag(["no_accidents"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.noAccidentsAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.reportButton) }
            .log("Проверяем октрытие отчета через преимущество ДТП")
            .should(provider: .carReportScreen, .exist)
    }

    // 1 владелец
    func test_shouldOneOwnerTag() {
        mockAdvantageTag(["online_view_available", "proven_owner", "no_accidents", "one_owner"])

        launchOnSaleCardAndFocusAdvantages()
            .log("Проверяем скролл преимуществ")
            .scroll(to: .oneOwnerAdvantage, direction: .left)
            .tap(.oneOwnerAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.reportButton) }
            .log("Проверяем октрытие отчета через преимущество 1 Владелец")
            .should(provider: .carReportScreen, .exist)
    }

    // Проверенный автомобиль
    func test_shouldCertificateManufacturerTag() {
        mockAdvantageTag(["certificate_manufacturer"]) { res in
            res.offer.brandCertInfo.certStatus = .brandCertActive
            res.offer.brandCertInfo.view.name = "Rolf"
        }

        launchOnSaleCardAndFocusAdvantages()
            .tap(.certificateManufacturerAdvantage)
            .should(provider: .genericModalPopup, .exist)
            .wait(for: 1)
            .focus { $0.validateSnapshot(snapshotId: "advantage_certificate_manufacturer") }
    }

    // На гарантии
    func test_shouldWarrantyTag() {
        mockAdvantageTag(["warranty"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.warrantyAdvantage)
            .should(provider: .genericModalPopup, .exist)
            .focus { $0.validateSnapshot(snapshotId: "advantage_warranty") }
    }

    // Почти как новый
    func test_shouldAlmostNewTag() {
        mockAdvantageTag(["almost_new"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.almostNewAdvantage)
            .should(provider: .genericModalPopup, .exist)
            .focus { $0.validateSnapshot(snapshotId: "advantage_almost_new") }
    }

    // Отличная модель
    func test_shouldHighReviewsMarkTag() {
        mockAdvantageTag(["high_reviews_mark"])

        launchOnSaleCardAndFocusAdvantages()
            .tap(.highReviewsMarkAdvantage)
            .should(provider: .offerAdvantagesModalPopup, .exist)
            .focus { $0.tap(.reviewsButton) }
            .should(provider: .reviewListScreen, .exist)
    }

    // Стабильная цена 5%
    func test_shouldStablePriceTag() {
        api.stats.summary
            .get(parameters: [
                .complectationId(0), .configurationId(6268026), .mark("VAZ"),
                .model("1111"), .rid(10855), .superGen(6268019), .techParamId(20400346)
            ])
            .ok(mock: .file("stats_summary_sale_card"))

        mockAdvantageTag(["stable_price"])

        launchOnSaleCardAndFocusAdvantages()
            .focus(on: .stablePriceAdvantage) {
                $0.validateSnapshot(snapshotId: "advantage_stable_price_cell")
            }
            .tap(.stablePriceAdvantage)
            .should(provider: .genericModalPopup, .exist)
            .focus { $0.validateSnapshot(snapshotId: "advantage_stable_price") }
    }

    // Медленно теряет в цене
    func test_shouldStablePriceWithLowPriceStatsTag() {
        api.stats.summary
            .get(parameters: [
                .complectationId(0), .configurationId(6268026), .mark("VAZ"),
                .model("1111"), .rid(10855), .superGen(6268019), .techParamId(20400346)
            ])
            .ok(mock: .file("stats_summary_sale_card"))

        mockAdvantageTag(["stable_price"]) { res in
            res.offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 3
        }

        launchOnSaleCardAndFocusAdvantages()
            .focus(on: .stablePriceAdvantage) {
                $0.validateSnapshot(snapshotId: "advantage_stable_price_low_cell")
            }
    }

    // Score
    func test_scoreAdvantage() {
        mocker.mock_reportLayoutForOffer(bought: true)
        mocker.mock_reportLayoutForReport(bought: true)

        mockAdvantageTag(["no_accidents", "proven_owner", "low_mileage", "vin_offers_history"]) { res in
            res.offer.documents.vinResolution = .ok

            var score = Auto_Api_Score()
            score.transparency = 77.0
            res.offer.score = score
        }

        Step("Проверяем бейдж скора и попап, когда задан score у оффера") { }

        launchOnSaleCard()
            .checkAdvantagesWithScore()
            .tapOnScoreBadge()
            .checkPopupSnapshot(identifier: "score_advantage")
            .tapOnBottomButton(.score)
            .as(CarReportPreviewSteps.self)
            .waitForLoading()
            .shouldSeeContent()
    }

    func test_scoreAdvantageNoReport() {
        mockAdvantageTag(["no_accidents", "proven_owner", "low_mileage"]) { res in
            res.offer.documents.vinResolution = .notMatchedPlate

            var score = Auto_Api_Score()
            score.transparency = 77.0
            res.offer.score = score
        }

        Step("Проверяем попап, когда задан score у оффера, но нет отчета") { }

        launchOnSaleCard()
            .tapOnScoreBadge()
            .checkPopupSnapshot(identifier: "score_advantage_no_report")
    }

    func test_noScoreNoAdvantage() {
        mockAdvantageTag(["no_accidents", "proven_owner", "low_mileage"]) { res in
            res.offer.clearScore()
        }

        Step("Проверяем, что нет бейджа преимущества со скором, когда у оффера не задан score вообще") { }

        launchOnSaleCard()
            .shouldNotSeeScoreBadge()
    }

    func test_noTransparencyNoAdvantage() {
        mockAdvantageTag(["no_accidents", "proven_owner", "low_mileage"]) { res in
            res.offer.score = .with { score in
                score.health = Google_Protobuf_FloatValue(83.0)
                score.clearTransparency()
            }
        }

        Step("Проверяем, что нет бейджа преимущества со скором, когда у оффера не задан transparency (только health)") { }

        launchOnSaleCard()
            .shouldNotSeeScoreBadge()
    }

    private func launchOnSaleCardAndFocusAdvantages() -> AdvantagesCell {
        launch(
            on: .saleCardScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/cars/used/sale/\(Self.offerId)"),
                overrideAppSettings: ["electrocarsEnabled": true]
            )
        ) {
            $0.should(provider: .advantagesCell, .exist)
        }
    }

    private func launchOnSaleCard() -> SaleCardScreen_ {
        launch(
            on: .saleCardScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/cars/used/sale/\(Self.offerId)"),
                overrideAppSettings: ["electrocarsEnabled": true]
            )
        )
    }

    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .startMock()
    }

    private func mockAdvantageTag(
        _ tags: [String],
        mutate: ((inout Auto_Api_OfferResponse) -> Void)? = nil
    ) {
        mocker.mock_offerCars(id: Self.offerId) { res in
            res.offer.tags = tags
            mutate?(&res)
        }
    }
}
