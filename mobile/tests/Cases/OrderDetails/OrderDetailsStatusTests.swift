import MarketUITestMocks
import XCTest

final class OrderDetailsStatusTest: LocalMockTestCase {

    // MARK: - Public

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOutletTimeIntervalSubstatus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4863")
        Allure.addEpic("Детали заказа")
        Allure.addFeature("Время доставки в ПВЗ")
        Allure.addTitle("Подстатус заказа")

        var rootPage: RootPage!
        var mordaPage: MordaPage!
        var orderDetailsPage: OrderDetailsPage!
        let orderId = "4815230"

        let todayDate = getDate(withDateFormat: "dd-MM-yyyy", date: Date())
        let orderMapper = HandlerMapper(orders: [
            SimpleOrder(
                id: orderId,
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

        "Мокаем состояния".ybm_run { _ in
            setupState(with: orderMapper)
            rootPage = appAfterOnboardingAndPopups()
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем подстатус заказа и блок данные".ybm_run { _ in
            XCTAssertEqual(
                orderDetailsPage.substatus.label,
                "Доставим в пункт выдачи \(getDateString(for: .orderDetails)) к 15:00"
            )
            XCTAssertEqual(
                orderDetailsPage.deliveryDate.value.label,
                "\(getDateString(for: .orderDetails, withWeekday: true)), к 15:00"
            )
        }

        "Переходим на морду и проверяем наличие виджета".ybm_run { _ in
            mordaPage = goToMorda(root: rootPage)
            wait(forVisibilityOf: mordaPage.element)
            wait(forVisibilityOf: mordaPage.singleActionContainerWidget.container.orderSnippet().element)
        }

        "Проверяем текст с датой и временем доставки в виджете".ybm_run { _ in
            let orderSnippet = mordaPage.singleActionContainerWidget.container.orderSnippet()
            XCTAssertEqual(
                orderSnippet.subtitleLabel.label,
                "Доставим в пункт выдачи \(getDateString(for: .morda)) к 15:00"
            )
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = mordaPage.singleActionContainerWidget.container.orderSnippet().actionButton.tap()
            wait(forVisibilityOf: orderDetailsPage.element)
        }

        "Проверяем подстатус заказа и блок данные".ybm_run { _ in
            XCTAssertEqual(
                orderDetailsPage.substatus.label,
                "Доставим в пункт выдачи \(getDateString(for: .orderDetails)) к 15:00"
            )
            XCTAssertEqual(
                orderDetailsPage.deliveryDate.value.label,
                "\(getDateString(for: .orderDetails, withWeekday: true)), к 15:00"
            )
        }
    }

    // MARK: - Helper Methods and Nested Types

    typealias HandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias Delivery = Order.Mapper.SimpleDelivery
    typealias SimpleOrder = Order.Mapper

    private func setupState(with mapper: HandlerMapper) {
        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: mapper, for: [.all, .byId, .recent(withGrades: true)])
        stateManager?.setState(newState: orderState)
    }

    private func getDate(withDateFormat dateFormat: String, date: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.locale = Locale(identifier: "ru")
        return dateFormatter.string(from: date)
    }

    private func getDateString(for page: Page, withWeekday: Bool = false) -> String {
        switch page {
        case .morda:
            return DateFormatter.dayMonthWithNonBreakingSpace.string(from: Date())
        case .orderDetails:
            return withWeekday
                ? DateFormatter.dayMonthFullWeekDay.string(from: Date())
                : DateFormatter.dayMonth.string(from: Date())
        }
    }

    private enum Page {
        case morda
        case orderDetails
    }
}
