import XCTest
import AutoRuProtoModels

/// @depends_on AutoRuSafeDeal
final class SafeDealSellingPricePopupTests: BaseTest {
    private static let offerID = "1098252972-99d8c274"
    private static let dealID = "4eebee84-0919-4ace-8869-d899c455d885"

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_requestSafeDeal() {
        let expectationCreateSafeDeal = api.safeDeal.deal.create
            .post
            .expect { req, _ in
                .okIf(req.sellingPriceRub.value == 123456)
            }

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
            .wait(for: [expectationCreateSafeDeal])
    }

    func test_requestSafeDeal_openAgreement() {
        launchAndOpenSaleCard()
            .focus(on: .actionButtons, ofType: .actionButtonsCell) { cell in
                cell.tap(.safeDeal)
            }
            .focus(on: .safeDeal, ofType: .safeDealSaleCardCell) { cell in
                cell.tap(.requestButton)
            }
            .should(provider: .safeDealSellingPricePopup, .exist)
            .focus { popup in
                popup.tap(.agreement)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_requestSafeDeal_changeSellingPrice() {
        let expectationCreateSafeDeal = api.safeDeal.deal.create
            .post
            .expect { req, _ in
                .okIf(req.sellingPriceRub.value == 654321)
            }

        launchAndOpenSaleCard()
            .focus(on: .actionButtons, ofType: .actionButtonsCell) { cell in
                cell.tap(.safeDeal)
            }
            .focus(on: .safeDeal, ofType: .safeDealSaleCardCell) { cell in
                cell.tap(.requestButton)
            }
            .should(provider: .safeDealSellingPricePopup, .exist)
            .focus { popup in
                popup
                    .validateSnapshot()
                    .tap(.price)
                    .step("Удаляем текст в филде с ценой, тапаем на кнопку в расчете что ничего не закрылось") {
                        popup
                            .clearText(in: .price)
                            .tap(.submit)
                    }
                    .tap(.price)
                    .step("Вводим текст и отправляем запрос") {
                        popup
                            .type("654321", in: .price)
                            .tap(.submit)
                    }
            }
            .wait(for: [expectationCreateSafeDeal])
    }

    // MARK: - Private

    private func launchAndOpenSaleCard() -> SaleCardScreen_ {
        launch(on: .transportScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274"))) { screen in
            screen
                .should(provider: .saleCardScreen, .exist)
        }
    }

    private func setupServer() {
        let safeDealInfo: Auto_Api_SafeDealInfo = .with { model in
            model.commissionTariff = .with {
                $0.commissionRub = 0
                $0.commissionWithoutDiscont = 0
            }
        }

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_offerFromHistoryLastAll()
            .mock_safeDealCreate(offerID: Self.offerID)
            .mock_safeDealCancel(dealID: Self.dealID, offerID: Self.offerID)
            .mock_offerCars(
                id: Self.offerID,
                isSalon: false,
                safeDealInfo: safeDealInfo,
                price: 123456,
                tags: ["allowed_for_safe_deal"]
            )

        mocker.startMock()
    }
}
