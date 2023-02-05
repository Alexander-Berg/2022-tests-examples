import MarketUITestMocks
import XCTest

final class CheckoutCashbackDetailsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCashbackDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5614")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Чекаут")
        Allure.addTitle("Мультизаказ с акцией Mastercard")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var popupPage: CashbackDetailsAboutPage!

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cartState = CartState()

            var offer = FAPIOffer.default1
            offer.cartItemInCartId = 555
            cartState.setCartStrategy(with: [offer])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(for: [(value: 550, offer: offer)])
            orderOptionsState.cashback?.applicableOptions.emit.details = CashbackOptionsDetails
                .defaultAndExtraAndMastercardCashback

            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Нажимаем на кешбэк в саммари".ybm_run { _ in
            let summaryCashback = checkoutPage.summaryCashbackCell
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: summaryCashback.element)
            XCTAssertEqual(summaryCashback.details.label, " 550")
            summaryCashback.element.tap()
        }

        let cashbackItems = [
            ("Стандартный кешбэк", " 100"),
            ("Повышенный кешбэк", " 300"),
            ("По акции Mastercard", " 150")
        ]

        let groupTitles = ["Придёт с товаром", "Придёт после доставки последнего заказа"]

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackDetailsAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, "Вернётся на Плюс  550")

            for (index, cashbackItem) in cashbackItems.enumerated() {
                let detailsItem = popupPage.detailsItem(at: index)
                XCTAssertEqual(detailsItem.title.label, cashbackItem.0)
                XCTAssertEqual(detailsItem.value.label, cashbackItem.1)
            }

            for (index, title) in groupTitles.enumerated() {
                XCTAssertEqual(popupPage.groupTitle(at: index).label, title)
            }

            XCTAssertEqual(popupPage.linkButton.label, "Подробнее об акциях")
            XCTAssertEqual(popupPage.closeButton.label, "Понятно")
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

}

private extension CheckoutCashbackDetailsTests {

    struct CashbackDetailsPopupContent {
        var title: String
        var cashbackItems: [(String, String)]
        var buttonTitle: String
    }
}
