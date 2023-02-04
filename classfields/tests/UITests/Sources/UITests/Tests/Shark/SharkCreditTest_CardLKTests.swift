import AutoRuProtoModels
import Snapshots
import SwiftProtobuf
import XCTest

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_CardLKTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    var sharkMocker: SharkMocker!

    override var appSettings: [String: Any] {
        return settings
    }

    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        sharkMocker = SharkMocker(server: server)
    }

    override func tearDown() {
        super.tearDown()
        sharkMocker.stop()
    }

    private func mockFromReproducer(path: String) {
        advancedMockReproducer.setup(server: mocker.server, mockFolderName: path)
        sharkMocker
            .forceLogin(true)
            .start()
    }

    func test_lk_card_no_product() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2], appProduct: [])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .offer(id: offerId, isActive: true, allowedProducts: [])
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "no_product_title")
            .validateSnapshot(of: "credit_processing_snippet", snapshotId: "no_product_credit_processing_snippet")
            .onMainScreen().find(by: "credit_processing_snippet").firstMatch.tap()

        mainSteps
            .exist(selector: "empty_products")
            .validateSnapshot(
                of: "CreditLKViewController",
                perPixelTolerance: Snapshot.defaultPerPixelTolerance,
                overallTolerance: Snapshot.defaultOverallTolerance,
                ignoreEdges: UIEdgeInsets(top: 72, left: 0, bottom: 64, right: 0),
                snapshotId: "lk-noProducts"
            )
    }

    func test_add_offer_to_app_from_listing() {
        mockFromReproducer(path: "Shark/draftWithOutOffer")
        sharkMocker
            .mockCalculator()

        settings["skipCreditAlert"] = false
        launch()

        mainSteps
            .exist(selector: "CreditAlertViewController")
            .openCreditFormFromAlert()
            .selectOffer()
            .selectFromListing()
            .openCarOffer(with: "1101742428-6d20f9e0")
            .scrollTo("select_auto_button", windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0))
            .tapOnSelectCarForCredit()
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
            .validateSnapshot(of: "creditOfferCell")
    }

    func test_lk_card_one_approved() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.approved, .tinkoff_1)],
                offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1, .tinkoff_2])
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "creditTitle_card_one_approved")
            .validateSnapshot(of: "credit_processing_snippet", snapshotId: "one_approved_processing_snippet")
            .onMainScreen().find(by: "credit_processing_snippet").firstMatch.tap()

        mainSteps
            .exist(selector: "product-0-approve")
            .validateSnapshot(of: "CreditLKViewController", snapshotId: "lk-approve")
    }

    func test_lk_card_not_approve() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.new, .tinkoff_1)],
                offerType: .offer(
                    id: "\(offerId)",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "card_not_approve_creditTitle")
            .validateSnapshot(of: "credit_processing_snippet", snapshotId: "card_not_approve_processing_snippet")
            .onMainScreen().find(by: "creditProccesingSnippet").firstMatch.tap()

        mainSteps
            .scrollTo("product-1-inProgress")
            .validateSnapshot(of: "CreditLKViewController", snapshotId: "lk-inProgress")
    }

    func test_lk_card_all_reject_nothing_to_sent() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.reject, .tinkoff_1), .init(.cancel, .tinkoff_2), .init(.notSent, .raif)],
                offerType: .offer(
                    id: "1090794514-915f196d",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2, .raif]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .notExist("section_credit_title", maxSwipes: 4)
    }

    func test_lk_card_1reject_1canBeSend() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.reject, .tinkoff_1)],
                offerType: .offer(
                    id: "\(offerId)",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "1reject_1canBeSend_creditTitle")
            .validateSnapshot(of: "credit_processing_snippet", snapshotId: "1reject_1canBeSend_processing_snippet")
            .onMainScreen().find(by: "creditProccesingSnippet").firstMatch.tap()

        mainSteps
            .scrollTo("product-0-canBeSend")
            .validateSnapshot(of: "CreditLKViewController", snapshotId: "lk-canBeSend")
    }

    func test_lk_card_no_active_app() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0))
            .validateSnapshot(of: "section_credit_title")
            .scrollTo("credit_calculator", windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0))
            .validateSnapshot(of: "credit_calculator", snapshotId: "credit_calculator")
    }

    func test_lk_card_draft_with_offer_go_to_another_card_and_change_car() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .draft,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "section_credit_title_\(#function)")
            .scrollTo("select_auto_button", windowInsets: .init(top: 56, left: 0, bottom: 80, right: 0))
            .validateSnapshot(of: "select_auto_button", snapshotId: "select_auto_button_\(#function)")
            .tapOnSelectCarForCredit()
            .wait(for: 1)
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
    }

    func test_lk_card_draft_with_offer_go_to_another_card_and_continue() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .draft,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .validateSnapshot(of: "section_credit_title", snapshotId: "section_credit_title_\(#function)")
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
    }

    func test_lk_card_draft_without_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .draft,
                claims: [],
                offerType: .none
            )
            .start()

        settings["skipCreditAlert"] = true
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("select_auto_button", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .tapOnSelectCarForCredit()
            .wait(for: 1)
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
    }

    func test_lk_card_draft_continue_on_the_same_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .draft,
                claims: [],
                offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1, .tinkoff_2])
            )

            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("continue_credit")
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
    }

    func test_card_draft_sold_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .draft,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: false,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo(
                "section_credit_title",
                maxSwipes: 20,
                windowInsets: UIEdgeInsets(top: 50, left: 0, bottom: 80, right: 0),
                useLongSwipes: true,
                longSwipeAdjustment: 0.35
            )
            .validateSnapshot(of: "section_credit_title", snapshotId: "section_credit_title_\(#function)")
            .scrollTo("select_auto_button", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .tapOnSelectCarForCredit()
            .wait(for: 1)
            .continueSharkCredit()
            .exist(selector: "creditOfferCell")
    }

    func test_card_no_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .none
            )
            .start()

        settings["skipCreditAlert"] = true

        let updateApplicationExpectation = expectationForRequest(
            method: "POST",
            uri: "/shark/credit-application/update/\(sharkMocker.applicationId)"
        ) { (request: Vertis_Shark_CreditApplicationSource) in
            request.payload.autoru.offers.first?.id == self.offerId
        }

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("section_credit_title")
            .validateSnapshot(of: "section_credit_title", snapshotId: "section_credit_title_\(#function)")
            .scrollTo("select_auto_button", windowInsets: .init(top: 0, left: 0, bottom: 120, right: 0))
            .tapOnSelectCarForCredit()
            .wait(for: 1)
            .tapOnConfirm()

        wait(for: [updateApplicationExpectation], timeout: 10.0)
    }

    func test_lk_no_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .none
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .validateSnapshot(of: "Gallery")
            .tap("Выбрать автомобиль")
            .exist(selector: "OfferSelectionViewController")
    }

    func test_lk_all_productsType() {
        let products: [SharkMocker.Product] = [.tinkoff_auto, .tinkoff_cash, .tinkoff_creditCard, .tinkoff_refin]
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: products)
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .none
            )
            .start()

        settings["skipCreditAlert"] = true

        let step = openCredit()
        for product in products {
            step
                .scrollTo(product.rawValue)
                .validateSnapshot(of: product.rawValue, snapshotId: product.rawValue)
        }
    }

    func test_lk_sold_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: false,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .validateSnapshot(of: "Gallery", snapshotId: "LK-Gallery-Sold")
            .tap("Выбрать другой автомобиль")
            .exist(selector: "OfferSelectionViewController")
    }

    func test_lk_send_claim() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true
        let requestExpectationAddProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "PUT",
               request
               .uri ==
               "/shark/credit-application/add-products/\(self.sharkMocker.applicationId)?credit_product_ids=tinkoff-1&with_send_delay_secs=600"
            {
                return true
            }
            return false
        }

        let step = openCredit()
            .scrollTo("sendButton")

        _ = sharkMocker
            .mockApplication(
                status: .active,
                claims: [.init(.draft, .tinkoff_1)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )

        step
            .tap("sendButton")
            .exist(selector: "cancelProductButton")
            .validateSnapshot(of: "product-0-canBeRejected", snapshotId: "product-0-canBeRejected")

        wait(for: [requestExpectationAddProduct], timeout: 5)
    }

    func test_lk_cancel_claim() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.draft, .tinkoff_1)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true
        let requestExpectationCancelProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "DELETE",
               request
               .uri ==
               "/shark/credit-application/cancel-products/\(self.sharkMocker.applicationId)?credit_product_ids=tinkoff-1"
            {
                return true
            }
            return false
        }

        let step = openCredit()
            .scrollTo("cancelProductButton")

        _ = sharkMocker
            .mockApplication(
                status: .active,
                claims: [.init(.canceledDraft, .tinkoff_1)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )

        step
            .tap("cancelProductButton")
            .exist(selector: "sendButton")
            .validateSnapshot(of: "product-0-canBeSend", snapshotId: "product-0-canBeSend")

        wait(for: [requestExpectationCancelProduct], timeout: 5)
    }

    func test_lk_sberbankSnippetBasics() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.sberbank])
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sberbank)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sberbank]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .scrollTo("sberbankModalHelpButton")
            .tap("sberbankModalHelpButton")
            .wait(for: 1)
            .validateSnapshot(of: "sberbankModalDescription", snapshotId: "sberbankModalDescription")
            .tap("dismiss_modal_button")
            .as(SharkLKSteps.self)
            .tapCreditDescriptionLink()
            .exist(selector: "webView")
    }

    func test_lk_sberbankClaim_add() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.sberbank])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sberbank]
                )
            )
            .start()
        let requestExpectationAddProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "PUT",
               request.uri.contains("/shark/credit-application/add-products/\(self.sharkMocker.applicationId)")
            {
                return true
            }
            return false
        }
        settings["skipCreditAlert"] = true
        let safariAPP = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")

        // Launch safari app
        safariAPP.launch()
        safariAPP.activate()

        let step = openCredit()
            .scrollTo("sendIFrameButton")

        _ = sharkMocker
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sberbank)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sberbank]
                )
            )

        step
            .tap("sendIFrameButton")
            .step("Проверяем, что открылся сафари") {
                let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
                safari.shouldBeVisible()
            }
        wait(for: [requestExpectationAddProduct], timeout: 5)
    }

    func test_lk_sberbankClaim_alreadyAdded() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.sberbank])
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sberbank)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sberbank]
                )
            )
            .start()

        let requestExpectationAddProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "PUT",
               request.uri.contains("/shark/credit-application/add-products/\(self.sharkMocker.applicationId)")
            {
                return true
            }
            return false
        }
        requestExpectationAddProduct.isInverted = true

        settings["skipCreditAlert"] = true
        let safariAPP = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")

        // Launch safari app
        safariAPP.launch()
        safariAPP.activate()

        let step = openCredit()
            .scrollTo("sendIFrameButton")

        step
            .tap("sendIFrameButton")
            .step("Проверяем, что открылся сафари") {
                let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
                safari.shouldBeVisible()
            }

        wait(for: [requestExpectationAddProduct], timeout: 5)
    }

    func test_lk_sravniClaim_add() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.sravniru("")])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sravniru("")]
                )
            )
            .start()
        let requestExpectationAddProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "PUT",
               request.uri.contains("/shark/credit-application/add-products/\(self.sharkMocker.applicationId)")
            {
                return true
            }
            return false
        }
        settings["skipCreditAlert"] = true

        let step = openCredit()

        _ = sharkMocker
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sravniru(""))],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sravniru("")]
                )
            )
        step
            .tap("sravniButton")

        step.wait(for: 3)

        _ = sharkMocker
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sravniru("test.ru"))],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sravniru("")]
                )
            )

        step
            .exist(selector: "webView")

        wait(for: [requestExpectationAddProduct], timeout: 5)
    }

    func test_lk_sravniruClaim_alreadyAdded() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.sravniru("test.ru")])
            .mockApplication(
                status: .active,
                claims: [.init(.new, .sravniru("test.ru"))],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.sravniru("test.ru")]
                )
            )
            .start()

        let requestExpectationAddProduct: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "PUT",
               request.uri.contains("/shark/credit-application/add-products/\(self.sharkMocker.applicationId)")
            {
                return true
            }
            return false
        }
        requestExpectationAddProduct.isInverted = true

        settings["skipCreditAlert"] = true

        openCredit()
            .tap("sravniButton")
            .exist(selector: "webView")

        wait(for: [requestExpectationAddProduct], timeout: 5)
    }

    func test_lk_create_application() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        let step = openCredit()
            .as(PreliminaryCreditSteps.self)
            .enterFio("Петрулио Буэнди Сальвадор")
            .tap("Заполнить заявку")

        step
            .exist(selector: "WizardViewController")
    }

    func test_card_view_banks() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("product_banks")
            .tap("product_banks")
            .exist(selector: "ModalViewControllerHost")
            .validateSnapshot(of: "ModalViewControllerHost")
            .tap("Как работает «Авто.ру финанс»?")
            .exist(selector: "webView")
    }

    func test_card_create_application() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        let step = launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"))
        )
        .scrollTo("preliminary_claim_short")
        .as(PreliminaryCreditSteps.self)
        .enterFio("Петрулио Буэнди Сальвадор")
        .tap("Заполнить заявку")

        _ = sharkMocker
            .mockApplication(
                status: .draft,
                claims: [.init(.new, .tinkoff_1)],
                offerType: .offer(id: nil, isActive: true, allowedProducts: [.tinkoff_1, .tinkoff_2])
            )

        step
            .exist(selector: "WizardViewController")
            .tap("Закрыть")
            .validateSnapshot(of: "CloseModalViewController")
            .tap("Остаться")
            .exist(selector: "WizardViewController")
            .tap("Закрыть")
            .tap("close_button")
            .notExist(selector: "WizardViewController")
            .tap("Продолжить заполнение")
            .exist(selector: "CreditFormViewController")
    }

    func test_lk_edit() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.draft, .tinkoff_1)],
                offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1, .tinkoff_2])
            )
            .start()

        openCredit()
            .openEditForm()
            .exist(selector: "CreditFormViewController")

    }

    func test_lk_archive() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.draft, .tinkoff_1)],
                offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1, .tinkoff_2]),
                hasArchive: true
            )
            .start()

        openCredit()
            .wait(for: 1)
            .scrollTo("Archive", windowInsets: .init(top: 0, left: 0, bottom: 16, right: 0))
            .validateSnapshot(of: "Archive")
    }

    func test_lk_term_14() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [.init(.approved, .tinkoff_1, term: 14)],
                offerType: .offer(
                    id: "1090794514-915f196d",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .exist(selector: "product-0-approve")
            .validateSnapshot(of: "product-0-approve", snapshotId: "product-0-approve-term14")
    }

    func test_lk_hasNotPossibleProductCauseHasClaim() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2], appProduct: [])
            .mockApplication(
                status: .active,
                claims: [.init(.new, .tinkoff_1)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .exist(selector: "product-0-inProgress")
    }

    func test_lk_hasNotPossibleProduct() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2], appProduct: [])
            .mockApplication(
                status: .active,
                claims: [.init(.canceledDraft, .tinkoff_1)],
                offerType: .offer(
                    id: "1090794514-915f196d_test",
                    isActive: true,
                    allowedProducts: [.tinkoff_1, .tinkoff_2]
                )
            )
            .start()

        settings["skipCreditAlert"] = true

        openCredit()
            .wait(for: 2)
            .notExist(selector: "product-0-canBeSend")
    }

    func test_card_creditPromoBoth() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.alfa499, .alfa499])
            .mockNoApplication()
            .start()
        settings["creditPromo"] = "both"

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .validateSnapshot(of: "creditPromoWithBanks", snapshotId: "creditPromoWithBanks")
            .validateSnapshot(of: "creditPromoWithOneBank", snapshotId: "creditPromoWithOneBank")
            .tap("creditPromoWithOneBank")
            .exist(selector: "section_credit_title")
    }

    func test_card_noCreditPromoOnDealerOffer() {
        _ = sharkMocker
            .baseMock(offerId: offerId)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()

        mocker
            .mock_offerCars(id: "\(offerId)", isSalon: true)
            .startMock()

        settings["creditPromo"] = "both"

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .notExist("creditPromoWithBanks")
            .notExist("creditPromoWithOneBank")
    }

    func test_card_no_offer_no_percents() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(
                status: .active,
                claims: [],
                offerType: .none
            )
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditPercents())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipCreditAlert"] = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .exist(selector: "В кредит от 16 800 ₽/мес.")
            .exist(selector: "Тинькофф Банк — кредит от 16 800 ₽/мес.")
    }

    func test_lk_card_help_no_percents() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditPercents())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo(
                "section_credit_title",
                windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0),
                useLongSwipes: true,
                longSwipeAdjustment: 0.35
            )
            .tap("questionmark")
            .wait(for: 1)
            .exist(selector: "Автокредит")
    }

    func test_lk_credit_calculator() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .alfa499], isMany: true)
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openSuperMenu()
            .tap(.credit)
            .should(provider: .creditLKScreen, .exist)
            .focus {
                $0.exist(selector: "Подбор кредита в разных банках от 7,9%")
                $0.exist(selector: "Не требуется")
                $0.exist(selector: "pet@sa.ru")
                $0.exist(selector: "+7 987 564-32-12")
                $0.tap(.editCreditSum)
                    .wait(for: 1)

                var deleteButton: XCUIElement {
                    app
                        .descendants(matching: .any)
                        .matching(NSPredicate(format: "label IN %@", ["Удалить", "Delete", "delete"]))
                        .firstMatch
                }

                deleteButton.tap()
                deleteButton.tap()
                app.typeText("500")
                $0.tap(.fieldFIO)
                app.typeText("T")
                $0.tap(.fieldEmail)
                deleteButton.tap()
                deleteButton.tap()
                deleteButton.tap()
                deleteButton.tap()
                $0.tap(.fieldPhone)
                deleteButton.tap()
                $0.tap(.submitButton)
                    .wait(for: 1)
                $0.exist(selector: "Введите имя целиком")
                $0.exist(selector: "Укажите корректный e-mail")
                $0.exist(selector: "Укажите корректный телефон")
                $0.tap(.agreementCheckBoxOn)
                    .focus(on: .submitButton) { button in
                        button.validateSnapshot(snapshotId: "test_lk_credit_calculator_submit_disabled")
                    }
                $0.tap(.agreementCheckBoxOff)
                $0.tap(.agreementText)
                    .should(provider: .webViewPicker, .exist)
                    .focus { $0.tap(.closeButton) }
                $0.tap(.questionmark)
                    .should(provider: .genericModalPopup, .exist)
                    .focus { $0.tap("Как работает «Авто.ру финанс»?") }
                    .should(provider: .webViewPicker, .exist)
                    .focus { $0.tap(.closeButton) }
                    .focus(on: .creditSumSliderThumb) { thumb in
                        let touchPoint = thumb.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                        let whereToDrag = thumb.rootElement
                            .coordinate(withNormalizedOffset: CGVector(dx: -1.3, dy: 0.5))
                        touchPoint.press(
                            forDuration: 0.1,
                            thenDragTo: whereToDrag,
                            withVelocity: .slow,
                            thenHoldForDuration: 0.1
                        )
                    }
                $0.exist(selector: "1 000 000")
                    .focus(on: .creditSumSliderThumb) { thumb in
                        let touchPoint = thumb.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                        let whereToDrag = thumb.rootElement
                            .coordinate(withNormalizedOffset: CGVector(dx: -12.5, dy: 0.5))
                        touchPoint.press(
                            forDuration: 0.1,
                            thenDragTo: whereToDrag,
                            withVelocity: .slow,
                            thenHoldForDuration: 0.1
                        )
                    }
                $0.exist(selector: "300 000")
                    .focus(on: .creditSumSliderThumb) { thumb in
                        let touchPoint = thumb.rootElement.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                        let whereToDrag = thumb.rootElement
                            .coordinate(withNormalizedOffset: CGVector(dx: -3.2, dy: 0.5))
                        touchPoint.press(
                            forDuration: 0.1,
                            thenDragTo: whereToDrag,
                            withVelocity: .slow,
                            thenHoldForDuration: 0.1
                        )
                    }
                $0.exist(selector: "200 000")
                $0.tap(.fieldFIO)
                deleteButton.tap()
                app.typeText("Тест Тестович Тестов")
                $0.tap(.fieldEmail)
                app.typeText(".ru")
                $0.tap(.fieldPhone)
                app.typeText("1")
                $0.tap(.submitButton)
            }
            .should(provider: .creditWizardScreen, .exist)
    }

    private func openCredit() -> SharkLKSteps {
        launchMain()
            .container
            .focus(on: .tabBar, ofType: .tabBar) {
                $0.tap(.tab(.favorites))
            }
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.superMenuButton) }
            .should(provider: .superMenuScreen, .exist)
            .focus {
                $0.scroll(to: .credit, direction: .up)
                $0.tap(.credit)
            }
        return SharkLKSteps(context: self)
    }

    private func openSuperMenu() -> SuperMenuScreen {
        launchMain { screen in
            screen
                .toggle(to: \.favorites)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.superMenuButton) }
                .should(provider: .superMenuScreen, .exist)
        }
    }
}
