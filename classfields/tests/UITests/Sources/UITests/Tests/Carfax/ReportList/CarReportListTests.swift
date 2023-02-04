import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuCellHelpers AutoRuStandaloneCarHistory
final class CarReportListTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: -
    func test_canAddFavorite() {
        Step("Добавляем объявление из отчета в избранное")

        mocker
            .mock_reportLayoutForReport(bought: true)
            .mock_reportRaw { resp in
                var lastOffer = Auto_Api_Vin_OfferRecord()
                lastOffer.offerID = "1098230510-dd311329"
                lastOffer.offerStatus = .active
                resp.report.autoruOffers.offers.append(lastOffer)
            }

        launchMain()
            .should(provider: .mainScreen, .exist).focus {
                $0.tap(.navBarTab(.reports))
            }
            .should(provider: .carReportStandAloneScreen, .exist).focus { screen in
                screen
                    .focus(on: .reportButtons("WVWZZZ1KZBW515003"), ofType: .carReportStandAloneCell) { cell in
                        cell
                            .tap(.favoriteButton("WVWZZZ1KZBW515003"))
                            .focus(on: .favoriteButton("WVWZZZ1KZBW515003")) {
                                $0.wait(for: 1)
                                $0.validateSnapshot(snapshotId: "test_canAddFavorite_list()")
                            }
                            .shouldEventBeReported(
                                "ПроАвто. Стендалоун. Мои отчёты",
                                with: ["Тапы": "Кнопка Избранное (добавление)"]
                            )
                    }
            }
            .should(provider: .carReportStandAloneScreen, .exist).focus {
                $0.tap(.report("WVWZZZ1KZBW515003"))
            }
            .should(provider: .carReportScreen, .exist).focus { screen in
                screen
                    .tap(.favoriteButton)
                    .focus(on: .favoriteButton) {
                        $0.wait(for: 1)
                        $0.validateSnapshot(snapshotId: "test_canAddFavorite_card()")
                    }
                    .shouldEventBeReported(
                        "ПроАвто. Отчёт",
                        with: ["Тапы": "Кнопка Избранное (удаление)"]
                    )
            }
    }

    func test_canCopyVIN() {
        Step("VIN выделяется в списке отчётов") {}

        let steps = openReportList()
        let vinTextView = steps.findVIN("WVWZZZ1KZBW515003")
        vinTextView.shouldExist().tap(withNumberOfTaps: 2, numberOfTouches: 1)
        Snapshot.compareWithSnapshot(image: steps.snapshotVIN("WVWZZZ1KZBW515003"))
    }

    func test_shouldVisibleBlocksCertificationAndRecall() {
        Step("Проверяем отображение блоков сертификации и отзывных кампаний")

        api.carfax.boughtReports.raw
            .get(parameters: [.page(1), .pageSize(10)])
            .ok(mock: .model())

        launchMain()
            .should(provider: .mainScreen, .exist).focus {
                $0.tap(.navBarTab(.reports))
            }
            .should(provider: .carReportStandAloneScreen, .exist)
            .focus({ screen in
                screen
                    .scroll(to: .certificationBlock)
                    .should(.certificationBlock, .exist)
                    .scroll(to: .recallBlock)
                    .should(.recallBlock, .exist)
            })
    }

    // MARK: -

    private func setupServer() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /carfax/bought-reports/raw *") { (_, _) -> Response? in
            var lastOffer = Auto_Api_Vin_OfferRecord()
            lastOffer.offerID = "1098230510-dd311329"
            lastOffer.offerStatus = .active

            var resp: Auto_Api_RawBoughtReportsResponse = .init(mockFile: "carfax_bought-reports_GET_ok")
            resp.reports[0].rawReport.autoruOffers.offers.append(lastOffer)
            resp.reports[0].rawReport.offerID = "1098230510-dd311329"

            return Response.okResponse(message: resp)
        }

        server.addHandler("GET /offer/cars/1098230510-dd311329") { (_, _) -> Response? in
            var resp: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098230510-dd311329_ok")
            resp.offer.status = .active
            return Response.okResponse(message: resp)
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    private func openReportList() -> CarReportsListSteps {
        launch()
        return mainSteps
            .openTab(.reports)
            .as(CarReportsListSteps.self)
    }

    private func setupOpenDefaultReportByVin(mutation: (inout Auto_Api_RawVinReportResponse) -> Void = { _ in }) -> Self {
        app.launchEnvironment["LAUNCH_DEEPLINK_URL"] = "https://auto.ru/history/WP0ZZZ99ZHS112625"

        mocker.mock_reportRaw()

        return self
    }

    private func openReport(mutation: (inout Auto_Api_RawVinReportResponse) -> Void = { _ in }) -> CarfaxStandaloneCardBasicSteps {
        setupOpenDefaultReportByVin(mutation: mutation).launch()

        let startScreen = CarfaxStandaloneScreen(app)
        startScreen.openFullReportButton
            .shouldExist(timeout: 5).tap()

        return mainSteps.as(CarfaxStandaloneCardBasicSteps.self)
    }
}
