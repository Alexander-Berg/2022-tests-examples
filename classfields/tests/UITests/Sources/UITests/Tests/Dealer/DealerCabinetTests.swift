import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerCabinet
final class DealerCabinetTests: DealerBaseTest {
    enum Consts {
        static let paginationLastOffer = "1096920488-18d1b0dc"
        static let singleOfferID = "1096859248-8f2c7274"
        static let vinResolutionOkOfferID = "1096920488-18d1b001"
        static let vinResolutionErrorOfferID = "1096920488-18d1b002"
        static let vinResolutionUnknownOfferID = "1096920488-18d1b003"
        static let vinResolutionInvalidOfferID = "1096920488-18d1b004"
        static let vinResolutionNotMatchedPlateOfferID = "1096920488-18d1b005"
        static let vinResolutionUntrustedOfferID = "1096920488-18d1b006"
        static let vinResolutionInProgressOfferID = "1096920488-18d1b007"
        static let vinResolutionUndefinedOfferID = "1096920488-18d1b008"
        static let emptyChartsOfferID = "1096859248-8f2c7271"
        static let fullChartsOfferID = "1096859248-8f2c7270"

        static let truckOfferID = "16464818-f006b2e1"
        static let motoOfferID = "3467634-d0db78d6"
    }

    private static let insetsWithoutFilterAndTabBar = UIEdgeInsets(top: 0, left: 0, bottom: 120, right: 0)

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["MOCKED_DATE"] = "2020-10-01 15:00:00+03:00"
        return env
    }

    // MARK: - Tests

    func test_listingEmpty() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_empty", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .checkEmptyPlaceholderNoOffersWithoutFilters()
            .checkElementsVisibleWhenEmptyListing()
            .tapOnAddOfferButtonOnPlaceholder()
            .waitForLoading()
            .shouldSeeCommonContent(
                options: [
                    "Легковые новые", "Легковые с пробегом",
                    "Комтранс новые", "Комтранс с пробегом",
                    "Мото новые", "Мото с пробегом"
                ]
            )
    }

    func test_listingPagination() {
        self.server.addHandler("GET /user/offers/all?page=1&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_page_1", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all?page=2&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true") { (_, _) -> Response? in
            return nil
        }

        let secondPageExpectation: () -> XCTestExpectation = {
            self.expectationForRequest(
                method: "GET",
                uri: "/user/offers/all?page=2&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true"
            )
        }

        let secondPageExpectation1 = secondPageExpectation()
        let secondPageExpectation2 = secondPageExpectation()

        launch()
        let steps = self.openListingAndWaitForLoading()
            .scrollToBottom()

        self.wait(for: [secondPageExpectation1], timeout: Self.requestTimeout)

        steps.tapOnPaginationRetryButton()

        self.server.addHandler("GET /user/offers/all?page=2&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_page_2", userAuthorized: true)
        }

        self.wait(for: [secondPageExpectation2], timeout: Self.requestTimeout)

        steps.scrollToOfferTitle(offerID: Consts.paginationLastOffer)
    }

    func test_listingUpdateSorting() {
        self.server.addHandler("GET /user/offers/all?page=1&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }

        launch()
        let steps = self.openListingAndWaitForLoading()

        for sortingOption in DealerCabinetSortingScreen.SortingType.allCases {
            self.server.addHandler("GET /user/offers/all?page=1&page_size=20&sort=\(sortingOption.queryParam)&status=ACTIVE&with_daily_counters=true") { (_, _) -> Response? in
                Response.okResponse(fileName: "dealer_offers_sorting_date", userAuthorized: true)
            }

            let expectation = self.expectationForRequest(
                method: "GET",
                uri: "/user/offers/all?page=1&page_size=20&sort=\(sortingOption.queryParam)&status=ACTIVE&with_daily_counters=true"
            )

            steps.tapOnSortButton().tap(on: sortingOption)

            self.wait(for: [expectation], timeout: Self.requestTimeout)
        }
    }

    func test_snippetVINResolution() {

        mocker.mock_reportLayoutForOffer(bought: false)
        mocker.mock_reportLayoutForReport(bought: false)
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_vin_resolution", userAuthorized: true)
        }

        let offers: [(id: String, name: String, canOpenPreview: Bool, canOpenPopup: Bool, snapshot: String)] = [
            (Consts.vinResolutionOkOfferID, "OK", true, false, "vin_resolution_green"),
            (Consts.vinResolutionErrorOfferID, "ERROR", true, false, "vin_resolution_red"),
            (Consts.vinResolutionUnknownOfferID, "UNKNOWN", true, false, "vin_resolution_gray"),
            (Consts.vinResolutionInvalidOfferID, "INVALID", false, true, "vin_resolution_red"),
            (Consts.vinResolutionNotMatchedPlateOfferID, "NOT_MATCHED_PLATE", true, false, "vin_resolution_red"),
            (Consts.vinResolutionUntrustedOfferID, "UNTRUSTED", false, true, "vin_resolution_gray"),
            (Consts.vinResolutionInProgressOfferID, "IN_PROGRESS", false, false, "vin_resolution_default"),
            (Consts.vinResolutionUndefinedOfferID, "UNDEFINED", false, false, "vin_resolution_default")
        ]

        launch()
        let steps = self.openListingAndWaitForLoading()

        let data = Bundle.resources.url(forResource: "dealer_offer_active", withExtension: "json").flatMap {
            try? Data(contentsOf: $0)
        } ?? Data()
        var mockOffer = try! Auto_Api_OfferResponse(jsonUTF8Data: data)

        for offer in offers {
            self.server.addHandler("GET /user/offers/CARS/\(offer.id)") { (_, _) in
                mockOffer.offer.id = offer.id
                return Response.responseWithStatus(body: (try? mockOffer.jsonUTF8Data()) ?? Data(), userAuthorized: true)
            }

            self.server.addHandler("GET /carfax/offer/cars/\(offer.id)/raw *") { (_, _) in
                Response.okResponse(fileName: "dealer_sale_card_carfax_free", userAuthorized: true)
            }
            Step("Проверяем тап по резолюции \(offer.name): ожидается \(offer.canOpenPreview ? "открытие превью" : (offer.canOpenPopup ? "открытие попапа" : "переход в карточку"))") {
                let expectation: XCTestExpectation
                if offer.canOpenPreview {
                    expectation = self.expectationForRequest(method: "GET", uri: "/ios/makeXmlForReport?offer_id=\(offer.id)")
                } else if offer.canOpenPopup {
                    expectation = self.expectationForRequest(method: "GET", uri: "/carfax/offer/cars/\(offer.id)/raw")
                } else {
                    expectation = self.expectationForRequest(method: "GET", uri: "/user/offers/cars/\(offer.id)")
                }

                let secondSteps = steps.scrollToVINResolution(offerID: offer.id)
                     .checkSnippetVINResolutionSnapshot(offerID: offer.id, identifier: offer.snapshot)
                     .tapOnVINResolution(offerID: offer.id)

                self.wait(for: [expectation], timeout: Self.requestTimeout)

                if offer.canOpenPreview {
                    secondSteps.as(CarReportPreviewSteps.self)
                        .waitForLoading()
                        .shouldSeeContent()
                        .tapOnBackButton()
                } else if offer.canOpenPopup {
                    secondSteps.as(DealerCabinetVINResolutionSteps.self).shouldSeePopup().close()
                } else {
                    secondSteps.as(DealerSaleCardSteps.self).shouldSeeCommonContent().tapOnBackButton()
                }
            }
        }
    }

    func test_snippetActivateOfferButton() {
        self.test_offerActivation {
            self.openListingAndWaitForLoading()
                .scrollToActionButton(offerID: Consts.singleOfferID)
                .tapOnActionButton(offerID: Consts.singleOfferID, type: .activate)
        }
    }

    func test_snippetActivateOfferMenu() {
        self.test_offerActivation {
            self.openListingAndWaitForLoading()
                .tapOnMoreButton(offerID: Consts.singleOfferID)
                .tapOn(button: .activate)
        }
    }

    func test_snippetEditOfferButton() {
        self.test_offerEdit {
            self.openListingAndWaitForLoading()
                .scrollToActionButton(offerID: Consts.singleOfferID)
                .tapOnActionButton(offerID: Consts.singleOfferID, type: .edit)
        }
    }

    func test_snippetEditOfferMenu() {
        self.test_offerEdit {
            self.openListingAndWaitForLoading()
                .scrollToMoreButton(offerID: Consts.singleOfferID)
                .tapOnMoreButton(offerID: Consts.singleOfferID)
                .tapOn(button: .edit)
        }
    }

    func test_snippetShareOffer() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .scrollToMoreButton(offerID: Consts.singleOfferID)
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .tapOn(button: .share)
            .exist(selector: "ActivityListView")
    }

    func test_snippetDeactivateOffer() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        let expectationOfferDeactivation = self.expectationForRequest(
            method: "POST",
            uri: "/user/offers/cars/\(Consts.singleOfferID)/hide"
        )
        let expectationOfferUpdate = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/cars/\(Consts.singleOfferID)"
        )

        launch()
        self.openListingAndWaitForLoading()
            .scrollToMoreButton(offerID: Consts.singleOfferID)
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .tapOn(button: .deactivate)

        self.wait(for: [expectationOfferDeactivation, expectationOfferUpdate], timeout: Self.requestTimeout)
    }

    func test_snippetDeleteOfferMenu() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        launch()
        let steps = self.openListingAndWaitForLoading().scrollToMoreButton(offerID: Consts.singleOfferID)
        self.test_offerDeletion {
            steps.tapOnMoreButton(offerID: Consts.singleOfferID).tapOn(button: .delete)
        }
    }

    func test_snippetDeleteOfferButton() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        launch()
        let steps = self.openListingAndWaitForLoading().scrollToActionButton(offerID: Consts.singleOfferID)
        self.test_offerDeletion {
            steps.tapOnActionButton(offerID: Consts.singleOfferID, type: .delete).as(DealerOfferActionSteps.self)
        }
    }

    func test_snippetCharts() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_charts", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .checkSnippetChartSnapshot(offerID: Consts.emptyChartsOfferID, identifier: "snippet_empty_charts")
            .checkSnippetChartSnapshot(offerID: Consts.fullChartsOfferID, identifier: "snippet_full_charts", scrollTo: true)
    }

    func test_snippetActive() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .tapOnActionButton(offerID: Consts.singleOfferID, type: .expand)
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_active_expanded")
            .tapOnActionButton(offerID: Consts.singleOfferID, type: .collapse)
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_active_collapsed")
            .scrollToMoreButton(offerID: Consts.singleOfferID)
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .containsOnly(buttons: [.edit, .share, .delete, .deactivate])
    }

    func test_snippetInactive() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_inactive", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/cars/\(Consts.singleOfferID) *") { (_, _) in Response.notFoundResponse() }

        let expectation = self.expectationForRequest(method: "GET", uri: "/user/offers/cars/\(Consts.singleOfferID)")

        launch()
        let steps = self.openListingAndWaitForLoading()
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_inactive")
            .tapOnVINResolution(offerID: Consts.singleOfferID)

        self.wait(for: [expectation], timeout: Self.requestTimeout)

        steps.tapOnBackButton()
            .as(DealerCabinetSteps.self)
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .containsOnly(buttons: [.edit, .delete, .activate])
    }

    func test_snippetNeedActivation() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_need_activation", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_need_activation")
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .containsOnly(buttons: [.deactivate, .delete])
    }

    func test_snippetBannedCanNotEdit() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_banned_can_not_edit")
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .containsOnly(buttons: [.delete])
    }

    func test_snippetBannedCanEdit() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_edit", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_banned_can_edit")
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .containsOnly(buttons: [.edit, .delete])
    }

    func test_addNewPickerAllOptions() {
        launch()
        self.openListingAndWaitForLoading()
            .tapOnAddButton()
            .waitForLoading()
            .shouldSeeCommonContent(
                options: [
                    "Легковые новые", "Легковые с пробегом",
                    "Комтранс новые", "Комтранс с пробегом",
                    "Мото новые", "Мото с пробегом"
                ]
            )
    }

    func test_addNewPickerNewAutoOnly() {
        self.server.addHandler("GET /dealer/campaigns") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_new_auto_only_campaigns", userAuthorized: true)
        }

        launch()
        self.openListingAndWaitForLoading()
            .tapOnAddButton()
            .waitForLoading()
            .shouldSeeCommonContent(options: ["Легковые новые"])
    }

    func test_openFormAndUpdateOffer() {
        var priceChanged: Bool = false
        let updPriceExp = expectation(description: "Должны были обновить цену на форме")
        let offerReloadedExp = expectation(description: "После закрытия формы должны были запросить обновленный оффер")

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.singleOfferID)/edit") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_edit_active_offer_resp", userAuthorized: true)
        }

        self.server.addHandler("GET /user/draft/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_cabinet_active_offer_draft", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            if priceChanged {
                offerReloadedExp.fulfill()
                return Response.okResponse(fileName: "dealer_cabinet_active_offer_updated_price_root", userAuthorized: true)
            } else {
                return Response.okResponse(fileName: "dealer_cabinet_active_offer_root", userAuthorized: true)
            }
        }

        self.server.addHandler("PUT /user/draft/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_active_offer_updated_price_draft", userAuthorized: true)
        }

        self.server.addHandler("POST /user/draft/CARS/\(Consts.singleOfferID)/publish") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_active_offer_updated_price_draft", userAuthorized: true)
        }

        launch()
        let steps = openListingAndWaitForLoading()

        steps
            .scrollToMoreButton(offerID: Consts.singleOfferID)
            .tapOnMoreButton(offerID: Consts.singleOfferID)
            .tapOnEdit()

        let formSteps = onDealerFormScreen()
        let formPicker = formSteps.onDealerFormScreen()
        let priceField = formPicker.priceField

        formPicker.scrollableElement.swipe(.up) { (numSwipes) -> Bool in
            if priceField.exists || numSwipes >= 10 {
                return false
            }
            return true
        }

        priceField
            .shouldExist().tap()

        if let currentText = self.app.textFields.firstMatch.value as? String {
            let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: currentText.count)
            priceField.typeText(deleteString)
            priceField.typeText("2500000")
            priceChanged = true
            formPicker.scrollableElement.gentleSwipe(.up)
            priceChanged = true
            formPicker.publishButton.tap()
            updPriceExp.fulfill()
        }

        wait(for: [updPriceExp], timeout: 5)

        steps
            .onDealerCabinetScreen()
            .snippetDescription(offerID: Consts.singleOfferID)
            .staticTexts["2 500 000 ₽"]
            .shouldExist()

        wait(for: [offerReloadedExp], timeout: 3)
    }

    func test_offerEditTrucksHasMarkModelCategoryDisabled() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_one_truck", userAuthorized: true)
        }

        self.server.addHandler("GET /user/draft/TRUCKS/\(Consts.truckOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_trucks_active", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/TRUCKS/\(Consts.truckOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_trucks_active", userAuthorized: true)
        }

        self.server.addHandler("PUT /user/draft/TRUCKS/\(Consts.truckOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_trucks_active", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/TRUCKS/suggest *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_trucks_empty_draft_suggest", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/TRUCKS/\(Consts.truckOfferID)/edit") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_edit_single_truck", userAuthorized: true)
        }

        launch()
        let steps = openListingAndWaitForLoading()

        steps
            .scrollToMoreButton(offerID: Consts.truckOfferID)
            .tapOnMoreButton(offerID: Consts.truckOfferID)
            .tapOnEdit()

        let formSteps = steps.wait(for: 1).as(DealerFormSteps.self)
        let formPicker = formSteps.onDealerFormScreen()

        Step("Проверяем, что пикер `Марка` некликабелен") {
            formPicker.trucksMarkField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }

        Step("Проверяем, что пикер `Модель` некликабелен") {
            formPicker.trucksModelField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }

        Step("Проверяем, что пикер `Категория` некликабелен") {
            formPicker.trucksCategoryField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoCategoryPicker(for: .trucks)
        }
    }

    func test_offerEditMotoHasMarkModelCategoryDisabled() {
        self.server.addHandler("GET /user/offers/all *") { _, _ -> Response? in
            Response.okResponse(fileName: "dealer_offers_one_moto", userAuthorized: true)
        }

        self.server.addHandler("GET /user/draft/MOTO/\(Consts.motoOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_moto_active", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/MOTO/\(Consts.motoOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_moto_active", userAuthorized: true)
        }

        self.server.addHandler("PUT /user/draft/MOTO/\(Consts.motoOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_moto_active", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/MOTO/suggest *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_moto_empty_draft_suggest", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/MOTO/\(Consts.motoOfferID)/edit") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_edit_single_moto", userAuthorized: true)
        }

        launch()
        let steps = openListingAndWaitForLoading()

        steps
            .scrollToMoreButton(offerID: Consts.motoOfferID)
            .tapOnMoreButton(offerID: Consts.motoOfferID)
            .tapOnEdit()

        let formSteps = steps.wait(for: 1).as(DealerFormSteps.self)
        let formPicker = formSteps.onDealerFormScreen()

        Step("Проверяем, что пикер `Марка` некликабелен") {
            formPicker.motoMarkField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }

        Step("Проверяем, что пикер `Модель` некликабелен") {
            formPicker.motoModelField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }

        Step("Проверяем, что пикер `Категория` некликабелен") {
            formPicker.motoCategoryField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoCategoryPicker(for: .moto)
        }
    }

    // MARK: - Private

    private func test_offerEdit(_ action: () -> BaseSteps) {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_edit", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.singleOfferID)/edit") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_cabinet_edit_active_offer_resp", userAuthorized: true)
        }

        self.server.addHandler("GET /user/draft/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_non_empty_draft", userAuthorized: true)
        }

        let expectationEdit = self.expectationForRequest(method: "POST", uri: "/user/offers/CARS/\(Consts.singleOfferID)/edit")
        let expectationDraft = self.expectationForRequest(method: "GET", uri: "/user/draft/CARS/\(Consts.singleOfferID)")

        launch()
        let steps = action()

        self.wait(for: [expectationEdit, expectationDraft], timeout: Self.requestTimeout)

        steps.as(DealerFormSteps.self)
            .waitForLoading()
            .checkForFormScreen()
    }

    private func test_offerActivation(_ action: () -> Void) {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_inactive", userAuthorized: true)
        }

        let expectationOfferActivation = self.expectationForRequest(
            method: "POST",
            uri: "/user/offers/cars/\(Consts.singleOfferID)/activate"
        )
        let expectationOfferUpdate = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/cars/\(Consts.singleOfferID)"
        )

        launch()
        action()

        self.wait(for: [expectationOfferActivation, expectationOfferUpdate], timeout: Self.requestTimeout)
    }

    func test_offerDeletion(_ action: () -> DealerOfferActionSteps) {
        let expectationOfferDeletion = self.expectationForRequest(
            method: "DELETE",
            uri: "/user/offers/cars/\(Consts.singleOfferID)"
        )

        let expectationWithoutDeletion = self.expectationForRequest(
            method: "DELETE",
            uri: "/user/offers/cars/\(Consts.singleOfferID)"
        )
        expectationWithoutDeletion.isInverted = true

        action().tapOnConfirmationCancel()

        self.wait(for: [expectationWithoutDeletion], timeout: Self.requestTimeout)

        action().tapOnConfirmationDelete()

        self.wait(for: [expectationOfferDeletion], timeout: Self.requestTimeout)
    }

    func test_addPanoramaBanner() {
        launch()
        self.openListingAndWaitForLoading()
            .log("Проверяем появление и функционал баннера добавления панорамы для диллера.")
            .tap(.addPanoramaDealerBannerLink)
            .should(provider: .webViewPicker, .exist)
            .focus { webPicker in
                webPicker.tap(.closeButton)
            }
            .tap(.addPanoramaDealerBannerCloseButton)
            .should(.addPanoramaDealerBanner, .be(.hidden))
    }

    @discardableResult
    private func openListingAndWaitForLoading() -> DealerCabinetSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
    }

    @discardableResult
    private func onDealerFormScreen() -> DealerFormSteps {
        return DealerFormSteps(context: self)
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

        self.server.addHandler("POST /user/offers/CARS/\(Consts.singleOfferID)/activate") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.singleOfferID)/hide") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("DELETE /user/offers/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("GET /dealer/campaigns") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_all_campaigns", userAuthorized: true)
        }
    }
}
