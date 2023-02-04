import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuComparison AutoRuComparisonsList AutoRuSaleCard
final class ComparisonIntegrationTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        self.setupServer()
        super.setUp()
    }
    
    func test_offerCardComparisonStatusBackUpdate() {
        self.mocker.server.addHandler("DELETE /user/compare/cars/favorite-1101172822-56c9ffd1") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        let removeExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/favorite-1101172822-56c9ffd1" && req.method == "DELETE"
        }

        let newOffersExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/offers" && req.method == "GET"
        }

        self.launch()

        Step("Проверяем, что статус корректно обновляется на всех экранах") {}

        let steps = self.openOffersComparison()
            .tapOnHeader(column: 0)
            .as(SaleCardSteps.self)
            .wait(for: 2)
            .checkCompareButton(isOn: true)

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7_remove_first", userAuthorized: true)
        }

        steps.tapOnCompareButton()

        wait(for: [removeExpectation, newOffersExpectation], timeout: Self.requestTimeout)

        steps.checkCompareButton(isOn: false)
            .tapOnBackButton()
            .as(OffersComparisonSteps.self)
            .checkScreenshotOfComparison(identifier: "offers_comparison_after_remove_first_offer")
    }

    func test_offerCardComparisonStatusRevertDelete() {
        self.mocker.server.addHandler("DELETE /user/compare/cars/favorite-1101172822-56c9ffd1") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.mocker.server.addHandler("PUT /user/compare/cars/favorite-1101172822-56c9ffd1") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.launch()

        Step("Проверяем, что можно отменить удаление с карточки") {}

        let steps = self.openOffersComparison()
            .tapOnHeader(column: 0)
            .as(SaleCardSteps.self)
            .wait(for: 2)

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7_remove_first", userAuthorized: true)
        }

        let addExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/favorite-1101172822-56c9ffd1" && req.method == "PUT"
        }

        steps.tapOnCompareButton()
            .checkComparisonSnackbar(isInComparison: false)
            .tapOnSnackbarButton()

        wait(for: [addExpectation], timeout: Self.requestTimeout)

        steps.checkCompareButton(isOn: true)
    }

    func test_offerCardAddToComparisonAndGoto() {
        self.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7_remove_first", userAuthorized: true)
        }

        self.server.addHandler("PUT /user/compare/cars/favorite-1101172822-56c9ffd1") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        let addExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/favorite-1101172822-56c9ffd1" && req.method == "PUT"
        }

        let newOffersExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/offers" && req.method == "GET"
        }

        self.launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101172822-56c9ffd1")))

        Step("Проверяем, что можно добавить карточку оффера в сравнение и перейти в него") {}

        let steps = SaleCardSteps(context: self)
            .checkCompareButton(isOn: false)

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7", userAuthorized: true)
        }

        steps.tapOnCompareButton()

        wait(for: [addExpectation, newOffersExpectation], timeout: Self.requestTimeout)

        steps
            .checkComparisonSnackbar(isInComparison: true)
            .tapOnSnackbarButton()
            .as(OffersComparisonSteps.self)
            .checkComparisonScreenTitle()
    }

    // MARK: - Private
    private func openOffersComparison() -> OffersComparisonSteps {
        return self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .tapOffersComparison()
            .as(OffersComparisonSteps.self)
    }

    private func setupServer() {
        _ = self.mocker.setForceLoginMode(.forceLoggedIn)
        defer {
            self.mocker.startMock()
        }

        self.mocker.server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_favorites", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /offer/cars/1101172822-56c9ffd1 *") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offer_1101172822-56c9ffd1", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "comparison_history", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/models") { (_, _) -> Response? in
            Response.okResponse(fileName: "success")
        }
    }
}
