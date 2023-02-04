import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuCellHelpers AutoRuSaleCard AutoRuPayments AutoRuStandaloneCarHistory
final class SaleCardPaymentTests: BaseTest {
    private static let searchURI = "POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
    private static let offerID = "1101101721-a355a648-sale-card-payments"
    private static let vinURI = "GET /carfax/offer/cars/\(offerID)/raw?version=2"

    override var appSettings: [String: Any] {
        var settings = super.appSettings
        for (k, v) in desiredAppSettings { settings[k] = v }
        return settings
    }

    private let suiteName = SnapshotIdentifier.suiteName(from: #file)
    private let profileData: Data = {
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "112231"
            profile.user.profile.autoru.about = ""
            return profile
        }()
        return try! userProfile.jsonUTF8Data()
    }()

    private lazy var mainSteps = MainSteps(context: self)

    private var desiredAppSettings: [String: Any] = [
        "shouldUseTestKassaKey": true
    ]

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: -

    func test_report_discountTakesPrecedenceOverBenefit_card() {
        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "PaymentHistoryAll_discount-precedence")
        }
        server.addHandler("GET /offer/CARS/1101613244-b69e1290") { (_, _) -> Response? in
            return Response.okResponse(fileName: "PaymentHistoryAll_discount-precedence_offer")
        }
        server.addHandler("GET /carfax/offer/cars/1101613244-b69e1290/raw?version=2") { (_, _) -> Response? in
            return Response.okResponse(fileName: "PaymentHistoryAll_discount-precedence_carfax")
        }
        mocker.mock_reportLayoutForOffer(bought: false)

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
            .scrollToReportBuySingleButton(windowInsets: .init(top: 0, left: 0, bottom: 80, right: 0))

        let buttonsContainer = steps.onSaleCardScreen()
            .find(by: "backend_layout_cell_2").firstMatch

        Snapshot.compareWithSnapshot(image: buttonsContainer.waitAndScreenshot().image)
    }

    func test_report_discountTakesPrecedenceOverBenefit_payment() {
        server.forceLoginMode = .forceLoggedOut
        advancedMockReproducer.setup(server: server, mockFolderName: "Payment/SaleCardReportDiscountPrecedence")
        mocker.mock_reportLayoutForOffer(bought: false)

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
            .scrollToReportBuySingleButton()
            .tapReportBuySingleButton()

        let authSteps = steps.as(LoginSteps.self)

        authSteps.enterPhone(number: "76665552221")

        server.forceLoginMode = .forceLoggedIn
        authSteps.enterCode("9277")

        let packagesContainer = steps.onModalScreen().packagesContainer
        Snapshot.compareWithSnapshot(image: packagesContainer.waitAndScreenshot().image)
    }

    func test_report_Benefit_card() {
        advancedMockReproducer.setup(server: server, mockFolderName: "Payment/SaleCardReportBenefit")
        mocker.mock_reportLayoutForOffer(bought: false)

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
            .scrollToReportBuySingleButton(windowInsets: .init(top: 0, left: 0, bottom: 80, right: 0))

        let buttonsContainer = steps.onSaleCardScreen()
            .find(by: "backend_layout_cell_2").firstMatch

        Snapshot.compareWithSnapshot(image: buttonsContainer.waitAndScreenshot().image)
    }

    func test_report_Benefit_payment() {
        advancedMockReproducer.setup(server: server, mockFolderName: "Payment/SaleCardReportBenefit")
        mocker.mock_reportLayoutForOffer(bought: false)

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
            .scrollToReportBuySingleButton()
            .tapReportBuySingleButton()
            .wait(for: 1)

        let packagesContainer = steps.onModalScreen().packagesContainer
        Snapshot.compareWithSnapshot(
            image: packagesContainer.waitAndScreenshot().image,
            overallTolerance: 0.005
        )
    }

    func test_exp_preselectMaxSize() {
        mocker.mock_reportLayoutForOffer(bought: false)

        server.addHandler("POST /billing/autoru/payment/init") { (_, _) -> Response? in
            return Response.okResponse(
                message: Auto_Api_Billing_InitPaymentResponse.with { (response: inout Auto_Api_Billing_InitPaymentResponse) in
                    response.cost = 99000
                    response.baseCost = 99000
                    response.ticketID = "xxx"
                    response.salesmanDomain = "autoru"
                    response.detailedProductInfos.append(
                        Auto_Api_Billing_InitPaymentResponse.DetailedProductInfo.with { (response: inout Auto_Api_Billing_InitPaymentResponse.DetailedProductInfo) in
                            response.days = 365
                            response.effectivePrice = 99000
                            response.basePrice = 99000
                            response.duration = .init(seconds: 31_536_000, nanos: 0)
                            response.name = "Отчет о проверке по VIN"
                        }
                    )
                },
                userAuthorized: true
            )
        }

        launch()

        let steps = openListing()
            .openCarOffer(with: Self.offerID)
            .scrollToReportBuySingleButton()

        Snapshot.compareWithSnapshot(
            image: steps.onSaleCardScreen().reportBuySingleButton.waitAndScreenshot().image,
            identifier: "\(#function)_single_button"
        )

        _ = steps.tapReportBuySingleButton()

        let paymentSteps = SaleCardPaymentModalSteps(context: steps.context, source: steps)
        let wholeScreen = paymentSteps.wait(for: 1).onModalScreen().paymentOptionsScreen
        Snapshot.compareWithSnapshot(
            image: wholeScreen.waitAndScreenshot().image,
            overallTolerance: 0.005
        )
    }

    // MARK: -

    private static func carfaxFreePreviewOkResponse(mutation: (inout Auto_Api_RawVinReportResponse) -> Void = { _ in }) -> Response {
        var model: Auto_Api_RawVinReportResponse = .init(mockFile: "CarfaxCard_free-preview")
        mutation(&model)

        let encodingOptions: JSONEncodingOptions = {
            var o = JSONEncodingOptions()
            o.preserveProtoFieldNames = true
            return o
        }()

        return Response.okResponse(message: model, options: encodingOptions, userAuthorized: true)
    }

    private static func listingOkResponse(mutation: (inout Auto_Api_Offer) -> Void) -> Response {
        var model: Auto_Api_OfferListingResponse = .init(mockFile: "SaleListHeaderTests_single-offer")
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "CarfaxCard_mercedes-e_info")
        model.offers[0] = offerResponse.offer
        model.offers[0].id = Self.offerID
        mutation(&model.offers[0])
        return Response.okResponse(message: model)
    }

    private func setupServer() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1101296108-80ca49a5") { (_, _) -> Response? in
            Response.okResponse(fileName: "CarfaxCard_mercedes-e_info", userAuthorized: true)
        }

        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { _ in }
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    private func openListing() -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
    }
}
