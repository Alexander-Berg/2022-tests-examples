import MarketUITestMocks
import XCTest

class OrderEditCallCourierTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func test_callCourierPopup_deliverNow() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5273")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Привезти сейчас для ЧС")
        Allure.addTitle("Привезти сейчас")

        let editPage = checkPopup()

        "Нажимаем на кнопку 'Привезти сейчас' и ждём открытия веб вью".run {
            editPage.saveButton.button.tap()
            let webView = WebViewPage.current
            wait(forVisibilityOf: webView.element)
        }
    }

    func test_callCourierPopup_deliverInHourInterval() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5283")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Привезти сейчас для ЧС")
        Allure.addTitle("Привезти в часовой слот")

        let editPage = checkPopup()

        "Нажимаем на кнопку 'Привезти с 10.00 до 18.00' и ждем закрытия попапа".run {
            editPage.cancelButton.button.tap()
            wait(forInvisibilityOf: editPage.element)
            let webView = WebViewPage.current
            wait(forInvisibilityOf: webView.element)
        }
    }

    func test_callCourier_morda() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5466")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Привезти сейчас для ЧС")
        Allure.addTitle("Виджет на морде")

        setupState()

        var morda: MordaPage!

        "Авторизуемся, открываем морду".run {
            morda = goToMorda()
        }

        "Проверяем наличие виджета".run {
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("resolveRecentUserOrders") } == true
            })

            ybm_wait(forFulfillmentOf: {
                morda.element.isVisible
                    && morda.singleActionContainerWidget.container.element.isVisible
            })
        }

        var snippet: HoveringSnippetPage!

        "Проверяем виджет".run {
            snippet = morda.singleActionContainerWidget.container.orderSnippet(after: [])
            morda.singleActionContainerWidget.container.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: snippet.element
            )
            XCTAssertEqual(snippet.titleLabel.label, "Уже в пути")
            XCTAssertEqual(
                snippet.subtitleLabel.label,
                "Доставим \(Constants.date.ybm_dateString(format: "d MMMM")) с \(Constants.fromTime) до \(Constants.toTime)"
            )
            XCTAssertEqual(snippet.actionButton.element.label, "Подробнее")
        }

        "Переходим в детали заказа".run {
            let orderPage: OrderDetailsPage = snippet.actionButton.tap()
            wait(forVisibilityOf: orderPage.element)
        }
    }

    func test_callCourier_ordersList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5467")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Привезти сейчас для ЧС")
        Allure.addTitle("Детали заказа")

        setupState()

        var orderPage: OrderDetailsPage!

        "Авторизуемся, открываем детали заказа".run {
            orderPage = goToOrderDetailsPage(orderId: Constants.orderId)
            wait(forVisibilityOf: orderPage.element)
        }

        "Проверяем наличие кнопки вызова курьера и жмем на нее".run {
            let button = orderPage.callCourierButton
            wait(forVisibilityOf: button)
            XCTAssertEqual(button.label, "Привезти сейчас")
            button.tap()
        }

        "Ждём открытия веб вью".run {
            let webView = WebViewPage.current
            wait(forVisibilityOf: webView.element)
        }
    }

    // MARK: - Private

    private func checkPopup() -> OrderEditPage {
        setupState()

        "Переходим на попап вызова курьера по диплинку".run {
            _ = appAfterOnboardingAndPopups()
            open(market: .callCourier(orderId: Constants.orderId))
        }

        var editPage: OrderEditPage!

        "Ждём попап".run {
            editPage = OrderEditPage.current
            wait(forVisibilityOf: editPage.element, timeout: 20)
        }

        "Проверяем текст на попапе".run {
            XCTAssertEqual(editPage.callCourierTitle.label, Constants.title)
            XCTAssertEqual(editPage.callCourierDisclaimer.label, Constants.disclaimer)
        }

        "Проверяем кнопки".run {
            XCTAssertEqual(editPage.saveButton.button.label, Constants.callCourierButton)
            XCTAssertEqual(editPage.cancelButton.button.label, Constants.deliverInHourIntervalButton)
        }

        "Проверяем карусель с картинками товаров".run {
            XCTAssertTrue(editPage.callCourierItems.isVisible)
        }

        return editPage
    }

    private func setupState() {
        "Включаем тоггл".run {
            enable(toggles: FeatureNames.callCourier)
        }

        "Мокаем состояние".run {
            setupOrderState()
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    typealias AvailiabilityHandlerMapper = ResolveOrderOptionAvailiability.Mapper
    typealias Availiability = AvailiabilityHandlerMapper.OrderAvailabilities

    private func setupOrderState() {
        var orderState = OrdersState()

        let order = SimpleOrder(
            id: Constants.orderId,
            status: .delivery,
            payment: .prepaid,
            delivery: .init(
                deliveryPartnerType: .yandex,
                fromDate: Constants.dateString,
                toDate: Constants.dateString,
                fromTime: Constants.fromTime,
                toTime: Constants.toTime,
                type: .service,
                trackCode: Constants.trackCode
            )
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId, .recent(withGrades: true)])

        let availabilityMapper: AvailiabilityHandlerMapper = .makeCallCourierForOrders(ids: [Constants.orderId])
        orderState.setAvailabilityOrderOption(mapper: availabilityMapper)

        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension OrderEditCallCourierTests {

    enum Constants {
        static let orderId = "123456"
        static let date = Date().ybm_date(daysOffset: 1)! // swiftlint:disable:this force_unwrapping

        static let fromTime = "10:00"
        static let toTime = "18:00"
        static let trackCode = "1234567890"

        static var dateString: String {
            date.ybm_dateString(format: "dd-MM-yyyy")
        }

        static let title = "Можем привезти заказ сейчас, в течение 15–30 минут"
        static let disclaimer = "Вдруг вам удобно получить его раньше."
        static let callCourierButton = "Привезти сейчас"
        static let deliverInHourIntervalButton = "Привезти с \(Constants.fromTime) до \(Constants.toTime)"
    }

}
