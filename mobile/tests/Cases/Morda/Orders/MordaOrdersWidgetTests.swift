import MarketUITestMocks
import XCTest

final class MordaOrdersWidgetTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testStoragePeriod() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3314")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет Мои заказы")
        Allure.addTitle("Сроки хранения (c&c)")

        var morda: MordaPage!

        let recentUserOrdersMapper = HandlerMapper(orders: [
            SimpleOrder(
                status: .pickup,
                delivery: Delivery(
                    outletStorageLimitDate: "2020-04-14",
                    type: .service
                )
            ),

            SimpleOrder(
                status: .pickup,
                delivery: Delivery(outletStorageLimitDate: "2021-03-11")
            ),

            SimpleOrder(
                status: .processing
            )
        ])

        "Мокаем состояние".ybm_run { _ in
            setupState(with: recentUserOrdersMapper)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("resolveRecentUserOrders") } == true
            })

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        var previousSnippets: [String] = []

        "Проверяем заказ в статусе DELIVERED".ybm_run { _ in
            let snippetDelivered = morda.singleActionContainerWidget.container.orderSnippet(after: previousSnippets)
            morda.singleActionContainerWidget.container.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: snippetDelivered.element
            )
            previousSnippets.append(snippetDelivered.element.identifier)
            XCTAssertEqual(snippetDelivered.titleLabel.label, "Забрать до 14.04")
        }

        "Проверяем заказ в статусе PICKUP".ybm_run { _ in
            let snippetPickup = morda.singleActionContainerWidget.container.orderSnippet(after: previousSnippets)
            morda.singleActionContainerWidget.container.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: snippetPickup.element
            )
            previousSnippets.append(snippetPickup.element.identifier)
            XCTAssertEqual(snippetPickup.titleLabel.label, "Забрать до 11.03")
        }

        "Проверяем заказ в статусе PROCESSING".ybm_run { _ in
            let snippetProcessing = morda.singleActionContainerWidget.container.orderSnippet(after: previousSnippets)
            morda.singleActionContainerWidget.container.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: snippetProcessing.element
            )
            previousSnippets.append(snippetProcessing.element.identifier)
            XCTAssertEqual(snippetProcessing.titleLabel.label, "Срок хранения 10 дней")
        }
    }

    func testOutletTimeInterval() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4861")
        Allure.addEpic("Морда")
        Allure.addFeature("Время доставки в ПВЗ")
        Allure.addTitle("Виджет заказа")

        var morda: MordaPage!

        let todayDate = getDate(withDateFormat: "dd-MM-yyyy", date: Date())
        let recentUserOrdersMapper = HandlerMapper(orders: [
            SimpleOrder(
                status: .processing,
                delivery: Delivery(
                    deliveryPartnerType: .yandex,
                    fromDate: todayDate,
                    toDate: todayDate,
                    fromTime: "12:00",
                    toTime: "15:00",
                    type: .outlet
                )
            )
        ])

        "Мокаем состояние".ybm_run { _ in
            setupState(with: recentUserOrdersMapper)
        }

        "Открываем морду и проверяем наличие виджета".ybm_run { _ in
            morda = goToMorda()
            wait(forVisibilityOf: morda.element)
            wait(forVisibilityOf: morda.singleActionContainerWidget.container.orderSnippet().element)
        }

        "Проверяем текст с датой и временем доставки".ybm_run { _ in
            let orderSnippet = morda.singleActionContainerWidget.container.orderSnippet()
            let todayDateString = DateFormatter.dayMonthWithNonBreakingSpace.string(from: Date())
            XCTAssertEqual(orderSnippet.subtitleLabel.label, "Доставим в пункт выдачи \(todayDateString) к 15:00")
        }
    }

    func testUnpaidAwaitPayment() {
        Allure.addTestPalmLink("")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет Мои заказы")
        Allure.addTitle("Заказ в статусе 'Ожидает подтверждения оплаты'")

        var morda: MordaPage!

        let recentUserOrdersMapper = HandlerMapper(orders: [
            SimpleOrder(
                status: .unpaid,
                substatus: .awaitPayment
            )
        ])

        "Мокаем состояние".ybm_run { _ in
            setupState(with: recentUserOrdersMapper)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("resolveRecentUserOrders") } == true
            })

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        "Проверяем заказ в статусе UNPAID".ybm_run { _ in
            let snippetUnpaid = morda.singleActionContainerWidget.container.orderSnippet(after: [])
            morda.singleActionContainerWidget.container.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: snippetUnpaid.element
            )
            XCTAssertEqual(snippetUnpaid.titleLabel.label, "Ожидает подтверждения оплаты")
        }
    }

    // MARK: - Helper Methods

    typealias HandlerMapper = ResolveRecentUserOrders.Mapper
    typealias SimpleOrder = Order.Mapper
    typealias Delivery = SimpleOrder.SimpleDelivery
    typealias Feedback = ResolveRecentUserOrders.OrderFeedback
    typealias Chat = ResolveRecentUserOrders.OrderChat
    typealias SimpleOutlet = ResolveOutlets.Mapper.SimpleOutlet

    private func setupState(with mapper: HandlerMapper) {
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: mapper, for: [.recent(withGrades: true)])
        stateManager?.setState(newState: orderState)
    }

    private func getDate(withDateFormat dateFormat: String, date: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.locale = Locale(identifier: "ru")
        return dateFormatter.string(from: date)
    }
}
