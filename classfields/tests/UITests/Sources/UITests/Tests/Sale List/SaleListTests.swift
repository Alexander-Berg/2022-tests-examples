import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuSaleList AutoRuCellHelpers
final class SaleListTests: BaseTest {
    private static let searchURI = "POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
    private static let offerID = "1101101721-a355a648"

    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)

    override var appSettings: [String: Any] {
        var settings = super.appSettings
        settings["mixedListingBestPriceRegionIds"] = [1, 2, 10174]
        return settings
    }

    override func setUp() {
        super.setUp()
        mocker
            .startMock()
    }

    // MARK: - Helpers

    @discardableResult
    private func openListing() -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
    }

    @discardableResult
    private func routeToListing(offerId: String, position: SaleCardListSteps.OfferPosition = .body) -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToOffer(with: offerId, position: position, maxSwipes: 15)
    }

    private func screenshotForElement(acessibilityId: String) -> UIImage {
        return app.descendants(matching: .any).matching(identifier: acessibilityId).firstMatch.screenshot().image
    }

    private func validateElementSnapshots(accessibilityId: String, snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let screenshot = screenshotForElement(acessibilityId: accessibilityId)

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.01)
    }

    private func validateBadgeExists(text: String) {
        let badge = app.staticTexts[text].firstMatch
        XCTAssertTrue(badge.waitForExistence(timeout: 1))
    }

    // MARK: - Сначала от собственников

    func test_canSortByProvenOwner() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "sale_list_by_fresh")
        }
        launch()
        let steps = openListing().wait(for: 1)

        Step("Фильтруем по `Сначала от собственников`") {
            let sortSteps = steps.tapSortButton()
            server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=proven_owner-desc") { (_, _) -> Response? in
                Response.okResponse(fileName: "sale_list_by_fresh")
            }

            let sortByProvenOwnerExp = expectationForRequest(
                method: "POST",
                uri: "/search/cars?context=listing&page=1&page_size=20&sort=proven_owner-desc"
            )

            sortSteps
                .tapByProvenOwner()
                .wait(for: 1)
                .checkHasSelected(sorting: "Cначала от собственников")

            wait(for: [sortByProvenOwnerExp], timeout: 5)
        }

        Step("При поиске по бу не должна сброситься сортировка") {
            let sortByProvenOwnerExp = expectationForRequest(
                method: "POST",
                uri: "/search/cars?context=listing&page=1&page_size=20&sort=proven_owner-desc"
            )

            steps
                .tapConditionSegment(index: 2)
                .wait(for: 2)
                .checkHasSelected(sorting: "Cначала от собственников")

            wait(for: [sortByProvenOwnerExp], timeout: 5)
        }

        Step("А при поиске по новым - должна") {
            server.addHandler("POST /search/cars?context=listing&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
                Response.okResponse(fileName: "sale_list_by_fresh")
            }

            server.addHandler("POST /search/cars/context/premium-new-cars?page=1&page_size=10") { (_, _) -> Response? in
                Response.okResponse(fileName: "sale_list_by_fresh")
            }

            let sortByProvenOwnerExp = expectationForRequest(
                method: "POST",
                uri: "/search/cars?context=listing&page=1&page_size=20&sort=proven_owner-desc"
            )
            sortByProvenOwnerExp.isInverted = true

            steps
                .tapConditionSegment(index: 1)
                .wait(for: 1)
                .checkHasSelected(sorting: "По актуальности")

            wait(for: [sortByProvenOwnerExp], timeout: 5)
        }

        app.launchArguments.append("--resetDefaults")
    }

    func test_bestPriceMixedListing() {
        advancedMockReproducer.setup(server: server, mockFolderName: "GetBestOffer")
        let requestExpectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/match-applications".lowercased()
        }
        launch()
        let steps = MainSteps(context: self)
            .openSearchHistory("Все марки автомобилей")
            .tapBestPriceBanner()
            .tapSendButton()
            .checkErrorAlert(message: "Заполните марку автомобиля")
            .pickMark("BMW")
            .tapSendButton()

        wait(for: [requestExpectation], timeout: 5)
        steps.validateSuccessHUD()
    }

    func test_bestPriceMixedListingWithMarkModel() {
        advancedMockReproducer.setup(server: server, mockFolderName: "GetBestOffer")
        server.forceLoginMode = .forceLoggedIn
        let requestExpectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/match-applications".lowercased()
        }
        launch()
        let steps = MainSteps(context: self)
            .openSearchHistory("BMW 3 серии")
            .tapBestPriceBanner()
            .validatePicked(mark: "BMW", model: "3 серии")
            .tapSendButton()

        wait(for: [requestExpectation], timeout: 5)
        steps.validateSuccessHUD()
    }

    func test_emptyListing() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "sale_list_empty")
        }
        launch()

        let steps = openListing()
        steps.validateEmptyResultsItem()
    }

    // MARK: -

    func test_proAutoButtonInSnippet() {
        Step("Отчет проавто в крутилке") {}
        let sellerLocation = Auto_Api_DistanceToTarget.with { (distance: inout Auto_Api_DistanceToTarget) in
            distance.distance = 333
            distance.regionInfo.genitive = "Зеленогорска"
        }
        mocker
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1, distanceToOffer: sellerLocation)])

        launch()

        openFilters()
            .tapRegionField()
            .toggleExpandRegion(index: 1)
            .toggleSelectCity(regionIndex: 1, index: 1)
            .wait(for: 1)
            .confirmFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToOffer(with: "400_0", position: .body)
            .swipeGalleryLeft(offer: "400_0").swipeGalleryLeft(offer: "400_0")
            .swipeGalleryLeft(offer: "400_0").swipeGalleryLeft(offer: "400_0")
            .swipeGalleryLeft(offer: "400_0").swipeGalleryLeft(offer: "400_0")
            .tapGalleryReportButton().checkHasNoGallery()
    }

    func test_complain() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            var listingResponse: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
            var offer = listingResponse.offers.first!
            offer.id = Self.offerID
            listingResponse.offers[0] = offer

            return Response.okResponse(message: listingResponse)
        }

        server.addMessageHandler("POST /offer/cars/\(Self.offerID)/complaints") { _, _ in
            Auto_Api_SuccessResponse.with { response in
                response.status = .success
            }
        }

        let complainExpectation = expectationForRequest { req -> Bool in
            guard let data = req.messageBody,
                  let model = try? Auto_Api_ComplaintRequest(jsonUTF8Data: data),
                  model.placement == "serp_offers_item",
                  req.uri.lowercased() == "/offer/cars/\(Self.offerID)/complaints".lowercased(),
                  req.method == "POST"
            else { return false }

            return true
        }

        launch()

        openListing()
            .focus(on: .offerCell(.custom(Self.offerID), .body), ofType: .offerSnippet) { snippet in
                snippet.tap(.moreButton)
            }
            .should(provider: .actionsMenuPopup, .exist)
            .focus { popup in
                popup.tap(.complain)
            }
            .should(provider: .complainMenuPopup, .exist)
            .focus { popup in
                popup.tap(.didSale)
            }

        wait(for: [complainExpectation], timeout: 10)
    }

    func test_analyticsAddAndRemoveSearchFavorites() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .mock_addToFavouriteSubscriptions()
            .mock_searchCars(newCount: 5, usedCount: 5)

        launch()
        openListing()
            .focus(on: .addToSavedSearchIcon) { $0
                .tap()
            }
            .tap(.okButton)
            .shouldEventBeReported("Сохранить поиск. save_search_button. all_cars. save_search")
            .focus(on: .savedSearchIcon) { $0
                .tap()
            }
            .shouldEventBeReported("Сохранить поиск. save_search_button. all_cars. delete_search")
    }

    func test_analyticsSearchSubscriptionBanner() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .mock_addToFavouriteSubscriptions()
            .mock_searchCars(newCount: 1, usedCount: 1)

        launch()
        openListing()
            .scroll(to: .searchSubscriptionButton)
            .shouldEventBeReported("Сохранить поиск. native_banner. all_cars. show_banner")
            .focus(on: .searchSubscriptionButton) { $0
                .tap()
            }
            .tap(.okButton)
            .shouldEventBeReported("Сохранить поиск. native_banner. all_cars. click_banner")
    }

    // MARK: -

    private func openFilters() -> FiltersSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
    }
}
