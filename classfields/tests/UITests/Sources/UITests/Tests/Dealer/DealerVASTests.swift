import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerCabinet AutoRuDealerSaleCard
final class DealerVASTests: DealerBaseTest {
    enum Consts {
        static let singleOfferID = "1096859248-8f2c7274"
    }

    private static let insetsWithoutFilterAndTabBar = UIEdgeInsets(top: 0, left: 0, bottom: 120, right: 0)

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["MOCKED_DATE"] = "2020-04-06 15:00:00+03:00"
        return env
    }

    override func setUp() {
        super.setUp()
        _ = self.addAlertInterceptor(description: "Алерт перед деактивации васа", button: "Отключить")
        _ = self.addAlertInterceptor(description: "Алерт перед активацией васа", button: "Подключить")
    }

    // MARK: - Tests

    func test_vasFreshOld() {
        self.server.addHandler("GET /user/offers/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_vas_list_old_fresh", userAuthorized: true)
        }

        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.singleOfferID)
            .as(DealerVASContainingSteps.self)
            .checkVASItemSnapshot(offerID: Consts.singleOfferID, identifier: "vas_fresh_activated_old", vas: .fresh)
        self.test_vasFresh(steps: steps)
    }

    func test_vasFreshToday() {
        launch()
        let steps = self.expandSnippet()
        self.test_vasFresh(steps: steps)
    }

    func test_vasAutoUp() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_autoup_active", userAuthorized: true)
        }

        launch()
        self.expandSnippet()
            .as(DealerVASContainingSteps.self)
            .checkVASItemSnapshot(offerID: Consts.singleOfferID, identifier: "vas_autoup", vas: .autoUp)
    }

    func test_vasSingleVASActivate() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        launch()
        let steps = self.expandSnippet()
        for vas in [DealerVASContainingScreen.VASListItemType.premium, DealerVASContainingScreen.VASListItemType.special] {
            Step("Проверяем активацию васа '\(vas.rawValue)'") {
                self.test_vasSingleActivate(steps: steps, vas: vas)
            }
        }
    }

    func test_vasSingleVASDeactivate() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_all_single_vas_activated", userAuthorized: true)
        }

        launch()
        let steps = self.expandSnippet()
        for vas in [DealerVASContainingScreen.VASListItemType.premium, DealerVASContainingScreen.VASListItemType.special] {
            Step("Проверяем деактивацию васа '\(vas.rawValue)'") {
                self.test_vasSingleDeactivate(steps: steps, vas: vas)
            }
        }
    }

    func test_vasTurboActivate() {
        let activateFromList: XCTestExpectation = {
            self.expectationForVASActivation(
                method: "POST",
                uri: "/user/offers/cars/\(Consts.singleOfferID)/products",
                name: DealerVASContainingScreen.VASListItemType.turbo.backendAlias
            )
        }()

        launch()
        let steps = self.expandSnippet()
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .turbo)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .turbo)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

        self.wait(for: [activateFromList], timeout: Self.requestTimeout)

        steps.checkVASPurchaseElementNotExists(offerID: Consts.singleOfferID, type: .turbo)
            .checkVASItemsSnapshot(
                offerID: Consts.singleOfferID,
                identifier: "vas_list_expanded_turbo",
                from: .fresh,
                to: .premium
            )

        let checkVASDescription: (DealerVASContainingScreen.VASListItemType) -> Void = { vas in
            steps.tapOnVASItem(offerID: Consts.singleOfferID, type: vas)
                .shouldSeeTitleAndDescription(for: vas)
                .shouldSeeBottomButton(type: .activated)
                .tapOnCloseButton()
        }

        checkVASDescription(.turbo)
        checkVASDescription(.premium)
        checkVASDescription(.special)
    }

    func test_vasTurboDeactivate() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_turbo_vas_activated", userAuthorized: true)
        }

        launch()
        let steps = self.expandSnippet()
            .scrollToVASTitle(offerID: Consts.singleOfferID, type: .turbo)
            .checkVASPurchaseElementNotExists(offerID: Consts.singleOfferID, type: .turbo)
            .checkVASItemsSnapshot(
                offerID: Consts.singleOfferID,
                identifier: "vas_list_expanded_turbo",
                from: .fresh,
                to: .premium
            )

        let checkVASDescription: (DealerVASContainingScreen.VASListItemType) -> Void = { vas in
            Step("Проверяем полноэкранный вас '\(vas.rawValue)'") {
                steps.tapOnVASItem(offerID: Consts.singleOfferID, type: vas)
                    .shouldSeeTitleAndDescription(for: vas)
                    .shouldSeeBottomButton(type: .activated)
                    .tapOnCloseButton()
            }
        }

        checkVASDescription(.turbo)
        checkVASDescription(.premium)
        checkVASDescription(.special)
    }

    func test_vasFullscreenDescriptionActivation() {
        launch()
        let steps = self.expandSnippet()
        let vasList: [DealerVASContainingScreen.VASListItemType] = [.special, .premium]
        for vas in vasList {
            Step("Проверяем активацию васа '\(vas.rawValue)' с полноэкранного описания") {
                let activateExpectation: XCTestExpectation = {
                    self.expectationForVASActivation(
                        method: "POST",
                        uri: "/user/offers/cars/\(Consts.singleOfferID)/products",
                        name: vas.backendAlias
                    )
                }()

                steps.scrollToVASTitle(offerID: Consts.singleOfferID, type: vas)
                    .tapOnVASItem(offerID: Consts.singleOfferID, type: vas)
                    .shouldSeeTitleAndDescription(for: vas)
                    .shouldSeeBottomButton(type: .activate)
                    .tapOnBottomButton(type: .activate)
                    .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

                self.wait(for: [activateExpectation], timeout: Self.requestTimeout)

                steps.as(DealerVASDescriptionSteps.self)
                    .tapOnCloseButton()
                    .as(DealerVASContainingSteps.self)
                    .checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: true)
            }
        }
    }

    func test_vasTurboFullscreenDescriptionActivation() {
        let vas = DealerVASContainingScreen.VASListItemType.turbo

        let activateExpectation: XCTestExpectation = {
            self.expectationForVASActivation(
                method: "POST",
                uri: "/user/offers/cars/\(Consts.singleOfferID)/products",
                name: vas.backendAlias
            )
        }()

        launch()
        let steps = self.expandSnippet()
            .scrollToVASTitle(offerID: Consts.singleOfferID, type: vas)
            .tapOnVASItem(offerID: Consts.singleOfferID, type: vas)
            .shouldSeeTitleAndDescription(for: vas)
            .shouldSeeBottomButton(type: .activate)
            .tapOnBottomButton(type: .activate)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

        self.wait(for: [activateExpectation], timeout: Self.requestTimeout)

        steps.as(DealerVASDescriptionSteps.self)
            .tapOnCloseButton()
            .as(DealerVASContainingSteps.self)
            .checkVASPurchaseElementNotExists(offerID: Consts.singleOfferID, type: .turbo)
            .checkVASPurchaseElementNotExists(offerID: Consts.singleOfferID, type: .special)
            .checkVASPurchaseElementNotExists(offerID: Consts.singleOfferID, type: .premium)
    }

    func test_vasFullscreenDescriptionDeactivation() {
        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_all_single_vas_activated", userAuthorized: true)
        }

        launch()
        let steps = self.expandSnippet()
        let vasList: [DealerVASContainingScreen.VASListItemType] = [.special, .premium]
        for vas in vasList {
            Step("Проверяем деактивацию васа '\(vas.rawValue)' с полноэкранного описания") {
                let deactivateExpectation: XCTestExpectation = {
                    self.expectationForVASDeactivation(
                        method: "DELETE",
                        uri: "/user/offers/cars/\(Consts.singleOfferID)/products?product=\(vas.backendAlias)",
                        name: vas.backendAlias
                    )
                }()

                steps.scrollToVASTitle(offerID: Consts.singleOfferID, type: vas)
                    .tapOnVASItem(offerID: Consts.singleOfferID, type: vas)
                    .shouldSeeTitleAndDescription(for: vas)
                    .shouldSeeBottomButton(type: .deactivate)
                    .tapOnBottomButton(type: .deactivate)
                    .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

                self.wait(for: [deactivateExpectation], timeout: Self.requestTimeout)

                steps.as(DealerVASDescriptionSteps.self)
                    .tapOnCloseButton()
                    .as(DealerVASContainingSteps.self)
                    .checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: false)
            }
        }
    }

    func test_snippetVASList() {
        launch()
        let steps = self.expandSnippet()
        self.test_vasList(steps: steps)

        steps.as(DealerCabinetSteps.self)
            .scrollToActionButton(offerID: Consts.singleOfferID)
            .tapOnActionButton(offerID: Consts.singleOfferID, type: .collapse)
            .checkCollapsedVASListSnapshot(offerID: Consts.singleOfferID, identifier: "vas_list_collapsed")
    }

    func test_saleCardVASList() {
        launch()
        let steps = self.openSaleCardScreen(offerID: Consts.singleOfferID).as(DealerVASContainingSteps.self)
        self.test_vasList(steps: steps)
    }

    // MARK: - Private

    private func expandSnippet() -> DealerVASContainingSteps {
        return self.openListing()
            .scrollToActionButton(offerID: Consts.singleOfferID)
            .tapOnActionButton(offerID: Consts.singleOfferID, type: .expand)
            .as(DealerVASContainingSteps.self)
    }

    private func test_vasList(steps: DealerVASContainingSteps) {
        steps
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .fresh)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .fresh)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .special)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .special)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])
            .as(DealerVASContainingSteps.self)
            .checkVASItemsSnapshot(offerID: Consts.singleOfferID, identifier: "vas_list_expanded", from: .fresh, to: .special)
    }

    func test_vasFresh(steps: DealerVASContainingSteps) {
        let makeActivateExpectation: () -> XCTestExpectation = {
            self.expectationForVASActivation(
                method: "POST",
                uri: "/user/offers/cars/\(Consts.singleOfferID)/products",
                name: DealerVASContainingScreen.VASListItemType.fresh.backendAlias
            )
        }

        let activateFromList = makeActivateExpectation()

        let steps = steps
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .fresh)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: .fresh)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

        self.wait(for: [activateFromList], timeout: Self.requestTimeout)

        steps.checkVASItemSnapshot(offerID: Consts.singleOfferID, identifier: "vas_fresh_activated_today", vas: .fresh)

        Step("Проверяем, что можно активировать с полноэкранного описания") {
            let activateFromDescription = makeActivateExpectation()
            steps.tapOnVASItem(offerID: Consts.singleOfferID, type: .fresh)
                .shouldSeeTitleAndDescription(for: .fresh)
                .tapOnBottomButton(type: .activate)
                .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

            self.wait(for: [activateFromDescription], timeout: Self.requestTimeout)
        }
    }

    private func test_vasSingleActivate(steps: DealerVASContainingSteps, vas: DealerVASContainingScreen.VASListItemType) {
        let activateExpectation: XCTestExpectation = {
            self.expectationForVASActivation(
                method: "POST",
                uri: "/user/offers/cars/\(Consts.singleOfferID)/products",
                name: vas.backendAlias
            )
        }()

        let steps = steps
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: vas)
            .checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: false)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: vas)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

        self.wait(for: [activateExpectation], timeout: Self.requestTimeout)

        steps.checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: true)
    }

    private func test_vasSingleDeactivate(steps: DealerVASContainingSteps, vas: DealerVASContainingScreen.VASListItemType) {
        let deactivateExpectation: XCTestExpectation = {
            self.expectationForVASDeactivation(
                method: "DELETE",
                uri: "/user/offers/cars/\(Consts.singleOfferID)/products?product=\(vas.backendAlias)",
                name: vas.backendAlias
            )
        }()

        let steps = steps
            .scrollToVASButtonOrSwitch(offerID: Consts.singleOfferID, type: vas)
            .checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: true)
            .tapVASButtonOrSwitch(offerID: Consts.singleOfferID, type: vas)
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Подключить", "Отключить"])

        self.wait(for: [deactivateExpectation], timeout: Self.requestTimeout)

        steps.checkVASSwitch(offerID: Consts.singleOfferID, type: vas, isOn: false)
    }

    private func addAlertInterceptor(description: String, button: String) -> NSObjectProtocol {
        return self.addUIInterruptionMonitor(withDescription: description) { alert -> Bool in
            if alert.buttons[button].isFullyVisible() {
                alert.buttons[button].tap()
                return true
            }
            return false
        }
    }

    private func expectationForVASActivation(method: String, uri: String, name: String) -> XCTestExpectation {
        return self.expectationForRequest { req -> Bool in
            guard let json = req.messageBodyString(),
                  let body = try? Auto_Api_ApplyAutoruProductsRequest(jsonString: json) else {
                return false
            }

            if body.products.count > 1 || body.products.first?.code != name {
                return false
            }

            return req.method == method && req.uri.lowercased() == uri.lowercased()
        }
    }

    private func expectationForVASDeactivation(method: String, uri: String, name: String) -> XCTestExpectation {
        return self.expectationForRequest { req -> Bool in
            if !req.uri.contains("product=\(name)") {
                return false
            }

            return req.method == method && req.uri.lowercased() == uri.lowercased()
        }
    }

    @discardableResult
    private func openSaleCardScreen(offerID: String) -> DealerSaleCardSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .scrollToOfferTitle(offerID: offerID)
            .tapOnOfferSnippet(offerID: offerID)
            .waitForLoading()
    }

    @discardableResult
    private func openListing() -> DealerCabinetSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        self.server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_single_active", userAuthorized: true)
        }

        self.server.addHandler("POST /user/offers/cars/\(Consts.singleOfferID)/products") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("DELETE /user/offers/cars/\(Consts.singleOfferID)/products *") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/cars/\(Consts.singleOfferID)/products *") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_vas_list", userAuthorized: true)
        }
    }
}
