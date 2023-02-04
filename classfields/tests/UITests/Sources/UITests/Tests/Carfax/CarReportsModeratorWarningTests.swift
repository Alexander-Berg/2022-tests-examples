import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuStandaloneCarHistory AutoRuServices
final class CarReportsModeratorWarningTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        self.setupServer()
    }

    func test_card_moderatorWarning_withoutModeratorGrants() {
        self.mockWithoutModeratorGrants()

        self.launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))

        Step("Проверяем, что без прав модератора не виден ворнинг в отчёте") { }

        SaleCardSteps(context: self)
            .scrollAndTapOnShowReportButton()
            .shouldSeeContent()
            .checkModeratorWarning(visible: false)
    }

    func test_search_notFound_moderatorWarning_withoutModeratorGrants() {
        self.mockWithoutModeratorGrants()

        self.server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "car_report_search_not_found", userAuthorized: true)
        }

        self.launch()

        Step("Проверяем, что без прав модератора не виден ворнинг в поиске (ошибка поиска)") { }

        self.openReportSearch()
            .typeInSearchBar(text: "B004OK12")
            .tapOnSearchButton()
            .wait(for: 3)
            .checkModeratorWarning(visible: false)
    }

    func test_search_notFound_moderatorWarning_withModeratorGrants() {
        self.mockWithModeratorGrants()

        self.server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "car_report_search_not_found", userAuthorized: true)
        }

        self.launch()

        Step("Проверяем, что с правами модератора виден ворнинг в поиске (ошибка поиска)") { }

        self.openReportSearch()
            .typeInSearchBar(text: "B004OK12")
            .tapOnSearchButton()
            .wait(for: 3)
            .checkModeratorWarning(visible: true)
    }

    func test_search_found_moderatorWarning_withoutModeratorGrants() {
        self.mockWithoutModeratorGrants()

        self.server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "car_report_search_found", userAuthorized: true)
        }

        self.launch()

        Step("Проверяем, что без прав модератора не виден ворнинг в поиске (результат поиска)") { }

        self.openReportSearch()
            .typeInSearchBar(text: "B004OK64")
            .tapOnSearchButton()
            .wait(for: 3)
            .checkModeratorWarning(visible: false)
    }

    func test_search_found_moderatorWarning_withModeratorGrants() {
        self.mockWithModeratorGrants()

        self.server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "car_report_search_found", userAuthorized: true)
        }

        self.launch()

        Step("Проверяем, что с правами модератора виден ворнинг в поиске (результат поиска)") { }

        self.openReportSearch()
            .typeInSearchBar(text: "B004OK64")
            .tapOnSearchButton()
            .wait(for: 3)
            .checkModeratorWarning(visible: true)
    }

    // MARK: - Private

    private func openReportSearch() -> CarReportSearchSteps {
        self.mainSteps
            .openCarReportsList()
            .tapOnSearch()
    }

    private func mockWithoutModeratorGrants() {
        self.server.addHandler("GET /session *") { (_, _) -> Response? in
            Response.okResponse(fileName: "session", userAuthorized: true)
        }
    }

    private func mockWithModeratorGrants() {
        self.server.addHandler("GET /session *") { (_, _) -> Response? in
            let encodingOptions: JSONEncodingOptions = {
                var o = JSONEncodingOptions()
                o.preserveProtoFieldNames = true
                return o
            }()

            let model: Vertis_Passport_SessionResult = Vertis_Passport_SessionResult.with { model in
                model.grants.grants = ["MODERATOR_AUTORU"]
            }

            return Response.okResponse(message: model, options: encodingOptions, userAuthorized: true)
        }
    }

    private func setupServer() {

        self.server.forceLoginMode = .forceLoggedIn

        self.server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /history/last/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "history_last_all_characteristic_ok", userAuthorized: true)
        }

        mocker.mock_reportLayoutForOffer(bought: true)
        mocker.mock_reportLayoutForReport(bought: true)
        self.server.addHandler("GET /carfax/offer/cars/1098230510-dd311329/raw *") { (_, _) -> Response? in
            Response.okResponse(fileName: "carfax_offer_cars_1090794514-915f196d_raw_GET_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            Response.okResponse(fileName: "CarfaxCard_mercedes-e_info", userAuthorized: true)
        }

        self.server.addHandler("GET /search/cars *") { (_, _) -> Response? in
            Response.okResponse(fileName: "SaleListHeaderTests_single-offer", userAuthorized: true)
        }

        try! self.server.start()
    }
}
