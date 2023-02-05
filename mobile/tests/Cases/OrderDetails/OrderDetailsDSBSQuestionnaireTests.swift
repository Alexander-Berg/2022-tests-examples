import MarketUITestMocks
import XCTest

final class OrderDetailsDSBSQuestionnaireTests: DSBSQuestionnaireTestCase {

    // MARK: - Public

    var orderDetailsPage: OrderDetailsPage!
    let orderId = "4815230"

    func test_OpenOrderDetailsDSBSOrder_NotReceived_Continue() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3954")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, нажимаем продолжить")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_tapContinue()
    }

    func test_OpenOrderDetailsDSBSOrder_NotReceived_CallSupport() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3955")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новой даты нет, звоним в поддержку")

        setupAndAnswerStillNo()

        notDeliveredNoNewDateFlow_consultationChat()
    }

    func test_OpenOrderDetailsDSBSOrder_NotReceived_NewDateFoundOut() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3946")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ не получен, новая дата есть")

        setupAndAnswerStillNo()

        notDelivered_NewDateFoundOut()
    }

    func test_OpenOrderDetailsDSBSOrder_Received_RateViewAppears() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3946")
        Allure.addEpic("DSBS")
        Allure.addFeature("Виджет Заказ у меня")
        Allure.addTitle("Заказ получен")

        setupOrderDetailsWithDSBSOrderAndQuestionnaireConditions()

        "Переходим в детали заказа и проверяем кнопку 'У меня'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.alreadyGotItButton)
            XCTAssertEqual(orderDetailsPage.alreadyGotItButton.label, "У меня")
        }

        "Нажимаем кнопку 'У меня'".ybm_run { _ in
            orderDetailsPage.alreadyGotItButton.tap()
        }

        alreadyGotItScenario_feedbackPopupOpens()
    }

    // MARK: - Private

    private func setupAndAnswerStillNo() {
        setupOrderDetailsWithDSBSOrderAndQuestionnaireConditions()

        "Переходим в детали заказа и проверяем кнопку 'Еще нет'".ybm_run { _ in
            wait(forVisibilityOf: orderDetailsPage.stillNoButton)
            XCTAssertEqual(orderDetailsPage.stillNoButton.label, "Еще нет")
        }

        "Нажимаем кнопку 'Еще нет'".ybm_run { _ in
            orderDetailsPage.stillNoButton.tap()
        }
    }

    private func setupOrderDetailsWithDSBSOrderAndQuestionnaireConditions() {
        "Мокаем состояния".ybm_run { _ in
            setState()
        }

        "Переходим в детали заказа".ybm_run { _ in
            orderDetailsPage = goToOrderDetailsPage(orderId: orderId)
        }
    }

    typealias OrdersHandlerMapper = OrdersState.UserOrdersHandlerMapper
    typealias SimpleOrder = Order.Mapper

    private func setState() {
        let toDate = Date().addingTimeInterval(-(.day * 2))
        let endDate = getDateString(withDateFormat: "dd-MM-yyyy", date: toDate)

        let orderMapper = OrdersHandlerMapper(
            orders: [
                SimpleOrder(
                    id: orderId,
                    status: .delivery,
                    substatus: .service,
                    rgb: "WHITE",
                    delivery: .init(
                        deliveryPartnerType: .shop,
                        toDate: endDate,
                        type: .service
                    )
                )
            ]
        )

        var orderState = OrdersState()
        orderState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byId])
        orderState.setGradesById(orderGradesById: .notSubmittedFeedback(orderId: Int(orderId) ?? 0))

        stateManager?.setState(newState: orderState)
    }

}
