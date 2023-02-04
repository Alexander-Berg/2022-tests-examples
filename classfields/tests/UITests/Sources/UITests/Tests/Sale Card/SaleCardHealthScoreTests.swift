import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuStandaloneCarHistory
final class SaleCardHealthScoreTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        self.setupServer()
    }

    // MARK: - Health

    func test_freeReport() {
        self.server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            Response.okResponse(fileName: "CarfaxCard_mercedes-e_info", userAuthorized: true)
        }

        mocker
            .mock_reportLayoutForOffer(bought: false, quotaLeft: 0)
            .mock_reportLayoutForReport(bought: false, quotaLeft: 0)
        Step("Тапаем на иконку скора в превью отчета и проверяем попап с переходом на оплату") { }

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scrollAndTapOnScore()
            .tapOnBuyReportButton()
            .shouldSeePaymentModal()
    }

    func test_boughtReport() {
        mocker
            .mock_reportLayoutForOffer(bought: true, quotaLeft: 0)
            .mock_reportLayoutForReport(bought: true, quotaLeft: 0)
        self.server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            Response.okResponse(fileName: "CarfaxCard_mercedes-e_info", userAuthorized: true)
        }

        Step("Тапаем на иконку скора в превью отчета и проверяем попап с переходом в отчет") { }

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scrollAndTapOnScore()
            .tapOnSeeReportButton()
            .tapStatusBar()
            .shouldSeeContent()
    }

    // MARK: - Private

    private func setupServer() {

        self.server.forceLoginMode = .forceLoggedIn

        self.server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /history/last/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "history_last_all_characteristic_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /carfax/offer/cars/1098230510-dd311329/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "carfax_offer_cars_1090794514-915f196d_raw_GET_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /carfax/offer/cars/1101296108-80ca49a5/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "carfax_bought_1072802872-386ec5_score", userAuthorized: true)
        }

        self.server.addHandler("GET /search/cars *") { (_, _) -> Response? in
            Response.okResponse(fileName: "SaleListHeaderTests_single-offer", userAuthorized: true)
        }

        try! self.server.start()
    }
}
