import MarketUITestMocks
import XCTest

final class StoragePeriodExtendTests: LocalMockTestCase {

    // MARK: - Properties

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper
    typealias Delivery = Order.Mapper.SimpleDelivery

    private let oneDayInSeconds: Double = 86_400
    private let orderId = "32803403"

    private lazy var limitDate = makeLimitDate()
    private lazy var limitDateString = makeLimitDateString()
    private lazy var limitDateDayMonthShortString = makeLimitDateDayMonthShortString()
    private lazy var limitDateDayMonthString = makeLimitDateDayMonthString()
    private lazy var limitDateDayString = makeLimitDateDayString()
    private lazy var limitDateFapiString = makeLimitDateFapiString()
    private lazy var extendedLimitDate = makeExtendedLimitDate()
    private lazy var extendedLimitDateString = makeExtendedLimitDateString()
    private lazy var extendedLimitDateDayMonthString = makeExtendedLimitDateDayMonthString()

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // MARK: - Tests

    func testExtensionCancelledFromMorda() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4795")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Отмена продления срока хранения")
        Allure.addTitle("Отмена продления срока хранения, переход с морды")

        var morda: MordaPage!
        var outletMapInfoPage: OutletMapInfoPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        "Проверяем виджет и переходим на карту".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.orderSnippet()

            XCTAssertEqual(snippet.titleLabel.label, "Забрать до \(limitDateDayMonthShortString)")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "На карте")

            outletMapInfoPage = snippet.additionalActionButton.tap()
        }

        "Отменяем продление срока хранения".ybm_run { _ in
            cancelExtension(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionCancelledFromOrderDetailsOnMap() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4796")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Отмена продления срока хранения")
        Allure.addTitle("Отмена продления срока хранения, переход с деталей заказа")

        var outletMapInfoPage: OutletMapInfoPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем срок хранения и нажимаем кнопку 'Показать на карте'".ybm_run { _ in
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
        }

        "Отменяем продление срока хранения".ybm_run { _ in
            cancelExtension(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionDoneFromMorda() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4798")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Продление срока хранения, переход с морды")

        var morda: MordaPage!
        var outletMapInfoPage: OutletMapInfoPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        "Проверяем виджет и переходим на карту".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.orderSnippet()

            XCTAssertEqual(snippet.titleLabel.label, "Забрать до \(limitDateDayMonthShortString)")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "На карте")

            outletMapInfoPage = snippet.additionalActionButton.tap()
        }

        "Продлеваем срок хранения".ybm_run { _ in
            makeExtension(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionFromOrderDetailsError() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4802")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Ошибка продления срока хранения, переход с деталей заказа. Ошибка 404")

        var orderDetailsPage: OrderDetailsPage!
        var outletMapInfoPage: OutletMapInfoPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: nil
            )
        }

        "Переходим в детали заказа и проверяем срок хранения".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем срок хранения и нажимаем кнопку 'Показать на карте'".ybm_run { _ in
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
            wait(forVisibilityOf: outletMapInfoPage.element)
        }

        "На экране 'Пункт самовывоза' нажимаем кнопку 'Продлить'".ybm_run { _ in
            outletMapInfoPage.extendButton.tap()
        }

        "На экране редактирования нажимаем кнопку 'Продлить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditPage.current.element)
            OrderEditPage.current.saveButton.element.tap()
        }

        "Получаем ошибку".ybm_run { _ in
            extensionError(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionFromOrderDetailsEmptyReplyError() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4803")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Ошибка продления срока хранения, переход с деталей заказа. Пустой ответ")

        var orderDetailsPage: OrderDetailsPage!
        var ordersListPage: OrdersListPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .emptyEditingOptions(orderId: orderId),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Переходим в \"Мои заказы\" и проверяем срок хранения".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
            XCTAssertEqual(
                "Хранится до \(limitDateDayMonthString)",
                ordersListPage.storagePeriod(orderId: orderId).label
            )
        }

        "Переходим в детали заказа и проверяем срок хранения".ybm_run { _ in
            ordersListPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: ordersListPage.detailsButton(orderId: orderId).element
            )
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)

            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
        }

        "На экране 'Детали заказа' нажимаем кнопку 'Продлить'".ybm_run { _ in
            orderDetailsPage.storagePeriod.extendButton.element.tap()
        }

        "Продлеваем срок хранения и получаем ошибку".ybm_run { _ in
            extensionError(orderDetailsPage: orderDetailsPage)
        }
    }

    func testExtensionFromMordaError() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4801")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Ошибка продления срока хранения, переход с морды. Ошибка 400")

        var morda: MordaPage!
        var outletMapInfoPage: OutletMapInfoPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: nil,
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        "Проверяем виджет и переходим на карту".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.orderSnippet()

            XCTAssertEqual(snippet.titleLabel.label, "Забрать до \(limitDateDayMonthShortString)")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "На карте")

            outletMapInfoPage = snippet.additionalActionButton.tap()
        }

        "На экране 'Пункт самовывоза' нажимаем кнопку 'Продлить'".ybm_run { _ in
            wait(forVisibilityOf: outletMapInfoPage.extendButton)
            outletMapInfoPage.extendButton.tap()
        }

        "Получаем ошибку".ybm_run { _ in
            extensionError(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionDoneFromOrderDetailsOnMap() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4799")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Продление срока хранения")
        Allure.addTitle("Продление срока хранения, переход с деталей заказа")

        var outletMapInfoPage: OutletMapInfoPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [.changeDeliveryDatesRequest(orderId: orderId, status: .applied)]
                )
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем срок хранения и нажимаем кнопку 'Показать на карте'".ybm_run { _ in
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
        }

        "Продлеваем срок хранения".ybm_run { _ in
            makeExtension(outletMapInfoPage: outletMapInfoPage)
        }
    }

    func testExtensionDoneFromOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4797")
        Allure.addEpic("Срок хранения заказа")
        Allure.addFeature("Отмена продления срока хранения")
        Allure.addTitle("Отмена продления срока хранения, переход с делатей заказа")

        var orderDetailsPage: OrderDetailsPage!
        var ordersListPage: OrdersListPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [],
                    orderEditRequest: [
                        .changeStoragePeriodRequest(
                            orderId: orderId,
                            status: .applied,
                            newDate: limitDateFapiString
                        )
                    ]
                )
            )
        }

        "Переходим в \"Мои заказы\" и проверяем срок хранения".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
            XCTAssertEqual(
                "Хранится до \(limitDateDayMonthString)",
                ordersListPage.storagePeriod(orderId: orderId).label
            )
        }

        "Переходим в детали заказа и проверяем срок хранения".ybm_run { _ in
            ordersListPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: ordersListPage.detailsButton(orderId: orderId).element
            )
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
        }

        "Отменяем продление срока хранения".ybm_run { _ in
            cancelExtension(orderDetailsPage: orderDetailsPage)
        }
    }

    func testExtensionCancelledFromOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4800")
        Allure.addEpic("Срок хранения заказа")
        Allure.addFeature("Продление")
        Allure.addTitle("Продление срока хранения, переход с деталей заказа")

        var orderDetailsPage: OrderDetailsPage!
        var ordersListPage: OrdersListPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [.init(fromTime: "10:00:00", toTime: "18:00:00")],
                    orderEditRequest: [
                        .changeStoragePeriodRequest(
                            orderId: orderId,
                            status: .applied,
                            newDate: limitDateFapiString
                        )
                    ]
                )
            )
        }

        "Переходим в \"Мои заказы\" и проверяем срок хранения".ybm_run { _ in
            ordersListPage = goToOrdersListPage()
            wait(forVisibilityOf: ordersListPage.element)
            XCTAssertEqual(
                "Хранится до \(limitDateDayMonthString)",
                ordersListPage.storagePeriod(orderId: orderId).label
            )
        }

        "Переходим в детали заказа и проверяем срок хранения".ybm_run { _ in
            ordersListPage.collectionView.ybm_swipeCollectionView(
                toFullyReveal: ordersListPage.detailsButton(orderId: orderId).element
            )
            orderDetailsPage = ordersListPage.detailsButton(orderId: orderId).tap()
            wait(forVisibilityOf: orderDetailsPage.element)
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
        }

        "Продлеваем срок хранения".ybm_run { _ in
            makeExtension(orderDetailsPage: orderDetailsPage)
        }
    }

    func testAlreadyExtendedOnOrderDetails() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4805")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Срок хранения уже продлён")
        Allure.addTitle("На экране 'Детали заказа' отсутствует возможность продления")

        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [],
                    orderEditRequest: [
                        .changeStoragePeriodRequest(
                            orderId: orderId,
                            status: .applied,
                            newDate: limitDateFapiString
                        )
                    ]
                ),
                with: [
                    .changeStoragePeriodRequest(
                        orderId: orderId,
                        status: .applied,
                        newDate: limitDateFapiString
                    )
                ]
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
        }

        "Проверяем остутствие кнопки 'Продлить'".ybm_run { _ in
            XCTAssertFalse(orderDetailsPage.storagePeriod.extendButton.element.isVisible)
        }
    }

    func testAlreadyExtendedOnMap() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4806")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Срок хранения уже продлён")
        Allure.addTitle("На карте отсутствует возможность продления. Переход с деталей заказа.")

        var outletMapInfoPage: OutletMapInfoPage!
        var orderDetailsPage: OrderDetailsPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [],
                    orderEditRequest: [
                        .changeStoragePeriodRequest(
                            orderId: orderId,
                            status: .applied,
                            newDate: limitDateFapiString
                        )
                    ]
                ),
                with: [
                    .changeStoragePeriodRequest(
                        orderId: orderId,
                        status: .applied,
                        newDate: limitDateFapiString
                    )
                ]
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }

        "Проверяем срок хранения и нажимаем кнопку 'Показать на карте'".ybm_run { _ in
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
            outletMapInfoPage = orderDetailsPage.showOnMapButton.tap()
            wait(forVisibilityOf: outletMapInfoPage.element)
        }

        "Проверяем остутствие кнопки 'Продлить'".ybm_run { _ in
            XCTAssertFalse(outletMapInfoPage.extendButton.isVisible)
        }
    }

    func testAlreadyExtendedOnMorda() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4807")
        Allure.addEpic("Срок хранения")
        Allure.addFeature("Срок хранения уже продлён")
        Allure.addTitle("На карте отсутствует возможность продления. Переход с морды.")

        var morda: MordaPage!
        var outletMapInfoPage: OutletMapInfoPage!

        "Мокаем состояния".ybm_run { _ in
            setupState(
                with: .defaultEditingOptions(orderId: orderId, storageLimitDatesOptions: [limitDateString]),
                with: .init(
                    deliveryTimeInterval: [],
                    orderEditRequest: [
                        .changeStoragePeriodRequest(
                            orderId: orderId,
                            status: .applied,
                            newDate: limitDateFapiString
                        )
                    ]
                ),
                with: [
                    .changeStoragePeriodRequest(
                        orderId: orderId,
                        status: .applied,
                        newDate: limitDateFapiString
                    )
                ]
            )
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        "Проверяем виджет и переходим на карту".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.orderSnippet()

            XCTAssertEqual(snippet.titleLabel.label, "Забрать до \(limitDateDayMonthShortString)")
            XCTAssertEqual(snippet.additionalActionButton.element.label, "На карте")

            outletMapInfoPage = snippet.additionalActionButton.tap()
            wait(forVisibilityOf: outletMapInfoPage.element)
        }

        "Проверяем остутствие кнопки 'Продлить'".ybm_run { _ in
            XCTAssertFalse(outletMapInfoPage.extendButton.isVisible)
        }
    }
}

// MARK: - Private

private extension StoragePeriodExtendTests {

    func setupState(
        with editingOptions: ResolveOrderEditingOptions.OrderEditingOptions?,
        with orderEditingRequest: SaveOrderEditingRequest.OrderEditingRequest?,
        with orderEditRequest: [ResolveRecentUserOrders.RecentUserOrders.OrderEditRequest] = []
    ) {
        var myOutlet: Order.SimpleOutlet = .rublevskoye
        myOutlet.setDataForExtensionStoragePeriodAvailability()

        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .pickup,
                    outlet: myOutlet,
                    delivery: Delivery(
                        outletStorageLimitDate: limitDateString,
                        outlet: myOutlet,
                        type: .service
                    )
                )
            ],
            orderEditRequest: orderEditRequest
        )

        var orderState = OrdersState()

        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .recent(withGrades: true), .byId])
        orderState.setOutlet(outlets: [myOutlet])

        if let editingOptions = editingOptions {
            orderState.setOrderEditingOptions(orderEditingOptions: editingOptions)
        }
        if let orderEditingRequest = orderEditingRequest {
            orderState.setOrderEditingRequest(orderEditingRequest: orderEditingRequest)
        }
        stateManager?.setState(newState: orderState)
    }

    func extensionError(orderDetailsPage: OrderDetailsPage) {
        "Появляется сообщение об ошибке и нажимаем кнопку 'Понятно'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditFinishedPage.current.element)
            XCTAssertEqual("Простите, срок хранения не продлён", OrderEditFinishedPage.current.titleCell.label)
            XCTAssertEqual("Попробуйте еще раз позже", OrderEditFinishedPage.current.subtitleCell.label)
            XCTAssertEqual("Понятно", OrderEditFinishedPage.current.nextButton.label)
            OrderEditFinishedPage.current.nextButton.tap()
        }

        "Возвращаемся на экран 'Детали заказа'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.element)

            XCTAssertEqual(
                "до \(limitDateDayMonthString)",
                orderDetailsPage.storagePeriod.value.label
            )
        }
    }

    func extensionError(outletMapInfoPage: OutletMapInfoPage) {
        "Появляется сообщение об ошибке и нажимаем кнопку 'Понятно'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditFinishedPage.current.element)
            XCTAssertEqual("Простите, срок хранения не продлён", OrderEditFinishedPage.current.titleCell.label)
            XCTAssertEqual("Попробуйте еще раз позже", OrderEditFinishedPage.current.subtitleCell.label)
            XCTAssertEqual("Понятно", OrderEditFinishedPage.current.nextButton.label)
            OrderEditFinishedPage.current.nextButton.tap()
        }

        "Возвращаемся на экран 'Пункт самовывоза'".ybm_run { _ in
            wait(forVisibilityOf: outletMapInfoPage.element)

            XCTAssertEqual(
                "Срок хранения до \(limitDateDayMonthString)",
                outletMapInfoPage.storagePeriod.label
            )
        }
    }

    func cancelExtension(outletMapInfoPage: OutletMapInfoPage) {
        "На экране 'Пункт самовывоза' нажимаем кнопку 'Продлить'".ybm_run { _ in
            outletMapInfoPage.extendButton.tap()
        }

        "Нажимаем кнопку 'Отменить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditPage.current.element)

            OrderEditPage.current.cancelButton.element.tap()
        }

        "Возвращаемся на экран 'Пункт самовывоза'".ybm_run { _ in
            wait(forVisibilityOf: outletMapInfoPage.element)

            XCTAssertEqual(
                "Срок хранения до \(limitDateDayMonthString)",
                outletMapInfoPage.storagePeriod.label
            )
        }
    }

    func cancelExtension(orderDetailsPage: OrderDetailsPage) {
        "На экране 'Детали заказа' нажимаем кнопку 'Продлить'".ybm_run { _ in
            orderDetailsPage.storagePeriod.extendButton.element.tap()
        }

        "На экране редактирования нажимаем кнопку 'Отменить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditPage.current.element)
            XCTAssertEqual(
                "Продлить срок хранения до \(limitDateDayMonthString)",
                OrderEditPage.current.storagePeriodTitle.label
            )
            OrderEditPage.current.cancelButton.element.tap()
        }

        "Возвращаемся на экран 'Детали заказа'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.element)

            XCTAssertEqual(
                "до \(limitDateDayMonthString)",
                orderDetailsPage.storagePeriod.value.label
            )
        }
    }

    func makeExtension(orderDetailsPage: OrderDetailsPage) {
        "На экране 'Детали заказа' нажимаем кнопку 'Продлить'".ybm_run { _ in
            orderDetailsPage.storagePeriod.extendButton.element.tap()
        }

        "На экране редактирования нажимаем кнопку 'Продлить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditPage.current.element)
            XCTAssertEqual(
                "Продлить срок хранения до \(limitDateDayMonthString)",
                OrderEditPage.current.storagePeriodTitle.label
            )
            OrderEditPage.current.saveButton.element.tap()
        }

        "Нажимаем кнопку 'Продолжить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditFinishedPage.current.nextButton)
            XCTAssertEqual(
                "Срок хранения продлен до \(extendedLimitDateDayMonthString)",
                OrderEditFinishedPage.current.titleCell.label
            )
            XCTAssertEqual("Спасибо", OrderEditFinishedPage.current.nextButton.label)
            OrderEditFinishedPage.current.nextButton.tap()
        }

        "Возвращаемся на экран 'Детали заказа'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.element)
            XCTAssertEqual("до \(limitDateDayMonthString)", orderDetailsPage.storagePeriod.value.label)
        }
    }

    func makeExtension(outletMapInfoPage: OutletMapInfoPage) {
        "На экране 'Пункт самовывоза' нажимаем кнопку 'Продлить'".ybm_run { _ in
            outletMapInfoPage.extendButton.tap()
        }

        "На экране редактирования нажимаем кнопку 'Продлить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditPage.current.element)
            OrderEditPage.current.saveButton.element.tap()
        }

        "Нажимаем кнопку 'Продолжить'".ybm_run { _ in
            wait(forVisibilityOf: OrderEditFinishedPage.current.nextButton)
            OrderEditFinishedPage.current.nextButton.tap()
        }

        "Возвращаемся на экран 'Пункт самовывоза'".ybm_run { _ in
            wait(forVisibilityOf: outletMapInfoPage.element)
        }
    }

    func makeLimitDate() -> Date {
        let timezoneOffset = TimeZone.current.secondsFromGMT()
        return Date(timeIntervalSinceNow: oneDayInSeconds + Double(timezoneOffset))
    }

    func makeLimitDateString() -> String {
        DateFormatter.gmtYearMonthDay.string(from: limitDate)
    }

    func makeLimitDateFapiString() -> String {
        DateFormatter.fapi.string(from: extendedLimitDate)
    }

    func makeLimitDateDayMonthShortString() -> String {
        DateFormatter.gmtDayMonthShort.string(from: limitDate)
    }

    func makeLimitDateDayMonthString() -> String {
        DateFormatter.gmtDayMonth.string(from: limitDate)
    }

    func makeLimitDateDayString() -> String {
        DateFormatter.day.string(from: limitDate)
    }

    func makeExtendedLimitDate() -> Date {
        Date(timeIntervalSinceNow: oneDayInSeconds * 2)
    }

    func makeExtendedLimitDateString() -> String {
        DateFormatter.gmtYearMonthDay.string(from: extendedLimitDate)
    }

    func makeExtendedLimitDateDayMonthString() -> String {
        DateFormatter.dayMonth.string(from: extendedLimitDate)
    }
}
