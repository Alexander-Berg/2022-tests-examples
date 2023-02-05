import MarketUITestMocks
import XCTest

class OrderEditHourIntervalsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testWaitingForDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5175")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Перенос доставки в ЧС")
        Allure.addTitle("Жду курьера")

        checkPopup(with: .editDeliveryAvailable)

        "Нажимаем на кнопку 'Жду курьера' и ждём скрытия попапа".ybm_run { _ in
            let editPage = OrderEditPage.current
            editPage.saveButton.button.tap()
            wait(forInvisibilityOf: editPage.element)
        }
    }

    func testEditDeliveryDate() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5181")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Перенос доставки в ЧС")
        Allure.addTitle("Перенос доставки")

        checkPopup(with: .editDeliveryAvailable)

        "Нажимаем на кнопку 'Хочу перенести доставку'".ybm_run { _ in
            let editPage = OrderEditPage.current
            editPage.cancelButton.button.tap()
        }

        "Ждём попап с изменением даты доставки".ybm_run { _ in
            let finishedPage = OrderEditPage.current
            wait(forVisibilityOf: finishedPage.element)
            XCTAssertEqual(finishedPage.deliveryDateTitle.label, "Изменение даты доставки")
        }
    }

    func testEditDeliveryDateUnavailable() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5182")
        Allure.addEpic("Часовые слоты")
        Allure.addFeature("Перенос доставки в ЧС")
        Allure.addTitle("Перенос доставки невозможен")

        checkPopup(with: .editDeliveryUnavailable)

        "Нажимаем на кнопку 'Посмотреть заказ' и ждём скрытия попапа".ybm_run { _ in
            let finishedPage = OrderEditFinishedPage.current
            finishedPage.nextButton.tap()
            wait(forInvisibilityOf: finishedPage.element)
        }
    }

    // MARK: - Private

    private func checkPopup(with config: Config) {
        "Мокаем состояние".ybm_run { _ in
            setupState(config)
        }

        "Переходим в шторку изменения часовых интервалов по диплинку".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .editHourIntervals(orderId: Constants.orderId))
        }

        switch config {
        case .editDeliveryAvailable:
            checkAvailablePopup(config)
        case .editDeliveryUnavailable:
            checkUnavailablePopup(config)
        default:
            break
        }
    }

    private func checkAvailablePopup(_ config: Config) {
        var editPage: OrderEditPage!

        "Ждём попап".ybm_run { _ in
            editPage = OrderEditPage.current
            wait(forVisibilityOf: editPage.element)
        }

        "Проверяем текст на попапе".ybm_run { _ in
            XCTAssertEqual(editPage.hourIntervalsTitle.label, config.title)
            XCTAssertEqual(editPage.hourIntervalsDisclaimer.label, config.disclaimer)
        }

        "Проверяем кнопки".ybm_run { _ in
            XCTAssertEqual(editPage.saveButton.button.label, config.confirmButton)
            XCTAssertEqual(editPage.cancelButton.button.label, config.rejectButton)
        }

        "Проверяем карусель с картинками товаров".ybm_run { _ in
            XCTAssertTrue(editPage.hourIntervalsItems.isVisible)
        }
    }

    private func checkUnavailablePopup(_ config: Config) {
        var finishedPage: OrderEditFinishedPage!

        "Ждём попап".ybm_run { _ in
            finishedPage = OrderEditFinishedPage.current
            wait(forVisibilityOf: finishedPage.element)
        }

        "Проверяем текст на попапе".ybm_run { _ in
            XCTAssertEqual(finishedPage.titleCell.label, config.title)
            XCTAssertEqual(finishedPage.subtitleCell.label, config.disclaimer)
        }

        "Проверяем кнопку".ybm_run { _ in
            XCTAssertEqual(finishedPage.nextButton.label, config.confirmButton)
        }
    }

    // MARK: - Helper Methods

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setupState(_ config: Config) {
        var orderState = OrdersState()

        let order = SimpleOrder(
            id: Constants.orderId,
            status: .delivery,
            payment: .prepaid,
            delivery: .init(
                deliveryPartnerType: .yandex,
                fromDate: Constants.date,
                toDate: Constants.date,
                fromTime: Constants.fromTime,
                toTime: Constants.toTime,
                type: .service
            )
        )
        let orderMapper = OrdersHandlerMapper(orders: [order])
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.byId])

        orderState.setOrderEditingOptions(
            orderEditingOptions: config.editingAvailable
                ? .defaultEditingOptions(orderId: Constants.orderId)
                : .emptyEditingOptions(orderId: Constants.orderId)
        )

        orderState.setEditVariants(orderEditVariants: .boxberry(
            orderId: Int(Constants.orderId) ?? 0,
            possibilities: [.deliveryDates(enabled: config.editingAvailable)]
        ))

        stateManager?.setState(newState: orderState)
    }
}

// MARK: - Nested Types

private extension OrderEditHourIntervalsTests {

    enum Constants {
        static let orderId = "123456"
        static let date = "05-08-2020"
        static let fromTime = "10:00"
        static let toTime = "18:00"
    }

    struct Config: Equatable {
        let editingAvailable: Bool
        let title: String
        let disclaimer: String
        let confirmButton: String
        let rejectButton: String

        static var editDeliveryAvailable: Self {
            .init(
                editingAvailable: true,
                title: "Курьер привезёт заказ \(Constants.orderId) с 10:00 до 18:00",
                disclaimer: "Если ваши планы поменялись, доставку ещё можно перенести",
                confirmButton: "Жду курьера",
                rejectButton: "Хочу перенести доставку"
            )
        }

        static var editDeliveryUnavailable: Self {
            .init(
                editingAvailable: false,
                title: "Перенести доставку уже не получится",
                disclaimer: "Похоже, вы уже получили заказ или курьер в пути и скоро будет у вас",
                confirmButton: "Посмотреть заказ",
                rejectButton: ""
            )
        }
    }
}
