import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuComparisonsList AutoRuComparison AutoRuServices
final class ComparisonsListTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        self.setupServer()
        super.setUp()
    }
    
    func test_unauthorizedUser() {
        _ = self.mocker.setForceLoginMode(.forceLoggedOut)

        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_favorites_empty", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_0", userAuthorized: true)
        }

        self.launch()

        self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .shouldSeeUnauthorizedPlaceholder()
    }

    func test_emptyFavorites() {
        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_favorites_empty", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_0", userAuthorized: true)
        }

        self.launch()

        self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .waitForLoading()
            .checkScreenshotOfEmptyOffers(identifier: "comparisons_list_offers_empty")
            .tapOnListingButton()
            .as(SaleCardListSteps.self)
            .checkSegmentedControl()
    }

    func test_offersAddFromFavoritesLessThan3() {
        // В избранном < 3 офферов => не можем предложить юзеру добавление из избранного
        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "comparison_favorites_less_than_three", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_0", userAuthorized: true)
        }

        self.launch()

        self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .waitForLoading()
            .checkScreenshotOfEmptyOffers(identifier: "comparisons_list_offers_empty")
    }

    func test_offersAddFromFavorites() {
        let favoritedOffers = [
            "favorite-1100907874-72e3c5e5", "favorite-1100924706-c7722662", "favorite-1100879554-1121e872",
            "favorite-1093278040-65e493da", "favorite-1093061882-6e68bf4f", "favorite-1092206512-012acda2"
        ]

        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "comparison_favorites", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_0", userAuthorized: true)
        }

        self.mocker.server.addHandler("PUT /user/compare/cars/\(favoritedOffers.joined(separator: ","))") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.launch()

        let expectationFavorites = expectationForRequest { req -> Bool in
            req.uri == "/user/compare/cars/\(favoritedOffers.joined(separator: ","))" && req.method == "PUT"
        }

        let steps = self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .waitForLoading()
            .checkScreenshotOfEmptyOffersFromFavorites(identifier: "comparisons_list_offers_favorites")

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_6", userAuthorized: true)
        }

        steps.tapEmptyOffersFromFavorites()
        wait(for: [expectationFavorites], timeout: Self.requestTimeout)

        steps.as(OffersComparisonSteps.self).checkComparisonScreenTitle()
    }

    func test_openOffersComparison() {
        self.checkOffersInComparison(count: 6)
            .tapOffersComparison()
            .as(OffersComparisonSteps.self)
            .checkComparisonScreenTitle()
    }

    func test_offersInComparison1() {
        self.checkOffersInComparison(count: 1)
            .tapOnAddOfferToComparisonButton()
            .as(SaleCardListSteps.self)
            .checkSegmentedControl()
    }

    func test_offersInComparison2() {
        self.checkOffersInComparison(count: 2)
    }

    func test_offersInComparison3() {
        self.checkOffersInComparison(count: 3)
    }

    func test_offersInComparison4() {
        self.checkOffersInComparison(count: 4)
    }

    func test_offersInComparison5() {
        self.checkOffersInComparison(count: 5)
    }

    func test_offersInComparison6() {
        self.checkOffersInComparison(count: 6)
    }

    func test_offersInComparison7AndMore() {
        self.checkOffersInComparison(count: 7)
    }

    // MARK: - Private
    @discardableResult
    private func checkOffersInComparison(count: Int) -> ComparisonsListSteps {
        self.mocker.server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_favorites", userAuthorized: true)
        }

        self.mocker.server.addHandler("GET /user/compare/cars/offers") { (_, _) -> Response? in
            Response.okResponse(fileName: "comparison_offers_\(count)", userAuthorized: true)
        }

        self.launch()

        return self.mainSteps
            .openFavoritesTab()
            .tapSegment(at: .comparison)
            .as(ComparisonsListSteps.self)
            .waitForLoading()
            .checkScreenshotOfOffers(identifier: "comparisons_list_offers_\(count)")
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

        self.mocker.server.addHandler("GET /user/compare/cars/models *") { (_, _) -> Response? in
            Response.okResponse(fileName: "success")
        }
    }
}
