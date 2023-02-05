import MarketUITestMocks
import XCTest

class StationSubscriptionCheckoutTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCheckoutPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5414")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Чекаут")

        var checkoutPage: CheckoutPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
            setupUserAddress()
            setupUserOrderOptionsState()
        }

        "Открываем КМ".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = PurchaseSubscriptionViewPage.current.purchaseSubscriptionButton.tap()
            wait(forExistanceOf: checkoutPage.element)
        }

        "Проверяем отображение лигала и текст".ybm_run { _ in
            let disclaimer = checkoutPage.stationDisclaimerCell()
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: disclaimer.element)

            XCTAssert(disclaimer.title.isVisible, "Лигал не отображается")
            XCTAssertEqual(disclaimer.title.text, "Правила пользования подпиской Плюс")
        }

        "Проверяем суммы к оплате".ybm_run { _ in
            checkoutPage.element
                .ybm_swipeCollectionView(toFullyReveal: checkoutPage.summaryItemsCell.element)
            XCTAssertEqual(checkoutPage.summaryItemsCell.details.label, "599 ₽ × 36 мес")
            XCTAssertEqual(checkoutPage.summaryTotalCell.details.label, "599 ₽ × 36 мес")
        }
    }

    func testUnpaidPurchase() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5523")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Дооплата")

        var checkoutPage: CheckoutPage!
        var mediabillingPaymentPage: MediabillingPaymentPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
            enable(toggles: FeatureNames.stationSubscription, FeatureNames.paymentSDK)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
            setupUserAddress()
            setupUserOrderOptionsState()
            setupUnpaidOrderState()
        }

        "Открываем КМ".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = PurchaseSubscriptionViewPage.current.purchaseSubscriptionButton.tap()
            wait(forExistanceOf: checkoutPage.element)
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Ждем появления виджета медиабиллинга и закрываем его".ybm_run { _ in
            mediabillingPaymentPage = MediabillingPaymentPage.current
            wait(forVisibilityOf: mediabillingPaymentPage.element)
            // Свайпаем вниз для закрытия виджета
            mediabillingPaymentPage.element.swipe(
                from: .init(dx: 0, dy: 0),
                to: .init(dx: 0, dy: 3),
                withVelocity: .fast
            )
        }

        "Проверяем, что открылся экран с кнопкой дооплаты".ybm_run { _ in
            finishMultiorderPage = FinishMultiorderPage.current
            wait(forVisibilityOf: finishMultiorderPage.element)
            XCTAssert(finishMultiorderPage.payButton.isVisible)
        }
    }

    func testSuccessPurchase() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5678")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("КМ")
        Allure.addTitle("Спасибо за покупку")

        var checkoutPage: CheckoutPage!
        var mediabillingPaymentPage: MediabillingPaymentPage!
        var finishMultiorderPage: FinishMultiorderPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.insideUITestsKTCreditCheckout] = String(true)
            enable(toggles: FeatureNames.stationSubscription, FeatureNames.paymentSDK)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
            setupUserAddress()
            setupUserOrderOptionsState()
            setupPaidOrderState()
        }

        "Открываем КМ".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Нажимаем на кнопку \"Оформить\"".ybm_run { _ in
            checkoutPage = PurchaseSubscriptionViewPage.current.purchaseSubscriptionButton.tap()
            wait(forExistanceOf: checkoutPage.element)
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Ждем появления виджета медиабиллинга и закрываем его".ybm_run { _ in
            mediabillingPaymentPage = MediabillingPaymentPage.current
            wait(forVisibilityOf: mediabillingPaymentPage.element)
            // Свайпаем вниз для закрытия виджета
            mediabillingPaymentPage.element.swipe(
                from: .init(dx: 0, dy: 0),
                to: .init(dx: 0, dy: 3),
                withVelocity: .fast
            )
        }

        "Проверяем, что открылся экран \"Спасибо за покупку\"".ybm_run { _ in
            finishMultiorderPage = FinishMultiorderPage.current
            wait(forVisibilityOf: finishMultiorderPage.element)
        }

        "Проверяем, способ оплаты и отсутствие кнопки \"Документы по заказу\"".ybm_run { _ in
            XCTAssertEqual(finishMultiorderPage.paymentStatus(at: 0).text, "Оплачено по подписке")
            XCTAssertFalse(finishMultiorderPage.receiptsButton(at: 0).isVisible)
        }
    }
}

// MARK: - Helper Methods

private extension StationSubscriptionCheckoutTests {

    typealias Experiment = ResolveBlueStartup.Experiment

    func setupExperiments(_ experiments: [Experiment]) {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        defaultState.setExperiments(experiments: experiments)
        stateManager?.setState(newState: defaultState)
    }

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        var config = CustomSKUConfig(productId: 123, offerId: "offerId")
        config.title = "Яндекс.Станция, подписка на 3 года, черная"
        config.price = 599
        config.isYaSubscriptionOffer = true
        skuState.setSkuInfoState(with: .custom(config))
        stateManager?.setState(newState: skuState)
    }

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        userState.setFavoritePickupPoints(favoritePickups: [.rublevskoye])
        stateManager?.setState(newState: userState)
    }

    func setupUserOrderOptionsState() {
        var cartState = CartState()
        cartState.setUserOrdersState(with: .stationSubscription)
        stateManager?.setState(newState: cartState)
    }

    func setupUnpaidOrderState() {
        var orderState = OrdersState()
        let order = Order.Mapper(
            status: .unpaid,
            payment: .prepaid,
            paymentSubmethod: .stationSubscription,
            delivery: .init(deliveryPartnerType: .yandex, type: .service)
        )
        let orderMapper = OrdersState.UserOrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all])
        orderState.setOutlet(outlets: [.rublevskoye])
        orderState.setOrderPaymentByOrderIds(orderPaymentByOrderIds: .station)
        stateManager?.setState(newState: orderState)
    }

    func setupPaidOrderState() {
        var orderState = OrdersState()
        let order = Order.Mapper(
            id: Constants.orderId,
            status: .unpaid,
            payment: .prepaid,
            paymentSubmethod: .stationSubscription,
            delivery: .init(deliveryPartnerType: .yandex, type: .service),
            msku: [Constants.stationItemId]
        )
        let orderMapper = OrdersState.UserOrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byIds])
        orderState.setOutlet(outlets: [.rublevskoye])
        orderState.setOrderPaymentByOrderIds(orderPaymentByOrderIds: .station)
        orderState.setOrderPaymentById(orderPaymentByOrderIds: .station)
        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension StationSubscriptionCheckoutTests {

    enum Constants {
        static let orderId = "123"
        static let stationItemId = "123"
    }
}
