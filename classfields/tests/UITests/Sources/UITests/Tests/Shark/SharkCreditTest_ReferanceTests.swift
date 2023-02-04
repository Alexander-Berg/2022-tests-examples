import AutoRuProtoModels
import Snapshots
import SwiftProtobuf
import XCTest

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_ReferanceTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    var userDefaults: [String: Any] = [:]
    var sharkMocker: SharkMocker!
    var draft: String {
        struct Draft: Codable {
            let name: String
            let email: String
            let phone: String
        }

        let draft = Draft(name: "Петрулио Буэнди Сальвадор", email: "pet@sa.ru", phone: "9875643212")
        let encoder = JSONEncoder()
        encoder.dataEncodingStrategy = .base64
        let data = (try? JSONEncoder().encode(draft)) ?? Data()
        return String(data: data, encoding: .utf8)!
    }

    override var appSettings: [String: Any] {
        return settings
    }

    override var launchEnvironment: [String: String] {
        var value = super.launchEnvironment
        let userDefaultsJsonData = try! JSONSerialization.data(withJSONObject: userDefaults, options: [])
        value["STANDARD_USER_DEFAULTS"] = userDefaultsJsonData.base64EncodedString()
        return value
    }

    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        userDefaults["preliminaryClaimDraft"] = draft
        sharkMocker = SharkMocker(server: server)
    }

    func test_favorites_noBannerExperiment() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .notExist()
    }

    func test_favorites_noBannerIfHasActiveCredit() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [], offerType: .none)
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .notExist()
    }

    func test_favorites_noApp_loggined_closeBanner() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.ShowCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        let steps = mainSteps
            .openFavoritesTab()
            .creditPromo()
        steps
            .tap("closeFavoritesCreditPromoBannerButton")
        steps
            .notExist()
        app.terminate()

        // Как если бы переставили дату перед перезапуском, только тут сдвигаем выставленное ограничение баннера.
        let options = AppLaunchOptions(
            launchType: .default,
            userDefaults: ["hideCreditPromoUntilDateTs": Date().timeIntervalSinceReferenceDate]
        )
        launch(options: options)

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .exist()
    }

    func test_favorites_noApp_loggined() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.ShowCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .tapNewApplicationButton()
            .enterFio("Вася Пупкин")
            .tapSubmit()
            .exist(selector: "WizardViewController")
    }

    func test_favorites_hasDraftApp_loggined() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.ShowCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .tapContinueApplicationButton()
            .exist(selector: "CreditFormViewController")
    }

    func test_favorites_hasActiveApp_loggined() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)
            .favoriteLast()
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.ShowCreditBannerInFavourites())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()

        mainSteps
            .openFavoritesTab()
            .creditPromo()
            .notExist()
    }

    func test_listing_noApp_notLoggined() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        let step = openListingBanner(isDraft: false)
            .exist(selector: "field_name")
            .validateSnapShot(
                accessibilityId: "InlinePreliminaryCreditClaimViewController",
                snapshotId: "creditBanerModal_Empty"
            )
            .fill()

        _ = sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)

        step
            .enterLoginCode("1221")
            .exist(selector: "WizardViewController")
    }

    func test_listing_noApp_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        openListingBanner(isDraft: false)
            .exist(selector: "field_name")
            .validateSnapShot(
                accessibilityId: "InlinePreliminaryCreditClaimViewController",
                snapshotId: "creditBanerModal_Filled"
            )
            .enterFio("Вася Пупкин")
            .tapSubmit()
            .exist(selector: "WizardViewController")
    }

    func test_listing_noApp_promo_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["creditPromoBannerEnable"] = true

        launch()

        openListingBanner(isDraft: false, isPromo: true)
            .exist(selector: "field_name")
    }

    func test_listing_hasDraftApp_promo_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        launch()

        openListingBanner(isDraft: true, isPromo: true)
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_hasDraftApp_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()

        openListingBanner(isDraft: true)
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_hasDraftApp_notLoggined() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        let step = openListingBanner(isDraft: false)
            .exist(selector: "field_name")
            .validateSnapShot(
                accessibilityId: "InlinePreliminaryCreditClaimViewController",
                snapshotId: "creditBanerModal_Empty"
            )
            .fill()

        _ = sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .enterLoginCode("1221")
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_hasActiveApp_notLoggined() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true

        launch()

        let step = openListingBanner(isDraft: false)
            .exist(selector: "field_name")
            .validateSnapShot(
                accessibilityId: "InlinePreliminaryCreditClaimViewController",
                snapshotId: "creditBanerModal_Empty"
            )
            .fill()

        _ = sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)

        step
            .enterLoginCode("1221")
            .exist(selector: "CreditLKViewController")
    }

    func test_listing_hasActiveApp_loggined() {
        sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)
            .start()

        settings["skipPreliminaryClaimDraft"] = true

        launch()

        openListingBanner(isDraft: false)
            .exist(selector: "CreditLKViewController")
    }

    func test_listing_hasDraftApp_snippetRef_Extended_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .scrollSnippetGallery(offerID: "1100934254-6093fdd8", elementToFind: "extendedCreditLayout")
            .tap("extendedCreditLayout")
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_hasDraftApp_snippetRef_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .scrollSnippetGallery(offerID: "1100945548-4b50e2ee", elementToFind: "smallCreditLayout")
            .tap("smallCreditLayout")
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_noApp_priceSnippet() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .tapOnCreditPrice()
            .exist(selector: "InlinePreliminaryCreditClaimViewController")
    }

    func test_listing_draft_priceSnippet() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .tapOnCreditPrice()
            .exist(selector: "CreditFormViewController")
    }

    func test_listing_active_priceSnippet() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .tapOnCreditPrice()
            .exist(selector: "CreditLKViewController")
    }

    func test_listing_no_app_auto_show() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        userDefaults["creditPromoShowed"] = false
        settings["creditAutoShowOnListingEnable"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .exist(selector: "InlinePreliminaryCreditClaimViewController")
    }

    func test_listing_no_app_no_auto_show_openCardFromDeeplink() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        userDefaults["creditPromoShowed"] = false
        settings["creditAutoShowOnListingEnable"] = true

        launch(link: "https://auto.ru/cars/used/sale/\(offerId)")
        mainSteps
            .notExist(selector: "InlinePreliminaryCreditClaimViewController")
    }

    func test_listing_no_app_not_auto_show_alreadyShowed() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        userDefaults["creditPromoShowed"] = true
        settings["creditAutoShowOnListingEnable"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 2)
            .notExist(selector: "InlinePreliminaryCreditClaimViewController")
    }

    func test_listing_no_app_not_auto_show_allowedForCredit() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .mockSearch(photoCount: 1)
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        userDefaults["creditPromoShowed"] = false
        settings["creditAutoShowOnListingEnable"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .tapOnField(.creditPrice)
            .tap("В кредит")
            .tap("Готово")
            .as(FiltersSteps.self)
            .showResultsTap()
            .wait(for: 2)
            .notExist(selector: "InlinePreliminaryCreditClaimViewController")
    }

    func test_report_noApp_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true

        openReportBanner(state: .new)
            .exist(selector: "field_name")
            .validateSnapShot(
                accessibilityId: "InlinePreliminaryCreditClaimViewController",
                snapshotId: "creditBanerModal_Filled"
            )
            .enterFio("Вася Пупкин")
            .tapSubmit()
            .exist(selector: "WizardViewController")
    }

    func test_report_hasDraftApp_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        openReportBanner(state: .draft)
            .exist(selector: "CreditFormViewController")
    }

    func test_report_hasDraftApp_promo_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openReportBanner(state: .draft, isPromo: true)
            .exist(selector: "CreditFormViewController")
    }

    func test_report_hasActiveApp_promo_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openReportBanner(state: .active, isPromo: true)
            .exist(selector: "CreditLKViewController")
    }

    func test_report_hasActiveApp_loggined() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        openReportBanner(state: .active)
            .exist(selector: "CreditLKViewController")
    }

    func test_form_promo() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        launch(
            on: .creditFormScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/my/credits"),
                overrideAppSettings: [
                    "skipPreliminaryClaimDraft": true,
                    "skipCreditAlert": true,
                    "creditPromoBannerEnable": true
                ]
            )
        )
        .as(SharkFullFormSteps.self)
        .exist(selector: ".credit_root.creditPromo")
        .tap(".credit_root.creditPromo")
        .exist(selector: "webView")
    }

    func test_lk_promo_draft() {
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
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openCredit()
            .exist(selector: "promo")
            .tap("promo")
            .exist(selector: "webView")
    }

    func test_lk_promo_noApp() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openCredit()
            .exist(selector: "promo")
            .tap("promo")
            .exist(selector: "webView")
    }

    func test_card_promo_approved() {
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

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .scrollTo("credit_promo")
            .exist(selector: "credit_promo")
            .tap("credit_promo")
            .exist(selector: "webView")
    }

    func test_card_promo_and_auth_noApp() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        let steps = launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"))
        )

        steps.scrollTo("credit_promo")
            .exist(selector: "credit_promo")
            .tap("credit_promo")
            .exist(selector: "webView")
            .wait(for: 1)
            .tap("webViewCloseButton")

        let preliminarySteps = steps.scrollTo(
            "credit_calculator",
            windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0)
        )
        .openPreliminarySteps()

        preliminarySteps
            .enterFio("Петрулио Буэнди Сальвадор")
            .enterEmail("pet@sa.ru")
            .enterPhone("9875643212")
            .tapDone()
            .as(SaleCardSteps.self)
            .scrollTo(
                "preliminary_credit_claim_agreeement",
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0)
            )
            .as(PreliminaryCreditSteps.self)
            .tapSubmit()
            .enterLoginCode("1234")
            .exist(selector: "WizardViewController")
    }

    func test_card_promo_and_auth_draft() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        let steps = launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"))
        )
        steps.scrollTo("credit_promo", windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0))
            .exist(selector: "credit_promo")
            .tap("credit_promo")
            .exist(selector: "webView")
            .wait(for: 1)
            .tap("webViewCloseButton")

        let preliminarySteps = steps.scrollTo(
            "credit_calculator",
            windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0)
        )
        .openPreliminarySteps()

        sharkMocker.mockApplication(
            status: .draft,
            claims: [],
            offerType: .offer(
                id: "1090794514-915f196d_test",
                isActive: true,
                allowedProducts: [.tinkoff_1, .tinkoff_2]
            )
        )

        preliminarySteps
            .enterFio("Петрулио Буэнди Сальвадор")
            .enterEmail("pet@sa.ru")
            .enterPhone("9875643212")
            .tapDone()
            .as(SaleCardSteps.self)
            .scrollTo(
                "preliminary_credit_claim_agreeement",
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0)
            )
            .as(PreliminaryCreditSteps.self)
            .tapSubmit()
            .enterLoginCode("1234")
            .exist(selector: "CreditFormViewController")
    }

    func test_card_auth_activeApp() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true

        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))

        let steps = SaleCardSteps(context: self)
        let preliminarySteps = steps.scrollTo(
            "credit_calculator",
            windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0)
        )
        .openPreliminarySteps()

        preliminarySteps
            .enterFio("Петрулио Буэнди Сальвадор")
            .enterEmail("pet@sa.ru")
            .enterPhone("9875643212")
            .tapDone()
            .as(SaleCardSteps.self)
            .scrollTo(
                "preliminary_credit_claim_agreeement",
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0)
            )
            .as(PreliminaryCreditSteps.self)
            .tapSubmit()

        _ = sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)

        preliminarySteps
            .enterLoginCode("1221")
            .exist(selector: "CreditLKViewController")
    }

    func test_listing_hasDraftApp_snippetRef_Extended_loggined_no_percents() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .mockSearch(photoCount: 1)
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditPercents())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .scrollSnippetGallery(
                offerID: "1100934254-6093fdd8",
                elementToFind: "extendedCreditLayout",
                scrollForcefully: true
            )
            .exist(selector: "Этот автомобиль в кредит")
    }

    func test_listing_hasDraftApp_snippetRef_loggined_no_percents() {
        let mocker = sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .mockSearch(photoCount: 1)
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.HideCreditPercents())
        api.device.hello.post.ok(mock: experiments.toMockSource())
        mocker.start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch()
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .scrollSnippetGallery(
                offerID: "1100945548-4b50e2ee",
                elementToFind: "smallCreditLayout",
                scrollForcefully: true
            )
            .exist(selector: "В кредит")
    }

    func test_superMenu_and_auth_noApp() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openSuperMenu()
            .tap(.credit)
            .should(provider: .creditLKScreen, .exist)
            .focus {
                $0.tap(.fieldFIO)
                app.typeText("Тест Тестович Тестов")
                $0.tap(.fieldEmail)
                app.typeText("test@test.ru")
                $0.tap(.fieldPhone)
                app.typeText("9875643212")
                $0.tap(.submitButton)
                    .wait(for: 1)
                $0.tap(.fieldSMSCode)
                app.typeText("1234")
            }
            .should(provider: .creditWizardScreen, .exist)
    }

    func test_superMenu_and_auth_draft() {
        sharkMocker
            .forceLogin(false)
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

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        openSuperMenu()
            .tap(.credit)
            .should(provider: .creditLKScreen, .exist)
            .focus {
                $0.tap(.fieldFIO)
                app.typeText("Тест Тестович Тестов")
                $0.tap(.fieldEmail)
                app.typeText("test@test.ru")
                $0.tap(.fieldPhone)
                app.typeText("9875643212")
                $0.tap(.submitButton)
                    .wait(for: 1)
                $0.tap(.fieldSMSCode)
                app.typeText("1234")
            }
            .should(provider: .creditFormScreen, .exist)
    }

    func test_superMenu_and_auth_activeApp() {
        sharkMocker
            .forceLogin(false)
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        settings["creditPromoBannerEnable"] = true

        let screen = openSuperMenu()
            .tap(.credit)
            .should(provider: .creditLKScreen, .exist)
        screen.focus {
            $0.tap(.fieldFIO)
            app.typeText("Тест Тестович Тестов")
            $0.tap(.fieldEmail)
            app.typeText("test@test.ru")
            $0.tap(.fieldPhone)
            app.typeText("9875643212")
            $0.tap(.submitButton)
                .wait(for: 1)
        }

        _ = sharkMocker
            .forceLogin(true)
            .baseMock(offerId: offerId)
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)

        screen.focus {
            $0.tap(.fieldSMSCode)
            app.typeText("1234")
        }
        .wait(for: 1)
        .should(provider: .creditLKScreen, .exist)
    }

    private func openListingBanner(isDraft: Bool, isPromo: Bool = false) -> PreliminaryCreditSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollTo("credit_baner", maxSwipes: 25)
            .validateSnapShot(
                accessibilityId: "credit_baner",
                snapshotId: "creditBanerListing_\(isDraft ? "draft" : "noApp")\(isPromo ? "_promo" : "")"
            )
            .tap("credit_baner").as(PreliminaryCreditSteps.self)
    }

    enum BannerStatus {
        case new
        case draft
        case active
        case newCar(isFirstCar: Bool)
        case noProduct
    }

    private func openReportBanner(state: BannerStatus, isPromo: Bool = false) -> PreliminaryCreditSteps {
        let snapshotId: String
        switch state {
        case .new:
            snapshotId = "creditBanerReport_noApp\(isPromo ? "_promo" : "")"
        case .draft, .newCar, .noProduct:
            snapshotId = "creditBanerReport_draft\(isPromo ? "_promo" : "")"
        case .active:
            snapshotId = "creditBanerReport_active\(isPromo ? "_promo" : "")"
        }

        app.launchEnvironment["LAUNCH_DEEPLINK_URL"] = "https://auto.ru/history/WP0ZZZ99ZHS112625"
        mocker.mock_reportLayoutForSearch(bought: true)
        mocker.mock_reportLayoutForReport(bought: true)

        launch()
        let startScreen = CarfaxStandaloneScreen(app)
        startScreen.openFullReportButton
            .shouldExist(timeout: 5).tap()

        let steps = mainSteps.as(CarfaxStandaloneCardBasicSteps.self)
            .tap(headerButton: .expand)
            .scrollToHeaderBottom()
            .tap(headerButton: .characteristics)
            .scrollToCredit()
            .validateSnapshot(of: "creditBanner", snapshotId: snapshotId)
            .as(ReportCreditSteps.self)

        switch state {
        case .new:
            return steps.openCredit()
        case .draft, .newCar, .noProduct:
            return steps.openDraftCredit()
        case .active:
            return steps.openActiveCredit()
        }
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
