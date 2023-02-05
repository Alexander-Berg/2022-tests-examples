import MarketUITestMocks
import XCTest

final class CashbackWithoutYandexPlusTestCase: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        disable(toggles: FeatureNames.cartRedesign)
    }

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testNoPointsWithCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4032")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Баллы для неплюсовика")
        Allure.addTitle("Баллов нет, в заказе есть кешбэк.")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackWithoutYandexPlus")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Сравнить блок с баллами".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.cashbackSpendOptionCell.element)

            XCTAssertEqual(checkoutPage.cashbackEmitOptionCell.title.label, "Получить")
            XCTAssertEqual(checkoutPage.cashbackSpendOptionCell.title.label, "Списать пока нечего")
        }

        "Нажимаем на кнопку 'У Вас копятся баллы' и переходим в Дом Плюса".ybm_run { _ in
            checkoutPage.homePlusCell.element.tap()

            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    func testNoPointsNoCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4034")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Баллы для неплюсовика")
        Allure.addTitle(" Баллов нет, в заказе нет кешбэка")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "NoCashbackWithoutYandexPlus")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Убеждаемся, что секции с кэшбеком нет".ybm_run { _ in
            XCTAssertFalse(checkoutPage.homePlusCell.element.exists)
        }
    }

    func testWithPointsNoCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4036")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Баллы для неплюсовика")
        Allure.addTitle("Баллы есть, в заказе нет кешбэка")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setNoZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "WithPointsNoCashbackWithoutYandexPlus")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Убеждаемся, что секция с кешбэком и дисклеймером есть, заголовки правильные".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.homePlusCell.element)

            XCTAssertEqual(checkoutPage.homePlusCell.title.label, "У вас копятся баллы")
            XCTAssertEqual(checkoutPage.disclaimerHomePlusCell.title.label, "Подключите Плюс, чтобы их тратить")

            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.cashbackSpendOptionCell.element)
        }

        "Нажимаем на заголовок и переходим в Дом Плюса".ybm_run { _ in
            checkoutPage.homePlusCell.element.tap()

            wait(forExistanceOf: HomePlusPage.current.element)
        }

        "Закрываем Дом Плюса и нажимаем кнопку 'Не списывать баллы', переходим в Дом Плюса".ybm_run { _ in
            HomePlusPage.current.element.swipeDown()

            checkoutPage.cashbackSpendOptionCell.element.tap()
            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    func testWithPointsWithCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4035")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Баллы для неплюсовика")
        Allure.addTitle("Баллы есть, в заказе есть кешбэк")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setNoZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "WithPointsWithCashbackWithoutYandexPlus")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Убеждаемся, что секция с кешбэком и дисклеймером есть, заголовки правильные".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.homePlusCell.element)

            XCTAssertEqual(checkoutPage.homePlusCell.title.label, "У вас копятся баллы")
            XCTAssertEqual(checkoutPage.disclaimerHomePlusCell.title.label, "Подключите Плюс, чтобы их тратить")

            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.cashbackSpendOptionCell.element)
        }

        "Нажимаем на кнопку 'Получить X', ничего не должно произойти".ybm_run { _ in
            checkoutPage.cashbackEmitOptionCell.element.tap()
        }

        "Нажимаем кнопку 'Списать X', переходим в Дом Плюса".ybm_run { _ in
            checkoutPage.cashbackSpendOptionCell.element.tap()
            wait(forExistanceOf: HomePlusPage.current.element)
        }

        "Закрываем Дом Плюса, нажимаем на заголовок и переходим опять в Дом Плюса".ybm_run { _ in
            HomePlusPage.current.element.swipeDown()

            checkoutPage.homePlusCell.element.tap()
            wait(forExistanceOf: HomePlusPage.current.element)
        }

        "Закрываем Дом Плюса".ybm_run { _ in
            HomePlusPage.current.element.swipeDown()

            wait(forExistanceOf: checkoutPage.homePlusCell.element)
        }
    }

    func testCashbackSummarySection() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4033")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Кешбэк для неплюсовика")
        Allure.addTitle("Баллов нет, в заказе есть кешбэк")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        setZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackWithoutYandexPlus")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Листаем до блока саммари кешбэка и нажимаем на 'Вернётся на Плюс'".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.summaryTotalCell.element)

            XCTAssertEqual(checkoutPage.summaryCashbackCell.title.label, "Вернётся на Плюс")
            checkoutPage.summaryCashbackCell.element.tap()

            wait(forExistanceOf: CashbackAboutPage.current.element)
        }
    }

    func testCashbackPaymentAfterCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3892")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Кешбэк для неплюсовика")
        Allure.addTitle("Оплата после оформления заказа")

        enable(toggles: FeatureNames.applePay)

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var paymentMethodPopupPage: CheckoutPaymentMethodPopupPage!
        var myOrdersPage: OrdersListPage!

        let orderId = "32803403"

        setNoZeroBalanceState()

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackAfterPayment")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Листаем до блока способа оплаты и нажимаем на него".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentMethodCell.element)
            paymentMethodPopupPage = checkoutPage.paymentMethodCell.tap()

            wait(forVisibilityOf: paymentMethodPopupPage.element)
        }

        "Проверяем subtitle и закрываем попап".ybm_run { _ in
            XCTAssertFalse(paymentMethodPopupPage.paymentMethod(with: "APPLE_PAY").subtitle.exists)
            XCTAssertEqual(paymentMethodPopupPage.paymentMethod(with: "APPLE_PAY").title.label, "Apple Pay")
            XCTAssertFalse(paymentMethodPopupPage.paymentMethod(with: "YANDEX").subtitle.exists)
            XCTAssertEqual(paymentMethodPopupPage.paymentMethod(with: "YANDEX").title.label, "Картой онлайн")

            paymentMethodPopupPage.continueButton.tap()
        }

        "Листаем вниз и \"Подтверждаем заказ\"".ybm_run { _ in
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            let finishPage = checkoutPage.paymentButton.tap()

            wait(forExistanceOf: finishPage.element)
        }

        "Закрываем страничку и переходим в \"Мои заказы\"".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()

            root.tabBar.profileTabItem.tap()
            wait(forVisibilityOf: root.tabBar.profilePage.element)
            myOrdersPage = root.tabBar.profilePage.myOrders.tap()
            wait(forVisibilityOf: myOrdersPage.element)
        }

        "Нажимаем \"Оплатить заказ\", проверяем subtitle-ы у способов оплаты".ybm_run { _ in
            let editPaymentPage = myOrdersPage.payButton(orderId: orderId).tap()

            wait(forVisibilityOf: editPaymentPage.element)

            XCTAssertFalse(editPaymentPage.paymentMethod(withIdentifier: "APPLE_PAY").subtitle.exists)
            XCTAssertEqual(editPaymentPage.paymentMethod(withIdentifier: "APPLE_PAY").title.label, "Apple Pay")
            XCTAssertFalse(editPaymentPage.paymentMethod(withIdentifier: "YANDEX").subtitle.exists)
            XCTAssertEqual(editPaymentPage.paymentMethod(withIdentifier: "YANDEX").title.label, "Картой онлайн")
        }
    }

    func testYandexPlusBlockInProfileWithoutScores() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3789")
        Allure.addEpic("Кешбэк")
        Allure.addFeature("Кешбэк для неплюсовика без баллов")
        Allure.addTitle("Блок Плюса в профиле")

        setZeroBalanceState()

        testYandexPlusBlockInProfile(
            bundleName: "UserNoPlusNoScores",
            title: "Подключите Плюс",
            description: "И получайте кешбэк за покупки"
        )
    }

    func testYandexPlusBlockInProfileWithScores() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3788")
        Allure.addEpic("Кешбэк")
        Allure.addFeature("Кешбэк для неплюсовика с баллами")
        Allure.addTitle("Блок Плюса в профиле")

        setNoZeroBalanceState()

        testYandexPlusBlockInProfile(
            bundleName: "UserNoPlusWithScores",
            title: "Яндекс Плюс",
            description: "Подключите Плюс, чтобы тратить баллы"
        )
    }

    private func testYandexPlusBlockInProfile(bundleName: String, title: String, description: String) {
        var root: RootPage!
        var profile: ProfilePage!

        enable(toggles: FeatureNames.showPlus)

        "Запускаем приложение, авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Переходим в профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.collectionView)
        }

        "Проверяем отображение блока Плюса, переходим в Дом Плюса".ybm_run { _ in
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.yandexPlus.element)

            XCTAssertEqual(title, profile.yandexPlus.title.label)
            XCTAssertEqual(description, profile.yandexPlus.description.label)

            profile.yandexPlus.element.tap()

            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    private func setZeroBalanceState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }
    }

    private func setNoZeroBalanceState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }
    }
}
