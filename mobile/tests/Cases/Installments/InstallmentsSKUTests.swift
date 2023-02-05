import MarketUITestMocks
import XCTest

final class InstallmentsSKUTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testInstallmentsWidgetVisibility() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5168")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("КМ")
        Allure.addTitle("Отображение виджета рассрочки")

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!
        var skuPage: SKUPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
            setupSKUInfoState()
        }

        "Открываем выдачу. Находим товар стоимостью от 3 000Р до 200 000Р".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "Xiaomi Redmi Note 10 Pro")
        }

        "Проверяем лейблы рассрочки".ybm_run { _ in
            snippetPage = feedPage.collectionView.cellPage(at: 0)
            let productName = snippetPage.financialProductName
            let paymentAmount = snippetPage.financialProductPaymentAmount
            let paymentPeriod = snippetPage.financialProductPaymentPeriod

            wait(forVisibilityOf: snippetPage.element)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssert(productName.isVisible, "Лейбл фин. продукта не виден")
            XCTAssert(paymentAmount.isVisible, "Лейбл суммы платежа не виден")
            XCTAssert(paymentPeriod.isVisible, "Лейбл периода платежа не виден")
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            let installmentsInfo = skuPage.installmentsInfo.element
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo)

            XCTAssert(installmentsInfo.isVisible, "Виджет рассрочки не виден")
        }

        "Проверяем описание рассрочки".ybm_run { _ in
            let title = skuPage.installmentsInfo.title

            XCTAssert(title.isVisible, "Лейбл описания рассрочки не виден")
            XCTAssertEqual(title.label, "Рассрочка от Тинькофф")
        }

        "Проверяем выбранный срок рассрочки".ybm_run { _ in
            let term = skuPage.installmentsInfo.selector.selectedCell.term

            XCTAssert(term.isVisible, "Лейбл выбранного срока рассрочки не виден")
            XCTAssertEqual(term.label, "12 мес")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            let payment = skuPage.installmentsInfo.monthlyPayment

            XCTAssert(payment.isVisible, "Лейбл ежемесячного платежа не виден")
            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Проверяем кнопку \"Оформить\"".ybm_run { _ in
            XCTAssert(skuPage.installmentsInfo.checkoutButton.element.isVisible, "Кнопки \"Оформить\" не видно")
        }
    }

    func testSKUMonthlyPaymentImmutability() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5171")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("КМ")
        Allure.addTitle("Виджет. Перерасчет суммы платежа")

        var skuPage: SKUPage!
        var installmentsInfo: XCUIElement!
        var payment: XCUIElement!
        var compactOfferCartButton: XCUIElement!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
            wait(forVisibilityOf: skuPage.element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            installmentsInfo = skuPage.installmentsInfo.element
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo)

            XCTAssert(installmentsInfo.isVisible, "Виджет рассрочки не виден")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            payment = skuPage.installmentsInfo.monthlyPayment

            XCTAssert(payment.isVisible, "Лейбл ежемесячного платежа не виден")
            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            setupAddItemsToCartState()

            let addButton = skuPage.addToCartButton.element
            skuPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: addButton)
            addButton.tap()

            compactOfferCartButton = CompactOfferViewPage.current.cartButton.element
            ybm_wait(forFulfillmentOf: { compactOfferCartButton.label == "1" })
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo)

            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Увеличиваем каунтер товара до 3 шт.".ybm_run { _ in
            setupChangeCartItemsState(count: 3)

            let plusButton = skuPage.addToCartButton.plusButton
            skuPage.collectionView.ybm_swipeCollectionView(to: .up, toFullyReveal: plusButton)
            plusButton.tap()

            ybm_wait(forFulfillmentOf: { compactOfferCartButton.label == "3" })
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo)

            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }
    }

    func testSKUMonthlyPaymentChangeWhenSelectorScrolled() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5169")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("КМ")
        Allure.addTitle("Виджет. Скролл барабана (км)")

        var skuPage: SKUPage!
        var payment: XCUIElement!
        var selector: SKUPage.InstallmentsSelectorPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
            wait(forVisibilityOf: skuPage.element)
        }

        "Проверяем наличие виджета рассрочки".ybm_run { _ in
            let installmentsInfo = skuPage.installmentsInfo.element
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo)

            XCTAssert(installmentsInfo.isVisible, "Виджет рассрочки не виден")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            payment = skuPage.installmentsInfo.monthlyPayment

            XCTAssert(payment.isVisible, "Лейбл ежемесячного платежа не виден")
            XCTAssertEqual(payment.label, "2 333 ₽ / мес")
        }

        "Свайпаем селектор вверх и проверяем ежемесячный платёж".ybm_run { _ in
            selector = skuPage.installmentsInfo.selector
            selector.element.swipe(from: .zero, to: .init(dx: 0, dy: -20), withVelocity: .slow)
            ybm_wait { payment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(payment.label, "1 167 ₽ / мес")
        }

        "Свайпаем селектор вниз и проверяем ежемесячный платёж".ybm_run { _ in
            selector.element.swipe(from: .zero, to: .init(dx: 0, dy: 20), withVelocity: .slow)
            ybm_wait { payment.label != "1 167 ₽ / мес" }

            XCTAssertEqual(payment.label, "4 665 ₽ / мес")
        }
    }

    func testGoToCheckoutWithoutAddingToCart_OrdinaryGoods() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5210")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("КМ")
        Allure.addTitle("Переход по виджету в чекаут. Обычный товар")

        var skuPage: SKUPage!
        var skuInstallmentsInfo: SKUPage.InstallmentsInfo!
        var skuInstallmentsPayment: XCUIElement!
        var checkoutPage: CheckoutPage!
        var checkoutInstallmentsCell: CheckoutPage.InstallmentsCell!

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupUserAddress()
            setupOrdersState()
            setupSKUInfoState()
            setupCartState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Проверяем наличие виджета рассрочки и ежемесячный платёж".ybm_run { _ in
            skuInstallmentsInfo = skuPage.installmentsInfo
            skuInstallmentsPayment = skuInstallmentsInfo.monthlyPayment
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuInstallmentsInfo.element)

            XCTAssert(skuInstallmentsInfo.element.isVisible, "Виджет рассрочки не виден")
            XCTAssertEqual(skuInstallmentsPayment.label, "2 333 ₽ / мес")
        }

        "Свайпаем селектор вниз и проверяем ежемесячный платёж".ybm_run { _ in
            skuInstallmentsInfo.selector.element.swipe(from: .zero, to: .init(dx: 0, dy: 20), withVelocity: .slow)
            ybm_wait { skuInstallmentsPayment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(skuInstallmentsPayment.label, "4 665 ₽ / мес")
        }

        "Проверяем кнопку \"Оформить\"".ybm_run { _ in
            XCTAssert(skuInstallmentsInfo.checkoutButton.element.isVisible, "Кнопки \"Оформить\" не видно")
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = skuInstallmentsInfo.checkoutButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем способ оплаты - в рассрочку от Тинькофф".ybm_run { _ in
            let paymentMethodCell = checkoutPage.paymentMethodCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentMethodCell.element)

            XCTAssert(paymentMethodCell.element.isVisible, "Способ оплаты не виден")
            XCTAssertEqual(paymentMethodCell.title.label, "В рассрочку от Тинькофф")
        }

        "Проверяем лейбл названия срока рассрочки".ybm_run { _ in
            checkoutInstallmentsCell = checkoutPage.installmentsCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutInstallmentsCell.element)
            let periodLabel = checkoutInstallmentsCell.periodTitle

            XCTAssert(periodLabel.isVisible, "Лейбл названия срока рассрочки не виден")
            XCTAssertEqual(periodLabel.label, "Срок рассрочки:")
        }

        "Проверяем выбранный срок рассрочки на селекторе".ybm_run { _ in
            let selectedTerm = checkoutInstallmentsCell.selector.selectedCell.term

            XCTAssert(selectedTerm.isVisible, "Срок рассрочки не виден")
            XCTAssertEqual(selectedTerm.label, "6 мес")
        }

        "Проверяем ежемесячный платёж".ybm_run { _ in
            let payment = checkoutInstallmentsCell.monthlyPayment

            XCTAssert(payment.isVisible, "Ежемесячный платёж не виден")
            XCTAssertEqual(payment.label, "4 665 ₽ / мес")
        }

        "Проверяем лейбл Итого в саммари с рассрочкой".ybm_run { _ in
            let summaryTotalCell = checkoutPage.summaryTotalCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: summaryTotalCell.element)

            XCTAssertEqual(summaryTotalCell.details.label, "4 665 ₽ × 6 мес")
            XCTAssertEqual(summaryTotalCell.title.label, "Итого")
        }

        "Проверяем кнопку оформления оплаты в рассрочку".ybm_run { _ in
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            XCTAssertEqual(paymentButton.title.label, "Оформить рассрочку")
        }

        "Проверяем лейбл юридической информации".ybm_run { _ in
            let termsDisclaimerCell = checkoutPage.termsDisclaimerCell
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: termsDisclaimerCell.element)

            XCTAssert(termsDisclaimerCell.element.isVisible, "Лейбл юридической информации не виден")
            XCTAssertEqual(
                termsDisclaimerCell.title.label,
                "Нажимая «Оформить рассрочку», вы соглашаетесь с условиями использования сервиса Яндекс.Маркет"
            )
        }
    }

    func testOpenInstallmentsSelectorPopupAndSelectAnotherTerm() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5170")
        Allure.addEpic("Рассрочка. Тинькофф")
        Allure.addFeature("КМ")
        Allure.addTitle("Виджет. Тап на барабан")

        var skuPage: SKUPage!
        var installmentsInfo: SKUPage.InstallmentsInfo!
        var selectorPopupPage: InstallmentsSelectorPopupPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.paymentSDK,
                FeatureNames.tinkoffInstallments
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Проверяем наличие виджета рассрочки и ежемесячный платёж".ybm_run { _ in
            installmentsInfo = skuPage.installmentsInfo
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: installmentsInfo.element)

            XCTAssert(installmentsInfo.element.isVisible, "Виджет рассрочки не виден")
            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "2 333 ₽ / мес")
        }

        "Нажимаем на барабан селектора рассрочи и ждём поднятия попапа селектора".ybm_run { _ in
            selectorPopupPage = installmentsInfo.selector.tap()
            wait(forVisibilityOf: selectorPopupPage.element)
        }

        "Проверяем хедер попапа и лейбл кнопки Выбрать".ybm_run { _ in
            XCTAssertEqual(selectorPopupPage.titleLabel.label, "Выберите срок рассрочки")
            XCTAssertEqual(selectorPopupPage.selectButton.label, "Выбрать")
        }

        "Проверяем наличие ячеек со всеми сроками и платежами".ybm_run { _ in
            selectorPopupPage.collectionView.enumerateCells(cellCount: Constants.terms.count) { cell, indexPath in
                XCTAssert(cell.element.isVisible)
                XCTAssertEqual(cell.term.label, Constants.terms[indexPath.item])
                XCTAssertEqual(cell.monthlyPayment.label, Constants.monthlyPayments[indexPath.item])
            }
        }

        "Проверяем, выбран ли срок рассрочки по умолчанию".ybm_run { _ in
            let selectedCell = selectorPopupPage.collectionView.selectedCell

            XCTAssertEqual(selectedCell.term.label, "12 мес")
            XCTAssertEqual(selectedCell.monthlyPayment.label, "по 2 333 ₽")
        }

        "Выбираем новый срок рассрочки".ybm_run { _ in
            selectorPopupPage.collectionView.cellPage(at: IndexPath(item: 2, section: 0)).element.tap()
        }

        "Проверяем, выбран ли новый срок рассрочки".ybm_run { _ in
            let newSelectedCell = selectorPopupPage.collectionView.selectedCell

            XCTAssertEqual(newSelectedCell.term.label, "24 мес")
            XCTAssertEqual(newSelectedCell.monthlyPayment.label, "по 1 167 ₽")
        }

        "Нажимаем кнопку Выбрать".ybm_run { _ in
            selectorPopupPage.selectButton.tap()
        }

        "Проверяем новый ежемесячный платёж на КМ".ybm_run { _ in
            wait(forVisibilityOf: installmentsInfo.element)
            ybm_wait { installmentsInfo.monthlyPayment.label != "2 333 ₽ / мес" }

            XCTAssertEqual(installmentsInfo.monthlyPayment.label, "1 167 ₽ / мес")
        }
    }
}

// MARK: - Helper Methods

private extension InstallmentsSKUTests {

    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(mapper: .init(fromOffers: [Constants.capiOffer]))
        feedState.setSearchStateFAPI(mapper: .init(fromOffers: [Constants.capiOffer]))
        stateManager?.setState(newState: feedState)
    }

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(offer: Constants.fapiOffer)
        stateManager?.setState(newState: skuState)
    }

    func setupAddItemsToCartState() {
        var cartState = CartState()
        cartState.addItemsToCartState(with: .init(offers: [Constants.capiOffer]))
        cartState.setCartStrategy(with: [Constants.fapiOffer])
        stateManager?.setState(newState: cartState)
    }

    func setupChangeCartItemsState(count: Int) {
        let cartItem = CartItemInCart.makeFromOffer(Constants.fapiOffer, count: count)
        let strategy = modify(
            ResolveUserCartWithStrategiesAndBusinessGroups
                .VisibleStrategiesFromUserCart(offers: [Constants.fapiOffer])
        ) {
            $0.cartItem = [cartItem]
        }

        var cartState = CartState()
        cartState.changeCartItemsState(with: .init(
            cartItems: [cartItem],
            threshold: [.already_free]
        ))
        cartState.setCartStrategy(with: strategy)
        stateManager?.setState(newState: cartState)
    }

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    func setupOrdersState() {
        var ordersState = OrdersState()
        ordersState.setOrdersResolvers(mapper: .default, for: [.all])
        stateManager?.setState(newState: ordersState)
    }

    func setupCartState() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .installments)
        stateManager?.setState(newState: cartState)
    }
}

// MARK: - Nested Types

private extension InstallmentsSKUTests {

    enum Constants {

        static let tinkoffInstallments = "TINKOFF_INSTALLMENTS"
        static let terms = ["6 мес", "12 мес", "24 мес"]
        static let monthlyPayments = ["по 4 665 ₽", "по 2 333 ₽", "по 1 167 ₽"]

        static let capiOffer = modify(CAPIOffer.protein) {
            $0.installmentsInfo = .default
            $0.financialProductPriority = [tinkoffInstallments]
        }

        static let fapiOffer = modify(FAPIOffer.default) {
            $0.installmentsInfo = .default
            $0.financialProductPriority = [tinkoffInstallments]
        }
    }
}
