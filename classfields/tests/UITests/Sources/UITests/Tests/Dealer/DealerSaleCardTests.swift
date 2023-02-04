import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerSaleCard
final class DealerSaleCardTests: DealerBaseTest {
    enum Consts {
        static let activeOfferID = "1096920474-dae8fa6d"
        static let inactiveOfferID = "1096920476-6bea0723"
        static let bannedOfferID = "1096859248-8f2c7274"
        static let needActivationOfferID = "1096859248-8f2c7274"

        static let longAgoCreatedOfferID = "1096920474-dae8f000"
        static let twoDaysAgoCreatedOfferID = "1096920474-dae8f001"

        static let oldPrice = 1300000
        static let oldPriceFormatted = "1 300 000 ₽"
        static let newPriceFirst = 1000000
        static let newPriceSecond = 2500000
        static let newPriceSecondFormatted = "2 500 000 ₽"
    }

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["MOCKED_DATE"] = "2020-05-12 15:00:00+03:00"
        return env
    }

    // MARK: - Tests

    func test_saleCardActive() {
        launch()
        self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .checkSaleCardTopBlocksSnapshot(identifier: "dealer_sale_card_top_active")
            .shouldSeeAllBlocks()
            .shouldSeeEditButton()
            .tapOnMoreButton()
            .containsOnly(buttons: [.deactivate, .share, .delete, .edit])
    }

    func test_saleCardInactive() {
        launch()
        self.openSaleCardScreen(offerID: Consts.inactiveOfferID)
            .checkSaleCardTopBlocksSnapshot(identifier: "dealer_sale_card_top_inactive")
            .shouldSeeAllBlocks()
            .shouldSeeActivateButton()
            .tapOnMoreButton()
            .containsOnly(buttons: [.activate, .delete, .edit])
    }

    func test_saleCardBannedCanEdit() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_edit", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.bannedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_banned_can_edit", userAuthorized: true)
        }

        launch()
        self.openSaleCardScreen(offerID: Consts.bannedOfferID)
            .checkSaleCardTopBlocksSnapshot(identifier: "dealer_sale_card_top_banned")
            .shouldSeeAllBlocks()
            .shouldSeeEditButton()
            .tapOnMoreButton()
            .containsOnly(buttons: [.delete, .edit])
    }

    func test_saleCardBannedCanNotEdit() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.bannedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_banned_can_not_edit", userAuthorized: true)
        }

        launch()
        self.openSaleCardScreen(offerID: Consts.bannedOfferID)
            .checkSaleCardTopBlocksSnapshot(identifier: "dealer_sale_card_top_banned")
            .shouldSeeAllBlocks()
            .shouldSeeDeleteButton()
            .tapOnMoreButton()
            .containsOnly(buttons: [.delete])
    }

    func test_saleCardNeedActivation() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_need_activation", userAuthorized: true)
        }

        launch()
        self.openSaleCardScreen(offerID: Consts.needActivationOfferID)
            .checkSaleCardTopBlocksSnapshot(identifier: "dealer_sale_card_top_need_activated")
            .shouldSeeAllBlocks()
            .shouldSeeEditButton()
            .tapOnMoreButton()
            .containsOnly(buttons: [.delete, .edit, .deactivate])
    }

    func test_viewsChartNoData() {
        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_long_ago", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()

        validateChartSnapshots(
            steps,
            withTap: false,
            chartIdentifier: "dealer_views_chart_no_data",
            phonesChartIdentifier: "dealer_phones_chart_no_data"
        )
    }

    func test_viewsChartLongAgo() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_stat_charts", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.longAgoCreatedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_long_ago", userAuthorized: true)
        }

        let testCases: [(stub: String, snapshotViews: String, snapshotPhoneViews: String, withTap: Bool)] = [
            ("dealer_sale_stat_long_ago", "dealer_views_chart_long_ago", "dealer_phones_chart_long_ago", false),
            ("dealer_sale_stat_long_ago_fresh", "dealer_views_chart_long_ago_fresh", "dealer_phones_chart_long_ago_fresh", false),
            ("dealer_sale_stat_long_ago", "dealer_views_chart_long_ago_tap", "dealer_phones_chart_long_ago_tap", true)
        ]

        launch()
        let steps = self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        for testCase in testCases {
            self.server.addHandler("GET /user/offers/CARS/\(Consts.longAgoCreatedOfferID)/stats?from=2020-04-14&to=2020-05-12") { (_, _) -> Response? in
                Response.okResponse(fileName: testCase.stub, userAuthorized: true)
            }

            let expectation = self.expectationForRequest(
                method: "GET",
                uri: "/user/offers/CARS/\(Consts.longAgoCreatedOfferID)/stats?from=2020-04-14&to=2020-05-12"
            )

            let saleCardSteps = steps
                .scrollToOfferTitle(offerID: Consts.longAgoCreatedOfferID)
                .tapOnOfferSnippet(offerID: Consts.longAgoCreatedOfferID)

            self.wait(for: [expectation], timeout: Self.requestTimeout)

            validateChartSnapshots(
                saleCardSteps,
                withTap: testCase.withTap,
                chartIdentifier: testCase.snapshotViews,
                phonesChartIdentifier: testCase.snapshotPhoneViews
            )

            saleCardSteps.tapOnBackButton()
        }

        steps
            .scrollToOfferTitle(offerID: Consts.longAgoCreatedOfferID)
            .tapOnOfferSnippet(offerID: Consts.longAgoCreatedOfferID)
            .waitForLoading()
            .scrollToCounters()
            .checkCountersSnapshot(identifier: "dealer_counters")
    }

    func test_viewsChartTwoDaysAgo() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_stat_charts", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.twoDaysAgoCreatedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_two_days_ago", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.twoDaysAgoCreatedOfferID)/stats?from=2020-05-10&to=2020-05-17") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_sale_stat_two_days_ago", userAuthorized: true)
        }

        let expectation = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/CARS/\(Consts.twoDaysAgoCreatedOfferID)/stats?from=2020-05-10&to=2020-05-17"
        )

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.twoDaysAgoCreatedOfferID).waitForLoading()

        self.wait(for: [expectation], timeout: Self.requestTimeout)

        validateChartSnapshots(
            steps,
            withTap: false,
            chartIdentifier: "dealer_views_chart_two_days_ago",
            phonesChartIdentifier: "dealer_phones_chart_two_days_ago"
        )
    }

    func test_updatePriceFromSaleCardSuccess() {
        launch()
        let picker = self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .checkPrice(value: Consts.oldPriceFormatted)
            .tapOnUpdatePriceButton()
            .shouldSeePicker()
            .shouldBeFocusedOnEditField()
            .typePrice(value: "\(Consts.newPriceFirst)")
            .tapOnResetAndCheckEditField()
            .typePrice(value: "\(Consts.newPriceSecond)")

        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active_updated_price", userAuthorized: true)
        }

        let expectationPriceUpdate = self.expectationForRequest(
            method: "POST",
            uri: "/user/offers/cars/\(Consts.activeOfferID)/price"
        )
        let expectationOfferUpdate = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/cars/\(Consts.activeOfferID)"
        )

        let card = picker.tapOnDone()

        self.wait(for: [expectationPriceUpdate, expectationOfferUpdate], timeout: Self.requestTimeout)

        card.checkPrice(value: Consts.newPriceSecondFormatted)
    }

    func test_activateOfferMoreButton() {
        self.test_activateOffer { steps in
            steps.tapOnMoreButton().tapOn(button: .activate)
        }
    }

    func test_activateOfferActionButton() {
        self.test_activateOffer { steps in
            steps.tapOnActivationButton()
        }
    }

    func test_shareOfferMoreButton() {
        launch()
        let picker = self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .tapOnMoreButton()

        picker.tapOn(button: .share)
            .exist(selector: "ActivityListView")
    }

    func test_deactivateOfferMoreButton() {
        launch()
        let picker = self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .tapOnMoreButton()

        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_inactive", userAuthorized: true)
        }

        let expectationOfferDeactivation = self.expectationForRequest(
            method: "POST",
            uri: "/user/offers/cars/\(Consts.activeOfferID)/hide"
        )
        let expectationOfferUpdate = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/cars/\(Consts.activeOfferID)"
        )

        picker.tapOn(button: .deactivate)

        self.wait(for: [expectationOfferDeactivation, expectationOfferUpdate], timeout: Self.requestTimeout)
    }

    func test_deleteOfferMoreButton() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.bannedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_banned_can_not_edit", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.bannedOfferID).waitForLoading()

        self.test_deleteOffer {
            steps.tapOnMoreButton().tapOn(button: .delete)
        }
    }

    func test_deleteOfferActionButton() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.bannedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_banned_can_not_edit", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.bannedOfferID).waitForLoading()

        self.test_deleteOffer {
            steps.tapOnDeleteButton()
        }
    }

    func test_editOfferMoreButton() {
        launch()
        self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .tapOnMoreButton()
            .tapOn(button: .edit)
            .as(DealerFormSteps.self)
            .waitForLoading()
            .checkForFormScreen()
    }

    func test_editOfferActionButton() {
        launch()
        self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .tapOnEditButton()
            .waitForLoading()
            .checkForFormScreen()
    }

    func test_editOfferEmptyImage() {
        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active_no_images", userAuthorized: true)
        }

        launch()
        self.openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()
            .tapOnEmptyImagesPlaceholder()
            .waitForLoading()
            .checkForFormScreen()
    }

    func test_emptyImagesCanNotEdit() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_banned_only_delete", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.bannedOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_banned_can_not_edit_no_images", userAuthorized: true)
        }

        launch()
        self.openSaleCardScreen(offerID: Consts.bannedOfferID)
            .waitForLoading()
            .checkImagesPlaceholderSnapshot()
    }

    func test_openFormAndUpdateOffer() {
        var priceChanged: Bool = false
        let updPriceExp = expectation(description: "Должны были обновить цену на форме")
        let offerReloadedExp = expectation(description: "После закрытия формы должны были запросить обновленный оффер")

        launch()
        let picker = openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()

        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            if priceChanged {
                offerReloadedExp.fulfill()
                return Response.okResponse(fileName: "dealer_offer_active_updated_price", userAuthorized: true)
            } else {
                return Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
            }
        }

        self.server.addHandler("PUT /user/draft/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active_updated_price_draft", userAuthorized: true)
        }

        self.server.addHandler("POST /user/draft/CARS/\(Consts.activeOfferID)/publish") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active_updated_price_draft", userAuthorized: true)
        }

        picker.tapOnEditButton()

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
            formPicker.publishButton.tap()
            updPriceExp.fulfill()
        }

        wait(for: [updPriceExp], timeout: 5)

        picker.checkPrice(value: Consts.newPriceSecondFormatted)
        wait(for: [offerReloadedExp], timeout: 4)
    }

    func test_reportPreviewNoBuyPackButton() {
        mocker.mock_reportLayoutForOffer(bought: false)
        self.server.addHandler("GET /carfax/offer/cars/\(Consts.activeOfferID)/raw?version=2") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_sale_card_carfax_free", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)
        steps.scrollToCarReport(inset: true)

        Step("Проверяем, что на превью отчёта только одна кнопка `Купить`") {
            steps.snapshotCarReportBuyBblock(
                identifier: "dealer_sale_card_report_preview_single_buy_btn"
            )
        }
    }

    func test_fullReportNoBuyPackButton() {
        mocker.mock_reportLayoutForOffer(bought: false)
        mocker.mock_reportLayoutForReport(bought: false)
        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)
        steps.openFreeCarReport()

        let carReportSteps = steps.as(CarReportPreviewSteps.self)
        _ = carReportSteps.onCarReportPreviewScreen()

        Step("Проверяем кнопки снизу: должно быть купить, не должно быть купить пак") {
            carReportSteps.scrollToFreeReportBottom()
            carReportSteps.checkHasNoBuyPackageButton()
            carReportSteps.checkHasBuyButton()
        }
    }

    func test_buyCarReportSuccess() {
        mocker.mock_reportLayoutForOffer(bought: false)

        self.server.addHandler("GET /carfax/offer/cars/\(Consts.activeOfferID)/raw?decrement_quota=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_sale_card_carfax_buy_success", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)

        mocker.mock_reportLayoutForOffer(bought: true)
        Step("Покупаем отчёт") {

            steps.scrollToCarReport()
                .tapBuyFullReport()
                .tapConfirmBuyFullReport()
        }

        Step("Проверяем, что перешли на страницу отчета и он обновился") {
            steps.as(CarReportPreviewSteps.self)
                .scrollToPaidReportBottom()
                .checkHasNoBuyPackageButton()
                .checkHasNoBuyButton()
        }
    }

    func test_buyCarReportFail() {
        mocker.mock_reportLayoutForOffer(bought: false)

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)

        Step("Проверяем, что при ошибке покупки покажется HUD") {
            Step("Проверяем 400 Bad Request") {
                self.server.addHandler("GET /carfax/offer/CARS/\(Consts.activeOfferID)/raw?decrement_quota=true") { (_, _) -> Response? in
                    return Response.responseWithStatus(
                        body: nil,
                        protoName: "auto.api.ErrorResponse",
                        userAuthorized: true,
                        status: "HTTP/1.1 400 Bad Request"
                    )
                }

                steps.scrollToCarReport()
                    .tapBuyFullReport()
                    .tapConfirmBuyFullReport()

                steps.snapshotActivityHUD(
                    identifier: "dealer_sale_card_report_preview_buy_no_bad_request"
                )
            }

            Step("Проверяем 500 Bad Request") {
                self.server.addHandler("GET /carfax/offer/CARS/\(Consts.activeOfferID)/raw?decrement_quota=true") { (_, _) -> Response? in
                    return Response.responseWithStatus(
                        body: nil,
                        protoName: "auto.api.ErrorResponse",
                        userAuthorized: true,
                        status: "HTTP/1.1 500 Internal Server Error"
                    )
                }

                steps
                    .wait(for: 3)
                    .tapBuyFullReport()
                    .tapConfirmBuyFullReport()

                steps.snapshotActivityHUD(
                    identifier: "dealer_sale_card_report_preview_buy_no_500_error"
                )
            }

            Step("Проверяем NoMoney") {
                self.server.addHandler("GET /carfax/offer/CARS/\(Consts.activeOfferID)/raw?decrement_quota=true&version=2") { (_, _) -> Response? in
                    Response.okResponse(fileName: "dealer_sale_card_carfax_buy_same_response", userAuthorized: true)
                }
                steps
                    .wait(for: 3)
                    .tapBuyFullReport()
                    .tapConfirmBuyFullReport()

                steps.snapshotActivityHUD(
                    identifier: "dealer_sale_card_report_preview_buy_no_money"
                )
            }
        }
    }

    func test_buyCarReportNoBuyRights() {
        self.server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_offers_read_grants", userAuthorized: true)
        }
        mocker.mock_reportLayoutForOffer(bought: false)

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.activeOfferID)

        Step("Проверяем, что без доступа на покупку отчётов покажется ошибка") {
            steps.scrollToCarReport(inset: false)
                .tapBuyFullReport()

            steps.snapshotActivityHUD(
                identifier: "dealer_sale_card_report_buy_no_rights"
            )
        }
    }

    func test_offerEditCarHasMarkModelDisabled() {

        launch()
        let picker = openSaleCardScreen(offerID: Consts.activeOfferID)
            .waitForLoading()

        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
        }

        self.server.addHandler("PUT /user/draft/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/CARS/suggest *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_no_complectation_suggest", userAuthorized: true)
        }

        picker.tapOnEditButton()

        let formSteps = onDealerFormScreen()
        let formPicker = formSteps.onDealerFormScreen()

        Step("Проверяем, что пикер `Марка` некликабелен") {
            formPicker.autoMarkField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }

        Step("Проверяем, что пикер `Модель` некликабелен") {
            formPicker.autoModelField.shouldExist().tap()
            formSteps.wait(for: 1).checkHasNoNavbarSearch()
        }
    }

    // MARK: - Private

    func test_activateOffer(_ action: (DealerSaleCardSteps) -> Void) {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }

        let expectationOfferActivation = self.expectationForRequest(
            method: "POST",
            uri: "/user/offers/cars/\(Consts.inactiveOfferID)/activate"
        )
        let expectationOfferUpdate = self.expectationForRequest(
            method: "GET",
            uri: "/user/offers/cars/\(Consts.inactiveOfferID)"
        )

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.inactiveOfferID).waitForLoading()
        action(steps)

        self.wait(for: [expectationOfferActivation, expectationOfferUpdate], timeout: Self.requestTimeout)
    }

    func test_deleteOffer(_ action: () -> DealerOfferActionSteps) {
        let expectationOfferDeletion = self.expectationForRequest(
            method: "DELETE",
            uri: "/user/offers/cars/\(Consts.bannedOfferID)"
        )

        let expectationWithoutDeletion = self.expectationForRequest(
            method: "DELETE",
            uri: "/user/offers/cars/\(Consts.bannedOfferID)"
        )
        expectationWithoutDeletion.isInverted = true

        action().tapOnConfirmationCancel()

        self.wait(for: [expectationWithoutDeletion], timeout: Self.requestTimeout)

        action().tapOnConfirmationDelete()

        self.wait(for: [expectationOfferDeletion], timeout: Self.requestTimeout)
    }

    @discardableResult
    private func openSaleCardScreen(offerID: String) -> DealerSaleCardSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .scrollToOfferTitle(offerID: offerID)
            .tapOnOfferSnippet(offerID: offerID)
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

        self.server.addHandler("GET /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.inactiveOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_inactive", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.needActivationOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_need_activation", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.activeOfferID)/price") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.inactiveOfferID)/activate") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.activeOfferID)/hide") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("DELETE /user/offers/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/CARS/\(Consts.activeOfferID)/edit") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_req_edit_resp_ok", userAuthorized: true)
        }

        self.server.addHandler("GET /user/draft/CARS/\(Consts.activeOfferID)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_offer_active_draft", userAuthorized: true)
        }
    }

    private func validateChartSnapshots(
        _ steps: DealerSaleCardSteps,
        withTap: Bool,
        chartIdentifier: String,
        phonesChartIdentifier: String
    ) {
        let viewsChart = steps.onDealerSaleCardScreen().viewsChart
        let phoneViewsChart = steps.onDealerSaleCardScreen().phoneViewsChart

        steps.scrollToChart(chart: viewsChart, description: "Просмотры")

        steps.checkChartSnapshot(
            chart: viewsChart,
            identifier: .make(identifier: chartIdentifier, style: .light),
            withTap: withTap
        )

        appClient.setUserInterfaceStyleBlocking(.dark)

        steps.checkChartSnapshot(
            chart: viewsChart,
            identifier: .make(identifier: chartIdentifier, style: .dark),
            withTap: withTap
        )

        appClient.setUserInterfaceStyleBlocking(.light)

        steps.scrollToChart(chart: phoneViewsChart, description: "Просмотры телефонов")

        steps.checkChartSnapshot(
            chart: phoneViewsChart,
            identifier: .make(identifier: phonesChartIdentifier, style: .light),
            withTap: withTap
        )

        appClient.setUserInterfaceStyleBlocking(.dark)

        steps.checkChartSnapshot(
            chart: phoneViewsChart,
            identifier: .make(identifier: phonesChartIdentifier, style: .dark),
            withTap: withTap
        )

        appClient.setUserInterfaceStyleBlocking(.light)
    }
}
