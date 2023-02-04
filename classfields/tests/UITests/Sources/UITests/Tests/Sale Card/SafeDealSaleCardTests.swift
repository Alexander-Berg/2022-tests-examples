import XCTest
import AutoRuProtoModels

/// @depends_on AutoRuServices AutoRuSaleCard AutoRuSafeDeal
final class SafeDealSaleCardTests: BaseTest {
    private static let offerID = "1098252972-99d8c274"
    private static let dealID = "4eebee84-0919-4ace-8869-d899c455d885"

    private let safeDealInfo: Auto_Api_SafeDealInfo = .with { model in
        model.commissionTariff = .with {
            $0.commissionRub = 0
            $0.commissionWithoutDiscont = 0
        }
    }

    private let creditInfo: Auto_Api_CreditConfiguration = .with { creditInfo in
        creditInfo.creditAmountSliderStep = 100
        creditInfo.creditDefaultTerm = 5
        creditInfo.creditMaxAmount = 1_000_000
        creditInfo.creditMinAmount = 100_000
        creditInfo.creditMinRate = 0.08
        creditInfo.creditOfferInitialPaymentRate = 0.1
        creditInfo.creditStep = 100
        creditInfo.creditTermValues = [1, 2, 3, 4, 5]
    }

    private let userDefaults: [String: Any] = [
        "offerOpenCounter": 2,
        "shouldShowSafeDealOverlay": true
    ]

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_cancelSafeDealRequest() {
        var safeDealInfo = self.safeDealInfo

        safeDealInfo.deals = [
            .with { deal in
                deal.id = Self.dealID
                deal.step = .dealCreated
                deal.state = .inProgress
                deal.participantType = .buyer
            }
        ]

        let expectationCancelSafeDeal = api.safeDeal.deal.update.dealId(Self.dealID)
            .post(parameters: .wildcard)
            .expect { req, _ in
                .okIf(req.byBuyer.cancelDealWithReason.cancelReason == .buyerDoesntWantToBuy)
            }

        mockOffer(safeDealInfo: safeDealInfo)

        let saleCard = launchAndOpenSaleCard()

        saleCard
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell.tap(.cancel)
            }
            .should(provider: .safeDealRequestCancelPopup, .exist)
            .focus { popup in
                popup.tap(.close)
            }
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell.tap(.cancel)
            }
            .do {
                safeDealInfo.deals[0].step = .dealDeclined
                safeDealInfo.deals[0].state = .canceled
                safeDealInfo.deals[0].cancelledBy = .buyer
                mockOffer(safeDealInfo: safeDealInfo)
            }
            .should(provider: .safeDealRequestCancelPopup, .exist)
            .focus { popup in
                popup.tap(.reason("Передумал покупать"))
            }
            .wait(for: [expectationCancelSafeDeal])
            .should(.safeDealStatus, .be(.hidden))
            .scroll(to: .safeDeal)
            .should(.safeDeal, .exist)
    }

    func test_noSafeDealBlock_whenHasNoSafeDealTag() {
        mockOffer(safeDealInfo: .init(), hasNoSafeDealTag: true)

        launchAndOpenSaleCard()
            .focus(on: .actionButtons, ofType: .actionButtonsCell) { cell in
                cell.should(.safeDeal, .be(.hidden))
            }
            .should(.safeDealStatus, .be(.hidden))
            .scroll(to: .sellerInfo)
            .should(.safeDeal, .be(.hidden))
    }

    func test_safeDealStatusBanner_dealCancelled() {
        var safeDealInfo = self.safeDealInfo
        safeDealInfo.deals = [
            .with { deal in
                deal.id = Self.dealID
                deal.step = .dealCancelled
                deal.state = .canceled
                deal.webURL = "https://auto.ru/"
                deal.participantType = .buyer
            }
        ]

        mockOffer(safeDealInfo: safeDealInfo)

        launchAndOpenSaleCard()
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell
                    .should(.title, .contain("К сожалению"))
                    .should(.cancel, .be(.hidden))
                    .should(.link, .match("Перейти к сделкам"))
                    .tap()
            }
            .should(provider: .safeDealListScreen, .exist)
            .should(provider: .navBar, .exist).focus { $0.tap(.back) }
            .scroll(to: .safeDeal)
            .should(.safeDeal, .exist)
    }

    func test_safeDealStatusBanner_dealInviteAccepted() {
        var safeDealInfo = self.safeDealInfo
        safeDealInfo.deals = [
            .with { deal in
                deal.id = Self.dealID
                deal.step = .dealInviteAccepted
                deal.state = .inProgress
                deal.webURL = "https://auto.ru/"
                deal.participantType = .buyer
            }
        ]

        mockOffer(safeDealInfo: safeDealInfo)

        launchAndOpenSaleCard()
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell
                    .should(.title, .contain("Продавец согласился"))
                    .should(.cancel, .be(.hidden))
                    .should(.link, .match("Перейти к сделке"))
                    .tap()
            }
            .should(provider: .webViewPicker, .exist)
            .focus { picker in
                picker.closeBySwipe()
            }
            .scroll(to: .sellerInfo)
            .should(.safeDeal, .be(.hidden))
    }

    func test_safeDealStatusBanner_dealCreated() {
        var safeDealInfo = self.safeDealInfo
        safeDealInfo.deals = [
            .with { deal in
                deal.id = Self.dealID
                deal.step = .dealCreated
                deal.state = .inProgress
                deal.participantType = .buyer
            }
        ]

        mockOffer(safeDealInfo: safeDealInfo)

        launchAndOpenSaleCard()
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell
                    .should(.title, .contain("Подождите пока"))
                    .should(.cancel, .exist)
                    .should(.link, .match("Перейти к сделкам"))
                    .tap(.link)
            }
            .should(provider: .safeDealListScreen, .exist)
            .should(provider: .navBar, .exist).focus { $0.tap(.back) }
            .scroll(to: .sellerInfo)
            .should(.safeDeal, .be(.hidden))
    }

    func test_requestSafeDeal() {
        var safeDealInfo = safeDealInfo
        mockOffer(safeDealInfo: safeDealInfo)

        launchAndOpenSaleCard()
            .focus(on: .actionButtons, ofType: .actionButtonsCell) { cell in
                cell.tap(.safeDeal)
            }
            .focus(on: .safeDeal, ofType: .safeDealSaleCardCell) { cell in
                cell.tap(.requestButton)
            }
            .should(provider: .safeDealSellingPricePopup, .exist)
            .focus { popup in
                popup.tap(.submit)
            }
            .do {
                safeDealInfo.deals = [
                    .with { deal in
                        deal.id = Self.dealID
                        deal.step = .dealCreated
                        deal.state = .inProgress
                        deal.participantType = .buyer
                    }
                ]
                mockOffer(safeDealInfo: safeDealInfo)
            }
            .focus(on: .safeDealStatus, ofType: .safeDealStatusCell) { cell in
                cell.should(.title, .contain("Подождите пока"))
            }
            .scroll(to: .sellerInfo)
            .should(.safeDeal, .be(.hidden))
    }

    func test_noSafeDealsShowOverlayTapDetails() {
        mockOffer(safeDealInfo: safeDealInfo, creditInfo: creditInfo)

        launchAndOpenSaleCard(userDefaults: userDefaults)
            .should(provider: .safeDealOverlayPopup, .exist)
            .focus { $0.tap(.details) }
            .should(provider: .webViewPicker, .exist)
    }

    func test_noSafeDealsShowOverlayTapUnderstand() {
        mockOffer(safeDealInfo: safeDealInfo, creditInfo: creditInfo)

        launchAndOpenSaleCard(userDefaults: userDefaults)
            .should(provider: .safeDealOverlayPopup, .exist)
            .focus { $0.tap(.understand) }
    }

    func test_noNewBadgeOverlayHidden() {
        mockOffer(safeDealInfo: safeDealInfo, creditInfo: creditInfo)

        launchAndOpenSaleCard(overrideAppSettings: ["showSafeDealNewBadgeOnCard": false])
            .should(provider: .safeDealOverlayPopup, .be(.hidden))
    }

    // MARK: - Private

    private func launchAndOpenSaleCard(
        userDefaults: [String: Any] = [:],
        overrideAppSettings: [String: Any] = [:]
    ) -> SaleCardScreen_ {
        launch(
            on: .transportScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/cars/used/sale/\(Self.offerID)"),
                overrideAppSettings: overrideAppSettings,
                userDefaults: userDefaults
            )
        ) { screen in
            screen
                .should(provider: .saleCardScreen, .exist)
        }
    }

    private func mockOffer(safeDealInfo: Auto_Api_SafeDealInfo, hasNoSafeDealTag: Bool = false) {
        mocker
            .mock_offerCars(
                id: Self.offerID,
                isSalon: false,
                safeDealInfo: safeDealInfo,
                price: 123456,
                tags: hasNoSafeDealTag ? [] : ["allowed_for_safe_deal"]
            )
    }

    private func mockOffer(safeDealInfo: Auto_Api_SafeDealInfo, creditInfo: Auto_Api_CreditConfiguration) {
        mocker
            .mock_offerCars(
                id: Self.offerID,
                isSalon: false,
                safeDealInfo: safeDealInfo,
                price: 123456,
                tags: ["allowed_for_safe_deal", "allowed_for_credit"]
            ) { response in
                response.offer.dealerCreditConfig = creditInfo
            }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_offerFromHistoryLastAll()
            .mock_safeDealCreate(offerID: Self.offerID)
            .mock_safeDealCancel(dealID: Self.dealID, offerID: Self.offerID)

        mocker.startMock()
    }
}
