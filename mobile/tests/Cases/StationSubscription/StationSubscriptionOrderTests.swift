import MarketUITestMocks
import XCTest

class StationSubscriptionOrderTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testStationOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5603")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Отображение заказа стации по подписке")

        var ordersListPage: OrdersListPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupOrdersState()
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Проверяем общую сумму заказа".ybm_run { _ in
            let total = ordersListPage.total(orderId: Constants.stationItemId)
            ybm_wait(forVisibilityOf: [total])
            XCTAssertEqual(total.label, "Итого 599 ₽/мес")
        }

        "Проверяем название и цену товара".ybm_run { _ in
            XCTAssertEqual(
                ordersListPage.orderItemTitleLabel(itemId: Constants.stationItemId).label,
                "Яндекс.Станция, подписка на 3 года, черная"
            )
            XCTAssertEqual(
                ordersListPage.orderItemPriceLabel(itemId: Constants.stationItemId).label,
                "599 ₽ / мес"
            )
        }

        "Проверяем, что кнопки \"В корзину\" и  \"Повторить заказ\" не отображаются".ybm_run { _ in
            XCTAssertFalse(ordersListPage.cartButton(itemId: Constants.stationItemId).isVisible)
            XCTAssertFalse(ordersListPage.repeatOrderButton(orderId: Constants.orderId).isVisible)
        }
    }

    func testCancelStationOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5615")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Отображение кнопки отмены заказа стации по подписке")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupOrdersState()
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Нажимаем на кнопку \"Подробнее\" и переходим на экран деталей заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: Constants.orderId)
            wait(forVisibilityOf: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверям отображение кнопки \"Отменить заказ\"".ybm_run { _ in
            let cancellationButton = orderDetailsPage.cancellationButton
            orderDetailsPage.element.ybm_swipeCollectionView(toFullyReveal: cancellationButton.element)
            XCTAssert(cancellationButton.element.isVisible)
        }
    }

    func testStationOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5604")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Подробности заказа")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupOrdersState()
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Нажимаем на кнопку \"Подробнее\" и переходим на экран деталей заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: Constants.orderId)
            wait(forVisibilityOf: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем отображение станции в составе заказа".ybm_run { _ in
            let stationTitleLabel = orderDetailsPage.orderItemTitleLabel(itemId: Constants.stationItemId)
            let stationPriceLabel = orderDetailsPage.orderItemPriceLabel(itemId: Constants.stationItemId)
            orderDetailsPage.element.ybm_swipe(toFullyReveal: stationPriceLabel)
            XCTAssertEqual(
                stationTitleLabel.label,
                "Яндекс.Станция, подписка на 3 года, черная"
            )
            XCTAssertEqual(
                stationPriceLabel.label,
                "599 ₽ / мес"
            )
        }

        "Проверяем блок итоговой суммы".ybm_run { _ in
            let itemsSummary = orderDetailsPage.itemsSummaryTitle
            let totalSummary = orderDetailsPage.totalSummaryTitle
            orderDetailsPage.element.ybm_swipe(toFullyReveal: totalSummary.element)
            XCTAssertEqual(
                itemsSummary.amount.label,
                "599 ₽ / мес"
            )
            XCTAssertEqual(
                totalSummary.amount.label,
                "599 ₽ / мес"
            )
        }

        "Проверяем отсутствие кнопок \"В корзину\", \"Повторить заказ\" и \"Документы по заказу\"".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.cartButton(itemId: Constants.stationItemId).isVisible)
            XCTAssertFalse(orderDetailsPage.repeatOrderButton(orderId: Constants.orderId).isVisible)
            XCTAssertFalse(orderDetailsPage.receiptsButton.element.isVisible)
        }
    }

    func testOrderWithStatusDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5616")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Отображение заказа со статусом DELIVERY")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupOrdersState(status: .delivery)
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Нажимаем на кнопку \"Подробнее\" и переходим на экран деталей заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: Constants.orderId)
            wait(forVisibilityOf: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем отображение кнопки \"Отменить заказ\"".ybm_run { _ in
            let cancellationButton = orderDetailsPage.cancellationButton
            orderDetailsPage.element.swipe(
                to: .down,
                until: cancellationButton.element.isVisible
            )
            XCTAssert(cancellationButton.element.isVisible)
        }

        "Проверяем, что нет кнопок \"Документы по заказу\" и \"в корзину\"".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.cartButton(itemId: Constants.stationItemId).isVisible)
            XCTAssertFalse(orderDetailsPage.receiptsButton.element.isVisible)
        }
    }

    func testOrderWithStatusDelivered() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5630")
        Allure.addEpic("StationSubscription")
        Allure.addFeature("Мои заказы")
        Allure.addTitle("Отображение заказа со статусом DELIVERED")

        var ordersListPage: OrdersListPage!
        var orderDetailsPage: OrderDetailsPage!

        "Настраиваем FT и мокаем startup для получения эксперимента station_subscription_exp".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            enable(toggles: FeatureNames.stationSubscription)
            setupExperiments([.stationSubscriptionExp])
        }

        "Мокаем состояние".ybm_run { _ in
            setupOrdersState(status: .delivered)
        }

        "Идем на мои заказы".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
        }

        "Нажимаем на кнопку \"Подробнее\" и переходим на экран деталей заказа".ybm_run { _ in
            let detailsButton = ordersListPage.detailsButton(orderId: Constants.orderId)
            wait(forVisibilityOf: detailsButton.element)
            orderDetailsPage = detailsButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем что нет кнопки \"Вернуть\"".ybm_run { _ in
            let refundButton = orderDetailsPage.refundButton
            XCTAssertFalse(
                orderDetailsPage.element.ybm_checkingSwipe(
                    to: .down,
                    until: refundButton.isVisible,
                    checkConditionPerCycle: { refundButton.isVisible }
                )
            )
        }

        "Проверяем, что нет кнопок \"Документы по заказу\" и \"в корзину\"".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.cartButton(itemId: Constants.stationItemId).isVisible)
            XCTAssertFalse(orderDetailsPage.receiptsButton.element.isVisible)
        }
    }
}

// MARK: - Helper Methods

private extension StationSubscriptionOrderTests {

    typealias Experiment = ResolveBlueStartup.Experiment

    func setupExperiments(_ experiments: [Experiment]) {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        defaultState.setExperiments(experiments: experiments)
        stateManager?.setState(newState: defaultState)
    }

    func setupOrdersState(status: Order.Mapper.Status = .processing) {
        let orderMapper = Order.Mapper(
            id: Constants.orderId,
            status: status,
            substatus: .started,
            payment: .prepaid,
            paymentSubmethod: .stationSubscription,
            delivery: .service,
            msku: [Constants.stationItemId],
            buyerMoneyTotal: 599
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(
            mapper: OrdersState.UserOrdersHandlerMapper(orders: [orderMapper], sku: .empty),
            for: [.all, .byId]
        )

        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension StationSubscriptionOrderTests {

    enum Constants {
        static let orderId = "123"
        static let stationItemId = "123"
    }
}
