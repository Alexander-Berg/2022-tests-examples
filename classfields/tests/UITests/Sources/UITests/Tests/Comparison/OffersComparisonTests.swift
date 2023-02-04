import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuComparison AutoRuServices
final class OffersComparisonTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        self.setupServer()
        self.launch()
        super.setUp()
    }

    func test_pinOffer() {
        let pinnedColumn = 0

        self.openOffersComparison()
            .checkScreenshotOfComparison(identifier: "offers_comparison_default")
            .tapOnPinButton(column: pinnedColumn)
            .checkScreenshotOfComparison(identifier: "offers_comparison_pinned")
            .scrollLeft()
            .scrollLeft()
            .checkScreenshotOfComparison(identifier: "offers_comparison_scrolled")
            .scrollUp()
            .checkScreenshotOfHeader(identifier: "offers_comparison_pinned_collapsed_header")
            .scrollDown()
            .scrollRight()
            .scrollRight()
            .tapOnPinButton(column: pinnedColumn)
            .checkScreenshotOfComparison(identifier: "offers_comparison_default")
    }

    func test_removeOffer() {
        self.mocker.server.addHandler("DELETE /user/compare/cars/favorite-1101172822-56c9ffd1") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        let removeExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/favorite-1101172822-56c9ffd1" && req.method == "DELETE"
        }

        let newOffersExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/user/compare/cars/offers" && req.method == "GET"
        }

        let steps = self.openOffersComparison().checkScreenshotOfComparison(identifier: "offers_comparison_default")

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_7_remove_first", userAuthorized: true)
        }

        steps.tapOnRemoveButton(column: 0)

        wait(for: [removeExpectation, newOffersExpectation], timeout: Self.requestTimeout)

        steps.checkScreenshotOfComparison(identifier: "offers_comparison_after_remove_first_offer")
    }

    func test_callOffer() {
        self.mocker.server.addHandler("GET /offer/cars/1101172822-56c9ffd1/phones") { (_, _) -> Response? in
            Response.okResponse(fileName: "best_offers_phones", userAuthorized: true)
        }

        let phonesExpectation = expectationForRequest { req -> Bool in
            return req.uri == "/offer/cars/1101172822-56c9ffd1/phones" && req.method == "GET"
        }

        self.openOffersComparison()
            .checkScreenshotOfComparison(identifier: "offers_comparison_default")
            .tapOnCallButton(column: 0)

        wait(for: [phonesExpectation], timeout: Self.requestTimeout)
    }

    func test_removeAllOffers() {
        let offers = ["1101256662-6a476f2e", "1100740954-07cb56c1"]

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_2", userAuthorized: true)
        }

        for offer in offers {
            self.mocker.server.addHandler("DELETE /user/compare/cars/favorite-\(offer)") { (_, _) -> Response? in
                Response.okResponse(fileName: "success", userAuthorized: true)
            }
        }

        let steps = self.openOffersComparison()

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_1", userAuthorized: true)
        }

        steps.tapOnRemoveButton(column: 0)
            .wait(for: 1)

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_0", userAuthorized: true)
        }

        steps.tapOnRemoveButton(column: 0)
            .as(ComparisonsListSteps.self)
            .checkScreenshotOfEmptyOffersFromFavorites(identifier: "comparisons_list_offers_favorites")
    }

    func test_collapseGroup() {
        self.openOffersComparison()
            .tapOnOptionSection(name: "Объявление")
            .checkScreenshotOfComparison(identifier: "offers_comparison_collapsed_section")
            .tapOnOptionSection(name: "Объявление")
            .checkScreenshotOfComparison(identifier: "offers_comparison_default")
    }

    func test_tapOnReportLink() {
        let name = "Число владельцев"
        let column = 0

        mocker.mock_reportLayoutForReport(bought: false)

        self.openOffersComparison()
            .scrollTo(name: name, columnIndex: column)
            .tapOnCell(name: name, columnIndex: column)
            .as(CarReportPreviewSteps.self)
            .waitForLoading()
            .shouldSeeContent()
    }

    func test_openOffer() {
        _ = self.openOffersComparison()
            .tapOnHeader(column: 0)
            .as(SaleCardSteps.self)
            .checkScreenLoaded()
    }

    func test_singleArchivedOffer() {
        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_archived", userAuthorized: true)
        }

        self.openOffersComparison()
            .checkScreenshotOfComparison(identifier: "offers_comparison_archived_single")
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

        self.mocker.server.addHandler("GET /user/compare/cars/models *") { (_, _) -> Response? in
            Response.okResponse(fileName: "success")
        }
        
        self.mocker.mock_user()
    }
}
