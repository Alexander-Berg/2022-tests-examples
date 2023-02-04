import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerCabinet AutoRuDealerVINSearch
final class DealerVINSearchTests: DealerBaseTest {
    enum Consts {
        static let lastVINInEmptyRequest = "JN1TANT31U0017931"
        static let correctVINPrefixLong = "JN1T"
        static let correctVINPrefixShort = "JN"
        static let incorrectVINPrefix = "INCORRECTVIN"

        static let vinMultipleSuggest = ["JN1BBNV36U0300864", "JN1BBNV36U0401017", "JN1TANT31U0017931"]
        static let vinSingleSuggest = "JN1TANT31U0017931"
        static let vinSingleOfferID = "1096920474-dae8fa6d"
    }

    // MARK: - Tests

    func test_openAndCloseVINSearch() {
        launch()
        let steps = self
            .openVINSearchScreen()
            .shouldSeeVINSearchScreen()

        steps.tapOnCancelButton()
             .shouldSeeVINSearchBar()
    }

    func test_showVINEmptyRequestResults() {
        let expectation = self.expectationForRequest(method: "GET", uri: "/user/offers/vin-suggest?vin_prefix=")

        launch()
        let steps = self.openVINSearchScreen()

        self.wait(for: [expectation], timeout: Self.requestTimeout)
        steps.scrollTo(suggest: Consts.lastVINInEmptyRequest)
    }

    func test_showVINNotFoundResults() {
        let expectationIncorrect = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/vin-suggest?vin_prefix=\(Consts.incorrectVINPrefix)"
        )
        let expectationEmpty = self.expectationForRequest(method: "GET", uri: "/user/offers/vin-suggest?vin_prefix=")

        launch()
        let steps = self.openVINSearchScreen()

        steps.tapOnSearchBarAndType(text: Consts.incorrectVINPrefix)
        self.wait(for: [expectationIncorrect], timeout: Self.requestTimeout)

        steps.shouldSeeNotFoundPlaceholder()
             .tapOnResetQueryOnPlaceholder()
        self.wait(for: [expectationEmpty], timeout: Self.requestTimeout)

        steps.scrollTo(suggest: Consts.lastVINInEmptyRequest)
    }

    func test_updateVINSuggestResults() {
        let expectationShort = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/vin-suggest?vin_prefix=\(Consts.correctVINPrefixShort)"
        )
        let expectationLong = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/vin-suggest?vin_prefix=\(Consts.correctVINPrefixLong)"
        )

        launch()
        let screen = self.openVINSearchScreen()

        screen.tapOnSearchBarAndType(text: Consts.correctVINPrefixShort)
        self.wait(for: [expectationShort], timeout: Self.requestTimeout)

        Consts.vinMultipleSuggest.forEach {
            screen.scrollTo(suggest: $0)
        }

        let tail = String(Consts.correctVINPrefixLong.dropFirst(Consts.correctVINPrefixShort.count))
        screen.tapOnSearchBarAndType(text: tail, moveCursorToEnd: true)
        self.wait(for: [expectationLong], timeout: Self.requestTimeout)

        screen.shouldSeeOnlyOneSuggest()
              .scrollTo(suggest: Consts.vinSingleSuggest)
    }

    func test_selectVINSuggest() {
        let expectationOffer = self.expectationForRequest( method: "GET", uri: "/user/offers/cars/\(Consts.vinSingleOfferID)")

        launch()
        let saleCardSteps = self.openVINSearchScreen()
            .tapOnSearchBarAndType(text: Consts.correctVINPrefixLong)
            .scrollTo(suggest: Consts.vinSingleSuggest)
            .tapOnVINSuggest(text: Consts.vinSingleSuggest)
            .as(DealerSaleCardSteps.self)

        self.wait(for: [expectationOffer], timeout: Self.requestTimeout)

        saleCardSteps.waitForLoading().shouldSeeCommonContent()
    }

    // MARK: - Private

    @discardableResult
    private func openVINSearchScreen() -> DealerVINSearchSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .shouldSeeVINSearchBar()
            .tapOnVINSearchBar()
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        self.server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/vin-suggest?vin_prefix=") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_vin_suggest_empty", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/vin-suggest?vin_prefix=\(Consts.incorrectVINPrefix)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_vin_suggest_not_found", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/vin-suggest?vin_prefix=\(Consts.correctVINPrefixLong)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_vin_suggest_prefix_JN1T", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/vin-suggest?vin_prefix=\(Consts.correctVINPrefixShort)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_vin_suggest_prefix_JN", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.vinSingleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
        }
    }
}
