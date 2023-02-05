import MarketUITestMocks
import XCTest

class CreditCartTests: LocalMockTestCase {

    func testCartMonthlyPaymentVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4091")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Корзина. Ежемесячный платеж отображен")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var cartPage: CartPage!
        var rootPage: RootPage!
        var monthlyPaymentFeed: String?
        var monthlyPaymentCart: String?

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffCredit
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Credit_OrdinaryGoods")
        }

        "Переходим в выдачу. Находим товар с нужной стоимостью".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            feedPage = goToFeed(root: rootPage, with: "iPhone")
            wait(forExistanceOf: feedPage.element)
        }

        "Проверяем, что ежемесячный платеж отображен".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.element.ybm_swipeCollectionView(
                toFullyReveal: snippetPage.financialProductPaymentAmount,
                withVelocity: .slow
            )
            XCTAssert(snippetPage.financialProductPaymentAmount.isVisible)
            monthlyPaymentFeed = snippetPage.financialProductPaymentAmount.label
            XCTAssertEqual(monthlyPaymentFeed, "от 2\u{00a0}523\u{202f}₽")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let button = snippetPage.addToCartButton.element
            mockStateManager?.pushState(bundleName: "Credit_OrdinaryGoods_AddItemToCart")
            button.tap()
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart(root: rootPage)
            wait(forExistanceOf: cartPage.element)
        }

        "Проверяем, что ежемесячный платеж отображен, суммы платежа в выдаче и саммари совпадают".ybm_run { _ in
            let creditInfo = cartPage.credit.creditInfo
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditInfo.element)
            XCTAssert(creditInfo.element.isVisible)

            monthlyPaymentCart = creditInfo.monthlyPayment.label
            XCTAssertEqual(monthlyPaymentCart, "от 2 523 ₽ / мес")
        }

        "Проверяем, что кнопка \"Купить в кредит\" доступна".ybm_run { _ in
            XCTAssert(
                cartPage.credit.creditInfo.buyInCreditButton.element.isVisible,
                "Кнопка \"Купить в кредит\" не отображена"
            )
            XCTAssert(
                cartPage.credit.creditInfo.buyInCreditButton.element.isEnabled,
                "Кнопка \"Купить в кредит\" недоступна"
            )
        }
    }

    func testCartMonthlyPaymentInvisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4144")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Тинькофф")
        Allure.addTitle("Корзина. Ежемесячный платеж скрыт")

        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var cartPage: CartPage!
        var rootPage: RootPage!

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            mockStateManager?.pushState(bundleName: "Experiments_Tinkoff_Credit")
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffCredit
            )
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Basics")
            mockStateManager?.pushState(bundleName: "Credit_CheapGoods")
        }

        "Переходим в выдачу. Находим товар с нужной стоимостью".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            feedPage = goToFeed(root: rootPage, with: "карандаш")
            wait(forExistanceOf: feedPage.element)
        }

        "Проверяем, что ежемесячный платеж не отображен".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            feedPage.element.ybm_swipeCollectionView(toFullyReveal: snippetPage.element)
            XCTAssertFalse(snippetPage.financialProductPaymentAmount.isVisible, "Ежемесячный платеж отображен")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let button = snippetPage.addToCartButton.element
            mockStateManager?.pushState(bundleName: "Credit_CheapGoods_AddItemToCart")
            button.tap()
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart(root: rootPage)
            wait(forExistanceOf: cartPage.element)
        }

        "Проверяем, что ежемесячный платеж скрыт. Кнопка \"Купить в кредит\" недоступна".ybm_run { _ in
            let summary = cartPage.summary
            let credit = cartPage.credit
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: summary.totalPrice.element)
            XCTAssertFalse(credit.creditInfo.element.exists, "Отсутствует кредитный виджет")
            XCTAssertFalse(credit.creditInfo.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertFalse(credit.creditInfo.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }
    }

    func testCartMonthlyPaymentInvisibilityForInapropriateItems() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4787")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет скрыт / комбинация с C&C")

        var cartPage: CartPage!

        "Настраиваем FT".ybm_run { _ in
            disable(toggles: FeatureNames.cartRedesign)
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffCredit
            )
        }

        "Мокаем ручки".ybm_run { _ in
            var cartState = CartState()
            // Можно использовать любые товары - кредитный виджет строится из creditInformation в user/order/options
            cartState.setCartStrategy(with: [.protein, .protein])
            cartState.setUserOrdersState(with: .basic)
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что ежемесячный платеж скрыт. Кнопка \"Купить в кредит\" недоступна".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)
            let creditInfo = cartPage.credit.creditInfo

            XCTAssertFalse(creditInfo.element.exists, "Отсутствует кредитный виджет")
            XCTAssertFalse(creditInfo.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertFalse(creditInfo.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }
    }

    func testCartMonthlyPaymentInvisibilityAfterExceedingMaxAmount() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4825")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет скрывается после добавления второго подходящего товара (цена стала выше максимальной)")

        var cartPage: CartPage!
        var creditInfoPage: CartPage.CreditInfoCellPage!
        var pickerWheel: XCUIElement!

        var cartState = CartState()

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffCredit
            )
        }

        "Мокаем состояние".ybm_run { _ in
            cartState.setCartStrategy(with: [
                modify(FAPIOffer.protein) { $0.cartItemInCartId = Constants.itemId }
            ])
            cartState.setUserOrdersState(with: makeCreditUserOrderOptions())
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что отображен ежемесячный платеж. Кнопка \"Купить в кредит\" доступна".ybm_run { _ in
            creditInfoPage = cartPage.credit.creditInfo
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditInfoPage.element, withVelocity: .slow)

            XCTAssertTrue(creditInfoPage.element.exists, "Отсутствует кредитный виджет")
            XCTAssertTrue(creditInfoPage.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertTrue(creditInfoPage.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }

        "Открываем пикер количества товара".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: cartPage.cartItem(at: 0).element,
                withVelocity: .slow
            )
            cartPage.cartItem(at: 0).countPicker.tap()
            pickerWheel = cartPage.pickerWheel
            ybm_wait(forVisibilityOf: [pickerWheel])
        }

        "Мокаем состояние с 2 шт. товара".ybm_run { _ in
            cartState.changeCartItemsState(with: makeChangeCartItemsBody())
            cartState.setUserOrdersState(with: makeBasicUserOrderOptions())
            stateManager?.setState(newState: cartState)
        }

        "Выбираем количество товара в 2 шт.".ybm_run { _ in
            pickerWheel.adjust(toPickerWheelValue: "2")
            cartPage.countPickerDoneButton.tap()
            wait(forInvisibilityOf: pickerWheel)
        }

        "Проверяем количество товара".ybm_run { _ in
            let totalItems = cartPage.summary.totalItems
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: totalItems.element, withVelocity: .slow)

            XCTAssertEqual(totalItems.title.label, "Товары (2)")
        }

        "Проверяем, что кредитный виджет скрыт".ybm_run { _ in
            XCTAssertFalse(creditInfoPage.element.exists, "Присутствует кредитный виджет")
        }
    }

    func testCartMonthlyPaymentInvisibilityAfterPromocodeApply() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4824")
        Allure.addEpic("Кредиты")
        Allure.addFeature("Корзина")
        Allure.addTitle("Виджет скрывается после применения скидки (цена стала ниже минимальной)")

        var cartPage: CartPage!
        var creditInfoPage: CartPage.CreditInfoCellPage!

        var cartState = CartState()

        "Настраиваем FT".ybm_run { _ in
            disable(toggles: FeatureNames.cartRedesign)
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffCredit
            )
        }

        "Мокаем состояние".ybm_run { _ in
            cartState.setCartStrategy(with: [.protein])
            cartState.setUserOrdersState(with: makeCreditUserOrderOptions())
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем, что отображен ежемесячный платеж. Кнопка \"Купить в кредит\" доступна".ybm_run { _ in
            creditInfoPage = cartPage.credit.creditInfo
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: creditInfoPage.element)

            XCTAssertTrue(creditInfoPage.element.exists, "Отсутствует кредитный виджет")
            XCTAssertTrue(creditInfoPage.monthlyPayment.isVisible, "Ежемесячный платёж не отображён")
            XCTAssertTrue(creditInfoPage.buyInCreditButton.element.isVisible, "Кнопка \"Оформить\" не отображена")
        }

        "Мокаем состояние с отсутствием кредитного виджета".ybm_run { _ in
            cartState.setUserOrdersState(with: makeBasicUserOrderOptionsForPromocode())
            stateManager?.setState(newState: cartState)
        }

        "Применяем промокод".ybm_run { _ in
            let promocode = cartPage.promocode
            cartPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: promocode.element)

            promocode.input.tap()
            promocode.input.typeText("VKSPCXUG")
            promocode.applyButton.tap()
        }

        "Проверяем, что кредитный виджет скрыт".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)

            XCTAssertFalse(creditInfoPage.element.exists, "Присутствует кредитный виджет")
        }
    }

    // MARK: - Helper Methods

    typealias ChangeCartItemsBody = ChangeCartItems.ChangeCartItemsBody
    typealias UserOrderOptions = ResolveUserOrderOptions.UserOrderOptions

    private func makeChangeCartItemsBody() -> ChangeCartItemsBody {
        ChangeCartItems.ChangeCartItemsBody(
            cartItems: [.smartphone],
            threshold: [.already_free]
        )
    }

    private func makeCreditUserOrderOptions() -> UserOrderOptions {
        var item = Item.basic
        item.label = String(Constants.itemId)
        item.basePrice = Constants.basePrice
        item.price = Constants.basePrice

        var cartItem = CartItem.credit
        cartItem.items = [item]

        var summary = Summary.credit
        summary.baseAmount = Constants.basePrice
        summary.totalAmount = Constants.basePrice

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: summary,
            shops: [cartItem]
        )
    }

    private func makeBasicUserOrderOptions() -> UserOrderOptions {
        var item = Item.basic
        item.label = String(Constants.itemId)
        item.count = 2
        item.basePrice = Constants.doubleBasePrice
        item.price = Constants.doubleBasePrice

        var cartItem = CartItem.basic
        cartItem.items = [item]

        var summary = Summary.basic
        summary.baseAmount = Constants.doubleBasePrice
        summary.totalAmount = Constants.doubleBasePrice

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: summary,
            shops: [cartItem]
        )
    }

    private func makeBasicUserOrderOptionsForPromocode() -> UserOrderOptions {
        var summary = Summary.basic
        summary.baseAmount = 1_000
        summary.totalAmount = 1_000
        summary.promoCodeDiscount = 149_000

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: summary,
            shops: [.basic]
        )
    }
}

// MARK: - Nested Types

private extension CreditCartTests {

    enum Constants {
        static let itemId = 555
        static let basePrice = 150_000
        static let doubleBasePrice = 300_000
    }
}
