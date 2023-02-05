import MarketUITestMocks
import XCTest

final class CartCashbackDetailsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testDefaultCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5610")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Корзина")
        Allure.addTitle("Стандартный кешбэк")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cartState = CartState()

            var offer = FAPIOffer.default1
            offer.cartItemInCartId = 555
            cartState.setCartStrategy(with: [offer])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(for: [(value: 100, offer: offer)])
            orderOptionsState.cashback?.applicableOptions.emit.details = CashbackOptionsDetails.defaultCashback

            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)

            var сashbackCMSState = CMSState()
            сashbackCMSState.setCMSState(with: CMSState.CMSCollections.defaultCashbackCollections)
            stateManager?.setState(
                newState: сashbackCMSState,
                matchedBy: hasStringInBody("\"type\":\"mp_cashback_description_app\"")
            )
        }

        checkWithAboutPopup(
            cashbackText: " 100",
            popupContent: CashbackAboutPopupContent(
                title: "Кешбэк баллами",
                text: "Вам вернётся до 3 000 баллов.",
                buttonTitle: "Все акции с кешбэком"
            )
        )
    }

    func testExtraCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5611")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Корзина")
        Allure.addTitle("Повышенный кешбэк")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cartState = CartState()

            var offer = FAPIOffer.default1
            offer.cartItemInCartId = 555
            cartState.setCartStrategy(with: [offer])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(for: [(value: 300, offer: offer)])
            orderOptionsState.cashback?.applicableOptions.emit.details = CashbackOptionsDetails.extraCashback

            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)

            var сashbackCMSState = CMSState()
            сashbackCMSState.setCMSState(with: CMSState.CMSCollections.extraCashbackCollections)
            stateManager?.setState(
                newState: сashbackCMSState,
                matchedBy: hasStringInBody("\"type\":\"mp_cashback_description_app\"")
            )
        }

        checkWithAboutPopup(
            cashbackText: " 300",
            popupContent: CashbackAboutPopupContent(
                title: "Повышенный кешбэк баллами",
                text: "Вам вернётся до 3 000 баллов с повышенным кешбэком. Акции с повышенным кешбэком действуют на 7 заказов и ограничены по времени.",
                buttonTitle: "Все акции с кешбэком"
            )
        )
    }

    func testDefaultAndExtraCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5612")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Корзина")
        Allure.addTitle("Товары со стандартным кешбэком + с повышенным")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cartState = CartState()

            var offer = FAPIOffer.default1
            offer.cartItemInCartId = 555
            cartState.setCartStrategy(with: [offer])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(for: [(value: 400, offer: offer)])
            orderOptionsState.cashback?.applicableOptions.emit.details = CashbackOptionsDetails.defaultAndExtraCashback

            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)
        }

        checkWithDetailsPopup(
            cashbackText: " 400",
            popupContent: CashbackDetailsPopupContent(
                title: "Вернётся на Плюс  400",
                cashbackItems: [
                    ("Стандартный кешбэк", " 100"),
                    ("Повышенный кешбэк", " 300")
                ],
                buttonTitle: "Подробнее об акциях"
            )
        )
    }

    func testDefaultAndExtraAndFirstOrderCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5613")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("Корзина")
        Allure.addTitle("Товары со стандартным кешбэком + с повышенным + доступна акция за 1-й заказ")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cartState = CartState()

            var offer = FAPIOffer.default1
            offer.cartItemInCartId = 555
            cartState.setCartStrategy(with: [offer])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(for: [(value: 900, offer: offer)])
            orderOptionsState.cashback?.applicableOptions.emit.details = CashbackOptionsDetails
                .defaultAndExtraAndFirstOrderCashback

            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)
        }

        checkWithDetailsPopup(
            cashbackText: " 900",
            popupContent: CashbackDetailsPopupContent(
                title: "Вернётся на Плюс  900",
                cashbackItems: [
                    ("Стандартный кешбэк", " 100"),
                    ("Повышенный кешбэк", " 300"),
                    ("За первую покупку", " 500")
                ],
                buttonTitle: "Подробнее об акциях"
            )
        )
    }

    // MARK: - Private

    private func checkWithAboutPopup(cashbackText: String, popupContent: CashbackAboutPopupContent) {
        var cartPage: CartPage!
        var popupPage: CashbackAboutPage!

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Нажимаем на кешбэк в саммари".ybm_run { _ in
            let summaryCashback = cartPage.summary.totalCashback
            cartPage.collectionView.swipe(to: .down, until: summaryCashback.element.isVisible)
            XCTAssertEqual(summaryCashback.details.label, cashbackText)
            summaryCashback.element.tap()
        }

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, popupContent.title)
            XCTAssertEqual(popupPage.descriptionText(at: 0).label, popupContent.text)
            XCTAssertEqual(popupPage.linkButton.label, popupContent.buttonTitle)
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

    private func checkWithDetailsPopup(cashbackText: String, popupContent: CashbackDetailsPopupContent) {
        var cartPage: CartPage!
        var popupPage: CashbackDetailsAboutPage!

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Нажимаем на кешбэк в саммари".ybm_run { _ in
            let summaryCashback = cartPage.summary.totalCashback
            cartPage.collectionView.swipe(to: .down, until: summaryCashback.element.isVisible)
            XCTAssertEqual(summaryCashback.details.label, cashbackText)
            summaryCashback.element.tap()
        }

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackDetailsAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, popupContent.title)

            for (index, cashbackItem) in popupContent.cashbackItems.enumerated() {
                let detailsItem = popupPage.detailsItem(at: index)
                XCTAssertEqual(detailsItem.title.label, cashbackItem.0)
                XCTAssertEqual(detailsItem.value.label, cashbackItem.1)
            }

            XCTAssertEqual(
                popupPage.linkButton.label,
                popupContent.buttonTitle
            )
            XCTAssertEqual(popupPage.linkButton.label, popupContent.buttonTitle)
            XCTAssertEqual(popupPage.closeButton.label, "Понятно")
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

}

private extension CartCashbackDetailsTests {

    struct CashbackAboutPopupContent {
        var title: String
        var text: String
        var buttonTitle: String
    }

    struct CashbackDetailsPopupContent {
        var title: String
        var cashbackItems: [(String, String)]
        var buttonTitle: String
    }
}
